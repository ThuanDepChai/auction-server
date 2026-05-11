package com.nhom15.client.command;

import com.google.gson.JsonObject;
import com.nhom15.client.network.SocketClient;
import javafx.application.Platform;

import java.util.function.Consumer;

public class ProfileCommand {

    public static void fetchProfile(int userId, Consumer<JsonObject> callback) {
        JsonObject data = new JsonObject();
        data.addProperty("userId", userId);
        execute("GET_PROFILE", data, callback);
    }

    public static void updateProfile(int userId, String fullName,
                                     String email, String phone,
                                     Consumer<JsonObject> callback) {
        JsonObject data = new JsonObject();
        data.addProperty("userId",   userId);
        data.addProperty("fullName", fullName);
        data.addProperty("email",    email);
        data.addProperty("phone",    phone);
        execute("UPDATE_PROFILE", data, callback);
    }

    public static void changePassword(int userId, String oldPassword,
                                      String newPassword, Consumer<JsonObject> callback) {
        JsonObject data = new JsonObject();
        data.addProperty("userId",      userId);
        data.addProperty("oldPassword", oldPassword);
        data.addProperty("newPassword", newPassword);
        execute("CHANGE_PASSWORD", data, callback);
    }

    public static void upgradeToSeller(int userId,
                                       Consumer<Boolean> onSuccess, Runnable onFail) {
        System.out.println("[ProfileCommand] upgradeToSeller → userId = " + userId);
        JsonObject data = new JsonObject();
        data.addProperty("userId", userId);
        execute("UPGRADE_TO_SELLER", data, response -> {
            System.out.println("[ProfileCommand] upgradeToSeller response = " + response);
            if (response != null && "SUCCESS".equals(response.get("status").getAsString()))
                onSuccess.accept(true);
            else
                onFail.run();
        });
    }

    /** Lấy avatar từ server dưới dạng base64 */
    public static void fetchAvatar(int userId, Consumer<JsonObject> callback) {
        JsonObject data = new JsonObject();
        data.addProperty("userId", userId);
        execute("GET_AVATAR", data, callback);
    }

    /** Avatar dùng base64 binary — build riêng, không chung data pattern */
    public static void updateAvatar(int userId, String base64Image,
                                    String extension, Consumer<JsonObject> callback) {
        JsonObject data = new JsonObject();
        data.addProperty("userId",      userId);
        data.addProperty("imageBase64", base64Image);
        data.addProperty("extension",   extension);
        execute("UPDATE_AVATAR", data, callback);
    }

    // ── Core — giống pattern gốc của bạn ─────────────────────────────────────
    private static void execute(String action, JsonObject data, Consumer<JsonObject> callback) {
        JsonObject request = new JsonObject();
        request.addProperty("action", action);
        request.add("data", data);
        new Thread(() -> {
            try {
                JsonObject response = SocketClient.sendRequest(request);
                Platform.runLater(() -> callback.accept(response));
            } catch (Exception e) {
                System.err.println("[ProfileCommand] Lỗi " + action + ": " + e.getMessage());
                Platform.runLater(() -> callback.accept(null));
            }
        }).start();
    }
}