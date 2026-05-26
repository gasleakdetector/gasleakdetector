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
    <a target="_blank" href="README-de.md">Deutsch</a>&nbsp;&nbsp;|&nbsp;&nbsp;
    <a target="_blank" href="README-es.md">Español</a>&nbsp;&nbsp;|&nbsp;&nbsp;
    <a target="_blank" href="README-fr.md">Français</a>&nbsp;&nbsp;|&nbsp;&nbsp;
    日本語&nbsp;&nbsp;|&nbsp;&nbsp;
    <a target="_blank" href="README-ko.md">한국어</a>&nbsp;&nbsp;|&nbsp;&nbsp;
    <a target="_blank" href="README-zh.md">中文</a>
  </p>
  <p align="center">Android向けリアルタイムガス監視 - ESP8266、Supabase、サーバーレスエッジAPIによる実現。</p>
  <img src=".github/assets/intro.png" width="19.2%" alt="intro" />    
  <img src=".github/assets/config.png" width="19.2%" alt="config" />    
  <img src=".github/assets/home.png" width="19.2%" alt="home" />    
  <img src=".github/assets/stats.png" width="19.2%" alt="stats" />    
  <br/>    
  <br/>    
</div>

## 概要

Gas Leak DetectorはフルスタックのIoTセキュリティシステムです。ESP8266上のMQ-6センサーが周囲のガス濃度を継続的にサンプリングし、サーバーレスのVercel APIに測定値をプッシュします。データはSupabaseに保存され、WebSocketを通じてリアルタイムでAndroidアプリにストリーミングされます - ポーリングなし、遅延なし。

システムは1つのパイプラインを形成する3つの独立したリポジトリで構成されています：

| レイヤー | リポジトリ | スタック |
|---|---|---|
| ファームウェア | [gasleakdetector-esp](https://github.com/gasleakdetector/gasleakdetector-esp) | C++ / Arduino / ESP8266 |
| バックエンド | [gasleakdetector-server](https://github.com/gasleakdetector/gasleakdetector-server) | Node.js / Vercel / Supabase |
| モバイル | **gasleakdetector** *(このリポジトリ)* | Android / Java |

## デモ

[デモビデオを見る](https://www.youtube.com/watch?v=RLNf9Zphb1I)

## プロジェクト全体のセットアップ

プロジェクト全体のセットアップ方法についての詳細なガイドは[こちら](Tutorial/README-ja.md)をご覧ください

## システムフロー

<p align="center">
  <img src=".github/assets/flow.png" alt="System flow" width="100%"/>
</p>

1. ESP8266は400ミリ秒ごとにMQ-6センサーを読み取り、APIキー認証で`/api/ingest`にPOSTします。
2. Vercelエッジ関数が読み取り値を分類し（`normal / warning / danger`）、Supabaseに書き込みます。設定可能なクールダウンで`danger`時にメールアラートが送信されます。
3. Supabase内の`pg_cron`が自動的に生データ行を分単位・時間単位のバケットに集約します - 外部スケジューラーは不要です。
4. AndroidアプリはSupabase認証情報を`/api/realtime-config`から取得し、`gas_logs_raw`に直接WebSocketサブスクリプションを開いてゼロレイテンシのライブ更新を実現します。
5. 履歴チャートは事前集約された時間バケットから読み取り、デバイスの稼働時間に関わらず高速なクエリを維持します。

## 機能

- [x] アニメーション値とリアルタイムWebSocket更新を持つライブPPMゲージ
- [x] ステータス分類 - 正常 / 警告 / 危険のカラーフィードバック付き
- [x] ガスレベルが危険な状態の間持続する危険通知
- [x] 履歴チャート - 動的Y軸を持つ時間単位集約データ
- [x] マルチノードサポート - `device_id`でESPデバイス間を切り替え
- [x] カーソルベースのページネーション - gzip圧縮でリクエストごとに最大1,000行を取得
- [x] オフライン耐性 - ESPはローカルに最大60件の読み取りをキューイング；アプリはキャッシュデータを表示
- [x] 初回起動イントロ画面 - 一度だけ表示、以降すべての起動でスキップ
- [x] フィードバック - アプリバージョンが件名に事前入力されたワンタップメール
- [x] 国際化 - 8言語：英語、ベトナム語、ドイツ語、スペイン語、フランス語、日本語、韓国語、中国語
- [ ] ウィジェット
- [ ] デバイスごとのマルチ閾値設定
- [ ] FCM経由のプッシュ通知

## データ管理

このプロジェクトの核心は、Supabase内に完全に構築された**3層ストレージパイプライン**です。高頻度センサーの取り込みを処理しながら、あらゆる規模でストレージを制限し、クエリを高速に保つように設計されています。

**第1層 - `gas_logs_raw`**
各センサー読み取り値は到着時にここに書き込まれます。Supabase RealtimeはWebSocketを通じて各挿入をAndroidアプリに即座にブロードキャストします。このテーブルは書き込みが多く急速に成長します - デフォルト間隔で送信する単一デバイスは1時間に数千行を生成します。

**第2層 - `gas_logs_minute`**
`pg_cron`は毎分、生データ行を分単位バケットに集約し、デバイスごとに`avg / min / max / sample_count`を計算します。ステータスはワーストケースロジックを使用します：バケット内のどこかに1つでも`danger`読み取りがあると、バケット全体が`danger`とマークされます。これは中間層 - 短期ビューには十分に詳細で、高速クエリには十分に小さいです。

**第3層 - `gas_logs_hour`**
毎時間、分バケットが時間バケットに統合されます。アプリの統計チャートは専らこのテーブルから読み取ります - 生データには触れません。クエリ時間はデバイスの稼働時間に関わらず一定です。

**自動クリーンアップ**
`pg_cron`は毎日03:00 UTCにパージを実行し、48時間以上前の`gas_logs_raw`のすべての`normal`行を削除します。その時点でデータは分・時間単位集約に取り込まれているため、履歴情報は失われません。`warning`または`danger`ステータスの行は監査目的で無期限に保持されます。

これがなければ、5読み取り/秒で送信するデバイスは1日あたり約430,000件の生データ行を蓄積します。パージがあれば、`gas_logs_raw`は直近48時間の正常読み取りに制限され、稼働時間が増えてもストレージコストは一定に保たれます。

## はじめに

### 前提条件

- Android Studio Flamingo以降
- JDK 17
- [gasleakdetector-server](https://github.com/gasleakdetector/gasleakdetector-server)の実行中インスタンス

### ビルド

```shell
git clone https://github.com/gasleakdetector/gasleakdetector.git
cd gasleakdetector
./gradlew assembleDebug
```

### 設定

アプリの**設定**を開いて入力します：

| フィールド | 説明 |
|---|---|
| API URL | VercelデプロイメントURL、例：`https://your-app.vercel.app` |
| API Key | Vercel環境変数に設定した`VALID_API_KEY` |
| Device ID | ESPが送信している`device_id`、例：`ESP_GASLEAK_01`（すべてのデバイスを含める場合はこのフィールドを空白のままにしてください。） |

アプリは`/api/realtime-config`からSupabase認証情報を自動的に取得します - Supabaseキーを手動で入力する必要はありません。

## ダウンロード

- **最新アルファビルド**：[Actions](https://github.com/gasleakdetector/gasleakdetector/actions/)からダウンロード
- **最新安定ビルド**：[Releases](https://github.com/gasleakdetector/gasleakdetector/releases)からダウンロード

[<img src="https://raw.githubusercontent.com/Kunzisoft/Github-badge/main/get-it-on-github.png" alt="Get it on GitHub" height="80">](https://github.com/gasleakdetector/gasleakdetector/releases/latest)

## 関連リポジトリ

| リポジトリ | 説明 |
|---|---|
| [gasleakdetector-server](https://github.com/gasleakdetector/gasleakdetector-server) | Vercelサーバーレスレス API - 取り込み、履歴クエリ、統計、メールアラート |
| [gasleakdetector-esp](https://github.com/gasleakdetector/gasleakdetector-esp) | ESP8266ファームウェア - MQ-6リーダー、WiFiキャプティブポータル、オフラインキュー |

## ライセンス

Apache 2.0 © [Gas Leak Detector](LICENSE)
