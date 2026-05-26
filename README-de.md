<p align="center">    
  <img src=".github/assets/cover.png" alt="Cover" />    
</p>

<p align="center">
  <img alt="License" src="https://img.shields.io/github/license/gasleakdetector/gasleakdetector?color=04A8F4&style=flat-square"/>
  <img alt="Version" src="https://img.shields.io/badge/version-1.0.3-04A8F4?style=flat-square"/>
  <img alt="Min SDK" src="https://img.shields.io/badge/min%20SDK-21-04A8F4?style=flat-square&logo=android"/>
  <img alt="Build" src="https://img.shields.io/github/actions/workflow/status/gasleakdetector/gasleakdetector/build.yml?style=flat-square&color=04A8F4"/>
  <img alt="Last commit" src="https://img.shields.io/github/last-commit/gasleakdetector/gasleakdetector?color=04A8F4&style=flat-square"/>
</p>

<div align="center">
  <h1>Gas Leak Detector - App</h1>
  <p><a target="_blank" href="README.md">English</a>&nbsp;&nbsp;|&nbsp;&nbsp;
    <a target="_blank" href="README-vi.md">Tiếng Việt</a>&nbsp;&nbsp;|&nbsp;&nbsp;
    Deutsch&nbsp;&nbsp;|&nbsp;&nbsp;
    <a target="_blank" href="README-es.md">Español</a>&nbsp;&nbsp;|&nbsp;&nbsp;
    <a target="_blank" href="README-fr.md">Français</a>&nbsp;&nbsp;|&nbsp;&nbsp;
    <a target="_blank" href="README-ja.md">日本語</a>&nbsp;&nbsp;|&nbsp;&nbsp;
    <a target="_blank" href="README-ko.md">한국어</a>&nbsp;&nbsp;|&nbsp;&nbsp;
    <a target="_blank" href="README-zh.md">中文</a>
  </p>
  <p align="center">Echtzeit-Gasüberwachung für Android - powered by ESP8266, Supabase und einer serverlosen Edge-API.</p>
  <img src=".github/assets/intro.png" width="19.2%" alt="intro" />    
  <img src=".github/assets/config.png" width="19.2%" alt="config" />    
  <img src=".github/assets/home.png" width="19.2%" alt="home" />    
  <img src=".github/assets/stats.png" width="19.2%" alt="stats" />    
  <br/>    
  <br/>    
</div>

## Überblick

Gas Leak Detector ist ein vollständiges IoT-Sicherheitssystem. Ein MQ-6-Sensor auf einem ESP8266 misst kontinuierlich die Umgebungsgaskonzentration und sendet Messwerte an eine serverlose Vercel-API. Daten werden in Supabase gespeichert und in Echtzeit über WebSocket an die Android-App übertragen - kein Polling, keine Verzögerung.

Das System besteht aus drei unabhängigen Repositories, die eine Pipeline bilden:

| Schicht | Repo | Stack |
|---|---|---|
| Firmware | [gasleakdetector-esp](https://github.com/gasleakdetector/gasleakdetector-esp) | C++ / Arduino / ESP8266 |
| Backend | [gasleakdetector-server](https://github.com/gasleakdetector/gasleakdetector-server) | Node.js / Vercel / Supabase |
| Mobile | **gasleakdetector** *(dieses Repo)* | Android / Java |

## Demo

[Demo-Video ansehen](https://www.youtube.com/watch?v=RLNf9Zphb1I)

## Gesamtprojekt einrichten

Eine detaillierte Anleitung zur Einrichtung des gesamten Projekts finden Sie [hier](Tutorial/README-de.md)

## Systemablauf

<p align="center">
  <img src=".github/assets/flow.png" alt="System flow" width="100%"/>
</p>

1. ESP8266 liest den MQ-6-Sensor alle 400 ms und sendet POST-Anfragen an `/api/ingest` mit API-Key-Authentifizierung.
2. Die Vercel-Edge-Funktion klassifiziert den Messwert (`normal / warning / danger`) und schreibt in Supabase. E-Mail-Benachrichtigungen werden bei `danger` mit konfigurierbarer Abklingzeit ausgelöst.
3. `pg_cron` in Supabase aggregiert Rohzeilen automatisch in Minuten- und Stunden-Buckets - kein externer Scheduler erforderlich.
4. Die Android-App ruft Supabase-Anmeldeinformationen von `/api/realtime-config` ab und öffnet ein WebSocket-Abonnement direkt auf `gas_logs_raw` für latenzfreie Live-Updates.
5. Historische Diagramme lesen aus voraggregrierten Stunden-Buckets - Abfragezeit bleibt konstant, unabhängig von der Betriebsdauer des Geräts.

## Funktionen

- [x] Live-PPM-Anzeige mit animiertem Wert und WebSocket-Echtzeit-Updates
- [x] Statusklassifizierung - Normal / Warnung / Gefahr mit Farb-Feedback
- [x] Anhaltende Gefahrenbenachrichtigung solange Gaswerte kritisch bleiben
- [x] Historisches Diagramm - stündlich aggregierte Daten mit dynamischer Y-Achse
- [x] Multi-Node-Unterstützung - zwischen ESP-Geräten per `device_id` wechseln
- [x] Cursor-basierte Paginierung - ruft bis zu 1.000 Zeilen pro Anfrage mit Gzip-Komprimierung ab
- [x] Offline-Resilienz - ESP speichert bis zu 60 Messwerte lokal; App zeigt gecachte Daten
- [x] Erster-Start-Einführungsbildschirm - einmalig angezeigt, bei allen nachfolgenden Starts übersprungen
- [x] Feedback - Ein-Tap-E-Mail mit vorausgefüllter App-Version im Betreff
- [x] Internationalisierung - 8 Sprachen: Englisch, Vietnamesisch, Deutsch, Spanisch, Französisch, Japanisch, Koreanisch, Chinesisch
- [ ] Widget
- [ ] Multi-Schwellenwert-Konfiguration pro Gerät
- [ ] Push-Benachrichtigungen via FCM

## Datenverwaltung

Das Herzstück dieses Projekts ist eine **dreistufige Speicher-Pipeline**, die vollständig in Supabase aufgebaut wurde und hochfrequente Sensor-Ingestion bei begrenztem Speicher und schnellen Abfragen in jeder Größenordnung bewältigt.

**Stufe 1 - `gas_logs_raw`**
Jeder Sensorwert wird beim Eingang hier gespeichert. Supabase Realtime überträgt jeden Eintrag sofort per WebSocket an die Android-App. Diese Tabelle ist schreibintensiv und wächst schnell - ein einzelnes Gerät mit Standardintervall produziert tausende Zeilen pro Stunde.

**Stufe 2 - `gas_logs_minute`**
`pg_cron` aggregiert Rohzeilen minütlich in Minuten-Buckets und berechnet `avg / min / max / sample_count` pro Gerät. Der Status verwendet Worst-Case-Logik: ein einziger `danger`-Wert im Bucket markiert den gesamten Bucket als `danger`. Dies ist die Zwischenschicht - detailliert genug für kurzfristige Ansichten, klein genug für schnelle Abfragen.

**Stufe 3 - `gas_logs_hour`**
Stündlich werden Minuten-Buckets zu Stunden-Buckets zusammengefasst. Das Statistikdiagramm in der App liest ausschließlich aus dieser Tabelle - es berührt nie Rohdaten. Die Abfragezeit ist konstant, unabhängig von der Betriebsdauer der Geräte.

**Automatische Bereinigung**
`pg_cron` führt täglich um 03:00 UTC eine Bereinigung durch, die alle `normal`-Zeilen aus `gas_logs_raw` löscht, die älter als 48 Stunden sind. Zu diesem Zeitpunkt wurde jeder Messwert bereits in den Minuten- und Stunden-Aggregaten erfasst, sodass keine historischen Informationen verloren gehen. Zeilen mit `warning`- oder `danger`-Status werden für Prüfzwecke unbegrenzt aufbewahrt.

Ohne dies würde ein Gerät, das mit 5 Lesungen/Sekunde sendet, ~430.000 Rohzeilen pro Tag ansammeln. Mit der Bereinigung bleibt `gas_logs_raw` auf die letzten 48 Stunden normaler Messwerte begrenzt - die Speicherkosten bleiben konstant, während die Betriebszeit wächst.

## Erste Schritte

### Voraussetzungen

- Android Studio Flamingo oder neuer
- JDK 17
- Eine laufende Instanz von [gasleakdetector-server](https://github.com/gasleakdetector/gasleakdetector-server)

### Build

```shell
git clone https://github.com/gasleakdetector/gasleakdetector.git
cd gasleakdetector
./gradlew assembleDebug
```

### Konfiguration

Öffnen Sie **Einstellungen** in der App und füllen Sie aus:

| Feld | Beschreibung |
|---|---|
| API URL | Ihre Vercel-Deployment-URL, z.B. `https://your-app.vercel.app` |
| API Key | Der `VALID_API_KEY`, der in Ihren Vercel-Umgebungsvariablen festgelegt ist |
| Device ID | Die `device_id`, die Ihr ESP sendet, z.B. `ESP_GASLEAK_01` (Lassen Sie dieses Feld leer, wenn Sie alle Geräte einschließen möchten.) |

Die App ruft Supabase-Anmeldeinformationen automatisch von `/api/realtime-config` ab - keine Supabase-Schlüssel müssen manuell eingegeben werden.

## Download

- **Neuester Alpha-Build**: Herunterladen von [Actions](https://github.com/gasleakdetector/gasleakdetector/actions/)
- **Neuester stabiler Build**: Herunterladen von [Releases](https://github.com/gasleakdetector/gasleakdetector/releases)

[<img src="https://raw.githubusercontent.com/Kunzisoft/Github-badge/main/get-it-on-github.png" alt="Get it on GitHub" height="80">](https://github.com/gasleakdetector/gasleakdetector/releases/latest)

## Verwandte Repositories

| Repo | Beschreibung |
|---|---|
| [gasleakdetector-server](https://github.com/gasleakdetector/gasleakdetector-server) | Vercel serverlose API - Ingestion, historische Abfragen, Statistiken, E-Mail-Benachrichtigungen |
| [gasleakdetector-esp](https://github.com/gasleakdetector/gasleakdetector-esp) | ESP8266-Firmware - MQ-6-Leser, WiFi-Captive-Portal, Offline-Warteschlange |

## Lizenz

Apache 2.0 © [Gas Leak Detector](LICENSE)
