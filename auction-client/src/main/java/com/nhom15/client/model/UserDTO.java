package com.nhom15.client.model;

import com.google.gson.annotations.SerializedName;

/**
 * Lớp Data Transfer Object chứa thông tin người dùng. Chỉ dùng để vận chuyển dữ liệu từ Server về
 * Client và lưu tại Session.
 */
public class UserDTO {

  // Sử dụng @SerializedName để GSON biết cách map đúng tên cột nếu JSON trả về dạng snake_case
  // Nếu Server trả về JSON chuẩn camelCase (ví dụ: "userId") thì không cần dòng này.
  @SerializedName("user_id")
  private int userId;

  private String username;
  private String email;

  @SerializedName("full_name")
  private String fullName;

  private String phone;
  private String role;
  private double balance;

  @SerializedName("avatar_path")
  private String avatarPath;

  @SerializedName("created_at")
  private String createdAt;

  // 1. CONSTRUCTOR (Hàm khởi tạo)
  // Bắt buộc phải có một Constructor rỗng (không tham số) để thư viện GSON có thể tạo object tự động.
  public UserDTO() {
  }

  // Constructor đầy đủ tham số (Tùy chọn, dùng khi bạn muốn tự tạo object bằng tay)
  public UserDTO(int userId, String username, String email, String fullName, String phone,
      String role, double balance, String avatarPath, String createdAt) {
    this.userId = userId;
    this.username = username;
    this.email = email;
    this.fullName = fullName;
    this.phone = phone;
    this.role = role;
    this.balance = balance;
    this.avatarPath = avatarPath;
    this.createdAt = createdAt;
  }

  // 2. GETTER VÀ SETTER
  // Đây là các hàm BẮT BUỘC để các class khác (như Controller, SessionManager) có thể lấy và sửa dữ liệu.

  public int getUserId() {
    return userId;
  }

  public void setUserId(int userId) {
    this.userId = userId;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getFullName() {
    return fullName;
  }

  public void setFullName(String fullName) {
    this.fullName = fullName;
  }

  public String getPhone() {
    return phone;
  }

  public void setPhone(String phone) {
    this.phone = phone;
  }

  public String getRole() {
    return role;
  }

  public void setRole(String role) {
    this.role = role;
  }

  public double getBalance() {
    return balance;
  }

  public void setBalance(double balance) {
    this.balance = balance;
  }

  public String getAvatarPath() {
    return avatarPath;
  }

  public void setAvatarPath(String avatarPath) {
    this.avatarPath = avatarPath;
  }

  public String getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(String createdAt) {
    this.createdAt = createdAt;
  }

  // 3. TOSTRING (Hàm in ra chuỗi)
  // Rất quan trọng khi bạn muốn debug (System.out.println(userDTO)).
  // Nó sẽ in ra toàn bộ thông tin thay vì in ra cái địa chỉ bộ nhớ vô nghĩa.
  @Override
  public String toString() {
    return "UserDTO{" +
        "userId=" + userId +
        ", username='" + username + '\'' +
        ", email='" + email + '\'' +
        ", fullName='" + fullName + '\'' +
        ", phone='" + phone + '\'' +
        ", role='" + role + '\'' +
        ", balance=" + balance +
        ", avatarPath='" + avatarPath + '\'' +
        ", createdAt='" + createdAt + '\'' +
        '}';
  }
}