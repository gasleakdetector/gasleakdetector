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
    Tiếng Việt&nbsp;&nbsp;|&nbsp;&nbsp;
    <a target="_blank" href="README-de.md">Deutsch</a>&nbsp;&nbsp;|&nbsp;&nbsp;
    <a target="_blank" href="README-es.md">Español</a>&nbsp;&nbsp;|&nbsp;&nbsp;
    <a target="_blank" href="README-fr.md">Français</a>&nbsp;&nbsp;|&nbsp;&nbsp;
    <a target="_blank" href="README-ja.md">日本語</a>&nbsp;&nbsp;|&nbsp;&nbsp;
    <a target="_blank" href="README-ko.md">한국어</a>&nbsp;&nbsp;|&nbsp;&nbsp;
    <a target="_blank" href="README-zh.md">中文</a>
  </p>
  <p align="center">Giám sát khí gas thời gian thực cho Android — được hỗ trợ bởi ESP8266, Supabase và serverless edge API.</p>
  <img src=".github/assets/intro.png" width="19.2%" alt="intro" />    
  <img src=".github/assets/config.png" width="19.2%" alt="config" />    
  <img src=".github/assets/home.png" width="19.2%" alt="home" />    
  <img src=".github/assets/stats.png" width="19.2%" alt="stats" />    
  <br/>    
  <br/>    
</div>

## Tổng Quan

Gas Leak Detector là một hệ thống an toàn IoT toàn diện. Cảm biến MQ-6 trên ESP8266 liên tục đo nồng độ khí gas xung quanh và đẩy dữ liệu lên Vercel API serverless. Dữ liệu được lưu trữ trong Supabase và truyền trực tiếp đến ứng dụng Android qua WebSocket — không có polling, không có độ trễ.

Hệ thống gồm ba repository độc lập tạo thành một pipeline:

| Tầng | Repository | Công nghệ |
|---|---|---|
| Firmware | [gasleakdetector-esp](https://github.com/gasleakdetector/gasleakdetector-esp) | C++ / Arduino / ESP8266 |
| Backend | [gasleakdetector-server](https://github.com/gasleakdetector/gasleakdetector-server) | Node.js / Vercel / Supabase |
| Mobile | **gasleakdetector** *(repo này)* | Android / Java |

## Demo

[Xem Video Demo](https://www.youtube.com/watch?v=RLNf9Zphb1I)

## Cài Đặt Toàn Bộ Dự Án

Bạn có thể xem hướng dẫn chi tiết cách cài đặt toàn bộ dự án [tại đây](Tutorial/README-vi.md)

## Luồng Hoạt Động

<p align="center">
  <img src=".github/assets/flow.png" alt="System flow" width="100%"/>
</p>

1. ESP8266 đọc cảm biến MQ-6 mỗi 400 ms và gửi POST tới `/api/ingest` với xác thực API key.
2. Vercel edge function phân loại giá trị đọc được (`normal / warning / danger`) và ghi vào Supabase. Cảnh báo email được gửi khi trạng thái là `danger` với thời gian chờ có thể cấu hình.
3. `pg_cron` trong Supabase tự động tổng hợp các hàng thô thành các bucket theo phút và theo giờ — không cần scheduler bên ngoài.
4. Ứng dụng Android lấy thông tin xác thực Supabase từ `/api/realtime-config` và mở kết nối WebSocket trực tiếp tới `gas_logs_raw` để cập nhật trực tiếp không độ trễ.
5. Biểu đồ lịch sử đọc từ các bucket giờ đã được tổng hợp sẵn, giữ cho truy vấn nhanh bất kể thiết bị đã chạy bao lâu.

## Tính Năng

- [x] Đồng hồ PPM trực tiếp với giá trị động và cập nhật WebSocket thời gian thực
- [x] Phân loại trạng thái — Bình thường / Cảnh báo / Nguy hiểm với phản hồi màu sắc
- [x] Thông báo nguy hiểm liên tục khi mức khí gas vẫn còn nguy hiểm
- [x] Biểu đồ lịch sử — dữ liệu tổng hợp theo giờ với trục Y động
- [x] Hỗ trợ đa node — chuyển đổi giữa các thiết bị ESP theo `device_id`
- [x] Phân trang dựa trên cursor — lấy tối đa 1.000 hàng mỗi lần với nén gzip
- [x] Khả năng chịu lỗi offline — ESP lưu tối đa 60 lần đọc cục bộ; ứng dụng hiển thị dữ liệu đã cache
- [x] Màn hình giới thiệu lần đầu — hiển thị một lần, bỏ qua trong tất cả các lần khởi chạy sau
- [x] Phản hồi — email một chạm được điền sẵn phiên bản ứng dụng trong tiêu đề
- [x] Quốc tế hóa — 8 ngôn ngữ: Tiếng Anh, Tiếng Việt, Tiếng Đức, Tiếng Tây Ban Nha, Tiếng Pháp, Tiếng Nhật, Tiếng Hàn, Tiếng Trung
- [ ] Widget
- [ ] Cấu hình đa ngưỡng cho mỗi thiết bị
- [ ] Thông báo đẩy qua FCM

## Quản Lý Dữ Liệu

Cốt lõi của dự án này là một **pipeline lưu trữ ba tầng** được xây dựng hoàn toàn trong Supabase, được thiết kế để xử lý việc nhập cảm biến tần suất cao trong khi giữ bộ nhớ có giới hạn và truy vấn nhanh ở bất kỳ quy mô nào.

**Tầng 1 — `gas_logs_raw`**
Mỗi lần đọc cảm biến được ghi ở đây khi đến. Supabase Realtime phát sóng mỗi lần chèn qua WebSocket đến ứng dụng Android ngay lập tức. Bảng này ghi nhiều và phát triển nhanh — một thiết bị gửi ở khoảng thời gian mặc định tạo ra hàng nghìn hàng mỗi giờ.

**Tầng 2 — `gas_logs_minute`**
`pg_cron` tổng hợp các hàng thô thành các bucket theo phút mỗi phút, tính toán `avg / min / max / sample_count` cho mỗi thiết bị. Trạng thái sử dụng logic trường hợp xấu nhất: một lần đọc `danger` bất kỳ trong bucket đánh dấu toàn bộ bucket là `danger`. Đây là lớp trung gian — đủ chi tiết cho các chế độ xem ngắn hạn, đủ nhỏ để truy vấn nhanh.

**Tầng 3 — `gas_logs_hour`**
Mỗi giờ, các bucket phút được tổng hợp thành các bucket giờ. Biểu đồ thống kê trong ứng dụng đọc độc quyền từ bảng này — nó không bao giờ chạm vào dữ liệu thô. Thời gian truy vấn không đổi bất kể thiết bị đã chạy bao lâu.

**Tự động dọn dẹp**
`pg_cron` chạy xóa hàng ngày lúc 03:00 UTC xóa tất cả các hàng `normal` từ `gas_logs_raw` cũ hơn 48 giờ. Tại thời điểm đó, mọi lần đọc đã được ghi lại trong các tổng hợp phút và giờ, vì vậy không có thông tin lịch sử nào bị mất. Các hàng có trạng thái `warning` hoặc `danger` được giữ vô thời hạn cho mục đích kiểm tra.

Nếu không có điều này, một thiết bị gửi 5 lần đọc/giây sẽ tích lũy ~430.000 hàng thô mỗi ngày. Với việc xóa, `gas_logs_raw` vẫn giới hạn ở khoảng 48 giờ cuối của các lần đọc bình thường — chi phí lưu trữ vẫn ổn định khi thời gian hoạt động tăng.

## Bắt Đầu

### Yêu Cầu

- Android Studio Flamingo trở lên
- JDK 17
- Một phiên bản đang chạy của [gasleakdetector-server](https://github.com/gasleakdetector/gasleakdetector-server)

### Build

```shell
git clone https://github.com/gasleakdetector/gasleakdetector.git
cd gasleakdetector
./gradlew assembleDebug
```

### Cấu Hình

Mở **Cài đặt** trong ứng dụng và điền:

| Trường | Mô tả |
|---|---|
| API URL | URL triển khai Vercel của bạn, ví dụ: `https://your-app.vercel.app` |
| API Key | `VALID_API_KEY` được đặt trong biến môi trường Vercel của bạn |
| Device ID | `device_id` mà ESP của bạn đang gửi, ví dụ: `ESP_GASLEAK_01` (Để trống trường này nếu bạn muốn bao gồm tất cả thiết bị.) |

Ứng dụng tự động lấy thông tin xác thực Supabase từ `/api/realtime-config` — không cần nhập khóa Supabase thủ công.

## Tải Xuống

- **Bản Alpha Mới Nhất**: Tải từ [Actions](https://github.com/gasleakdetector/gasleakdetector/actions/)
- **Bản Ổn Định Mới Nhất**: Tải từ [Releases](https://github.com/gasleakdetector/gasleakdetector/releases)

[<img src="https://raw.githubusercontent.com/Kunzisoft/Github-badge/main/get-it-on-github.png" alt="Get it on GitHub" height="80">](https://github.com/gasleakdetector/gasleakdetector/releases/latest)

## Các Repository Liên Quan

| Repository | Mô tả |
|---|---|
| [gasleakdetector-server](https://github.com/gasleakdetector/gasleakdetector-server) | Vercel serverless API — nhập dữ liệu, truy vấn lịch sử, thống kê, cảnh báo email |
| [gasleakdetector-esp](https://github.com/gasleakdetector/gasleakdetector-esp) | Firmware ESP8266 — đọc MQ-6, captive portal WiFi, hàng đợi offline |

## Giấy Phép

Apache 2.0 © [Gas Leak Detector](LICENSE)
