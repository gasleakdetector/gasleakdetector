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
    <a target="_blank" href="README-ja.md">日本語</a>&nbsp;&nbsp;|&nbsp;&nbsp;
    <a target="_blank" href="README-ko.md">한국어</a>&nbsp;&nbsp;|&nbsp;&nbsp;
    中文
  </p>
  <p align="center">面向 Android 的实时气体监测 - 由 ESP8266、Supabase 和无服务器边缘 API 驱动。</p>
  <img src=".github/assets/intro.png" width="19.2%" alt="intro" />    
  <img src=".github/assets/config.png" width="19.2%" alt="config" />    
  <img src=".github/assets/home.png" width="19.2%" alt="home" />    
  <img src=".github/assets/stats.png" width="19.2%" alt="stats" />    
  <br/>    
  <br/>    
</div>

## 概述

Gas Leak Detector 是一个全栈 IoT 安全系统。ESP8266 上的 MQ-6 传感器持续采样环境气体浓度，并将读数推送到无服务器 Vercel API。数据持久化在 Supabase 中，并通过 WebSocket 实时流式传输到 Android 应用 - 无轮询，无延迟。

系统由三个独立的代码仓库组成，形成一条完整的流水线：

| 层级 | 仓库 | 技术栈 |
|---|---|---|
| 固件 | [gasleakdetector-esp](https://github.com/gasleakdetector/gasleakdetector-esp) | C++ / Arduino / ESP8266 |
| 后端 | [gasleakdetector-server](https://github.com/gasleakdetector/gasleakdetector-server) | Node.js / Vercel / Supabase |
| 移动端 | **gasleakdetector** *(本仓库)* | Android / Java |

## 演示

[观看演示视频](https://www.youtube.com/watch?v=RLNf9Zphb1I)

## 完整项目配置

您可以在[这里](Tutorial/README-zh.md)查看完整的项目配置详细指南

## 系统流程

<p align="center">
  <img src=".github/assets/flow.png" alt="System flow" width="100%"/>
</p>

1. ESP8266 每 400 毫秒读取 MQ-6 传感器，并使用 API 密钥认证向 `/api/ingest` 发送 POST 请求。
2. Vercel 边缘函数对读数进行分类（`normal / warning / danger`）并写入 Supabase。达到 `danger` 状态时触发带可配置冷却时间的邮件告警。
3. Supabase 内的 `pg_cron` 自动将原始行聚合到按分钟和按小时的存储桶中 - 无需外部调度器。
4. Android 应用从 `/api/realtime-config` 获取 Supabase 凭据，并直接向 `gas_logs_raw` 开启 WebSocket 订阅，实现零延迟实时更新。
5. 历史图表从预聚合的小时存储桶读取，无论设备运行多久，查询速度始终保持快速。

## 功能特性

- [x] 带动画值和实时 WebSocket 更新的实时 PPM 仪表
- [x] 状态分类 - 正常 / 警告 / 危险，带颜色反馈
- [x] 气体浓度处于危险状态时持续显示危险通知
- [x] 历史图表 - 动态 Y 轴的每小时聚合数据
- [x] 多节点支持 - 通过 `device_id` 在 ESP 设备之间切换
- [x] 基于游标的分页 - 每次请求最多获取 1,000 行，支持 gzip 压缩
- [x] 离线弹性 - ESP 在本地最多排队 60 条读数；应用显示缓存数据
- [x] 首次运行介绍页面 - 显示一次，后续所有启动均跳过
- [x] 反馈 - 一键发送邮件，主题栏预填应用版本
- [x] 国际化 - 8 种语言：英语、越南语、德语、西班牙语、法语、日语、韩语、中文
- [ ] 桌面小部件
- [ ] 每设备多阈值配置
- [ ] 通过 FCM 推送通知

## 数据管理

本项目的核心是完全构建在 Supabase 内部的**三层存储流水线**，旨在处理高频传感器数据摄取，同时在任何规模下保持存储有界且查询快速。

**第 1 层 - `gas_logs_raw`**
每条传感器读数到达时写入此处。Supabase Realtime 通过 WebSocket 立即将每次插入广播到 Android 应用。该表写入密集且增长快速 - 单个设备以默认间隔发送每小时可产生数千行数据。

**第 2 层 - `gas_logs_minute`**
`pg_cron` 每分钟将原始行聚合到按分钟的存储桶中，计算每个设备的 `avg / min / max / sample_count`。状态采用最坏情况逻辑：存储桶中任何一条 `danger` 读数都会将整个存储桶标记为 `danger`。这是中间层 - 对于短期视图足够精细，对于快速查询足够小巧。

**第 3 层 - `gas_logs_hour`**
每小时，分钟存储桶汇总为小时存储桶。应用中的统计图表专门从此表读取 - 从不触及原始数据。无论设备运行多久，查询时间保持恒定。

**自动清理**
`pg_cron` 每天 03:00 UTC 运行日常清理，删除 `gas_logs_raw` 中超过 48 小时的所有 `normal` 行。此时每条读数已经被捕获到分钟和小时聚合中，因此不会丢失任何历史信息。`warning` 或 `danger` 状态的行将无限期保留用于审计目的。

没有这一机制，以每秒 5 条读数发送的设备每天会积累约 430,000 条原始行。有了清理机制，`gas_logs_raw` 仅保留最近 48 小时的正常读数 - 随着运行时间增长，存储成本保持平稳。

## 快速开始

### 前置要求

- Android Studio Flamingo 或更高版本
- JDK 17
- 一个运行中的 [gasleakdetector-server](https://github.com/gasleakdetector/gasleakdetector-server) 实例

### 构建

```shell
git clone https://github.com/gasleakdetector/gasleakdetector.git
cd gasleakdetector
./gradlew assembleDebug
```

### 配置

在应用中打开**设置**并填写：

| 字段 | 说明 |
|---|---|
| API URL | 您的 Vercel 部署 URL，例如 `https://your-app.vercel.app` |
| API Key | 在 Vercel 环境变量中设置的 `VALID_API_KEY` |
| Device ID | 您的 ESP 发送的 `device_id`，例如 `ESP_GASLEAK_01`（如果想包含所有设备，请将此字段留空。） |

应用自动从 `/api/realtime-config` 获取 Supabase 凭据 - 无需手动输入 Supabase 密钥。

## 下载

- **最新 Alpha 构建**：从 [Actions](https://github.com/gasleakdetector/gasleakdetector/actions/) 下载
- **最新稳定构建**：从 [Releases](https://github.com/gasleakdetector/gasleakdetector/releases) 下载

[<img src="https://raw.githubusercontent.com/Kunzisoft/Github-badge/main/get-it-on-github.png" alt="Get it on GitHub" height="80">](https://github.com/gasleakdetector/gasleakdetector/releases/latest)

## 相关仓库

| 仓库 | 说明 |
|---|---|
| [gasleakdetector-server](https://github.com/gasleakdetector/gasleakdetector-server) | Vercel 无服务器 API - 数据摄取、历史查询、统计、邮件告警 |
| [gasleakdetector-esp](https://github.com/gasleakdetector/gasleakdetector-esp) | ESP8266 固件 - MQ-6 读取器、WiFi 强制门户、离线队列 |

## 许可证

Apache 2.0 © [Gas Leak Detector](LICENSE)
