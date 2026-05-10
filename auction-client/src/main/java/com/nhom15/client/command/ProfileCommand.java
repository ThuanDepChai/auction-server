package com.nhom15.client.command;

import com.google.gson.JsonObject;
import com.nhom15.client.network.SocketClient;
import javafx.application.Platform;

import java.util.function.Consumer;

public class ProfileCommand {

    public static void fetchProfile(long userId, Consumer<JsonObject> callback) {
        JsonObject data = new JsonObject();
        data.addProperty("userId", userId);
        executeCommand("GET_PROFILE", data, callback);
    }

    public static void updateProfile(long userId, String fullName, String email, String phone, Consumer<JsonObject> callback) {
        JsonObject data = new JsonObject();
        data.addProperty("userId", userId);
        data.addProperty("fullName", fullName);
        data.addProperty("email", email);
        data.addProperty("phone", phone);
        executeCommand("UPDATE_PROFILE", data, callback);
    }

    public static void changePassword(long userId, String oldPassword, String newPassword, Consumer<JsonObject> callback) {
        JsonObject data = new JsonObject();
        data.addProperty("userId", userId);
        data.addProperty("oldPassword", oldPassword);
        data.addProperty("newPassword", newPassword);
        executeCommand("CHANGE_PASSWORD", data, callback);
    }

    public static void updateAvatar(long userId, String base64Image, String extension, Consumer<JsonObject> callback) {
        JsonObject data = new JsonObject();
        data.addProperty("userId", userId);
        data.addProperty("imageBase64", base64Image);
        data.addProperty("extension", extension);
        executeCommand("UPDATE_AVATAR", data, callback);
    }

    public static void upgradeToSeller(long userId, Consumer<Boolean> onSuccess, Runnable onFail) {
        JsonObject data = new JsonObject();
        data.addProperty("userId", userId);
        executeCommand("UPGRADE_TO_SELLER", data, response -> {
            if (response != null && "SUCCESS".equals(response.get("status").getAsString())) {
                onSuccess.accept(true);
            } else {
                onFail.run();
            }
        });
    }

    // --- Lõi thực thi Command mạng ---
    private static void executeCommand(String action, JsonObject data, Consumer<JsonObject> callback) {
        JsonObject request = new JsonObject();
        request.addProperty("action", action);
        if (data != null && data.size() > 0) request.add("data", data);

        new Thread(() -> {
            try {
                JsonObject response = SocketClient.sendRequest(request);
                Platform.runLater(() -> callback.accept(response));
            } catch (Exception e) {
                System.err.println("[ProfileCommand] Lỗi gọi API " + action + ": " + e.getMessage());
                Platform.runLater(() -> callback.accept(null));
            }
        }).start();
    }
}