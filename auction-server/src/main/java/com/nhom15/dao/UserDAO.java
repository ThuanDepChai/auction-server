package com.nhom15.dao;

import com.nhom15.util.DBConnection; // lấy Connection
import java.sql.*;

public class UserDAO {
    // Hàm thêm người dùng mới
    public boolean registerUser(String username, String password, String email) {
        try (Connection conn = DBConnection.getConnection()) {
            // 1. Kiểm tra username
            String checkUserSql = "SELECT user_id FROM user WHERE username = ?";
            try(PreparedStatement ps = conn.prepareStatement(checkUserSql)) {
                ps.setString(1, username);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    System.out.println("Username đã tồn tại, vui lòng chọn tên khác!");
                    return false;
                }
            }
            
            // 2. Kiểm tra email
            String checkEmailSql = "SELECT user_id FROM user WHERE email = ?";
            try (PreparedStatement ps = conn.prepareStatement(checkEmailSql)) {
                ps.setString(1, email);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    System.out.println("Email đã đăng kí, vui lòng chọn email khác!");
                    return false;
                }
            }
            
            // 3. Nếu chưa tồn tại thì tạo user mới
            String sql = "INSERT INTO user (username, password, email) VALUES (?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, username);
                ps.setString(2, password);
                ps.setString(3, email);
                ps.executeUpdate();
                return true;
            }
        } catch (SQLException e) {
            System.out.println("Lỗi đăng ký user: " + e.getMessage());
            return false;
        }
    }

    // Hàm đăng nhập
    public boolean loginUser(String username, String password) {
       
        String sql = "SELECT * FROM user WHERE username = ? AND password = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            stmt.setString(2, password); // so sánh hash mật khẩu sau
            ResultSet rs = stmt.executeQuery();
            return rs.next(); // nếu có kết quả thì đăng nhập thành công
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}
