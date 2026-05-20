# 配置指南

Gas Leak Detector 项目的完整配置指南。本文档按照配置顺序介绍全部四个组件——服务器后端、Supabase、ESP8266 固件和 Android 应用。

<div align="center">
	<p><a target="_blank" href="README.md">English</a>&nbsp;&nbsp;|&nbsp;&nbsp;
	    <a target="_blank" href="README-vi.md">Tiếng Việt</a>&nbsp;&nbsp;|&nbsp;&nbsp;
	    <a target="_blank" href="README-de.md">Deutsch</a>&nbsp;&nbsp;|&nbsp;&nbsp;
	    <a target="_blank" href="README-es.md">Español</a>&nbsp;&nbsp;|&nbsp;&nbsp;
	    <a target="_blank" href="README-fr.md">Français</a>&nbsp;&nbsp;|&nbsp;&nbsp;
	    <a target="_blank" href="README-ja.md">日本語</a>&nbsp;&nbsp;|&nbsp;&nbsp;
	    <a target="_blank" href="README-ko.md">한국어</a>&nbsp;&nbsp;|&nbsp;&nbsp;
	    中文
</div>

## 前提条件

### 硬件

| 组件 | 说明 |
|------|------|
| ESP8266 | NodeMCU 或同等产品 |
| MQ-6 气体传感器 | 液化石油气 / 丙烷检测 |
| OLED SSD1306 0.96" | 可选 — 用于设备端显示 |
| 蜂鸣器 | 有源或无源 |

### 账号

- [Vercel](https://vercel.com) — 无服务器部署
- [Supabase](https://supabase.com) — 数据库与实时功能
- [Resend](https://resend.com) — 邮件提醒（可选）

---

## 1. 服务器后端

### 部署到 Vercel

**第 1 步。** 进入 [gasleakdetector-server](https://github.com/gasleakdetector/gasleakdetector-server) 仓库，点击 **Deploy** 按钮。

![部署到 Vercel](../images/vercel_deploy_button.jpg)

**第 2 步。** 在 Vercel 控制台中打开你的项目，进入 **Settings**。

![Vercel 项目设置](../images/vercel_project_settings.jpg)

**第 3 步。** 导航到 **Environment Variables**。

![Environment Variables 标签](../images/vercel_environment_variables.jpg)

**第 4 步。** 点击 **Add Environment Variable**。

![添加环境变量](../images/vercel_add_variable.jpg)

**第 5 步。** 逐一添加以下变量：

![填写变量值](../images/vercel_fill_variables.jpg)

| 变量 | 说明 |
|------|------|
| `SUPABASE_URL` | 你的 Supabase 项目 URL |
| `SUPABASE_ANON_KEY` | Supabase anonymous key — Android 应用 WebSocket 使用 |
| `SUPABASE_SERVICE_KEY` | Supabase service role key — 所有服务端写入操作使用 |
| `VALID_API_KEY` | ESP 和应用通过 `x-api-key` 请求头发送的共享密钥 |
| `RESEND_API_KEY` | 用于发送邮件提醒的 Resend API key |
| `ALERT_EMAIL` | 接收危险级别提醒的邮箱地址 |
| `DANGER_THRESHOLD` | 触发 `danger` 状态的 PPM 值。MQ-6 推荐：`800` |
| `WARNING_THRESHOLD` | 触发 `warning` 状态的 PPM 值。MQ-6 推荐：`300` |
| `EMAIL_COOLDOWN_MINUTES` | 重复发送提醒邮件的最小间隔（分钟）。默认：`2` |

### API Key

`VALID_API_KEY` 是你自己定义的密钥——在服务器、ESP 固件和 Android 应用之间共享。无需注册或使用任何第三方服务。

推荐：8–10 位字母数字组合。示例：`Abc12345`

### Resend（邮件提醒）

本节为可选内容。如无需邮件提醒，可跳过。

**第 1 步。** 登录或在 [resend.com](https://resend.com/login) 创建账号。

**第 2 步。** 进入 **API Keys**，点击 **Create API Key**。

![Resend API Keys 页面](../images/resend_api_keys_page.jpg)

**第 3 步。** 填写 key 名称，点击 **Add**。

![创建 API Key 表单](../images/resend_create_api_key.jpg)

**第 4 步。** 复制生成的 key。

![复制 key](../images/resend_copy_key.jpg)

将其赋值给 Vercel 中的 `RESEND_API_KEY`。将 `ALERT_EMAIL` 设置为接收提醒的邮箱地址（例如 `you@gmail.com`）。

---

## 2. Supabase

**第 1 步。** 在 Supabase 创建新项目。

**第 2 步。** 打开 SQL 编辑器，粘贴 [`supabase/schema.sql`](https://github.com/gasleakdetector/gasleakdetector-server/blob/main/supabase/schema.sql) 的内容并执行一次。

![Supabase SQL 编辑器](../images/supabase_sql_editor.jpg)

这将一次性创建所有表（`gas_logs_raw`、`gas_logs_minute`、`gas_logs_hour`、`devices`）、聚合函数、`pg_cron` 任务和行级安全策略。

**第 3 步。** 获取 API 凭证。进入 **Settings > API Keys > Legacy anon, service_role API keys**，复制 `anon` 和 `service_role` 两个密钥。

![Supabase API Keys](../images/supabase_api_keys.jpg)

将其赋值给 Vercel 中的 `SUPABASE_ANON_KEY` 和 `SUPABASE_SERVICE_KEY`。`SUPABASE_URL` 可在 **Project Overview** 中找到。

---

## 3. ESP8266 固件

### 接线

在通电前，按照下表将所有组件连接到 ESP8266。

**MQ-6 气体传感器**

| MQ-6 | ESP8266 |
|------|---------|
| VCC | VIN (5V) |
| GND | GND |
| AO | A0 |

**OLED SSD1306（可选）**

| OLED | ESP8266 |
|------|---------|
| VCC | 3.3V |
| GND | GND |
| SCL | D1 / GPIO5 |
| SDA | D2 / GPIO4 |

**蜂鸣器**

| 蜂鸣器 | ESP8266 |
|--------|---------|
| + | D5 / GPIO14 |
| − | GND |

### 烧录固件

从 [gasleakdetector-esp releases](https://github.com/gasleakdetector/gasleakdetector-esp/releases) 页面下载最新的 `.bin` 文件，使用你偏好的工具（esptool、Arduino IDE 或 ESP Flash Download Tool）烧录到 ESP8266。

烧录流程不在此详细介绍——请参阅固件仓库的说明。

### 首次配置

**第 1 步。** 给 ESP8266 上电。它会广播一个 Wi-Fi 热点，连接到该热点。

![ESP Wi-Fi 热点](../images/esp_wifi_ap.jpg)

**第 2 步。** 打开浏览器，访问 `http://192.168.4.1`。大多数设备会自动弹出强制门户。输入你的凭证：

- **SSID / 密码** — 家庭 Wi-Fi 凭证
- **API KEY** — 在 Vercel 中配置的 `VALID_API_KEY`

![ESP 强制门户](../images/esp_captive_portal.jpg)

**第 3 步。** 进入 **Settings**（右上角）。在 **API Host** 中输入你的 Vercel 部署 URL（例如 `https://your-app.vercel.app`），然后点击 **Save Config**。

![ESP 配置表单](../images/esp_config_form.jpg)

**第 4 步。** 重置 ESP 以应用新配置。设备将连接到你的 Wi-Fi 并开始发送数据。

---

## 4. Android 应用

### 安装

从[发布页面](https://github.com/gasleakdetector/gasleakdetector/releases/latest)下载最新 APK 并安装到你的设备。

### 配置

**第 1 步。** 打开应用，点击主屏幕右上角的铅笔图标。

![编辑配置按钮](../images/app_edit_config.jpg)

**第 2 步。** 填写你的设置：

![应用配置表单](../images/app_config_form.jpg)

| 字段 | 值 |
|------|-----|
| API URL | 你的 Vercel 部署 URL |
| API Key | 你定义的 `VALID_API_KEY` |
| Device ID | ESP 的 `device_id`（例如 `ESP_GASLEAK_01`）。留空则监控所有设备。 |

点击 **Save**。应用将连接并开始显示实时读数。

---

> ❕ 当前稳定版本仅支持**应用在后台运行时**发送推送提醒。
> 
> 如果你希望在**应用完全关闭的情况下也能收到通知**，可以参考以下开发分支：
> 
> * [Android FCM 分支](https://github.com/gasleakdetector/gasleakdetector/tree/feature/fcm-push-notification)
> * [服务端 FCM 支持分支](https://github.com/gasleakdetector/gasleakdetector-server/tree/feature/fcm-push-notification)
> 
> **注意：** 由于安全和服务配置原因（需要独立的 `google-services.json` 文件），这些分支无法合并到主分支。该功能仍在开发中，如果暂时用不到可以忽略此提示 😊。

---

## 故障排除

**ESP 无法连接 Wi-Fi** — 在强制门户中仔细检查 SSID 和密码。长按按钮 5 秒可恢复出厂设置并重新配置。

**应用无数据显示** — 检查 API URL 和 API Key 是否与 Vercel 中的设置完全一致。查看 Vercel 函数日志以排查错误。

**没有邮件提醒** — 确认 `RESEND_API_KEY` 和 `ALERT_EMAIL` 已在 Vercel 中设置。只有当状态达到 `danger` 且冷却时间窗口已过时，才会触发提醒。

**Supabase schema 错误** — 确保 SQL 在正确的项目中执行，且 `pg_cron` 扩展已启用（Supabase 在付费计划中默认启用；免费计划可能需要手动激活）。

---

*如有问题，请提交 [GitHub Issue](https://github.com/gasleakdetector/gasleakdetector/issues) 或联系 [pan2512811@gmail.com](mailto:pan2512811@gmail.com)。欢迎贡献代码 😊*
