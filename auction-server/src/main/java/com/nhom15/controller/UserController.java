package com.nhom15.controller;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.nhom15.model.user.User;
import com.nhom15.service.UserService;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;

public class UserController {
    private final UserService userService = new UserService();
    private final Gson gson = new Gson();

    public JsonObject handleCheckUsername(JsonObject d) {
        JsonObject res = new JsonObject();
        String user = d.get("username").getAsString();
        boolean exists = userService.isExists(user);
        res.addProperty("status", exists ? "EXISTS" : "AVAILABLE");
        res.addProperty("message", exists ? "Tên đã tồn tại!" : "Có thể sử dụng.");
        return res;
    }

    public JsonObject handleRegister(JsonObject d) {
        JsonObject res = new JsonObject();
        boolean ok = userService.register(d.get("username").getAsString(), d.get("password").getAsString(), d.get("email").getAsString());
        res.addProperty("status", ok ? "SUCCESS" : "FAIL");
        return res;
    }

    public JsonObject handleLogin(JsonObject d) {
        JsonObject res = new JsonObject();
        // Gọi hàm login (trả về User object)
        User user = userService.login(d.get("username").getAsString(), d.get("password").getAsString());
        if (user != null) {
            res.addProperty("status", "SUCCESS");
            res.add("user", gson.toJsonTree(user));
            res.addProperty("userId", user.getId());
            res.addProperty("role", user.getRole().name());
        } else {
            res.addProperty("status", "FAIL");
            res.addProperty("message", "Sai tài khoản hoặc mật khẩu!");
        }
        return res;
    }

    public JsonObject handleGetProfile(JsonObject d) {
        JsonObject res = new JsonObject();
        JsonObject profile = userService.getProfile(d.get("userId").getAsInt());
        if (profile != null) {
            res.addProperty("status", "SUCCESS");
            res.add("profile", profile);
        } else {
            res.addProperty("status", "FAIL");
        }
        return res;
    }

    public JsonObject handleUpdateProfile(JsonObject d) {
        JsonObject res = new JsonObject();
        boolean ok = userService.updateProfile(d.get("userId").getAsInt(), d.get("fullName").getAsString(), d.get("email").getAsString(), d.get("phone").getAsString());
        res.addProperty("status", ok ? "SUCCESS" : "FAIL");
        return res;
    }

    public JsonObject handleChangePassword(JsonObject d) {
        JsonObject res = new JsonObject(); // ĐÃ SỬA: Không còn là JsonProperty nữa!
        boolean ok = userService.changePassword(d.get("userId").getAsInt(), d.get("oldPassword").getAsString(), d.get("newPassword").getAsString());
        res.addProperty("status", ok ? "SUCCESS" : "FAIL");
        return res;
    }

    public JsonObject handleUpgradeToSeller(JsonObject d) {
        JsonObject res = new JsonObject();
        boolean ok = userService.upgradeToSeller(d.get("userId").getAsInt());
        res.addProperty("status", ok ? "SUCCESS" : "FAIL");
        return res;
    }

    public JsonObject handleUpdateAvatar(JsonObject d) {
        JsonObject res = new JsonObject();
        try {
            int uid = d.get("userId").getAsInt();
            byte[] bytes = Base64.getDecoder().decode(d.get("imageBase64").getAsString());
            String path = "avatars/avatar_" + uid + "." + d.get("extension").getAsString();
            Files.write(Paths.get(path), bytes);
            userService.updateAvatar(uid, path);
            res.addProperty("status", "SUCCESS");
            res.addProperty("avatarPath", path);
        } catch (Exception e) {
            res.addProperty("status", "FAIL");
        }
        return res;
    }

    public JsonObject handleGetAvatar(JsonObject d) {
        JsonObject res = new JsonObject();
        try {
            String path = userService.getAvatarPath(d.get("userId").getAsInt());
            if (path != null && new File(path).exists()) {
                res.addProperty("status", "SUCCESS");
                res.addProperty("imageBase64", Base64.getEncoder().encodeToString(Files.readAllBytes(Paths.get(path))));
            } else {
                res.addProperty("status", "NO_AVATAR");
            }
        } catch (Exception e) {
            res.addProperty("status", "FAIL");
        }
        return res;
    }
}