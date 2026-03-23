# Setup Guide

Complete setup guide for the Gas Leak Detector project. This document covers all four components — server backend, Supabase, ESP8266 firmware, and Android app — in the order they should be configured.

<div align="center">
	<p>English&nbsp;&nbsp;|&nbsp;&nbsp;
	    <a target="_blank" href="README-vi.md">Vietnamese</a>&nbsp;&nbsp&nbsp;&nbsp;
</div>

## Requirements

### Hardware

| Component | Notes |
|-----------|-------|
| ESP8266 | NodeMCU or equivalent |
| MQ-6 Gas Sensor | LPG / propane detection |
| OLED SSD1306 0.96" | Optional — for on-device display |
| Buzzer | Active or passive |

### Accounts

- [Vercel](https://vercel.com) — serverless deployment
- [Supabase](https://supabase.com) — database and realtime
- [Resend](https://resend.com) — email alerts (optional)

---

## 1. Server Backend

### Deploy to Vercel

**Step 1.** Go to the [gasleakdetector-server](https://github.com/gasleakdetector/gasleakdetector-server) repository and click the **Deploy** button.

![Deploy to Vercel](../images/vercel_deploy_button.jpg)

**Step 2.** Open your project in the Vercel dashboard and go to **Settings**.

![Vercel project settings](../images/vercel_project_settings.jpg)

**Step 3.** Navigate to **Environment Variables**.

![Environment Variables tab](../images/vercel_environment_variables.jpg)

**Step 4.** Click **Add Environment Variable**.

![Add Environment Variable](../images/vercel_add_variable.jpg)

**Step 5.** Add each of the following variables:

![Fill in variable values](../images/vercel_fill_variables.jpg)

| Variable | Description |
|----------|-------------|
| `SUPABASE_URL` | Your Supabase project URL |
| `SUPABASE_ANON_KEY` | Supabase anonymous key — used by the Android app WebSocket |
| `SUPABASE_SERVICE_KEY` | Supabase service role key — used for all server-side writes |
| `VALID_API_KEY` | Shared secret sent in `x-api-key` by the ESP and app |
| `RESEND_API_KEY` | Resend API key for email alerts |
| `ALERT_EMAIL` | Recipient address for danger-level alerts |
| `DANGER_THRESHOLD` | PPM level that triggers `danger` status. Recommended: `800` for MQ-6 |
| `WARNING_THRESHOLD` | PPM level that triggers `warning` status. Recommended: `300` for MQ-6 |
| `EMAIL_COOLDOWN_MINUTES` | Minimum minutes between repeated alert emails. Default: `2` |

### API Key

`VALID_API_KEY` is a secret you define yourself — it is shared between the server, ESP firmware, and Android app. There is no registration or third-party service involved.

Recommended: 8–10 alphanumeric characters. Example: `Abc12345`.

### Resend (Email Alerts)

This section is optional. Skip it if you do not need email alerts.

**Step 1.** Log in or create an account at [resend.com](https://resend.com/login).

**Step 2.** Go to **API Keys** and click **Create API Key**.

![Resend API Keys page](../images/resend_api_keys_page.jpg)

**Step 3.** Fill in the key name and click **Add**.

![Create API Key form](../images/resend_create_api_key.jpg)

**Step 4.** Copy the generated key.

![Copy key](../images/resend_copy_key.jpg)

Assign it to `RESEND_API_KEY` in Vercel. Set `ALERT_EMAIL` to the address that should receive alerts (e.g. `you@gmail.com`).

---

## 2. Supabase

**Step 1.** Create a new project in Supabase.

**Step 2.** Open the SQL Editor, paste the contents of [`supabase/schema.sql`](https://github.com/gasleakdetector/gasleakdetector-server/blob/main/supabase/schema.sql) and run it once.

![Supabase SQL Editor](../images/supabase_sql_editor.jpg)

This creates all tables (`gas_logs_raw`, `gas_logs_minute`, `gas_logs_hour`, `devices`), aggregation functions, `pg_cron` jobs, and row-level security policies in a single run.

**Step 3.** Retrieve your API credentials. Go to **Settings > API Keys > Legacy anon, service_role API keys** and copy both `anon` and `service_role` keys.

![Supabase API Keys](../images/supabase_api_keys.jpg)

Assign them to `SUPABASE_ANON_KEY` and `SUPABASE_SERVICE_KEY` in Vercel. Your `SUPABASE_URL` is available in **Project Overview**.

---

## 3. ESP8266 Firmware

### Wiring

Connect all components to the ESP8266 according to the tables below before powering on.

**MQ-6 Gas Sensor**

| MQ-6 | ESP8266 |
|------|---------|
| VCC | VIN (5V) |
| GND | GND |
| AO | A0 |

**OLED SSD1306 (optional)**

| OLED | ESP8266 |
|------|---------|
| VCC | 3.3V |
| GND | GND |
| SCL | D1 / GPIO5 |
| SDA | D2 / GPIO4 |

**Buzzer**

| Buzzer | ESP8266 |
|--------|---------|
| + | D5 / GPIO14 |
| − | GND |

### Flash Firmware

Download the latest `.bin` file from the [gasleakdetector-esp releases](https://github.com/gasleakdetector/gasleakdetector-esp/releases) page and flash it to the ESP8266 using your preferred tool (esptool, Arduino IDE, or ESP Flash Download Tool).

Flashing procedure is not covered in detail here — refer to the firmware repository for instructions.

### First-time Configuration

**Step 1.** Power on the ESP8266. It will broadcast a Wi-Fi access point. Connect to it.

![ESP Wi-Fi access point](../images/esp_wifi_ap.jpg)

**Step 2.** Open a browser and navigate to `http://192.168.4.1`. The captive portal will open automatically on most devices. Enter your credentials:

- **SSID / Password** — your home Wi-Fi credentials
- **API KEY** — the `VALID_API_KEY` you configured in Vercel

![ESP captive portal home](../images/esp_captive_portal.jpg)

**Step 3.** Go to **Settings** (top right). Enter the **API Host** with your Vercel deployment URL (e.g. `https://your-app.vercel.app`), then click **Save Config**.

![ESP configuration form](../images/esp_config_form.jpg)

**Step 4.** Reset the ESP to apply the new configuration. The device will connect to your Wi-Fi and begin sending data.

---

## 4. Android App

### Installation

Download the latest APK from the [releases page](https://github.com/gasleakdetector/gasleakdetector/releases/latest) and install it on your device.

### Configuration

**Step 1.** Open the app and tap the pencil icon in the top-right corner of the home screen.

![Edit config button](../images/app_edit_config.jpg)

**Step 2.** Fill in your settings:

![App configuration form](../images/app_config_form.jpg)

| Field | Value |
|-------|-------|
| API URL | Your Vercel deployment URL |
| API Key | The `VALID_API_KEY` you defined |
| Device ID | The `device_id` of your ESP (e.g. `ESP_GASLEAK_01`). Leave blank to monitor all devices. |

Tap **Save**. The app will connect and begin displaying live readings.

---

## Troubleshooting

**ESP not connecting to Wi-Fi** — double-check SSID and password in the captive portal. Hold the button for 5 seconds to factory reset and reconfigure.

**App shows no data** — verify that API URL and API Key match exactly what is set in Vercel. Check the Vercel function logs for errors.

**No email alerts** — confirm `RESEND_API_KEY` and `ALERT_EMAIL` are set in Vercel. Alerts only fire when status reaches `danger` and the cooldown window has passed.

**Supabase schema errors** — ensure the SQL was run in the correct project and that `pg_cron` extension is enabled (Supabase enables it by default on paid plans; free plans may require manual activation).

---

*For questions or issues, open a [GitHub issue](https://github.com/gasleakdetector/gasleakdetector/issues) or contact [pan2512811@gmail.com](mailto:pan2512811@gmail.com). Contributions are welcome 😊*
