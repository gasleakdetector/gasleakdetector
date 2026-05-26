# 설정 가이드

Gas Leak Detector 프로젝트의 완전한 설정 가이드입니다. 이 문서는 서버 백엔드, Supabase, ESP8266 펌웨어, Android 앱의 네 가지 구성 요소를 구성해야 하는 순서대로 설명합니다.

<div align="center">
	<p><a target="_blank" href="README.md">English</a>&nbsp;&nbsp;|&nbsp;&nbsp;
	    <a target="_blank" href="README-vi.md">Tiếng Việt</a>&nbsp;&nbsp;|&nbsp;&nbsp;
	    <a target="_blank" href="README-de.md">Deutsch</a>&nbsp;&nbsp;|&nbsp;&nbsp;
	    <a target="_blank" href="README-es.md">Español</a>&nbsp;&nbsp;|&nbsp;&nbsp;
	    <a target="_blank" href="README-fr.md">Français</a>&nbsp;&nbsp;|&nbsp;&nbsp;
	    <a target="_blank" href="README-ja.md">日本語</a>&nbsp;&nbsp;|&nbsp;&nbsp;
	    한국어&nbsp;&nbsp;|&nbsp;&nbsp;
	    <a target="_blank" href="README-zh.md">中文</a>
</div>

## 요구 사항

### 하드웨어

| 부품 | 비고 |
|------|------|
| ESP8266 | NodeMCU 또는 동급 |
| MQ-6 가스 센서 | LPG / 프로판 감지 |
| OLED SSD1306 0.96" | 선택 사항 - 기기 화면 표시용 |
| 부저 | 능동형 또는 수동형 |

### 계정

- [Vercel](https://vercel.com) - 서버리스 배포
- [Supabase](https://supabase.com) - 데이터베이스 및 실시간
- [Resend](https://resend.com) - 이메일 알림 (선택 사항)

---

## 1. 서버 백엔드

### Vercel에 배포하기

**1단계.** [gasleakdetector-server](https://github.com/gasleakdetector/gasleakdetector-server) 저장소로 이동하여 **Deploy** 버튼을 클릭합니다.

![Vercel에 배포](../images/vercel_deploy_button.jpg)

**2단계.** Vercel 대시보드에서 프로젝트를 열고 **Settings**로 이동합니다.

![Vercel 프로젝트 설정](../images/vercel_project_settings.jpg)

**3단계.** **Environment Variables**로 이동합니다.

![Environment Variables 탭](../images/vercel_environment_variables.jpg)

**4단계.** **Add Environment Variable**을 클릭합니다.

![환경 변수 추가](../images/vercel_add_variable.jpg)

**5단계.** 다음 각 변수를 추가합니다:

![변수 값 입력](../images/vercel_fill_variables.jpg)

| 변수 | 설명 |
|------|------|
| `SUPABASE_URL` | Supabase 프로젝트 URL |
| `SUPABASE_ANON_KEY` | Supabase anonymous key - Android 앱 WebSocket에서 사용 |
| `SUPABASE_SERVICE_KEY` | Supabase service role key - 모든 서버 측 쓰기에 사용 |
| `VALID_API_KEY` | ESP와 앱이 `x-api-key` 헤더에 보내는 공유 시크릿 |
| `RESEND_API_KEY` | 이메일 알림용 Resend API key |
| `ALERT_EMAIL` | 위험 수준 알림을 받을 이메일 주소 |
| `DANGER_THRESHOLD` | `danger` 상태를 트리거하는 PPM 수준. MQ-6 권장값: `800` |
| `WARNING_THRESHOLD` | `warning` 상태를 트리거하는 PPM 수준. MQ-6 권장값: `300` |
| `EMAIL_COOLDOWN_MINUTES` | 반복 알림 이메일 사이의 최소 분. 기본값: `2` |

### API Key

`VALID_API_KEY`는 직접 정의하는 시크릿으로, 서버, ESP 펌웨어, Android 앱 사이에서 공유됩니다. 등록이나 서드파티 서비스가 필요하지 않습니다.

권장: 영숫자 8~10자. 예: `Abc12345`

### Resend (이메일 알림)

이 섹션은 선택 사항입니다. 이메일 알림이 필요하지 않으면 건너뛰세요.

**1단계.** [resend.com](https://resend.com/login)에서 로그인하거나 계정을 만듭니다.

**2단계.** **API Keys**로 이동하여 **Create API Key**를 클릭합니다.

![Resend API Keys 페이지](../images/resend_api_keys_page.jpg)

**3단계.** 키 이름을 입력하고 **Add**를 클릭합니다.

![API Key 생성 양식](../images/resend_create_api_key.jpg)

**4단계.** 생성된 키를 복사합니다.

![키 복사](../images/resend_copy_key.jpg)

Vercel의 `RESEND_API_KEY`에 할당합니다. `ALERT_EMAIL`을 알림을 받을 주소(예: `you@gmail.com`)로 설정합니다.

---

## 2. Supabase

**1단계.** Supabase에 새 프로젝트를 만듭니다.

**2단계.** SQL 편집기를 열고 [`supabase/schema.sql`](https://github.com/gasleakdetector/gasleakdetector-server/blob/main/supabase/schema.sql)의 내용을 붙여넣고 한 번 실행합니다.

![Supabase SQL 편집기](../images/supabase_sql_editor.jpg)

이렇게 하면 모든 테이블(`gas_logs_raw`, `gas_logs_minute`, `gas_logs_hour`, `devices`), 집계 함수, `pg_cron` 작업, Row-Level Security 정책이 한 번에 생성됩니다.

**3단계.** API 자격 증명을 가져옵니다. **Settings > API Keys > Legacy anon, service_role API keys**로 이동하여 `anon`과 `service_role` 키를 모두 복사합니다.

![Supabase API Keys](../images/supabase_api_keys.jpg)

Vercel의 `SUPABASE_ANON_KEY`와 `SUPABASE_SERVICE_KEY`에 할당합니다. `SUPABASE_URL`은 **Project Overview**에서 확인할 수 있습니다.

---

## 3. ESP8266 펌웨어

### 배선

전원을 켜기 전에 아래 표에 따라 모든 부품을 ESP8266에 연결합니다.

**MQ-6 가스 센서**

| MQ-6 | ESP8266 |
|------|---------|
| VCC | VIN (5V) |
| GND | GND |
| AO | A0 |

**OLED SSD1306 (선택 사항)**

| OLED | ESP8266 |
|------|---------|
| VCC | 3.3V |
| GND | GND |
| SCL | D1 / GPIO5 |
| SDA | D2 / GPIO4 |

**부저**

| 부저 | ESP8266 |
|------|---------|
| + | D5 / GPIO14 |
| − | GND |

### 펌웨어 플래싱

[gasleakdetector-esp releases](https://github.com/gasleakdetector/gasleakdetector-esp/releases) 페이지에서 최신 `.bin` 파일을 다운로드하고 원하는 도구(esptool, Arduino IDE 또는 ESP Flash Download Tool)를 사용하여 ESP8266에 플래싱합니다.

플래싱 절차는 여기서 자세히 다루지 않습니다 - 지침은 펌웨어 저장소를 참조하세요.

### 최초 설정

**1단계.** ESP8266의 전원을 켭니다. Wi-Fi 액세스 포인트를 브로드캐스트합니다. 거기에 연결합니다.

![ESP Wi-Fi 액세스 포인트](../images/esp_wifi_ap.jpg)

**2단계.** 브라우저를 열고 `http://192.168.4.1`로 이동합니다. 대부분의 기기에서 캡티브 포털이 자동으로 열립니다. 자격 증명을 입력합니다:

- **SSID / 비밀번호** - 가정용 Wi-Fi 자격 증명
- **API KEY** - Vercel에서 구성한 `VALID_API_KEY`

![ESP 캡티브 포털](../images/esp_captive_portal.jpg)

**3단계.** **Settings**(오른쪽 상단)로 이동합니다. Vercel 배포 URL(예: `https://your-app.vercel.app`)로 **API Host**를 입력한 다음 **Save Config**를 클릭합니다.

![ESP 설정 양식](../images/esp_config_form.jpg)

**4단계.** 새 설정을 적용하려면 ESP를 재설정합니다. 기기가 Wi-Fi에 연결되고 데이터 전송을 시작합니다.

---

## 4. Android 앱

### 설치

[릴리스 페이지](https://github.com/gasleakdetector/gasleakdetector/releases/latest)에서 최신 APK를 다운로드하여 기기에 설치합니다.

### 설정

**1단계.** 앱을 열고 홈 화면 오른쪽 상단의 연필 아이콘을 탭합니다.

![설정 편집 버튼](../images/app_edit_config.jpg)

**2단계.** 설정을 입력합니다:

![앱 설정 양식](../images/app_config_form.jpg)

| 필드 | 값 |
|------|-----|
| API URL | Vercel 배포 URL |
| API Key | 정의한 `VALID_API_KEY` |
| Device ID | ESP의 `device_id`(예: `ESP_GASLEAK_01`). 모든 기기를 모니터링하려면 비워두세요. |

**Save**를 탭합니다. 앱이 연결되고 실시간 측정값을 표시하기 시작합니다.

---

> ❕ 현재 안정 버전에서는 **앱이 백그라운드에서 실행 중일 때만** 푸시 알림을 지원합니다.
> 
> **앱을 완전히 종료한 상태에서도 알림을 받고 싶다면** 아래 개발 브랜치를 참고하세요:
> 
> * [FCM 지원 Android 브랜치](https://github.com/gasleakdetector/gasleakdetector/tree/feature/fcm-push-notification)
> * [FCM 지원 서버 브랜치](https://github.com/gasleakdetector/gasleakdetector-server/tree/feature/fcm-push-notification)
> 
> **참고:** 보안 및 서비스 설정 요건(개인 `google-services.json` 파일 필요)으로 인해 이 브랜치들은 메인 브랜치에 병합할 수 없습니다. 해당 기능은 아직 개발 중입니다. 필요하지 않다면 이 메시지는 무시해도 좋습니다 😊.

---

## 문제 해결

**ESP가 Wi-Fi에 연결되지 않는 경우** - 캡티브 포털에서 SSID와 비밀번호를 다시 확인하세요. 버튼을 5초간 누르면 공장 초기화되어 재설정할 수 있습니다.

**앱에 데이터가 표시되지 않는 경우** - API URL과 API Key가 Vercel에 설정된 것과 정확히 일치하는지 확인하세요. 오류는 Vercel 함수 로그를 확인하세요.

**이메일 알림이 없는 경우** - `RESEND_API_KEY`와 `ALERT_EMAIL`이 Vercel에 설정되어 있는지 확인하세요. 알림은 상태가 `danger`에 도달하고 쿨다운 기간이 지났을 때만 전송됩니다.

**Supabase 스키마 오류** - SQL이 올바른 프로젝트에서 실행되었고 `pg_cron` 확장이 활성화되어 있는지 확인하세요 (Supabase는 유료 플랜에서 기본적으로 활성화합니다. 무료 플랜은 수동 활성화가 필요할 수 있습니다).

---

*질문이나 문제가 있으면 [GitHub 이슈](https://github.com/gasleakdetector/gasleakdetector/issues)를 열거나 [pan2512811@gmail.com](mailto:pan2512811@gmail.com)으로 연락하세요. 기여를 환영합니다 😊*
