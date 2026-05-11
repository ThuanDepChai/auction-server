package com.nhom15.service;

import com.google.gson.JsonObject;
import com.nhom15.dao.UserDAO;
import com.nhom15.model.user.User;
import com.nhom15.util.PasswordUtil;

public class UserService {

  private final UserDAO userDAO;

  // ── Constructor mặc định – dùng trong production ──────────────────────
  // Toàn bộ code cũ gọi new UserService() vẫn chạy bình thường
  public UserService() {
    this.userDAO = new UserDAO();
  }

  // ── Constructor injection – dùng trong Unit Test ───────────────────────
  // Mockito sẽ inject UserDAO mock qua constructor này
  public UserService(UserDAO userDAO) {
    this.userDAO = userDAO;
  }

  public boolean isExists(String username) {
    return userDAO.isUsernameExists(username);
  }

  public boolean register(String username, String password, String email) {
      if (userDAO.isUsernameExists(username)) {
          return false;
      }
    String hashedPassword = PasswordUtil.hashPassword(password);
    return userDAO.registerUser(username, hashedPassword, email);
  }

  // Trả về đối tượng User để Controller bóc tách
  public User login(String username, String inputPassword) {
    User user = userDAO.findByUsername(username);
    if (user != null && PasswordUtil.verifyPassword(inputPassword, user.getPasswordHash())) {
      return user;
    }
    return null;
  }

  public JsonObject getProfile(int userId) {
    return userDAO.getProfile(userId);
  }

  public boolean updateAvatar(int userId, String avatarPath) {
    return userDAO.updateAvatar(userId, avatarPath);
  }

  public boolean changePassword(int userId, String oldPassword, String newPassword) {
    User user = userDAO.findById(userId);
      if (user == null) {
          return false;
      }
      if (!PasswordUtil.verifyPassword(oldPassword, user.getPasswordHash())) {
          return false;
      }
    return userDAO.updatePassword(userId, PasswordUtil.hashPassword(newPassword));
  }

  public boolean updateProfile(int userId, String fullName, String email, String phone) {
    return userDAO.updateProfile(userId, fullName, email, phone);
  }

  public boolean upgradeToSeller(int userId) {
    return userDAO.upgradeToSeller(userId);
  }

  public String getAvatarPath(int userId) {
    return userDAO.getAvatarPath(userId);
  }
}