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
  <h1>Gas Leak Detector — App</h1>
  <p><a target="_blank" href="README.md">English</a>&nbsp;&nbsp;|&nbsp;&nbsp;
    <a target="_blank" href="README-vi.md">Tiếng Việt</a>&nbsp;&nbsp;|&nbsp;&nbsp;
    <a target="_blank" href="README-de.md">Deutsch</a>&nbsp;&nbsp;|&nbsp;&nbsp;
    <a target="_blank" href="README-es.md">Español</a>&nbsp;&nbsp;|&nbsp;&nbsp;
    <a target="_blank" href="README-fr.md">Français</a>&nbsp;&nbsp;|&nbsp;&nbsp;
    <a target="_blank" href="README-ja.md">日本語</a>&nbsp;&nbsp;|&nbsp;&nbsp;
    한국어&nbsp;&nbsp;|&nbsp;&nbsp;
    <a target="_blank" href="README-zh.md">中文</a>
  </p>
  <p align="center">Android용 실시간 가스 모니터링 — ESP8266, Supabase 및 서버리스 엣지 API 기반.</p>
  <img src=".github/assets/intro.png" width="19.2%" alt="intro" />    
  <img src=".github/assets/config.png" width="19.2%" alt="config" />    
  <img src=".github/assets/home.png" width="19.2%" alt="home" />    
  <img src=".github/assets/stats.png" width="19.2%" alt="stats" />    
  <br/>    
  <br/>    
</div>

## 개요

Gas Leak Detector는 풀스택 IoT 안전 시스템입니다. ESP8266의 MQ-6 센서가 주변 가스 농도를 지속적으로 샘플링하여 서버리스 Vercel API로 측정값을 전송합니다. 데이터는 Supabase에 저장되고 WebSocket을 통해 Android 앱으로 실시간 스트리밍됩니다 — 폴링 없음, 지연 없음.

시스템은 하나의 파이프라인을 형성하는 세 개의 독립적인 저장소로 구성됩니다:

| 레이어 | 저장소 | 스택 |
|---|---|---|
| 펌웨어 | [gasleakdetector-esp](https://github.com/gasleakdetector/gasleakdetector-esp) | C++ / Arduino / ESP8266 |
| 백엔드 | [gasleakdetector-server](https://github.com/gasleakdetector/gasleakdetector-server) | Node.js / Vercel / Supabase |
| 모바일 | **gasleakdetector** *(이 저장소)* | Android / Java |

## 데모

[데모 영상 보기](https://www.youtube.com/watch?v=RLNf9Zphb1I)

## 전체 프로젝트 설정

전체 프로젝트 설정 방법에 대한 자세한 가이드는 [여기](Tutorial/README-ko.md)에서 확인하세요

## 시스템 흐름

<p align="center">
  <img src=".github/assets/flow.png" alt="System flow" width="100%"/>
</p>

1. ESP8266은 400ms마다 MQ-6 센서를 읽어 API 키 인증으로 `/api/ingest`에 POST 요청을 보냅니다.
2. Vercel 엣지 함수가 측정값을 분류하고(`normal / warning / danger`) Supabase에 기록합니다. 설정 가능한 쿨다운으로 `danger` 시 이메일 알림이 발송됩니다.
3. Supabase 내 `pg_cron`이 자동으로 원시 행을 분별, 시간별 버킷으로 집계합니다 — 외부 스케줄러가 필요 없습니다.
4. Android 앱은 `/api/realtime-config`에서 Supabase 자격 증명을 가져와 `gas_logs_raw`에 직접 WebSocket 구독을 열어 지연 없는 실시간 업데이트를 실현합니다.
5. 히스토리 차트는 사전 집계된 시간별 버킷에서 읽어 기기 가동 시간에 관계없이 빠른 쿼리를 유지합니다.

## 기능

- [x] 애니메이션 값과 실시간 WebSocket 업데이트가 있는 라이브 PPM 게이지
- [x] 상태 분류 — 정상 / 경고 / 위험 색상 피드백
- [x] 가스 수준이 위험한 상태일 때 지속적인 위험 알림
- [x] 히스토리 차트 — 동적 Y축을 가진 시간별 집계 데이터
- [x] 멀티 노드 지원 — `device_id`로 ESP 기기 간 전환
- [x] 커서 기반 페이지네이션 — gzip 압축으로 요청당 최대 1,000행 가져오기
- [x] 오프라인 복원력 — ESP는 로컬에서 최대 60개 측정값을 큐에 저장; 앱은 캐시 데이터 표시
- [x] 첫 실행 인트로 화면 — 한 번만 표시, 이후 모든 실행 시 건너뜀
- [x] 피드백 — 제목에 앱 버전이 미리 입력된 원 탭 이메일
- [x] 국제화 — 8개 언어: 영어, 베트남어, 독일어, 스페인어, 프랑스어, 일본어, 한국어, 중국어
- [ ] 위젯
- [ ] 기기별 다중 임계값 설정
- [ ] FCM을 통한 푸시 알림

## 데이터 관리

이 프로젝트의 핵심은 Supabase 내에 완전히 구축된 **3계층 스토리지 파이프라인**입니다. 고주파 센서 수집을 처리하면서도 저장 공간을 제한하고 어떤 규모에서도 쿼리를 빠르게 유지하도록 설계되었습니다.

**1계층 — `gas_logs_raw`**
각 센서 측정값이 도착 시 여기에 기록됩니다. Supabase Realtime은 각 삽입을 WebSocket을 통해 즉시 Android 앱에 브로드캐스트합니다. 이 테이블은 쓰기가 많고 빠르게 성장합니다 — 기본 간격으로 전송하는 단일 기기는 시간당 수천 행을 생성합니다.

**2계층 — `gas_logs_minute`**
`pg_cron`은 매분 원시 행을 분별 버킷으로 집계하여 기기별 `avg / min / max / sample_count`를 계산합니다. 상태는 최악 케이스 논리를 사용합니다: 버킷 내 어디에든 단 하나의 `danger` 측정값이 있으면 전체 버킷이 `danger`로 표시됩니다. 이것이 중간 레이어 — 단기 보기에는 충분히 세분화되고, 빠른 쿼리에는 충분히 작습니다.

**3계층 — `gas_logs_hour`**
매시간 분별 버킷이 시간별 버킷으로 통합됩니다. 앱의 통계 차트는 이 테이블에서만 읽습니다 — 원시 데이터를 절대 건드리지 않습니다. 쿼리 시간은 기기 가동 시간에 관계없이 일정합니다.

**자동 정리**
`pg_cron`은 매일 03:00 UTC에 48시간 이상 된 `gas_logs_raw`의 모든 `normal` 행을 삭제하는 정리를 실행합니다. 그 시점에 모든 측정값은 이미 분별, 시간별 집계에 캡처되어 있으므로 히스토리 정보는 손실되지 않습니다. `warning` 또는 `danger` 상태의 행은 감사 목적으로 무기한 보관됩니다.

이것 없이는 5 측정값/초로 전송하는 기기가 하루에 약 430,000개의 원시 행을 축적할 것입니다. 정리를 통해 `gas_logs_raw`는 마지막 48시간의 정상 측정값으로 제한되어 가동 시간이 늘어나도 스토리지 비용이 일정하게 유지됩니다.

## 시작하기

### 사전 요구 사항

- Android Studio Flamingo 이상
- JDK 17
- [gasleakdetector-server](https://github.com/gasleakdetector/gasleakdetector-server)의 실행 중인 인스턴스

### 빌드

```shell
git clone https://github.com/gasleakdetector/gasleakdetector.git
cd gasleakdetector
./gradlew assembleDebug
```

### 설정

앱에서 **설정**을 열고 입력합니다:

| 필드 | 설명 |
|---|---|
| API URL | Vercel 배포 URL, 예: `https://your-app.vercel.app` |
| API Key | Vercel 환경 변수에 설정한 `VALID_API_KEY` |
| Device ID | ESP가 전송하는 `device_id`, 예: `ESP_GASLEAK_01` (모든 기기를 포함하려면 이 필드를 비워두세요.) |

앱은 `/api/realtime-config`에서 Supabase 자격 증명을 자동으로 가져옵니다 — Supabase 키를 수동으로 입력할 필요가 없습니다.

## 다운로드

- **최신 알파 빌드**: [Actions](https://github.com/gasleakdetector/gasleakdetector/actions/)에서 다운로드
- **최신 안정 빌드**: [Releases](https://github.com/gasleakdetector/gasleakdetector/releases)에서 다운로드

[<img src="https://raw.githubusercontent.com/Kunzisoft/Github-badge/main/get-it-on-github.png" alt="Get it on GitHub" height="80">](https://github.com/gasleakdetector/gasleakdetector/releases/latest)

## 관련 저장소

| 저장소 | 설명 |
|---|---|
| [gasleakdetector-server](https://github.com/gasleakdetector/gasleakdetector-server) | Vercel 서버리스 API — 수집, 히스토리 쿼리, 통계, 이메일 알림 |
| [gasleakdetector-esp](https://github.com/gasleakdetector/gasleakdetector-esp) | ESP8266 펌웨어 — MQ-6 리더, WiFi 캡티브 포털, 오프라인 큐 |

## 라이선스

Apache 2.0 © [Gas Leak Detector](LICENSE)
