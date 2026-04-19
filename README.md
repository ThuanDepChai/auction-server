<img width="1799" height="895" alt="class-diagram" src="https://github.com/user-attachments/assets/005c1862-1148-4a52-9696-d66ce936faba" />
```text
Nhom 15
 auction-server/
 ├── pom.xml (hoặc build.gradle)      <-- File cấu hình thư viện (JDBC, JSON, JUnit)
 ├── src/
 │   ├── main/
 │   │   └── java/
 │   │       └── com/auction/server/
 │   │           ├── ServerMain.java  <-- File chạy server chính
 │   │           ├── network/         <-- Lớp quản lý Socket Server hoặc REST API endpoint
 │   │           ├── controller/      <-- Xử lý request từ Client (nhận Bid, tạo com.nhom15.model.user.User, update Item)
 │   │           ├── model/           <-- Chứa các class Entity cốt lõi (com.nhom15.model.user.User, Item, Auction, BidTransaction)
 │   │           ├── dao/             <-- Chứa logic truy xuất Database (Data Access Object)
 │   │           └── util/            <-- Các class tiện ích (Singleton kết nối DB, mã hóa mật khẩu)
 │   └── test/
 │       └── java/
 │           └── com/auction/server/  <-- Chứa các file JUnit test cho logic quan trọng (ví dụ: test placeBid)
 
```
 auction-client/
 ├── src/
 │   ├── main/
 │   │   ├── java/
 │   │   │   └── com/nhom15/client/
 │   │   │       ├── MainApplication.java       (Hàm main, khởi chạy JavaFX)
 │   │   │       │
 │   │   │       ├── controller/                (Chứa các JavaFX Controller xử lý logic giao diện)
 │   │   │       │   ├── LoginController.java
 │   │   │       │   ├── AuctionListController.java
 │   │   │       │   ├── BiddingRoomController.java (Màn hình đấu giá trực tiếp)
 │   │   │       │   └── SellerDashboardController.java (Quản lý sản phẩm cho com.nhom15.model.user.Seller)
 │   │   │       │
 │   │   │       ├── model/                     (Chứa các Data Transfer Object - DTO nhận từ Server)
 │   │   │       │   ├── UserDTO.java
 │   │   │       │   ├── ItemDTO.java
 │   │   │       │   └── BidMessage.java        (Model cho bản tin realtime)
 │   │   │       │
 │   │   │       ├── network/                   (Chịu trách nhiệm giao tiếp mạng với Server)
 │   │   │       │   ├── ApiClient.java         (Gửi/nhận HTTP request bằng JSON)
 │   │   │       │   └── SocketClient.java      (Lắng nghe Socket cho realtime update/Observer)
 │   │   │       │
 │   │   │       └── util/                      (Các lớp tiện ích dùng chung)
 │   │   │           ├── SessionManager.java    (Lưu trạng thái com.nhom15.model.user.User đang đăng nhập)
 │   │   │           └── ViewFactory.java       (Hỗ trợ load FXML và chuyển đổi giữa các màn hình)
 │   │   │
 │   │   └── resources/                         (Chứa các file tài nguyên tĩnh)
 │   │       ├── fxml/                          (Chứa file thiết kế giao diện)
 │   │       │   ├── login.fxml
 │   │       │   ├── auction_list.fxml
 │   │       │   ├── bidding_room.fxml
 │   │       │   └── seller_dashboard.fxml
 │   │       ├── css/                           (Tùy chỉnh giao diện đẹp hơn)
 │   │       │   └── styles.css
 │   │       └── assets/                        (Hình ảnh, icon...)
 │   │           └── logo.png
