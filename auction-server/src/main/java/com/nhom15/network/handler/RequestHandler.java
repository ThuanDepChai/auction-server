package com.nhom15.network.handler;

import com.google.gson.JsonObject;

/**
 * RequestHandler — chỉ routing, không chứa bất kỳ business logic nào.
 *
 * Nguyên tắc: nhìn vào "action" và chuyển sang đúng handler.
 * Mọi xử lý thực sự nằm trong UserHandler hoặc AuctionHandler.
 */
public class RequestHandler {

  private final UserHandler    userHandler    = new UserHandler();
  private final AuctionHandler auctionHandler = new AuctionHandler();

  public JsonObject handle(JsonObject request) {
    if (!request.has("action")) {
      return error("Request thiếu trường 'action'");
    }

    String action = request.get("action").getAsString();

    return switch (action) {
      // ── User actions ──────────────────────────────────────────────────
      case "CHECK_USERNAME",
           "REGISTER",
           "LOGIN",
           "GET_PROFILE",
           "UPDATE_PROFILE",
           "CHANGE_PASSWORD",
           "UPGRADE_TO_SELLER",
           "UPDATE_AVATAR",
           "GET_AVATAR"
        -> userHandler.handle(request);

      // ── Item + Auction actions ────────────────────────────────────────
      case "CREATE_ITEM",
           "GET_FEATURED_PRODUCTS",
           "SEARCH_PRODUCTS",
           "GET_MY_ITEMS",
           "DELETE_ITEM",
           "CREATE_AUCTION",
           "GET_ACTIVE_AUCTIONS",
           "GET_AUCTION_DETAIL",
           "PLACE_BID",
           "GET_BID_HISTORY",
           "GET_MY_AUCTIONS",
           "END_AUCTION"
        -> auctionHandler.handle(request);

      default -> error("Hành động không xác định: " + action);
    };
  }

  // ── Util ─────────────────────────────────────────────────────────────────

  private JsonObject error(String message) {
    JsonObject r = new JsonObject();
    r.addProperty("status",  "ERROR");
    r.addProperty("message", message);
    return r;
  }
}