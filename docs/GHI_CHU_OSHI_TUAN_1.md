# Ghi chú khảo sát OSHI & NetworkInterface — Nguyễn Quang Huy (Tuần 1)

## Các hàm sẽ dùng

| Lớp / hàm | Mục đích | Ghi chú |
|---|---|---|
| `new SystemInfo().getHardware()` | Lấy `HardwareAbstractionLayer` | Tạo một lần, dùng lại |
| `HardwareAbstractionLayer#getNetworkIFs()` | Danh sách card mạng (`List<NetworkIF>`) | Gọi lại để phát hiện card thêm/mất |
| `NetworkIF#getName()` / `getDisplayName()` | Mã / tên card | Dùng `getName()` làm khóa |
| `NetworkIF#updateAttributes()` | Làm mới số liệu | **Bắt buộc gọi trước mỗi lần đọc**; trả `false` nếu lỗi |
| `NetworkIF#getBytesRecv()` / `getBytesSent()` | Tổng byte nhận / gửi (counter tích lũy) | Không phải tốc độ |
| `NetworkIF#getSpeed()` | Tốc độ danh định (bit/s) | Có thể = 0 (card ảo/ngắt) |
| `java.net.NetworkInterface#getNetworkInterfaces()` | Đối chiếu danh sách card, `isUp()`, `isLoopback()` | Không có counter byte → chỉ để lọc/đối chiếu |

## Công thức

- `bytesPerSec = (bytes2 - bytes1) * 1000 / elapsedMillis` (thời gian đo bằng `System.nanoTime()`).
- `usage% = 8 * (recv + sent) / speed * 100`, chỉ tính khi `speed > 0`, giới hạn trong [0, 100].
- Bỏ mẫu và lấy lại mốc khi: counter giảm, thời gian không tăng, `updateAttributes()` lỗi, card biến mất/đổi ID.

## Quy ước
- Không cập nhật Swing trong thread lấy mẫu; listener nhận sự kiện trên thread nền.
- Demo: `BandwidthConsoleDemo` (tuần 1). Module tuần 2: `BandwidthMonitor` + `BandwidthSample` + `BandwidthListener`.

## Đối chiếu Task Manager (làm tuần 4, điền sau)

| Card | Cửa sổ thời gian | Task Manager | Ứng dụng | Sai lệch | Ghi chú làm tròn |
|---|---|---|---|---|---|
| | | | | | |

