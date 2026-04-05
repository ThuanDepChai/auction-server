package com.nhom15.database; // Phải khớp với tên folder ông vừa tạo

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {
    public static Connection getConnection() {
        Connection conn = null;
        try {
            // Nhớ kiểm tra XAMPP (MySQL) đã Start chưa nhé Đức
            Class.forName("com.mysql.cj.jdbc.Driver");

            // Thay 'auction_db' bằng tên database ông đã tạo trong phpMyAdmin
            String url = "jdbc:mysql://localhost:3306/auction_db";
            String user = "root";
            String password = ""; // XAMPP mặc định để trống

            conn = DriverManager.getConnection(url, user, password);
            System.out.println("Kết nối Database đấu giá thành công rồi Đức nhé!");
        } catch (ClassNotFoundException | SQLException e) {
            System.out.println("Lỗi kết nối rồi ông giáo ơi!");
            e.printStackTrace();
        }
        return conn;
    }

    // Hàm test nhanh xem máy Snapdragon của ông đã "thông" với MySQL chưa
    public static void main(String[] args) {
        getConnection();
    }
}