# Einrichtungsanleitung

Vollständige Einrichtungsanleitung für das Gas Leak Detector Projekt. Dieses Dokument behandelt alle vier Komponenten — Server-Backend, Supabase, ESP8266-Firmware und Android-App — in der Reihenfolge, in der sie konfiguriert werden sollten.

<div align="center">
	<p><a target="_blank" href="README.md">English</a>&nbsp;&nbsp;|&nbsp;&nbsp;
	    <a target="_blank" href="README-vi.md">Tiếng Việt</a>&nbsp;&nbsp;|&nbsp;&nbsp;
	    Deutsch&nbsp;&nbsp;|&nbsp;&nbsp;
	    <a target="_blank" href="README-es.md">Español</a>&nbsp;&nbsp;|&nbsp;&nbsp;
	    <a target="_blank" href="README-fr.md">Français</a>&nbsp;&nbsp;|&nbsp;&nbsp;
	    <a target="_blank" href="README-ja.md">日本語</a>&nbsp;&nbsp;|&nbsp;&nbsp;
	    <a target="_blank" href="README-ko.md">한국어</a>&nbsp;&nbsp;|&nbsp;&nbsp;
	    <a target="_blank" href="README-zh.md">中文</a>
</div>

## Voraussetzungen

### Hardware

| Komponente | Hinweise |
|-----------|----------|
| ESP8266 | NodeMCU oder gleichwertig |
| MQ-6 Gassensor | LPG / Propan-Erkennung |
| OLED SSD1306 0.96" | Optional — für Anzeige auf dem Gerät |
| Summer | Aktiv oder passiv |

### Konten

- [Vercel](https://vercel.com) — Serverless-Deployment
- [Supabase](https://supabase.com) — Datenbank und Echtzeit
- [Resend](https://resend.com) — E-Mail-Benachrichtigungen (optional)

---

## 1. Server-Backend

### Auf Vercel deployen

**Schritt 1.** Gehen Sie zum Repository [gasleakdetector-server](https://github.com/gasleakdetector/gasleakdetector-server) und klicken Sie auf die Schaltfläche **Deploy**.

![Auf Vercel deployen](../images/vercel_deploy_button.jpg)

**Schritt 2.** Öffnen Sie Ihr Projekt im Vercel-Dashboard und gehen Sie zu **Settings**.

![Vercel-Projekteinstellungen](../images/vercel_project_settings.jpg)

**Schritt 3.** Navigieren Sie zu **Environment Variables**.

![Tab Environment Variables](../images/vercel_environment_variables.jpg)

**Schritt 4.** Klicken Sie auf **Add Environment Variable**.

![Umgebungsvariable hinzufügen](../images/vercel_add_variable.jpg)

**Schritt 5.** Fügen Sie jede der folgenden Variablen hinzu:

![Variablenwerte ausfüllen](../images/vercel_fill_variables.jpg)

| Variable | Beschreibung |
|----------|--------------|
| `SUPABASE_URL` | Ihre Supabase-Projekt-URL |
| `SUPABASE_ANON_KEY` | Supabase Anonymous Key — wird von der Android-App-WebSocket verwendet |
| `SUPABASE_SERVICE_KEY` | Supabase Service Role Key — für alle serverseitigen Schreibvorgänge |
| `VALID_API_KEY` | Gemeinsames Geheimnis, das im `x-api-key`-Header von ESP und App gesendet wird |
| `RESEND_API_KEY` | Resend-API-Key für E-Mail-Benachrichtigungen |
| `ALERT_EMAIL` | Empfängeradresse für Gefahren-Benachrichtigungen |
| `DANGER_THRESHOLD` | PPM-Wert, der den `danger`-Status auslöst. Empfohlen: `800` für MQ-6 |
| `WARNING_THRESHOLD` | PPM-Wert, der den `warning`-Status auslöst. Empfohlen: `300` für MQ-6 |
| `EMAIL_COOLDOWN_MINUTES` | Mindestminuten zwischen wiederholten Benachrichtigungs-E-Mails. Standard: `2` |

### API-Key

`VALID_API_KEY` ist ein Geheimnis, das Sie selbst festlegen — es wird zwischen Server, ESP-Firmware und Android-App geteilt. Es ist keine Registrierung oder Drittanbieterdienst erforderlich.

Empfohlen: 8–10 alphanumerische Zeichen. Beispiel: `Abc12345`.

### Resend (E-Mail-Benachrichtigungen)

Dieser Abschnitt ist optional. Überspringen Sie ihn, wenn Sie keine E-Mail-Benachrichtigungen benötigen.

**Schritt 1.** Melden Sie sich an oder erstellen Sie ein Konto unter [resend.com](https://resend.com/login).

**Schritt 2.** Gehen Sie zu **API Keys** und klicken Sie auf **Create API Key**.

![Resend API Keys-Seite](../images/resend_api_keys_page.jpg)

**Schritt 3.** Geben Sie den Key-Namen ein und klicken Sie auf **Add**.

![API Key erstellen](../images/resend_create_api_key.jpg)

**Schritt 4.** Kopieren Sie den generierten Key.

![Key kopieren](../images/resend_copy_key.jpg)

Weisen Sie ihn `RESEND_API_KEY` in Vercel zu. Setzen Sie `ALERT_EMAIL` auf die Adresse, die Benachrichtigungen erhalten soll (z. B. `you@gmail.com`).

---

## 2. Supabase

**Schritt 1.** Erstellen Sie ein neues Projekt in Supabase.

**Schritt 2.** Öffnen Sie den SQL-Editor, fügen Sie den Inhalt von [`supabase/schema.sql`](https://github.com/gasleakdetector/gasleakdetector-server/blob/main/supabase/schema.sql) ein und führen Sie ihn einmal aus.

![Supabase SQL-Editor](../images/supabase_sql_editor.jpg)

Damit werden alle Tabellen (`gas_logs_raw`, `gas_logs_minute`, `gas_logs_hour`, `devices`), Aggregationsfunktionen, `pg_cron`-Jobs und Row-Level-Security-Richtlinien in einem einzigen Durchlauf erstellt.

**Schritt 3.** Rufen Sie Ihre API-Zugangsdaten ab. Gehen Sie zu **Settings > API Keys > Legacy anon, service_role API keys** und kopieren Sie beide Schlüssel `anon` und `service_role`.

![Supabase API Keys](../images/supabase_api_keys.jpg)

Weisen Sie sie `SUPABASE_ANON_KEY` und `SUPABASE_SERVICE_KEY` in Vercel zu. Ihre `SUPABASE_URL` finden Sie unter **Project Overview**.

---

## 3. ESP8266-Firmware

### Verkabelung

Verbinden Sie alle Komponenten mit dem ESP8266 gemäß den folgenden Tabellen, bevor Sie ihn einschalten.

**MQ-6 Gassensor**

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

**Summer**

| Summer | ESP8266 |
|--------|---------|
| + | D5 / GPIO14 |
| − | GND |

### Firmware flashen

Laden Sie die neueste `.bin`-Datei von der [gasleakdetector-esp Releases](https://github.com/gasleakdetector/gasleakdetector-esp/releases)-Seite herunter und flashen Sie sie mit Ihrem bevorzugten Tool auf den ESP8266 (esptool, Arduino IDE oder ESP Flash Download Tool).

Das Flash-Verfahren wird hier nicht im Detail beschrieben — siehe das Firmware-Repository für Anweisungen.

### Erstkonfiguration

**Schritt 1.** Schalten Sie den ESP8266 ein. Er sendet einen WLAN-Zugangspunkt aus. Verbinden Sie sich damit.

![ESP WLAN-Zugangspunkt](../images/esp_wifi_ap.jpg)

**Schritt 2.** Öffnen Sie einen Browser und navigieren Sie zu `http://192.168.4.1`. Das Captive Portal öffnet sich auf den meisten Geräten automatisch. Geben Sie Ihre Zugangsdaten ein:

- **SSID / Passwort** — Ihre Heim-WLAN-Zugangsdaten
- **API KEY** — der in Vercel konfigurierte `VALID_API_KEY`

![ESP Captive Portal](../images/esp_captive_portal.jpg)

**Schritt 3.** Gehen Sie zu **Settings** (oben rechts). Geben Sie den **API Host** mit Ihrer Vercel-Deployment-URL ein (z. B. `https://your-app.vercel.app`), dann klicken Sie auf **Save Config**.

![ESP Konfigurationsformular](../images/esp_config_form.jpg)

**Schritt 4.** Setzen Sie den ESP zurück, um die neue Konfiguration zu übernehmen. Das Gerät verbindet sich mit Ihrem WLAN und beginnt mit der Datenübertragung.

---

## 4. Android-App

### Installation

Laden Sie das neueste APK von der [Releases-Seite](https://github.com/gasleakdetector/gasleakdetector/releases/latest) herunter und installieren Sie es auf Ihrem Gerät.

### Konfiguration

**Schritt 1.** Öffnen Sie die App und tippen Sie auf das Stift-Symbol oben rechts im Startbildschirm.

![Konfiguration bearbeiten](../images/app_edit_config.jpg)

**Schritt 2.** Füllen Sie Ihre Einstellungen aus:

![App-Konfigurationsformular](../images/app_config_form.jpg)

| Feld | Wert |
|------|------|
| API URL | Ihre Vercel-Deployment-URL |
| API Key | Der von Ihnen definierte `VALID_API_KEY` |
| Device ID | Die `device_id` Ihres ESP (z. B. `ESP_GASLEAK_01`). Leer lassen, um alle Geräte zu überwachen. |

Tippen Sie auf **Save**. Die App verbindet sich und zeigt Live-Messwerte an.

---

> ❕ In der aktuellen stabilen Version unterstützt die App **Push-Benachrichtigungen nur, solange die App im Hintergrund läuft**.
> 
> Wenn du Benachrichtigungen **auch nach dem vollständigen Schließen der App** erhalten möchtest, findest du hier die entsprechenden Entwicklungszweige:
> 
> * [Android-Branch mit FCM](https://github.com/gasleakdetector/gasleakdetector/tree/feature/fcm-push-notification)
> * [Server-Branch mit FCM-Unterstützung](https://github.com/gasleakdetector/gasleakdetector-server/tree/feature/fcm-push-notification)
> 
> **Hinweis:** Aufgrund von Sicherheits- und Konfigurationsanforderungen (eine eigene `google-services.json` wird benötigt) können diese Branches nicht in den Hauptzweig gemergt werden. Das Feature ist noch in Entwicklung. Du kannst diesen Hinweis ignorieren, wenn er für dich nicht relevant ist 😊.

---

## Fehlerbehebung

**ESP verbindet sich nicht mit WLAN** — überprüfen Sie SSID und Passwort im Captive Portal. Halten Sie die Taste 5 Sekunden gedrückt, um auf Werkseinstellungen zurückzusetzen und neu zu konfigurieren.

**App zeigt keine Daten** — überprüfen Sie, ob API URL und API Key genau mit den Einstellungen in Vercel übereinstimmen. Prüfen Sie die Vercel-Funktions-Logs auf Fehler.

**Keine E-Mail-Benachrichtigungen** — bestätigen Sie, dass `RESEND_API_KEY` und `ALERT_EMAIL` in Vercel gesetzt sind. Benachrichtigungen werden nur ausgelöst, wenn der Status `danger` erreicht und das Abklingfenster verstrichen ist.

**Supabase-Schema-Fehler** — stellen Sie sicher, dass die SQL im richtigen Projekt ausgeführt wurde und die `pg_cron`-Erweiterung aktiviert ist (Supabase aktiviert sie standardmäßig bei kostenpflichtigen Tarifen; kostenlose Tarife erfordern möglicherweise manuelle Aktivierung).

---

*Bei Fragen oder Problemen öffnen Sie ein [GitHub-Issue](https://github.com/gasleakdetector/gasleakdetector/issues) oder kontaktieren Sie [pan2512811@gmail.com](mailto:pan2512811@gmail.com). Beiträge sind willkommen 😊*
