# Changelog

All notable changes to the [Gas Leak Detector](https://github.com/gasleakdetector/gasleakdetector) Android app will be
documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project adheres to
[Semantic Versioning](https://semver.org/spec/v2.0.0.html).


## [Unreleased]

### Features

- #68: Replace programmatic crash report UI with XML layout and `AppCompatActivity` for theme consistency and
  back-stack integration.
- #62: Add timeout to wake lock acquire in `MainActivity` to prevent indefinite hold.
- Stats caching: add `StatsLocalStorage` and cache-first loading in `StatisticsFragment` for reduced network
  round-trips and offline access.
- Alert settings: add configurable alert delay and minimum alert level preferences, with dedicated UI in Settings.
- Crash reporting: add `CrashReportActivity` to display crash reports in a dedicated dialog with copy and GitHub
  issue creation options.

### Fixed

- #64: Mark `cachedDataPoints` as `volatile` to fix visibility across threads.
- #67: Make `saveNodes` private and expose `replaceAll` with write-lock in data cache.
- #63: Catch `NumberFormatException` in threshold and delay input dialogs to prevent crash on invalid input.
- #62: Prevent `HomeFragment` crash on detach by adding `isSafe` guard to all async callbacks.
- #61: Guard `requireContext()` in `animateStatusText()` to avoid crash on detached fragment.
- #60: Truncate microseconds before `SimpleDateFormat` parsing to fix timestamp parsing on certain locales.
- #59: Add missing `R` and `ContextCompat` imports in widget classes, fix stale context reference in `onDraw`.
- #58: Prevent crash loop when `CrashReportActivity` itself crashes — properly hard-kill process instead of
  delegating to the default handler.
- #57: Add `slf4j-android` dependency to satisfy `Java-WebSocket` runtime requirement.
- #56: Fix Gradle wrapper verification by aligning checksum.

### Changed

- #69: Consolidate three WebSocket message handlers (gas reading, status, device) into a single unified handler
  with type routing.
- Move crash report storage from inline dialog to cache file, enabling robust crash report persistence across
  restarts.
- Extract vibration pattern to a named constant in notification service.
- Replace hardcoded paint colors in `ChartView`, `CircularGaugeView`, `StatsChartView`, and menu tab indicators
  with color resources (`chart_*`, `gauge_*`, `stats_*` dimen and color tokens).
- Clean up 4 unused public methods (`addDataPoint`, `setValueImmediate`, `showStatusUpdate`,
  `buildHeaderRow`/`buildDivider`).

### Added

- Benchmark module with high-volume stress tests for data parsing and chart rendering.
- Add 21 missing translations across all 8 supported languages (en, vi, ja, de, zh, es, fr, ko).

### Removed

- Remove stale `isMenuOpen` field and duplicate `PackageManager` import in `MainActivity`.
- Remove redundant section headers and awkwardly phrased comments across multiple files.


## [1.0.3] - 2026-05-21

Configurable gas thresholds, dynamic version display, and CI stability improvements.

### Features

- #45: Configurable gas threshold settings — users can now set custom Warning and Danger ppm levels directly from
  the Settings screen, with input validation ensuring warning is always below danger. Values persist across sessions
  and reset correctly with "Reset to Defaults".

### Fixed

- About screen always showing "Version 1.0" regardless of the actual app version — now reads dynamically from build
  config.
- #39: Real-time status card showing empty or incorrect device ID when receiving WebSocket data.
- French `strings.xml` build failure caused by unescaped apostrophes — affected all builds targeting `values-fr`.
- CI build failing due to missing Gradle wrapper jar and incompatible Gradle version.

### Added

- Multilingual support for gas threshold settings across all 8 languages (en, vi, ja, ko, zh, de, es, fr).

### Changed

- Add Dependabot for weekly Gradle dependency updates.


## [1.0.2] - 2026-04-23

Bug fixes and performance improvements.

### Fixed

- #36: Application package renamed from `com.gasleak` to `com.gasleakdetector`.
- #29: `getGasPpm()` treating valid `0 ppm` (clean air) as missing data.
- #30: Statistics table rendering without header row.
- #33: Chart empty message hardcoded in English, ignoring app locale.
- #27: Chart timestamp using device time instead of server timestamp.
- #26: `FileOutputStream` / `FileInputStream` not closed on exception in `LocalDataStorage`.
- #25: `ChartView.onDraw()` scanning full data list up to 13 times per frame.

### Added

- Proper stream resource cleanup in `LocalDataStorage` via try-with-resources.


## [1.0.1] - 2026-03-31

Minor source code updates and project documentation.

### Added

- Tutorial section for the project.
- #17: Project contribution guidelines.
- #15: Issue templates for bug reports and feature requests.
- #14: Code of conduct.
- #13: Security policy.
- #11: Pull request template.
- #10: Coding style guide.
- #9: Environment setup documentation.
- #8: API reference documentation.

### Changed

- README overhaul with comprehensive setup instructions.


## [1.0.0] - 2026-03-21

First stable release of the Gas Leak Detector Android app.

### Added

#### Real-time monitoring
- Live PPM gauge with smooth value animation driven by Supabase Realtime WebSocket.
- Status classification across three levels: Normal, Warning, Danger.
- Color feedback updates instantly on threshold breach.
- Persistent foreground notification while gas levels remain in Danger state.

#### Historical data
- Hourly aggregated chart powered by a three-tier Supabase pipeline (raw → minute → hour).
- Cursor-based pagination fetching up to 1,000 rows per request with server-side gzip compression.
- Supports time ranges: 1h, 6h, 1d, 7d, 30d.

#### Multi-node support
- Switch between ESP devices by device ID.
- Per-node statistics and independent configuration.

#### First-run experience
- Welcome intro screen shown once on first install, skipped on all subsequent launches.
- Guided setup flow directing to API configuration.

#### Feedback
- One-tap feedback from the overflow menu.
- Email pre-filled with app version in the subject line.

#### Offline resilience
- App displays cached data when the API is unreachable.
- ESP firmware queues up to 60 readings locally and drains them on reconnect.

#### Internationalization
- 8 languages: English, Vietnamese, German, Spanish, French, Japanese, Korean, Chinese.
