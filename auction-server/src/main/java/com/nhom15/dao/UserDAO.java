package com.nhom15.dao;

import com.google.gson.JsonObject;
import com.nhom15.model.user.Admin;
import com.nhom15.model.user.Bidder;
import com.nhom15.model.user.Seller;
import com.nhom15.model.user.User;
import com.nhom15.util.DBConnection;
import java.sql.*;

public class UserDAO {

    /**
     * 1. HÀM CHECK NHANH USERNAME
     * Dùng cho tính năng kiểm tra khi đang gõ
     */
    public boolean isUsernameExists(String username) {
        String sql = "SELECT 1 FROM user WHERE username = ? LIMIT 1";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next(); // Nếu có kết quả trả về true
            }
        } catch (SQLException e) {
            System.err.println("Lỗi check username: " + e.getMessage());
            return false;
        }
    }

    /**
     * 2. HÀM CHECK NHANH EMAIL
     * (Nên có để sau này bạn làm check email trùng tương tự username)
     */
    public boolean isEmailExists(String email) {
        String sql = "SELECT 1 FROM user WHERE email = ? LIMIT 1";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.err.println("Lỗi check email: " + e.getMessage());
            return false;
        }
    }

    /**
     * 3. HÀM ĐĂNG KÝ USER
     * Đã được tối ưu bằng cách gọi lại các hàm check ở trên
     */
    public boolean registerUser(String username, String password, String email) {
        // Tận dụng lại các hàm check nhanh để code ngắn gọn
        if (isUsernameExists(username) || isEmailExists(email)) {
            return false;
        }

        String sql = "INSERT INTO user (username, password, email, role) VALUES (?, ?, ?, 'BIDDER')";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            ps.setString(2, password); // Lưu ý: Nên hash password ở đây nếu có thể
            ps.setString(3, email);

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Lỗi đăng ký user: " + e.getMessage());
            return false;
        }
    }

    /**
     * 4. LẤY THÔNG TIN USER (Dành cho Đăng nhập)
     */
    public User findByUsername(String username) {
        String sql = "SELECT * FROM user WHERE username = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int id = rs.getInt("user_id");
                    String user = rs.getString("username");
                    String pass = rs.getString("password");
                    String mail = rs.getString("email");
                    String role = rs.getString("role");

                    if (role == null) role = "BIDDER"; // Mặc định nếu null

                    switch (role.toUpperCase()) {
                        case "ADMIN":  return new Admin(id, user, pass, mail);
                        case "SELLER": return new Seller(id, user, pass, mail);
                        default:       return new Bidder(id, user, pass, mail);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * LẤY  THÔNG TIN CÁ NHÂN
     */
    public JsonObject getProfile(int userId) {
        String sql = "SELECT username, email, full_name, phone, role, balance, created_at FROM user WHERE user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    JsonObject obj = new JsonObject();
                    obj.addProperty("username",  rs.getString("username"));
                    obj.addProperty("email",     rs.getString("email"));
                    obj.addProperty("fullName",  rs.getString("full_name")  != null ? rs.getString("full_name")  : "");
                    obj.addProperty("phone",     rs.getString("phone")      != null ? rs.getString("phone")      : "");
                    obj.addProperty("role",      rs.getString("role")       != null ? rs.getString("role")       : "BIDDER");
                    obj.addProperty("balance",   rs.getDouble("balance"));
                    // Format ngày tham gia
                    Timestamp ts = rs.getTimestamp("created_at");
                    String joinDate = ts != null ?
                            new java.text.SimpleDateFormat("dd/MM/yyyy").format(ts) : "---";
                    obj.addProperty("joinDate", joinDate);
                    return obj;
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }
}