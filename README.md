# Network Monitor Tool

> Đồ án môn **Lập trình mạng** của Nhóm 5 — xây dựng ứng dụng Java theo dõi kết nối mạng trên Windows.

Ứng dụng hướng tới việc xem kết nối TCP/UDP, tốc độ gửi nhận của card mạng và đo truyền dữ liệu qua Socket. Giao diện thống kê, biểu đồ và Agent–Server được phát triển theo từng giai đoạn của kế hoạch đồ án.

## Trạng thái hiện tại

| Hạng mục | Trạng thái |
| --- | --- |
| Khung dự án Maven và package `core`, `monitor`, `network`, `ui` | Hoàn thành |
| Liệt kê kết nối TCP/UDP bằng OSHI | Đã có bản console |
| Lọc kết nối theo IP và port | Đã có; lọc cả IP/port cục bộ và từ xa |
| Lấy mẫu băng thông theo thời gian thực | Đang phát triển |
| Agent–Server truyền dữ liệu qua Socket | Đang phát triển |
| Giao diện Swing, biểu đồ và thống kê | Đang phát triển |

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
4. Chọn **Run Project** để chạy `com.nhom5.netmonitor.App`.

NetBeans dùng classpath Maven để nạp OSHI và các thư viện đi kèm.

### Chạy bằng Maven và JAR

Cần cài Maven trong `PATH`, hoặc chạy Maven đi kèm NetBeans:

```powershell
mvn clean package
java -jar target/netmonitor-0.1.0-SNAPSHOT.jar
```

Lệnh trên liệt kê mọi kết nối tại thời điểm quét. Có thể lọc theo IP, port hoặc cả hai:

```powershell
java -jar target/netmonitor-0.1.0-SNAPSHOT.jar 127.0.0.1
java -jar target/netmonitor-0.1.0-SNAPSHOT.jar 127.0.0.1 8080
```

Khi truyền IP và port cùng lúc, một dòng cần khớp **cả hai** điều kiện; mỗi điều kiện có thể khớp đầu cục bộ hoặc đầu từ xa.

### Đầu ra console

```text
ConnectionMonitor | ket noi: <số dòng> | bo loc IP=*, port=*
PROTO  LOCAL IP                                PORT    REMOTE IP                               PORT    STATE         PID
TCP4   <IP cục bộ>                             <port>  <IP từ xa hoặc *>                       <port>  ESTABLISHED   <PID>
TCP4   <IP cục bộ>                             <port>  *                                       0       LISTEN        <PID>
UDP4   <IP cục bộ>                             <port>  *                                       0       N/A           <PID>
```

Danh sách và số dòng phụ thuộc vào máy, thời điểm chạy và bộ lọc. `N/A` là trạng thái hiển thị cho UDP; PID có thể là `-1` nếu hệ điều hành không cung cấp.

## Cấu trúc dự án

```text
src/main/java/com/nhom5/netmonitor/
├── App.java                         # Điểm chạy console hiện tại
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
