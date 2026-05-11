package com.nhom15.client.command;

import com.google.gson.JsonObject;
import com.nhom15.client.network.SocketClient;
import javafx.application.Platform;
import java.util.function.Consumer;

public class SellerCommand {

    public static void getMyItems(int sellerId, Consumer<JsonObject> callback) {
        JsonObject data = new JsonObject();
        data.addProperty("sellerId", sellerId);
        executeCommand("GET_MY_ITEMS", data, callback);
    }

    public static void getMyAuctions(int sellerId, Consumer<JsonObject> callback) {
        JsonObject data = new JsonObject();
        data.addProperty("sellerId", sellerId);
        executeCommand("GET_MY_AUCTIONS", data, callback);
    }

    public static void createItem(JsonObject data, Consumer<JsonObject> callback) {
        executeCommand("CREATE_ITEM", data, callback);
    }

    public static void createAuction(JsonObject data, Consumer<JsonObject> callback) {
        executeCommand("CREATE_AUCTION", data, callback);
    }

    public static void deleteItem(int itemId, Consumer<JsonObject> callback) {
        JsonObject data = new JsonObject();
        data.addProperty("itemId", itemId);
        executeCommand("DELETE_ITEM", data, callback);
    }

    public static void endAuction(int auctionId, Consumer<JsonObject> callback) {
        JsonObject data = new JsonObject();
        data.addProperty("auctionId", auctionId);
        executeCommand("END_AUCTION", data, callback);
    }

    private static void executeCommand(String action, JsonObject data, Consumer<JsonObject> callback) {
        JsonObject request = new JsonObject();
        request.addProperty("action", action);
        if (data != null) request.add("data", data);

        new Thread(() -> {
            try {
                JsonObject response = SocketClient.sendRequest(request);
                Platform.runLater(() -> callback.accept(response));
            } catch (Exception e) {
                System.err.println("[SellerCommand] Lỗi API " + action + ": " + e.getMessage());
                Platform.runLater(() -> callback.accept(null));
            }
        }).start();
    }
}