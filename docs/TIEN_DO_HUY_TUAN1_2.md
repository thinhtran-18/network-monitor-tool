# Báo cáo tiến độ — Nguyễn Quang Huy

**Dự án:** Network Monitor Tool — Nhóm 5  
**Nhánh:** `Huy` (commit `aeefaa1`)  
**Repo:** https://github.com/thinhtran-18/network-monitor-tool

---

## Tuần 1 — Khảo sát và chốt thiết kế

### Việc đã hoàn thành

**Khảo sát thư viện OSHI**
- Nghiên cứu `SystemInfo`, `HardwareAbstractionLayer`, `NetworkIF`.
- Xác định các hàm sẽ dùng: `getNetworkIFs()`, `updateAttributes()`, `getBytesRecv()`, `getBytesSent()`, `getSpeed()`.
- Ghi chú đầy đủ trong [`docs/GHI_CHU_OSHI_TUAN_1.md`](GHI_CHU_OSHI_TUAN_1.md).

**Khảo sát `java.net.NetworkInterface`**
- So sánh với OSHI: `java.net` không có counter byte nên chỉ dùng để đối chiếu danh sách card, trạng thái `isUp()`, `isLoopback()`.

**Demo console đọc băng thông (`BandwidthConsoleDemo.java`)**
- In toàn bộ danh sách card từ OSHI và từ `java.net.NetworkInterface` (để đối chiếu).
- Lấy mẫu mỗi giây, in tên card, tổng byte nhận/gửi, tốc độ và phần trăm băng thông sử dụng.
- Hỗ trợ tham số dòng lệnh: số giây lấy mẫu và danh sách tên card cần theo dõi.
- **Đã chạy thử thành công** bằng JDK đi kèm NetBeans, kết quả hợp lệ trên máy thật (card Realtek PCIe GbE).

### Sản phẩm bàn giao tuần 1
| Sản phẩm | File |
|---|---|
| Demo console đọc băng thông | `src/.../monitor/bandwidth/BandwidthConsoleDemo.java` |
| Ghi chú các hàm OSHI sẽ dùng | `docs/GHI_CHU_OSHI_TUAN_1.md` |

---

## Tuần 2 — Module băng thông lõi

### Việc đã hoàn thành

**`BandwidthSample.java`** — lớp dữ liệu bất biến (record) đặt trong `core`
- Các trường: `interfaceId`, `displayName`, `timestampMillis`, `totalBytesRecv`, `totalBytesSent`, `recvBytesPerSec`, `sentBytesPerSec`, `linkSpeedBitsPerSec`, `usagePercent`.
- Dùng `OptionalDouble` cho `usagePercent` vì card ảo không có tốc độ danh định.

**`BandwidthListener.java`** — interface lắng nghe sự kiện, đặt trong `core`
- `onBandwidthSample(BandwidthSample)` — nhận mẫu mới (gọi trên thread nền, không phải EDT).
- `onBandwidthError(String, Throwable)` — nhận lỗi (phương thức default, có thể bỏ qua).

**`BandwidthCalculator.java`** — công thức tính thuần túy, không phụ thuộc OSHI
- `bytesPerSecond(prev, current, elapsedMillis)` — trả `-1` nếu counter giảm hoặc thời gian không tăng.
- `usagePercent(recvBps, sentBps, speedBps)` — trả `OptionalDouble.empty()` nếu `speed ≤ 0`; giới hạn trong [0, 100].

**`BandwidthMonitor.java`** — module lõi chạy thread nền lấy mẫu
- Cho phép chọn tập card cần theo dõi (`selectInterfaces(Set<String>)`), có thể thay đổi khi đang chạy.
- Gọi `updateAttributes()` trước mỗi lần đọc, bỏ mẫu và lấy lại mốc khi counter giảm, thời gian không tăng, card biến mất hoặc `updateAttributes()` lỗi.
- `start()` / `stop()` quản lý vòng đời thread daemon; `stop()` chờ thread kết thúc (timeout 3×interval).
- Phát `BandwidthSample` cho mọi listener đã đăng ký.

### Điểm đã xác nhận
- Biên dịch thành công bằng JDK 21 đi kèm NetBeans.
- Chạy thử 5 giây trên máy thật, số liệu hợp lệ, tốc độ đọc được dao động từ vài chục KB/s đến trên 1 MB/s tuỳ lưu lượng thực tế.
- Đã đẩy lên nhánh `Huy` trên GitHub (commit `aeefaa1`).

### Sản phẩm bàn giao tuần 2
| Sản phẩm | File |
|---|---|
| Lớp dữ liệu băng thông | `src/.../core/BandwidthSample.java` |
| Interface sự kiện | `src/.../core/BandwidthListener.java` |
| Công thức tính (tái sử dụng được) | `src/.../monitor/bandwidth/BandwidthCalculator.java` |
| Module lấy mẫu (có listener) | `src/.../monitor/bandwidth/BandwidthMonitor.java` |

---

## Việc còn lại theo kế hoạch

| Tuần | Việc |
|---|---|
| **3** | Thêm thống kê (trung bình, lớn nhất, tổng); lưu lịch sử trong bộ nhớ cho biểu đồ; hỗ trợ chọn nhiều card. |
| **4** | Đối chiếu số liệu với Task Manager/Resource Monitor; điền bảng sai lệch trong `GHI_CHU_OSHI_TUAN_1.md`; tối ưu tần suất lấy mẫu và CPU usage; sửa lỗi còn lại. |

---

## Lưu ý kỹ thuật

> [!NOTE]
> OSHI trên Windows liệt kê các lớp bộ lọc ảo (`WFP`, `QoS`) là card riêng biệt. Cùng một card vật lý có thể xuất hiện 3–4 lần. Khi tích hợp UI, nên lọc chỉ lấy card không có hậu tố (card vật lý thực sự) hoặc lọc theo `isPhysicalEthernetIF()` nếu OSHI cung cấp.

> [!IMPORTANT]
> `BandwidthListener` được gọi trên **thread nền**, không phải Event Dispatch Thread. Tiến (UI) phải bọc cập nhật giao diện trong `SwingUtilities.invokeLater(...)`.
