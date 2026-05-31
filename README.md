# Hệ Thống Đấu Giá Trực Tuyến

## 1. Mô tả bài toán và phạm vi hệ thống
Hệ thống đấu giá trực tuyến hoạt động theo thời gian thực (real-time) dựa trên mô hình Client-Server. Hệ thống cho phép người dùng đăng ký tài khoản, quản lý sản phẩm, khởi tạo và tham gia các phiên đấu giá. Phạm vi hệ thống bao gồm 3 vai trò phân quyền rõ rệt: Admin (Quản trị viên), Seller (Người bán) và Bidder (Người mua).

## 2. Công nghệ sử dụng, môi trường chạy và yêu cầu cài đặt
* **Ngôn ngữ lập trình:** Java (JDK 26).
* **Giao diện (GUI):** JavaFX 21.0.1.
* **Cơ sở dữ liệu:** SQLite (Truy xuất qua JDBC).
* **Giao thức mạng:** TCP Socket (Truyền tải dữ liệu định dạng JSON).
* **Quản lý dự án & Đóng gói:** Maven (`maven-shade-plugin` tạo fat JAR).
* **Môi trường chạy:** Windows, macOS, Linux.
* **Yêu cầu cài đặt:** Máy tính cần cài đặt sẵn Java (JDK hoặc JRE) phiên bản 26 trở lên.

## 3. Cấu trúc thư mục và module chính
Project được chia thành 3 module Maven chính:
* `common/`: Chứa các model dữ liệu cốt lõi (User, Item, Auction, BidTransaction) và DTO (SocketMessage) dùng chung cho cả Client và Server.
* `server/`: Chứa logic xử lý đa luồng, định tuyến thông điệp mạng, lập lịch ngầm (Scheduler) và thao tác với cơ sở dữ liệu qua hàng đợi (Write Queue).
* `client/`: Chứa giao diện đồ họa JavaFX, Controller xử lý sự kiện và luồng kết nối mạng (NetworkClient).

## 4. Vị trí các file .jar
Sau khi build, các file executable fat JAR được đặt tại:
* **Server:** `server/target/server-runnable.jar`
* **Client:** `client/target/client-runnable.jar`

## 5. Hướng dẫn chạy Server/Client

**Bước 0: Cài đặt hiển thị log tiếng Việt (Windows, optional)**

Chạy lệnh
```bash
chcp 65001
````
tại terminal để dự án có thể in log tiếng Việt mà không bị lỗi encoding.

**Bước 1: Build project (Optional)**

Chạy lệnh
```bash
mvn clean package
```
tại terminal trong thư mục gốc của dự án để biên dịch và đóng gói mã nguồn thành file `.jar`. Hoặc có thể sử dụng trực tiếp các file `.jar` được tạo bởi Github Action.

**Bước 2: Khởi động Server**

Chạy file `.jar` của Server trước. Cổng (Port) mặc định là 8080, hệ thống sẽ tự động dò tìm cổng trống nếu 8080 bị chiếm.

```bash
java -jar server/target/server-runnable.jar
```

**Bước 3: Khởi động Client**

Mở một terminal khác và chạy file `.jar` của Client. Nhập IP và Port của Server lên giao diện để kết nối.

```bash
java -jar client/target/client-runnable.jar
```

*(Để giả lập nhiều người dùng, mở nhiều terminal mới và chạy lặp lại lệnh khởi động Client).*

**Tài khoản admin mặc định:**

* Username: `admin`
* Password: `admin123`

*(Có thể tự demote sau khi cấp quyền admin cho tài khoản khác)*

## 6. Danh sách chức năng đã hoàn thành

* Giao tiếp mạng thời gian thực (Real-time TCP Socket) bằng JSON.
* Giải quyết triệt để tranh chấp dữ liệu (Race Condition) bằng `ReentrantLock` và Single-Threaded Database Write Queue.
* Giao diện đồ họa đa màn hình, trực quan hóa lịch sử đặt giá bằng biểu đồ.
* Đăng ký, đăng nhập và phân quyền người dùng an toàn.
* **Bidder:** Tham gia phòng đấu giá, theo dõi đếm ngược, đặt giá đồng thời, xác nhận thanh toán.
* **Seller:** Quản lý kho (Thêm, sửa, xóa sản phẩm), đưa sản phẩm lên sàn đấu giá.
* **Admin:** Quản lý quyền người dùng (Cấp/Giáng quyền, Khóa), xóa sản phẩm vi phạm kho, dừng khẩn cấp các phiên đấu giá đang chạy.
* Hệ thống ngầm tự động rà soát, chốt kết quả và đóng phiên khi hết thời gian.

## 7. Tài liệu đính kèm

* **Link báo cáo PDF:** [Github Link](https://github.com/NguyenHuuNhatQuang/online-auction-system/blob/main/report.pdf) hoặc [Google Drive Link](https://drive.google.com/file/d/16QwXcvbkHzm8lJfUbeuD-g8VS25BNjKU/)
* **Link video demo:** [Chèn link YouTube / Google Drive tại đây]
