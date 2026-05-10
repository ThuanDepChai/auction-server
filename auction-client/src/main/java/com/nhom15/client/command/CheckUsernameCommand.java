package com.nhom15.client.command;

import com.google.gson.JsonObject;

/**
 * CheckUsernameCommand — kiểm tra username đã tồn tại chưa (debounce).
 *
 * Dùng trong RegisterController để check realtime khi người dùng gõ.
 * Server trả về status = "EXISTS" hoặc "AVAILABLE".
 */
public class CheckUsernameCommand extends ServerCommand {

    private final String username;

    public CheckUsernameCommand(String username) {
        this.username = username;
    }

    @Override
    protected JsonObject buildRequest() {
        JsonObject data = new JsonObject();
        data.addProperty("username", username);

        JsonObject req = new JsonObject();
        req.addProperty("action", "CHECK_USERNAME");
        req.add("data", data);
        return req;
    }

    /** Trả về true nếu username còn trống (có thể dùng). */
    public static boolean isAvailable(JsonObject response) {
        return response != null
                && "AVAILABLE".equals(response.get("status").getAsString());
    }
}