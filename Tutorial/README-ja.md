# セットアップガイド

Gas Leak Detector プロジェクトの完全なセットアップガイドです。このドキュメントでは、サーバーバックエンド、Supabase、ESP8266 ファームウェア、Android アプリの4つのコンポーネントについて、設定する順番通りに説明します。

<div align="center">
	<p><a target="_blank" href="README.md">English</a>&nbsp;&nbsp;|&nbsp;&nbsp;
	    <a target="_blank" href="README-vi.md">Tiếng Việt</a>&nbsp;&nbsp;|&nbsp;&nbsp;
	    <a target="_blank" href="README-de.md">Deutsch</a>&nbsp;&nbsp;|&nbsp;&nbsp;
	    <a target="_blank" href="README-es.md">Español</a>&nbsp;&nbsp;|&nbsp;&nbsp;
	    <a target="_blank" href="README-fr.md">Français</a>&nbsp;&nbsp;|&nbsp;&nbsp;
	    日本語&nbsp;&nbsp;|&nbsp;&nbsp;
	    <a target="_blank" href="README-ko.md">한국어</a>&nbsp;&nbsp;|&nbsp;&nbsp;
	    <a target="_blank" href="README-zh.md">中文</a>
</div>

## 必要なもの

### ハードウェア

| コンポーネント | メモ |
|--------------|------|
| ESP8266 | NodeMCU または同等品 |
| MQ-6 ガスセンサー | LPG / プロパン検知 |
| OLED SSD1306 0.96" | オプション - デバイス上での表示用 |
| ブザー | アクティブまたはパッシブ |

### アカウント

- [Vercel](https://vercel.com) - サーバーレスデプロイ
- [Supabase](https://supabase.com) - データベースとリアルタイム
- [Resend](https://resend.com) - メールアラート（オプション）

---

## 1. サーバーバックエンド

### Vercel へのデプロイ

**ステップ 1.** [gasleakdetector-server](https://github.com/gasleakdetector/gasleakdetector-server) リポジトリにアクセスし、**Deploy** ボタンをクリックします。

![Vercel へデプロイ](../images/vercel_deploy_button.jpg)

**ステップ 2.** Vercel ダッシュボードでプロジェクトを開き、**Settings** に移動します。

![Vercel プロジェクト設定](../images/vercel_project_settings.jpg)

**ステップ 3.** **Environment Variables** に移動します。

![Environment Variables タブ](../images/vercel_environment_variables.jpg)

**ステップ 4.** **Add Environment Variable** をクリックします。

![環境変数を追加](../images/vercel_add_variable.jpg)

**ステップ 5.** 以下の各変数を追加します：

![変数の値を入力](../images/vercel_fill_variables.jpg)

| 変数 | 説明 |
|------|------|
| `SUPABASE_URL` | Supabase プロジェクトの URL |
| `SUPABASE_ANON_KEY` | Supabase anonymous key - Android アプリの WebSocket で使用 |
| `SUPABASE_SERVICE_KEY` | Supabase service role key - サーバー側の書き込みすべてに使用 |
| `VALID_API_KEY` | ESP とアプリから `x-api-key` ヘッダーで送信される共有シークレット |
| `RESEND_API_KEY` | メールアラート用 Resend API key |
| `ALERT_EMAIL` | 危険レベルアラートの送信先メールアドレス |
| `DANGER_THRESHOLD` | `danger` ステータスをトリガーする PPM レベル。MQ-6 推奨値：`800` |
| `WARNING_THRESHOLD` | `warning` ステータスをトリガーする PPM レベル。MQ-6 推奨値：`300` |
| `EMAIL_COOLDOWN_MINUTES` | アラートメールの送信間隔（最小分数）。デフォルト：`2` |

### API キー

`VALID_API_KEY` は自分で定義するシークレットです - サーバー、ESP ファームウェア、Android アプリ間で共有されます。登録やサードパーティサービスは必要ありません。

推奨：英数字 8〜10 文字。例：`Abc12345`

### Resend（メールアラート）

このセクションはオプションです。メールアラートが不要な場合はスキップしてください。

**ステップ 1.** [resend.com](https://resend.com/login) でログインまたはアカウントを作成します。

**ステップ 2.** **API Keys** に移動し、**Create API Key** をクリックします。

![Resend API Keys ページ](../images/resend_api_keys_page.jpg)

**ステップ 3.** キー名を入力し、**Add** をクリックします。

![API Key 作成フォーム](../images/resend_create_api_key.jpg)

**ステップ 4.** 生成されたキーをコピーします。

![キーをコピー](../images/resend_copy_key.jpg)

Vercel の `RESEND_API_KEY` に割り当てます。`ALERT_EMAIL` をアラートを受信するアドレス（例：`you@gmail.com`）に設定します。

---

## 2. Supabase

**ステップ 1.** Supabase に新しいプロジェクトを作成します。

**ステップ 2.** SQL エディターを開き、[`supabase/schema.sql`](https://github.com/gasleakdetector/gasleakdetector-server/blob/main/supabase/schema.sql) の内容を貼り付けて、一度だけ実行します。

![Supabase SQL エディター](../images/supabase_sql_editor.jpg)

これにより、すべてのテーブル（`gas_logs_raw`、`gas_logs_minute`、`gas_logs_hour`、`devices`）、集約関数、`pg_cron` ジョブ、Row-Level Security ポリシーが一度に作成されます。

**ステップ 3.** API 認証情報を取得します。**Settings > API Keys > Legacy anon, service_role API keys** に移動し、`anon` と `service_role` の両方のキーをコピーします。

![Supabase API Keys](../images/supabase_api_keys.jpg)

Vercel の `SUPABASE_ANON_KEY` と `SUPABASE_SERVICE_KEY` に割り当てます。`SUPABASE_URL` は **Project Overview** で確認できます。

---

## 3. ESP8266 ファームウェア

### 配線

電源を入れる前に、以下の表に従ってすべてのコンポーネントを ESP8266 に接続してください。

**MQ-6 ガスセンサー**

| MQ-6 | ESP8266 |
|------|---------|
| VCC | VIN (5V) |
| GND | GND |
| AO | A0 |

**OLED SSD1306（オプション）**

| OLED | ESP8266 |
|------|---------|
| VCC | 3.3V |
| GND | GND |
| SCL | D1 / GPIO5 |
| SDA | D2 / GPIO4 |

**ブザー**

| ブザー | ESP8266 |
|--------|---------|
| + | D5 / GPIO14 |
| − | GND |

### ファームウェアの書き込み

[gasleakdetector-esp releases](https://github.com/gasleakdetector/gasleakdetector-esp/releases) ページから最新の `.bin` ファイルをダウンロードし、お好みのツール（esptool、Arduino IDE、または ESP Flash Download Tool）を使用して ESP8266 に書き込みます。

書き込み手順の詳細はここでは説明しません - ファームウェアリポジトリの手順を参照してください。

### 初回設定

**ステップ 1.** ESP8266 の電源を入れます。Wi-Fi アクセスポイントをブロードキャストします。それに接続します。

![ESP Wi-Fi アクセスポイント](../images/esp_wifi_ap.jpg)

**ステップ 2.** ブラウザを開き、`http://192.168.4.1` にアクセスします。キャプティブポータルはほとんどのデバイスで自動的に開きます。認証情報を入力します：

- **SSID / パスワード** - 自宅の Wi-Fi 認証情報
- **API KEY** - Vercel で設定した `VALID_API_KEY`

![ESP キャプティブポータル](../images/esp_captive_portal.jpg)

**ステップ 3.** **Settings**（右上）に移動します。**API Host** に Vercel デプロイ URL（例：`https://your-app.vercel.app`）を入力し、**Save Config** をクリックします。

![ESP 設定フォーム](../images/esp_config_form.jpg)

**ステップ 4.** ESP をリセットして新しい設定を適用します。デバイスは Wi-Fi に接続し、データの送信を開始します。

---

## 4. Android アプリ

### インストール

[リリースページ](https://github.com/gasleakdetector/gasleakdetector/releases/latest) から最新の APK をダウンロードし、デバイスにインストールします。

### 設定

**ステップ 1.** アプリを開き、ホーム画面の右上にある鉛筆アイコンをタップします。

![設定編集ボタン](../images/app_edit_config.jpg)

**ステップ 2.** 設定を入力します：

![アプリ設定フォーム](../images/app_config_form.jpg)

| フィールド | 値 |
|-----------|-----|
| API URL | Vercel デプロイ URL |
| API Key | 定義した `VALID_API_KEY` |
| Device ID | ESP の `device_id`（例：`ESP_GASLEAK_01`）。すべてのデバイスを監視する場合は空白のままにしてください。 |

**Save** をタップします。アプリが接続し、リアルタイムの読み取り値を表示し始めます。

---

> ❕ 現在の安定版では、**アプリがバックグラウンドで動作しているときのみ**プッシュ通知に対応しています。
> 
> **アプリを完全に閉じた状態でも通知を受け取りたい**場合は、以下の開発ブランチを参照してください：
> 
> * [FCM対応 Androidブランチ](https://github.com/gasleakdetector/gasleakdetector/tree/feature/fcm-push-notification)
> * [FCM対応 サーバーブランチ](https://github.com/gasleakdetector/gasleakdetector-server/tree/feature/fcm-push-notification)
> 
> **注意：** セキュリティおよびサービス設定上の理由（独自の `google-services.json` が必要）から、これらのブランチはメインブランチにマージできません。この機能は現在も開発中です。必要なければ、このメッセージは無視して構いません 😊。

---

## トラブルシューティング

**ESP が Wi-Fi に接続できない** - キャプティブポータルで SSID とパスワードを再確認してください。ボタンを5秒間押し続けると工場出荷時にリセットされ、再設定できます。

**アプリにデータが表示されない** - API URL と API Key が Vercel の設定と完全に一致しているか確認してください。エラーを確認するには Vercel の関数ログを確認してください。

**メールアラートが届かない** - `RESEND_API_KEY` と `ALERT_EMAIL` が Vercel に設定されていることを確認してください。アラートはステータスが `danger` に達し、クールダウン期間が過ぎた場合にのみ送信されます。

**Supabase スキーマエラー** - SQL が正しいプロジェクトで実行され、`pg_cron` 拡張機能が有効になっていることを確認してください（Supabase は有料プランでデフォルトで有効にしています。無料プランでは手動で有効化が必要な場合があります）。

---

*ご質問や問題がある場合は、[GitHub issue](https://github.com/gasleakdetector/gasleakdetector/issues) を開くか、[pan2512811@gmail.com](mailto:pan2512811@gmail.com) にお問い合わせください。貢献を歓迎します 😊*
