package com.nhom15.dao;

import com.nhom15.util.DBConnection; // lấy Connection
import java.sql.*;

public class UserDAO {
    // Hàm thêm người dùng mới
    public boolean registerUser(String username, String password, String email) {
        String sql = "INSERT INTO users (username, password, email) VALUES (?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);
            ps.setString(3, email);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.out.println("Lỗi đăng ký user: " + e.getMessage());
            return false;
        }
    }

    // Hàm đăng nhập
    public boolean loginUser(String username, String password) {
        String sql = "SELECT * FROM users WHERE username = ? AND password = ?";
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