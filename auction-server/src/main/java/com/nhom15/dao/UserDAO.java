package com.nhom15.dao;

import com.nhom15.util.DBConnection; // lấy Connection
import java.sql.*;

public class UserDAO {
    // Hàm thêm người dùng mới
    public boolean registerUser(String username, String password) {
        String sql = "INSERT INTO user (username, password) VALUES (?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.out.println("Lỗi đăng ký user: " + e.getMessage());
            return false;
        }
    }
}