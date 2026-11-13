# Network Monitor Tool

> Đồ án môn **Lập trình mạng** của Nhóm 5 — xây dựng ứng dụng Java theo dõi kết nối mạng trên Windows.

Ứng dụng hướng tới việc xem kết nối TCP/UDP, tốc độ gửi nhận của card mạng và đo truyền dữ liệu qua Socket. Giao diện thống kê, biểu đồ và Agent–Server được phát triển theo từng giai đoạn của kế hoạch đồ án.

## Trạng thái hiện tại

- Dashboard hiển thị mẫu băng thông thực tế theo card qua OSHI; chọn card đang hoạt động. Biểu đồ JFreeChart hiển thị 60 giây gần nhất.
- Connections quét kết nối thực tế định kỳ, cho phép lọc TCP/UDP và tìm IP, port, PID.
- Statistics tính trung bình, cực đại và dung lượng theo phiên kể từ khi chọn card. Có thể xem biểu đồ 1, 5 hoặc 15 phút, đếm kết nối theo IP/port đầu bên kia và xuất CSV lịch sử tối đa 15 phút.
- Socket Test truyền các mẫu tốc độ card thực qua TCP JSON một dòng giữa client và server. Chưa đo tốc độ truyền khối dữ liệu, độ trễ, hoặc thống kê lưu lượng theo IP/port; chưa kết nối với Agent/Server demo cũ.
- Chưa có xác thực hoặc mã hóa cho Socket Test; chỉ dùng trong mạng thử nghiệm tin cậy.

## Công nghệ

- Java 17 trở lên; Maven biên dịch mã nguồn với `release 17`.
- Apache NetBeans 31 hoặc IDE hỗ trợ Maven.
- OSHI 7.6.1 để đọc thông tin hệ điều hành và kết nối mạng.
- JFreeChart 1.5.6 cho biểu đồ; Gson 2.15.0 cho bản tin JSON.
- Môi trường đối chiếu theo kế hoạch: Windows.

Các phiên bản thư viện được khai báo trong [`pom.xml`](pom.xml).

## Bắt đầu

### Lấy mã nguồn

```powershell
git clone https://github.com/thinhtran-18/network-monitor-tool.git
cd network-monitor-tool
```

Nếu đã clone trước đó:

```powershell
git switch main
git pull --ff-only origin main
```

### Chạy bằng NetBeans

1. Mở NetBeans và chọn **File → Open Project**.
2. Chọn thư mục `network-monitor-tool` có chứa `pom.xml`.
3. Chọn **Clean and Build** để tải dependency và biên dịch.
4. Chọn **Run Project** để mở Dashboard gốc. Nếu NetBeans hỏi lớp chạy, chọn `com.nhom5.netmonitor.App`. Có thể nhấp phải `App.java` → **Run File**.

NetBeans dùng classpath Maven để nạp OSHI và các thư viện đi kèm.

### Chạy bằng Maven và JAR

Cần cài Maven trong `PATH`, hoặc chạy Maven đi kèm NetBeans:

```powershell
mvn clean package
java -jar target/netmonitor-0.1.0-SNAPSHOT.jar
```

Lệnh `java -jar` mở giao diện giám sát. Chọn card mạng đang hoạt động ở Dashboard; mỗi mẫu cập nhật sau khoảng một giây. Chỉ số MB/s dùng 1 MB = 1.000.000 byte. Tổng phiên bắt đầu từ các mẫu quan sát được sau khi chọn card.

### Thử Socket

Mở ứng dụng trên hai máy cùng mạng (hoặc hai cửa sổ trên cùng máy). Ở máy nhận chọn **Socket Test → Server**, nhập port rồi khởi động. Ở máy gửi chọn **Client**, nhập IP máy nhận và cùng port rồi kết nối. Chọn card mạng ở Dashboard của máy gửi; nhật ký server hiển thị mẫu tốc độ. Khi thử trên cùng máy dùng IP `127.0.0.1`. Cấu hình firewall nếu cần.

## Cấu trúc dự án

```text
src/main/java/com/nhom5/netmonitor/
├── App.java                         # Điểm chạy giao diện giám sát
├── core/                            # Dữ liệu dùng chung và bộ lọc
├── monitor/connection/              # Theo dõi kết nối TCP/UDP
├── monitor/bandwidth/               # Module băng thông
├── network/                         # Agent–Server qua Socket
└── ui/                              # Giao diện Swing
```

## Đóng góp và quy ước nhóm

Repo chính dùng nhánh `main`; thành viên làm trên fork và gửi pull request. **Trần Văn Thịnh là người merge** vào `main`. Đặt tên nhánh theo dạng `feature/<ten>-<noi-dung>` và commit theo mẫu `feat(scope): mô tả ngắn`. Chi tiết xem [`CONTRIBUTING.md`](CONTRIBUTING.md).

Sản phẩm bàn giao theo tuần và ranh giới các module được ghi trong [tài liệu kiến trúc](docs/KIEN_TRUC_TUAN_1.md) và [hướng dẫn thành viên](docs/HUONG_DAN_THANH_VIEN.md).

## Lưu ý dữ liệu

Ứng dụng hiển thị địa chỉ IP, port và PID trên máy đang chạy. Không đưa ảnh chụp chưa che thông tin máy, địa chỉ IP cá nhân hoặc log thực tế lên repo công khai.
