package com.nhom15.client.command;

import com.google.gson.JsonObject;
import java.util.function.Consumer;

/**
 * Command Pattern entrypoint for seller requests.
 *
 * <p>The public static methods are a small facade for controllers/services. The actual network
 * execution is still a real {@link ServerCommand}: request construction is isolated in
 * {@link #buildRequest()}, and threading/error dispatch stays in the base class.
 */
public class SellerCommand extends ServerCommand {

  private final String action;
  private final JsonObject data;

  private SellerCommand(String action, JsonObject data) {
    this.action = action;
    this.data = data;
  }

  @Override
  protected JsonObject buildRequest() {
    JsonObject request = new JsonObject();
    request.addProperty("action", action);
    if (data != null) {
      request.add("data", data);
    }
    return request;
  }

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
    new SellerCommand(action, data).executeAsync(callback, () -> {
      System.err.println("[SellerCommand] Loi ket noi API " + action);
      if (callback != null) {
        callback.accept(null);
      }
    });
  }
}
