package com.nhom15.controller;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.nhom15.service.UserService;
import com.nhom15.model.user.User; // Đảm bảo import đúng đường dẫn model của bạn

public class UserController {

    private UserService userService = new UserService();
    private Gson gson = new Gson();

    /**
     * Xử lý luồng Đăng nhập từ Client
     */
    public String handleLogin(JsonObject requestData) {
        JsonObject response = new JsonObject();

        try {
            // Lấy dữ liệu Client gửi lên
            String username = requestData.get("username").getAsString();
            String password = requestData.get("password").getAsString();

            // Gọi anh Đầu bếp (UserService) - ĐÃ SỬA LẠI TÊN HÀM THÀNH 'login'
            User user = userService.login(username, password);

            // Đóng gói trả về
            if (user != null) {
                response.addProperty("status", "SUCCESS");
                response.addProperty("message", "Đăng nhập thành công!");
                // GSON tự động bọc Object User thành chuỗi JSON
                response.add("user", gson.toJsonTree(user));
            } else {
                response.addProperty("status", "ERROR");
                response.addProperty("message", "Sai tài khoản hoặc mật khẩu!");
            }

        } catch (Exception e) {
            response.addProperty("status", "ERROR");
            response.addProperty("message", "Thiếu tham số bắt buộc!");
            System.err.println("[UserController-Login] Lỗi: " + e.getMessage());
        }

        return response.toString();
    }

    /**
     * Xử lý luồng Đăng ký từ Client
     */
    public String handleRegister(JsonObject requestData) {
        JsonObject response = new JsonObject();

        try {
            String username = requestData.get("username").getAsString();
            String password = requestData.get("password").getAsString();
            String email    = requestData.get("email").getAsString();

            boolean isSuccess = userService.register(username, password, email);

            if (isSuccess) {
                response.addProperty("status", "SUCCESS");
                response.addProperty("message", "Đăng ký thành công!");
            } else {
                response.addProperty("status", "ERROR");
                response.addProperty("message", "Tên đăng nhập đã tồn tại!");
            }
        } catch (Exception e) {
            response.addProperty("status", "ERROR");
            response.addProperty("message", "Lỗi định dạng dữ liệu gửi lên!");
            System.err.println("[UserController-Register] Lỗi: " + e.getMessage());
        }

        return response.toString();
    }
}