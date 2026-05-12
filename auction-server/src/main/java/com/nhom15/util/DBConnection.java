package com.nhom15.util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

/**
 * DBConnection — quản lý Connection Pool dùng HikariCP.
 *
 * <p>Thay thế hoàn toàn Singleton Connection cũ. Mỗi lần gọi getConnection()
 * sẽ mượn một Connection từ pool, dùng xong trả về pool tự động qua
 * try-with-resources — không cần thay đổi gì ở các DAO hiện tại.
 *
 * <p>Cấu hình đọc từ db.properties (giữ nguyên file cũ):
 * <pre>
 *   db.url=jdbc:mysql://localhost:3306/auction_db
 *   db.user=root
 *   db.password=12345678
 * </pre>
 */
public class DBConnection {

  private static final HikariDataSource dataSource;

  static {
    // Đọc db.properties một lần khi class được nạp
    Properties prop = new Properties();
    try (InputStream input = DBConnection.class.getClassLoader()
            .getResourceAsStream("db.properties")) {

      if (input == null) {
        throw new IOException("Không tìm thấy file db.properties trong classpath");
      }
      prop.load(input);

    } catch (IOException e) {
      throw new RuntimeException("Lỗi đọc file cấu hình database: " + e.getMessage(), e);
    }

    // Cấu hình HikariCP
    HikariConfig config = new HikariConfig();
    config.setJdbcUrl(prop.getProperty("db.url"));
    config.setUsername(prop.getProperty("db.user"));
    config.setPassword(prop.getProperty("db.password"));

    // Driver — HikariCP tự phát hiện, nhưng khai báo rõ để tránh nhầm
    config.setDriverClassName("com.mysql.cj.jdbc.Driver");

    // Kích thước pool: phù hợp với server nhỏ (nhiều client đồng thời)
    config.setMaximumPoolSize(10);       // tối đa 10 connection song song
    config.setMinimumIdle(2);            // giữ sẵn 2 connection khi rảnh
    config.setConnectionTimeout(10_000); // chờ tối đa 10s để mượn connection
    config.setIdleTimeout(300_000);      // connection rảnh 5 phút thì trả về pool
    config.setMaxLifetime(1_800_000);    // connection tối đa sống 30 phút rồi làm mới

    // Câu lệnh kiểm tra connection còn sống không trước khi trả cho DAO
    config.setConnectionTestQuery("SELECT 1");

    // Tên pool xuất hiện trong log — dễ debug
    config.setPoolName("AuctionPool");

    dataSource = new HikariDataSource(config);
    System.out.println("✅ [DBConnection] HikariCP pool đã khởi động (maxPool=10)");
  }

  // Ngăn khởi tạo instance bên ngoài
  private DBConnection() {}

  /**
   * Mượn một Connection từ pool.
   *
   * <p>Dùng bắt buộc trong try-with-resources để trả về pool tự động:
   * <pre>
   *   try (Connection conn = DBConnection.getConnection();
   *        PreparedStatement ps = conn.prepareStatement(sql)) {
   *       ...
   *   }
   * </pre>
   *
   * @return Connection sẵn sàng dùng
   * @throws RuntimeException nếu pool hết connection hoặc DB không phản hồi
   */
  public static Connection getConnection() {
    try {
      return dataSource.getConnection();
    } catch (SQLException e) {
      throw new RuntimeException("Không lấy được Connection từ pool: " + e.getMessage(), e);
    }
  }

  /**
   * Đóng toàn bộ pool — gọi khi server shutdown.
   * Tùy chọn: có thể gọi trong shutdown hook của AuctionServer.
   */
  public static void shutdown() {
    if (dataSource != null && !dataSource.isClosed()) {
      dataSource.close();
      System.out.println("🔌 [DBConnection] Pool đã đóng.");
    }
  }
}