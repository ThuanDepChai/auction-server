package com.nhom15.client.command;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.nhom15.client.network.SocketClient;
import javafx.application.Platform;
import java.util.function.Consumer;

public class HomeCommand {

    /** Gửi Command qua Socket và trả về kết quả bằng Callback (Consumer) */
    public static void fetchFeaturedProducts(Consumer<JsonArray> onSuccess, Runnable onFail) {
        executeCommand("GET_FEATURED_PRODUCTS", new JsonObject(), response -> {
            if (isSuccess(response) && response.has("items")) {
                onSuccess.accept(response.getAsJsonArray("items"));
            } else {
                onFail.run();
            }
        });
    }

    public static void fetchActiveAuctions(Consumer<JsonArray> onSuccess, Runnable onFail) {
        executeCommand("GET_ACTIVE_AUCTIONS", new JsonObject(), response -> {
            if (isSuccess(response) && response.has("auctions")) {
                onSuccess.accept(response.getAsJsonArray("auctions"));
            } else {
                onFail.run();
            }
        });
    }

    public static void searchProducts(String keyword, String category, Consumer<JsonArray> onSuccess, Runnable onFail) {
        JsonObject data = new JsonObject();
        data.addProperty("keyword", keyword);
        data.addProperty("category", category);

        executeCommand("SEARCH_PRODUCTS", data, response -> {
            if (isSuccess(response) && response.has("items")) {
                onSuccess.accept(response.getAsJsonArray("items"));
            } else {
                onFail.run();
            }
        });
    }

    // --- Lõi thực thi Command mạng (ẩn đi sự phức tạp của Thread và Platform.runLater) ---
    private static void executeCommand(String action, JsonObject data, Consumer<JsonObject> callback) {
        JsonObject request = new JsonObject();
        request.addProperty("action", action);
        if (data != null && data.size() > 0) request.add("data", data);

        new Thread(() -> {
            try {
                JsonObject response = SocketClient.sendRequest(request);
                Platform.runLater(() -> callback.accept(response));
            } catch (Exception e) {
                System.err.println("[HomeCommand] Lỗi gọi API " + action + ": " + e.getMessage());
                Platform.runLater(() -> callback.accept(null));
            }
        }).start();
    }

    private static boolean isSuccess(JsonObject response) {
        return response != null && response.has("status") && "SUCCESS".equals(response.get("status").getAsString());
    }

    public static void fetchUserAvatar(long userId, Consumer<String> onSuccess) {
        JsonObject data = new JsonObject();
        data.addProperty("userId", userId);

        executeCommand("GET_AVATAR", data, response -> {
            if (response != null && "SUCCESS".equals(response.get("status").getAsString())) {
                if (response.has("imageBase64") && !response.get("imageBase64").isJsonNull()) {
                    onSuccess.accept(response.get("imageBase64").getAsString());
                }
            }
        });
    }
}