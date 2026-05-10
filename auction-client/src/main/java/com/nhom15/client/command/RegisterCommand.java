package com.nhom15.client.command;

import com.google.gson.JsonObject;

/**
 * RegisterCommand — đóng gói request đăng ký tài khoản.
 *
 * Controller không tự build JsonObject — chỉ khởi tạo command này
 * và gọi executeAsync(), mọi chi tiết giao tiếp server ở đây.
 */
public class RegisterCommand extends ServerCommand {

    private final String username;
    private final String email;
    private final String password;

    public RegisterCommand(String username, String email, String password) {
        this.username = username;
        this.email    = email;
        this.password = password;
    }

    @Override
    protected JsonObject buildRequest() {
        JsonObject data = new JsonObject();
        data.addProperty("username", username);
        data.addProperty("email",    email);
        data.addProperty("password", password);

        JsonObject req = new JsonObject();
        req.addProperty("action", "REGISTER");
        req.add("data", data);
        return req;
    }
}