package com.nhom15.client.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.nhom15.client.command.SellerCommand;
import com.nhom15.client.command.ServerCommand;
import java.util.function.Consumer;

public class SellerDashboardService {

  public void fetchItems(int sellerId, Consumer<JsonArray> onSuccess, Runnable onFail) {
    SellerCommand.getMyItems(sellerId, response -> {
      if (ServerCommand.isSuccess(response) && response.has("items")) {
        onSuccess.accept(response.getAsJsonArray("items"));
      } else if (onFail != null) {
        onFail.run();
      }
    });
  }

  public void fetchAuctions(int sellerId, Consumer<JsonArray> onSuccess, Runnable onFail) {
    SellerCommand.getMyAuctions(sellerId, response -> {
      if (ServerCommand.isSuccess(response) && response.has("auctions")) {
        onSuccess.accept(response.getAsJsonArray("auctions"));
      } else if (onFail != null) {
        onFail.run();
      }
    });
  }

  public void createItem(JsonObject payload, Consumer<JsonObject> callback) {
    SellerCommand.createItem(payload, callback);
  }

  public void createAuction(JsonObject payload, Consumer<JsonObject> callback) {
    SellerCommand.createAuction(payload, callback);
  }

  public void deleteItem(int itemId, Consumer<JsonObject> callback) {
    SellerCommand.deleteItem(itemId, callback);
  }

  public void endAuction(int auctionId, Consumer<JsonObject> callback) {
    SellerCommand.endAuction(auctionId, callback);
  }
}
