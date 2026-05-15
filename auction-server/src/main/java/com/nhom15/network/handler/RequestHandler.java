package com.nhom15.network.handler;

import com.google.gson.JsonObject;

/**
 * RequestHandler — router trung tâm. Đọc "action" từ request, điều phối sang đúng Handler.
 * Không chứa bất kỳ business logic nào.
 */
public class RequestHandler {

    private final UserHandler    userHandler    = new UserHandler();
    private final AuctionHandler auctionHandler = new AuctionHandler();
    private final AdminHandler adminHandler = new AdminHandler();

    public JsonObject handle(JsonObject request) {
        if (request == null || !request.has("action")) {
            return error("Request không hợp lệ: thiếu trường 'action'");
        }

        String action = request.get("action").getAsString();

        return switch (action) {

            // ── User ────────────────────────────────────────────────────────────
            case "CHECK_USERNAME",
                 "REGISTER",
                 "LOGIN",
                 "GET_PROFILE",
                 "UPDATE_PROFILE",
                 "CHANGE_PASSWORD",
                 "UPGRADE_TO_SELLER",
                 "UPDATE_AVATAR",
                 "GET_AVATAR"          -> userHandler.handle(request);

            // ── Auction & Item ───────────────────────────────────────────────────
            case "GET_AUCTIONS",
                 "GET_ACTIVE_AUCTIONS",
                 "GET_AUCTION_DETAIL",
                 "CREATE_AUCTION",
                 "PLACE_BID",
                 "GET_BID_HISTORY",
                 "END_AUCTION",
                 "GET_MY_AUCTIONS",
                 "CREATE_ITEM",
                 "GET_FEATURED_PRODUCTS",
                 "SEARCH_PRODUCTS",
                 "GET_MY_ITEMS",
                 "DELETE_ITEM",
                 "GET_ITEM_IMAGE"      -> auctionHandler.handle(request);

            // FIX: Thêm routing cho Auto-Bid (trước đây bị thiếu → luôn trả "Action không được hỗ trợ")
            case "SET_AUTO_BID",
                 "CANCEL_AUTO_BID",
                 "GET_AUTO_BID_STATUS" -> auctionHandler.handle(request);
            // ADMIN:
            case "ADMIN_GET_ALL_USERS",
                 "ADMIN_BAN_USER",
                "ADMIN_GET_ALL_AUCTIONS",
                "ADMIN_CANCEL_AUCTION",
                "ADMIN_GET_STATS"       -> adminHandler.handle(request);

            default -> error("Action không được hỗ trợ: " + action);
        };
    }

    private JsonObject error(String message) {
        JsonObject r = new JsonObject();
        r.addProperty("status", "ERROR");
        r.addProperty("message", message);
        return r;
    }
}