package com.nhom15.network.handler;

import com.nhom15.model.user.User;
import com.nhom15.service.UserService;
import com.google.gson.JsonObject;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;

/**
 * Xử lý tất cả request liên quan đến User.
 * Không chứa routing — chỉ chứa business logic của từng action.
 */
public class UserHandler {

  private final UserService userService = new UserService();

  public JsonObject handle(JsonObject request) {
    String action = request.get("action").getAsString();
    JsonObject d  = request.has("data") ? request.getAsJsonObject("data") : new JsonObject();

    return switch (action) {
      case "CHECK_USERNAME"    -> handleCheckUsername(d);
      case "REGISTER"          -> handleRegister(d);
      case "LOGIN"             -> handleLogin(d);
      case "GET_PROFILE"       -> handleGetProfile(d);
      case "UPDATE_PROFILE"    -> handleUpdateProfile(d);
      case "CHANGE_PASSWORD"   -> handleChangePassword(d);
      case "UPGRADE_TO_SELLER" -> handleUpgradeToSeller(d);
      case "UPDATE_AVATAR"     -> handleUpdateAvatar(d);
      case "GET_AVATAR"        -> handleGetAvatar(d);
      default                  -> error("UserHandler không hỗ trợ action: " + action);
    };
  }

  // ── Handlers ─────────────────────────────────────────────────────────────

  private JsonObject handleCheckUsername(JsonObject d) {
    JsonObject response = new JsonObject();
    String username = d.get("username").getAsString();
    if (userService.isExists(username)) {
      response.addProperty("status",  "EXISTS");
      response.addProperty("message", "Tên đăng nhập đã tồn tại!");
    } else {
      response.addProperty("status",  "AVAILABLE");
      response.addProperty("message", "Tên này có thể sử dụng.");
    }
    return response;
  }

  private JsonObject handleRegister(JsonObject d) {
    JsonObject response = new JsonObject();
    String username = d.get("username").getAsString();
    String email    = d.get("email").getAsString();
    String password = d.get("password").getAsString();
    boolean ok = userService.register(username, password, email);
    response.addProperty("status",  ok ? "SUCCESS" : "FAIL");
    response.addProperty("message", ok ? "Tạo tài khoản thành công!"
      : "Đăng ký thất bại (Tên đã tồn tại hoặc lỗi hệ thống)!");
    return response;
  }

  private JsonObject handleLogin(JsonObject d) {
    JsonObject response = new JsonObject();
    String username = d.get("username").getAsString();
    String password = d.get("password").getAsString();
    User user = userService.getUserForLogin(username, password);
    if (user != null) {
      response.addProperty("status",     "SUCCESS");
      response.addProperty("message",    "Đăng nhập thành công!");
      response.addProperty("userId",     user.getId());
      response.addProperty("username",   user.getUsername());
      response.addProperty("email",      user.getEmail());
      response.addProperty("role",       user.getRole().name());
      response.addProperty("balance",    user.getBalance());
      response.addProperty("avatarPath", user.getAvatarPath());
      response.addProperty("fullName",   user.getFullName());
      response.addProperty("phone",      user.getPhone());
    } else {
      response.addProperty("status",  "FAIL");
      response.addProperty("message", "Sai tài khoản hoặc mật khẩu!");
    }
    return response;
  }

  private JsonObject handleGetProfile(JsonObject d) {
    JsonObject response = new JsonObject();
    int uid = d.get("userId").getAsInt();
    JsonObject profile = userService.getProfile(uid);
    if (profile != null) {
      response.addProperty("status", "SUCCESS");
      response.add("profile", profile);
    } else {
      response.addProperty("status",  "FAIL");
      response.addProperty("message", "Không tìm thấy người dùng!");
    }
    return response;
  }

  private JsonObject handleUpdateProfile(JsonObject d) {
    JsonObject response = new JsonObject();
    int    uid      = d.get("userId").getAsInt();
    String fullName = d.get("fullName").getAsString();
    String email    = d.get("email").getAsString();
    String phone    = d.get("phone").getAsString();
    boolean ok = userService.updateProfile(uid, fullName, email, phone);
    response.addProperty("status",  ok ? "SUCCESS" : "FAIL");
    response.addProperty("message", ok ? "Cập nhật thành công!" : "Cập nhật thất bại!");
    return response;
  }

  private JsonObject handleChangePassword(JsonObject d) {
    JsonObject response = new JsonObject();
    int    uid     = d.get("userId").getAsInt();
    String oldPass = d.get("oldPassword").getAsString();
    String newPass = d.get("newPassword").getAsString();
    boolean ok = userService.changePassword(uid, oldPass, newPass);
    response.addProperty("status",  ok ? "SUCCESS" : "FAIL");
    response.addProperty("message", ok ? "Đổi mật khẩu thành công!" : "Mật khẩu hiện tại không đúng!");
    return response;
  }

  private JsonObject handleUpgradeToSeller(JsonObject d) {
    JsonObject response = new JsonObject();
    int uid = d.get("userId").getAsInt();
    boolean ok = userService.upgradeToSeller(uid);
    response.addProperty("status",  ok ? "SUCCESS" : "FAIL");
    response.addProperty("message", ok ? "Đăng ký bán hàng thành công!" : "Thất bại, vui lòng thử lại!");
    return response;
  }

  private JsonObject handleUpdateAvatar(JsonObject d) {
    JsonObject response = new JsonObject();
    int    uid        = d.get("userId").getAsInt();
    String base64Data = d.get("imageBase64").getAsString();
    String extension  = d.get("extension").getAsString();
    try {
      byte[] imageBytes = Base64.getDecoder().decode(base64Data);
      String fileName   = "avatar_" + uid + "." + extension;
      String filePath   = "avatars/" + fileName;
      Files.write(Paths.get(filePath), imageBytes);
      boolean ok = userService.updateAvatar(uid, filePath);
      response.addProperty("status",     ok ? "SUCCESS" : "FAIL");
      response.addProperty("message",    ok ? "Cập nhật ảnh thành công!" : "Lưu DB thất bại!");
      response.addProperty("avatarPath", filePath);
    } catch (Exception e) {
      response.addProperty("status",  "FAIL");
      response.addProperty("message", "Lỗi xử lý ảnh: " + e.getMessage());
    }
    return response;
  }

  private JsonObject handleGetAvatar(JsonObject d) {
    JsonObject response = new JsonObject();
    int uid = d.get("userId").getAsInt();
    String avatarPath = userService.getAvatarPath(uid);
    try {
      File imgFile = avatarPath != null ? new File(avatarPath) : null;
      if (imgFile != null && !avatarPath.isEmpty() && imgFile.exists()) {
        byte[] imageBytes = Files.readAllBytes(imgFile.toPath());
        String base64 = Base64.getEncoder().encodeToString(imageBytes);
        response.addProperty("status",      "SUCCESS");
        response.addProperty("imageBase64", base64);
        response.addProperty("avatarPath",  avatarPath);
      } else {
        response.addProperty("status",  "NO_AVATAR");
        response.addProperty("message", "Chưa có ảnh đại diện!");
      }
    } catch (Exception e) {
      response.addProperty("status",  "FAIL");
      response.addProperty("message", "Lỗi đọc ảnh: " + e.getMessage());
    }
    return response;
  }

  // ── Util ─────────────────────────────────────────────────────────────────

  private JsonObject error(String message) {
    JsonObject r = new JsonObject();
    r.addProperty("status",  "ERROR");
    r.addProperty("message", message);
    return r;
  }
}