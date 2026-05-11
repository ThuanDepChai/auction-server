package com.nhom15.util;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DBConnection {

  private static Connection connection = null;
  private static final String dbUrl;
  private static final String dbUser;
  private static final String dbPass;

  // Load file cấu hình 1 lần duy nhất khi class được nạp
  static {
    try (InputStream input = DBConnection.class.getClassLoader()
        .getResourceAsStream("db.properties")) {
      Properties prop = new Properties();
      if (input == null) {
        throw new IOException("Không tìm thấy file db.properties");
      }
      prop.load(input);
      dbUrl = prop.getProperty("db.url");
      dbUser = prop.getProperty("db.user");
      dbPass = prop.getProperty("db.password");
    } catch (IOException e) {
      throw new RuntimeException("Lỗi đọc file cấu hình database", e);
    }
  }

  private DBConnection() {
  }

  // synchronized để đảm bảo Thread-Safe (chỉ 1 luồng được vào tạo kết nối)
  public static synchronized Connection getConnection() {
    try {
      if (connection == null || connection.isClosed()) {
        Class.forName("com.mysql.cj.jdbc.Driver");
        connection = DriverManager.getConnection(dbUrl, dbUser, dbPass);
        System.out.println(" [DBConnection] Kết nối Database thành công!");
      }
    } catch (ClassNotFoundException | SQLException e) {
      // Ném RuntimeException để tầng trên (Service/Controller) xử lý
      throw new RuntimeException("Kết nối DB thất bại!", e);
    }
    return connection;
  }
}