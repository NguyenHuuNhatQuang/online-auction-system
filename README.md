# Online Auction System (Hệ thống Đấu giá Trực tuyến)

Bài tập lớn môn **Lập trình Nâng cao (LTNC)** — hệ thống đấu giá trực tuyến theo mô
hình **Client – Server**, hỗ trợ nhiều người dùng đấu giá đồng thời (concurrent
bidding) và cập nhật kết quả theo thời gian thực (realtime).

---

## 1. Mô tả bài toán & phạm vi hệ thống

Hệ thống mô phỏng một sàn đấu giá trực tuyến gồm nhiều phiên đấu giá chạy song song.
Người dùng kết nối tới máy chủ trung tâm để:

- Đăng ký / đăng nhập với các vai trò **BIDDER** (người đấu giá), **SELLER** (người
  bán), **ADMIN** (quản trị viên).
- **Seller**: tạo sản phẩm (Electronics / Art) và mở phiên đấu giá có thời hạn.
- **Bidder**: tham gia phòng đấu giá, đặt giá (bid) theo thời gian thực; khi thắng
  thì thanh toán.
- **Admin**: quản lý người dùng, đổi vai trò, quản lý sản phẩm, cưỡng chế dừng phiên.

Phạm vi:

- Nhiều client kết nối đồng thời tới một server qua **TCP Socket**.
- Xử lý **đặt giá đồng thời an toàn** (mỗi phiên một khoá riêng, chống race condition).
- **Cập nhật realtime**: khi có người đặt giá / phiên kết thúc / thanh toán, server
  broadcast cho toàn bộ client đang kết nối.
- **Bền vững dữ liệu**: toàn bộ người dùng, sản phẩm, phiên đấu giá, lịch sử đặt giá
  được lưu xuống **SQLite**, khôi phục lại khi khởi động server.

---

## 2. Công nghệ sử dụng, môi trường chạy & yêu cầu cài đặt

| Thành phần        | Công nghệ                                                        |
|-------------------|------------------------------------------------------------------|
| Ngôn ngữ          | Java 26                                                          |
| Giao diện (Client)| JavaFX 21 (FXML + CSS)                                           |
| Mạng              | Java TCP Socket (giao thức JSON, newline-delimited)             |
| Cơ sở dữ liệu     | SQLite (sqlite-jdbc)                                            |
| JSON              | Jackson Databind                                                |
| Kiểm thử          | JUnit 5 (Jupiter)                                               |
| Build & đóng gói  | Maven (multi-module) + **maven-shade-plugin** (fat/uber JAR)    |
| Kiểm tra style    | Checkstyle (Google style)                                       |
| CI/CD             | GitHub Actions (`.github/workflows/ci.yml`)                     |

### Yêu cầu cài đặt
- **JDK 26** (dự án đặt `maven.compiler.release = 26`).
- **Maven 3.9+** (chỉ cần khi muốn tự build; nếu chạy file `.jar` có sẵn thì không cần).
- Không cần cài đặt riêng JavaFX hay SQLite — đã được đóng gói sẵn trong fat JAR của client/server.

---

## 3. Cấu trúc thư mục / các module chính

Dự án là một **Maven multi-module** gồm 3 module:

```
online-auction-system/
├── pom.xml                      # POM cha (quản lý 3 module + cấu hình shade cho server)
├── common/                      # Module dùng chung (DTO + Domain Models)
│   └── src/main/java/com/auction/common/
│       ├── dto/SocketMessage.java
│       └── models/              # Entity, User, Bidder, Seller, Admin,
│                                # Item, Electronics, Art, Auction, BidTransaction
├── server/                      # Máy chủ (logic nghiệp vụ + mạng + CSDL)
│   └── src/main/java/com/auction/server/
│       ├── Main.java            # Điểm khởi động server (cổng 8080)
│       ├── network/             # AuctionServer, ClientHandler, MessageRouter
│       ├── services/            # AuctionManager, BiddingService, AuctionScheduler,
│       │                        # ItemManager, UserManager
│       ├── factories/ItemFactory.java
│       └── database/            # DatabaseConnection, *DAO, DatabaseWriteQueue
│   └── src/test/java/...        # Unit test (JUnit 5)
└── client/                      # Ứng dụng JavaFX (giao diện người dùng)
    └── src/main/
        ├── java/com/auction/client/
        │   ├── Launcher.java    # Main class cho fat JAR (tránh lỗi module JavaFX)
        │   ├── AuctionClientApp.java
        │   ├── controllers/     # Login, Register, Dashboard/Lobby, AuctionRoom,
        │   │                    # ProductManagement, ListingWizard, AdminDashboard...
        │   ├── core/SceneManager.java
        │   ├── network/NetworkClient.java
        │   └── models/view/     # AuctionView (+ Art/Electronics view)
        └── resources/
            ├── fxml/            # Các màn hình (login, dashboard, auction_room, ...)
            └── css/app.css      # Theme giao diện "BidNow"
```

- **common**: tầng Model (kiến trúc MVC) + DTO, được cả client và server dùng chung.
- **server**: tầng Controller/Service/DAO phía máy chủ.
- **client**: tầng View (FXML) + Controller phía giao diện.

---

## 4. Vị trí các file `.jar` (fat / uber JAR)

Sau khi build (`mvn clean package`), hai file JAR chạy trực tiếp được tạo ra tại:

| Thành phần | Đường dẫn file JAR                          | Main class                       |
|------------|---------------------------------------------|----------------------------------|
| Server     | `server/target/server-runnable.jar`         | `com.auction.server.Main`        |
| Client     | `client/target/client-runnable.jar`         | `com.auction.client.Launcher`    |

Cả hai đều là **fat JAR** (đã nhúng đầy đủ thư viện: JavaFX, SQLite, Jackson…) nên
chạy trực tiếp bằng `java -jar` mà không cần thêm classpath.

---

## 5. Hướng dẫn build & chạy

### 5.1. (Tuỳ chọn) Tự build từ mã nguồn
```bash
# Tại thư mục gốc dự án (online-auction-system)
mvn clean package
```
Lệnh trên sẽ kiểm tra style, chạy unit test và sinh ra 2 file fat JAR ở mục 4.

### 5.2. Chạy theo thứ tự: Server trước → Client sau

**Bước 1 — Khởi động Server** (mở 1 terminal):
```bash
java -jar server/target/server-runnable.jar
```
Server lắng nghe tại cổng **8080**. Đợi tới khi thấy dòng
`[AuctionServer] Máy chủ đang hoạt động tại port 8080...`.

**Bước 2 — Khởi động Client** (mở terminal khác):
```bash
java -jar client/target/client-runnable.jar
```
Client mặc định kết nối tới `127.0.0.1:8080`.

**Chạy nhiều client cùng lúc**: mở thêm nhiều terminal và lặp lại lệnh chạy client để
mô phỏng nhiều người dùng đấu giá đồng thời:
```bash
java -jar client/target/client-runnable.jar   # client #2
java -jar client/target/client-runnable.jar   # client #3
```

> Gợi ý demo: tạo 1 tài khoản SELLER để mở phiên, và 2+ tài khoản BIDDER để cùng đặt
> giá vào một phiên → quan sát cập nhật realtime và xử lý concurrent bidding.

### 5.3. Kết nối tới server từ xa (vd qua ngrok)
Client mặc định nối tới `127.0.0.1:8080`, nhưng có thể truyền **host/port** qua tham số
dòng lệnh (hoặc biến môi trường `AUCTION_HOST` / `AUCTION_PORT`):
```bash
# Kết nối tới một server đang được expose ra internet (vd ngrok TCP):
java -jar client/target/client-runnable.jar 0.tcp.ngrok.io 17234
```
Nhờ vậy nhiều người ở các máy khác nhau có thể cùng tham gia một phiên đấu giá.

---

## 6. Danh sách chức năng đã hoàn thành

**Bắt buộc**
- [x] Thiết kế lớp & cây kế thừa: `Entity` → `User` → {`Bidder`, `Seller`, `Admin`};
      `Entity` → `Item` → {`Electronics`, `Art`}.
- [x] Áp dụng OOP: Encapsulation, Inheritance, Polymorphism, Abstraction
      (lớp/abstract method `printInfo()`, `getItemType()`…).
- [x] Design Patterns: **Singleton** (các *Manager*), **Factory Method** (`ItemFactory`),
      **DAO**, **MVC** (JavaFX + FXML), **Observer/broadcast** cho realtime.
- [x] Quản lý người dùng & sản phẩm (đăng ký/đăng nhập, phân quyền, CRUD sản phẩm).
- [x] Chức năng đấu giá (tạo phiên, đặt giá, kết thúc theo thời gian, thanh toán).
- [x] Xử lý lỗi & ngoại lệ (validate giá, trạng thái phiên, thông điệp lỗi gửi về client).
- [x] Xử lý đấu giá đồng thời (concurrency): `ReentrantLock` theo từng phiên +
      `DatabaseWriteQueue` ghi DB bất đồng bộ.
- [x] Realtime update: server broadcast `NEW_BID_BROADCAST`, `AUCTION_FINISHED`,
      `AUCTION_PAID`… qua Socket.
- [x] Kiến trúc Client–Server (TCP Socket, giao thức JSON).
- [x] MVC: JavaFX + FXML, Controller – Model – DAO.
- [x] Maven (multi-module) + coding convention (Checkstyle).
- [x] Unit Test (JUnit 5) cho service/factory/router phía server.
- [x] CI/CD (GitHub Actions: build, test, đóng gói, upload JAR).

**Tuỳ chọn**
- [x] Bid History Visualization (biểu đồ lịch sử đặt giá).
- [ ] Auto-Bidding *(chưa làm)*
- [ ] Anti-sniping *(chưa làm)*

---

## 7. Báo cáo & Video demo

- 📄 **Báo cáo PDF**: `<điền link báo cáo PDF tại đây>`
- 🎥 **Video demo**: `<điền link video demo tại đây>`

---

## 8. Tài khoản & ghi chú

- File CSDL `auction_system.db` được tạo tự động khi chạy server lần đầu (không commit
  vào repo vì là dữ liệu runtime).
- Có thể tự đăng ký tài khoản mới qua màn hình **Đăng ký** trên client.
