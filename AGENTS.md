# AGENTS.md — Gas Leak Detector

## Project Overview

Android app for real-time gas leak monitoring. An MQ-6 sensor on an ESP8266 pushes readings to a serverless Vercel API → Supabase DB → streamed to the Android app via Supabase Realtime WebSocket.

Three repos form the full pipeline:
- **Firmware**: [gasleakdetector-esp](https://github.com/gasleakdetector/gasleakdetector-esp) (C++/Arduino/ESP8266)
- **Backend**: [gasleakdetector-server](https://github.com/gasleakdetector/gasleakdetector-server) (Node.js/Vercel/Supabase)
- **Mobile**: This repo (Android/Java)

## Essential Commands

| Command | Description |
|---|---|
| `./gradlew assembleDebug` | Build debug APK |
| `./gradlew assembleRelease` | Build signed release APK (requires env vars: `KEYSTORE_PATH`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`) |
| `./gradlew testDebugUnitTest` | Run unit tests |
| `./gradlew spotlessApply` | Auto-format Java code (Google Java Format) |
| `./gradlew :benchmark:assembleRelease` | Build benchmark APK |
| `./gradlew :benchmark:connectedReleaseAndroidTest` | Run benchmarks on connected device/emulator |

## Project Structure

```
app/src/main/java/com/gasleakdetector/
├── app/
│   └── GasLeakApplication.java          # Application class, locale init, crash handler
├── data/
│   ├── api/                              # REST API clients (raw HttpURLConnection, no Retrofit)
│   │   ├── HistoricalApiService.java     # Cursor-based paginated historical data fetch
│   │   ├── LogsApiService.java           # Log fetch
│   │   └── StatsApiService.java          # Hourly stats fetch
│   ├── local/
│   │   ├── LocalDataStorage.java         # JSON file cache for gas data points
│   │   └── StatsLocalStorage.java        # JSON file cache for hourly stats
│   ├── model/
│   │   ├── GasStatus.java                # Level constants + calculateLevel()
│   │   ├── HistoricalDataPoint.java      # Single measurement (raw or aggregate bucket)
│   │   ├── HourlyStatPoint.java          # Aggregate hourly stat
│   │   └── RealtimeConfig.java           # API URL + key + device ID
│   ├── prefs/
│   │   └── SharedPrefs.java              # All SharedPreferences in one place
│   └── websocket/
│       └── WebSocketManager.java         # Supabase Realtime WS client
├── notification/
│   ├── GasNotificationHelper.java        # Builds/post gas alert notifications
│   └── NotificationChannelManager.java   # Creates O notification channels
├── service/
│   └── AppForegroundService.java         # Foreground service to keep WS alive
├── ui/
│   ├── dialog/
│   │   └── ConfigDialog.java             # API URL/key/device ID editor dialog
│   ├── main/
│   │   ├── MainActivity.java             # Shell activity: toolbar, drawer, WS lifecycle
│   │   ├── HomeFragment.java             # Live gauge + chart + node info
│   │   ├── StatisticsFragment.java       # Hourly stats chart + table
│   │   ├── SettingActivity.java          # All settings
│   │   ├── WelcomeActivity.java          # First-launch onboarding
│   │   └── InfoActivity.java             # About screen
│   └── widget/
│       ├── ChartView.java                # Custom scrollable/zoomable line chart
│       ├── CircularGaugeView.java        # Custom arc gauge (green→amber→red)
│       └── StatsChartView.java           # Stats bar chart
└── util/
    ├── CrashHandler.java                 # Uncaught exception handler → report file → Activity
    ├── CrashReportActivity.java          # Display crash report, copy, open GitHub issue
    ├── LocaleHelper.java                 # Apply locale to context
    └── ThemeUtil.java                    # Apply light/dark theme
```

## Architecture & Data Flow

### App startup flow
1. `WelcomeActivity` (launcher) → checks `intro_shown` pref → either shows onboarding or skips to `MainActivity`
2. `MainActivity` creates `WebSocketManager`, loads `HomeFragment`, optionally auto-starts monitoring if `auto_stream_enabled` is set
3. `AppForegroundService` starts immediately to keep the WebSocket alive

### Data flow (real-time)
```
ESP8266 → Vercel API → Supabase DB → Supabase Realtime CDC → WebSocket → Android app
```

`WebSocketManager` connects to the backend config endpoint (`/api/realtime-config`) to get the Supabase WS URL + anon key, then opens a WebSocket subscribing to `gas_logs_raw` table INSERT events. Heartbeat every 10s, auto-reconnect after 2s on drop.

### Data flow (historical)
App fetches from `GET /api/historical?range=1d&cursor=...` with cursor-based pagination. Results cached to local JSON file (`gas_nodes_cache.json`). Cache-first pattern: show cached data immediately, replace with network data when available.

### Theme system
- `ThemeUtil.applyTheme()` must be called **before** `super.onCreate()` in every Activity
- Theme 1 = dark (`AppTheme.Dark`), anything else = light (`AppTheme`)
- Theme setting stored in SharedPrefs

### Locale system
- `LocaleHelper.applyLocale()` called in `attachBaseContext()` of every Activity AND in `GasLeakApplication.attachBaseContext()`
- Supported: `en, vi, zh, ja, ko, fr, es, de`

## Key Patterns & Gotchas

### Non-obvious patterns
- **No ViewBinding/DataBinding** — all views resolved via `findViewById()`
- **No Retrofit** — all HTTP calls use raw `HttpURLConnection` with manual JSON parsing
- **No dependency injection** — manual instantiation everywhere (`new SharedPrefs(this)`, etc.)
- **No Jetpack Navigation** — manual fragment transactions with `getSupportFragmentManager().beginTransaction().replace()`
- **No Kotlin** — 100% Java
- **No ViewModel** — state managed directly in Activities/Fragments with fields
- **No Room** — local persistence is manual JSON file I/O
- **WeakReference callback pattern** — `WebSocketManager` holds a `WeakReference<Callback>` to avoid leaking Activities
- **`isSafe()` guard** — `HomeFragment` nulls out view references in `onDestroyView()` and checks `isSafe()` before touching views in async callbacks
- **`everConnected` flag** — `WebSocketManager` only fires `onConnected()` once per connection session to avoid duplicate toasts on reconnect
- **`cachedWsUrl`** — cached after first fetch so reconnects skip the config endpoint; cleared on explicit `connect()` call
- **`AtomicInteger ref`** — incrementing message ref so Supabase doesn't discard duplicate messages
- **`GasStatus` dual constructor** — `calculateLevel(int ppm)` uses default thresholds, `calculateLevel(int ppm, int warning, int danger)` uses user-configured thresholds from SharedPrefs
- **`HistoricalDataPoint` dual shape** — same class handles both raw rows (`gas_ppm`) and aggregate buckets (`avg_gas`/`min_gas`/`max_gas`). `getGasPpm()` falls back to `avgGas` if `gasPpm` is unset (-1). `getTimestamp()` uses `bucket` over `createdAt` if both are set.
- **Timestamp parsing** — `HistoricalDataPoint.getTimestamp()` strips colon from ISO 8601 offset (SimpleDateFormat limitation), handles `Z` suffix, truncates microsecond fractional seconds to milliseconds
- **Crash reporting** — `CrashHandler` writes to file (avoids `TransactionTooLargeException`), launches a separate Activity (avoids dead Looper), does NOT delegate to the default handler (to prevent system restart loop)

### Alert cooldown
- `lastNotifiedLevel` + `lastAlertTimestamp` in `HomeFragment` track the last alert sent
- `alert_delay_minutes` from SharedPrefs controls the cooldown between repeated alerts of the same level
- `alert_min_level` controls whether to notify from Warning or Danger only

### ProGuard
- `minifyEnabled true` on release builds
- Uses `proguard-android-optimize.txt` + `proguard-rules.pro`

### Signing
- Release signing reads from env vars: `KEYSTORE_PATH`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`
- Falls back to `keystore.jks` (file in project root) if env not set
- V1, V2, V3 signing all enabled

## Testing

- **Unit tests**: JUnit 4, in `app/src/test/java/`
- **Instrumented tests**: `app/src/androidTest/java/`
- **Benchmarks**: `benchmark/` module, requires emulator/device
- Only 3 test files exist: `GasStatusTest.java`, `RealtimeConfigTest.java`, `HistoricalDataPointTest.java` — all pure model unit tests with no Android dependencies

## CI/CD

- **Build workflow**: `assembleDebug` + `assembleRelease` (with keystore decode from secrets)
- **Test workflow**: `testDebugUnitTest` via Gradle 8.6
- **Benchmark workflow**: Runs on Android emulator (API 33, x86_64, Nexus 6 profile)
- All workflows use JDK 17 (Temurin)

## Branch Naming

| Type | Pattern | Example |
|---|---|---|
| Feature | `feature/<short-description>` | `feature/fcm-push-notification` |
| Bug fix | `fix/<short-description>` | `fix/fcm-crash-killed-process` |
| Chore | `chore/<short-description>` | `chore/upgrade-firebase-bom` |

## Commit Style

[Conventional Commits](https://www.conventionalcommits.org/): `type(scope): short imperative summary`

Allowed types: `feat`, `fix`, `refactor`, `chore`, `docs`, `test`, `style`, `perf`.

## Pre-commit Hook

Runs `spotlessApply` and re-stages formatted Java/Gradle files. Located in `.githooks/pre-commit`.

## Gradle Configuration

- AGP 8.4.0, Kotlin plugin 2.3.21 (for benchmark module only)
- Spotless plugin 8.8.0 (Google Java Format)
- Benchmark plugin 1.4.1
- Java 11 source/target compatibility
- `compileSdk` / `targetSdk` 34, `minSdk` 21
- `appcompat:1.7.1` (with annotation-experimental excluded), `material:1.9.0`, `constraintlayout:2.2.1`
- `Java-WebSocket:1.6.0` (slf4j-api excluded) + `slf4j-android:1.7.36`
- Kotlin stdlib forced to 1.8.22 across all configurations
- AndroidX + Jetifier enabled

## Color Scheme

| Role | Normal | Warning | Danger |
|---|---|---|---|
| Color | `#4CAF50` (green) | `#FFC107` (amber) | `#F44336` (red) |

Gauge color blends between these based on ppm value relative to thresholds.

## Strings & Localization

8 languages: English (default), Vietnamese, Chinese, Japanese, Korean, French, Spanish, German. All strings in `res/values-*/strings.xml`.