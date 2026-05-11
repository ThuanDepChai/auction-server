package com.nhom15.dao;

import com.google.gson.JsonObject;
import com.nhom15.model.user.Admin;
import com.nhom15.model.user.Bidder;
import com.nhom15.model.user.Seller;
import com.nhom15.model.user.User;
import com.nhom15.util.DBConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

public class UserDAO {

  /**
   * 1. HÀM CHECK NHANH USERNAME Dùng cho tính năng kiểm tra khi đang gõ
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
   * 2. HÀM CHECK NHANH EMAIL (Nên có để sau này bạn làm check email trùng tương tự username)
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
   * 3. HÀM ĐĂNG KÝ USER Đã được tối ưu bằng cách gọi lại các hàm check ở trên
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
          String userStr = rs.getString("username");
          String pass = rs.getString("password");
          String mail = rs.getString("email");
          String role = rs.getString("role");

          // --- BỔ SUNG CÁC DÒNG NÀY ---
          String fullName = rs.getString("full_name");
          String phone = rs.getString("phone");
          double balance = rs.getDouble("balance");
          String avatar = rs.getString("avatar_path");
          // ----------------------------

            if (role == null) {
                role = "BIDDER";
            }

          User userObj;
          switch (role.trim().toUpperCase()) {
            case "ADMIN":
              userObj = new Admin(id, userStr, pass, mail);
              break;
            case "SELLER":
              userObj = new Seller(id, userStr, pass, mail);
              break;
            default:
              userObj = new Bidder(id, userStr, pass, mail);
              break;
          }

          // Gán các thông tin bổ sung vào đối tượng user
          userObj.setFullName(fullName != null ? fullName : "");
          userObj.setPhone(phone != null ? phone : "");
          userObj.setBalance(balance);
          userObj.setAvatarPath(avatar != null ? avatar : "");

          return userObj;
        }
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return null;
  }

  /**
   * 5.LẤY  THÔNG TIN CÁ NHÂN
   */
  public JsonObject getProfile(int userId) {
    String sql = "SELECT username, email, full_name, phone, role, balance, created_at FROM user WHERE user_id = ?";
    try (Connection conn = DBConnection.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setInt(1, userId);
      System.out.println("DEBUG SERVER: Đang tìm profile cho userId = " + userId);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          JsonObject obj = new JsonObject();
          obj.addProperty("username", rs.getString("username"));
          obj.addProperty("email", rs.getString("email"));
          obj.addProperty("fullName",
              rs.getString("full_name") != null ? rs.getString("full_name") : "");
          obj.addProperty("phone", rs.getString("phone") != null ? rs.getString("phone") : "");
          obj.addProperty("role", rs.getString("role") != null ? rs.getString("role") : "BIDDER");
          obj.addProperty("balance", rs.getDouble("balance"));
          // Format ngày tham gia
          Timestamp ts = rs.getTimestamp("created_at");
          String joinDate = ts != null ?
              new java.text.SimpleDateFormat("dd/MM/yyyy").format(ts) : "---";
          obj.addProperty("joinDate", joinDate);
          return obj;
        }
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return null;
  }

  // 6.Cập nhật AVATAR
  public boolean updateAvatar(int userId, String avatarPath) {
    String sql = "UPDATE user SET avatar_path = ? WHERE user_id = ?";
    try (Connection conn = DBConnection.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, avatarPath);
      ps.setInt(2, userId);
      return ps.executeUpdate() > 0;
    } catch (SQLException e) {
      e.printStackTrace();
      return false;
    }
  }
  //7.THAY ĐỔI PASSWORD

  /**
   * Tìm User theo ID (Dùng cho việc lấy mật khẩu cũ ra kiểm tra)
   */
  public User findById(int userId) {
    String sql = "SELECT * FROM `user` WHERE user_id = ?";
    try (java.sql.Connection conn = com.nhom15.util.DBConnection.getConnection();
        java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {

      ps.setInt(1, userId);
      try (java.sql.ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          String user = rs.getString("username");
          String pass = rs.getString("password");
          String mail = rs.getString("email");
          String role = rs.getString("role");

            if (role == null) {
                role = "BIDDER";
            }

          switch (role.toUpperCase()) {
            case "ADMIN":
              return new Admin(userId, user, pass, mail);
            case "SELLER":
              return new Seller(userId, user, pass, mail);
            default:
              return new Bidder(userId, user, pass, mail);
          }
        }
      }
    } catch (java.sql.SQLException e) {
      e.printStackTrace();
    }
    return null;
  }

  /**
   * Cập nhật mật khẩu mới (đã được băm) vào Database
   */
  public boolean updatePassword(int userId, String newHashedPassword) {
    String sql = "UPDATE `user` SET password = ? WHERE user_id = ?";
    try (java.sql.Connection conn = com.nhom15.util.DBConnection.getConnection();
        java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {

      ps.setString(1, newHashedPassword);
      ps.setInt(2, userId);

      return ps.executeUpdate() > 0;
    } catch (java.sql.SQLException e) {
      System.err.println("Lỗi cập nhật mật khẩu: " + e.getMessage());
      return false;
    }
  }

  /**
   * 8. CẬP NHẬT THÔNG TIN CÁ NHÂN
   */
  public boolean updateProfile(int userId, String fullName, String email, String phone) {
    String sql = "UPDATE `user` SET full_name = ?, email = ?, phone = ? WHERE user_id = ?";
    try (Connection conn = DBConnection.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {

      ps.setString(1, fullName);
      ps.setString(2, email);
      ps.setString(3, phone);
      ps.setInt(4, userId);

      return ps.executeUpdate() > 0;
    } catch (SQLException e) {
      System.err.println("Lỗi cập nhật profile: " + e.getMessage());
      return false;
    }
  }

  /**
   * 9. NÂNG CẤP LÊN SELLER
   */
  public boolean upgradeToSeller(int userId) {
    String sql = "UPDATE `user` SET role = 'SELLER' WHERE user_id = ?";
    try (Connection conn = DBConnection.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {

      ps.setInt(1, userId);
      return ps.executeUpdate() > 0;
    } catch (SQLException e) {
      System.err.println("Lỗi nâng cấp Seller: " + e.getMessage());
      return false;
    }
  }

  /**
   * 10. LẤY ĐƯỜNG DẪN ẢNH ĐẠI DIỆN
   */
  public String getAvatarPath(int userId) {
    String sql = "SELECT avatar_path FROM `user` WHERE user_id = ?";
    try (Connection conn = DBConnection.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {

      ps.setInt(1, userId);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return rs.getString("avatar_path");
        }
      }
    } catch (SQLException e) {
      System.err.println("Lỗi lấy avatar: " + e.getMessage());
    }
    return "";
  }
}