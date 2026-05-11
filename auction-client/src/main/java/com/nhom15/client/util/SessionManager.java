package com.nhom15.client.util;

import com.nhom15.client.model.UserDTO;

/**
 * SessionManager — lưu trạng thái người dùng đang đăng nhập. Dùng UserDTO làm container thay vì
 * static fields rời rạc.
 */
public final class SessionManager {

  private static UserDTO currentUser;

  // Chặn khởi tạo đối tượng từ bên ngoài
  private SessionManager() {
  }

  // ── Login / Logout ───────────────────────────────────────────────────────

  /**
   * CHUẨN CLEAN CODE: Nhận thẳng 1 đối tượng DTO đã được Parse từ GSON. Không nhận các tham số rời
   * rạc nữa.
   */
  public static void login(UserDTO user) {
    currentUser = user;
  }

  public static void logout() {
    currentUser = null;
  }

  public static boolean isLoggedIn() {
    return currentUser != null;
  }

  // ── Phân quyền (Các hàm này bắt buộc phải có cho HomeController) ─────────

  public static boolean isBidder() {
    return currentUser != null && "BIDDER".equals(currentUser.getRole());
  }

  public static boolean isSeller() {
    return currentUser != null && "SELLER".equals(currentUser.getRole());
  }

  public static boolean isAdmin() {
    return currentUser != null && "ADMIN".equals(currentUser.getRole());
  }

  // ── Getters (delegate sang UserDTO) ─────────────────────────────────────

  public static int getUserId() {
    return currentUser != null ? currentUser.getUserId() : 0;
  }

  public static String getUsername() {
    return currentUser != null ? currentUser.getUsername() : null;
  }

  public static String getEmail() {
    return currentUser != null ? currentUser.getEmail() : null;
  }

  public static String getRole() {
    return currentUser != null ? currentUser.getRole() : null;
  }

  public static String getAvatarPath() {
    return currentUser != null ? currentUser.getAvatarPath() : null;
  }

  public static double getBalance() {
    return currentUser != null ? currentUser.getBalance() : 0.0;
  }

  public static String getFullName() {
    return currentUser != null ? currentUser.getFullName() : null;
  }

  public static String getPhone() {
    return currentUser != null ? currentUser.getPhone() : null;
  }

  public static String getJoinDate() {
    return currentUser != null ? currentUser.getCreatedAt() : null;
  }

  // ── Updaters ─────────────────────────────────────────────────────────────

  public static void setProfileDetail(String fullName, String phone, String joinDate) {
    if (currentUser == null) {
      return;
    }
    currentUser.setFullName(fullName);
    currentUser.setPhone(phone);
    currentUser.setCreatedAt(joinDate);
  }

  public static void updateProfile(String fullName, String email, String phone) {
    if (currentUser == null) {
      return;
    }
    currentUser.setFullName(fullName);
    currentUser.setEmail(email);
    currentUser.setPhone(phone);
  }

  public static void updateBalance(double balance) {
    if (currentUser != null) {
      currentUser.setBalance(balance);
    }
  }

  public static void updateAvatar(String path) {
    if (currentUser != null) {
      currentUser.setAvatarPath(path);
    }
  }

  public static void updateRole(String role) {
    if (currentUser != null) {
      currentUser.setRole(role);
    }
  }

  /**
   * Trả về nguyên đối tượng DTO hiện tại. (Nếu thực sự muốn bảo vệ dữ liệu tuyệt đối, có thể cân
   * nhắc Implement giao diện Cloneable cho UserDTO)
   */
  public static UserDTO getCurrentUser() {
    return currentUser;
  }
}