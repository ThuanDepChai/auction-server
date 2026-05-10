package com.nhom15.network.handler;

import com.nhom15.network.AuctionManager;
import com.nhom15.service.AuctionService;
import com.nhom15.service.ItemService;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * Xử lý tất cả request liên quan đến Item và Auction.
 * PLACE_BID đi qua AuctionManager để có per-auction lock.
 */
public class AuctionHandler {

  private final AuctionService auctionService = new AuctionService();
  private final ItemService    itemService    = new ItemService();
  private final AuctionManager auctionManager = AuctionManager.getInstance();

  public JsonObject handle(JsonObject request) {
    String action = request.get("action").getAsString();
    JsonObject d  = request.has("data") ? request.getAsJsonObject("data") : new JsonObject();

    return switch (action) {
      // ── Item ──────────────────────────────────────────────────────────
      case "CREATE_ITEM"          -> handleCreateItem(d);
      case "GET_FEATURED_PRODUCTS"-> handleGetFeaturedProducts();
      case "SEARCH_PRODUCTS"      -> handleSearchProducts(d);
      case "GET_MY_ITEMS"         -> handleGetMyItems(d);
      case "DELETE_ITEM"          -> handleDeleteItem(d);

      // ── Auction ───────────────────────────────────────────────────────
      case "CREATE_AUCTION"       -> handleCreateAuction(d);
      case "GET_ACTIVE_AUCTIONS"  -> handleGetActiveAuctions();
      case "GET_AUCTION_DETAIL"   -> handleGetAuctionDetail(d);
      case "PLACE_BID"            -> handlePlaceBid(d);   // ← qua AuctionManager
      case "GET_BID_HISTORY"      -> handleGetBidHistory(d);
      case "GET_MY_AUCTIONS"      -> handleGetMyAuctions(d);
      case "END_AUCTION"          -> handleEndAuction(d);

      default -> error("AuctionHandler không hỗ trợ action: " + action);
    };
  }

  // ── Item Handlers ─────────────────────────────────────────────────────────

  private JsonObject handleCreateItem(JsonObject d) {
    int    sellerId    = d.get("sellerId").getAsInt();
    String name        = d.get("name").getAsString();
    String description = d.get("description").getAsString();
    String category    = d.get("category").getAsString();
    double startPrice  = d.get("startPrice").getAsDouble();
    String imgBase64   = d.has("imageBase64") ? d.get("imageBase64").getAsString() : "";
    String extension   = d.has("extension")   ? d.get("extension").getAsString()   : "jpg";
    return itemService.createItem(sellerId, name, description, category, startPrice, imgBase64, extension);
  }

  private JsonObject handleGetFeaturedProducts() {
    JsonObject response = new JsonObject();
    JsonArray items = itemService.getFeaturedItems();
    response.addProperty("status", "SUCCESS");
    response.add("items", items);
    return response;
  }

  private JsonObject handleSearchProducts(JsonObject d) {
    JsonObject response = new JsonObject();
    String keyword  = d.has("keyword")  ? d.get("keyword").getAsString()  : "";
    String category = d.has("category") ? d.get("category").getAsString() : "";
    JsonArray items = itemService.searchItems(keyword, category);
    response.addProperty("status", "SUCCESS");
    response.add("items", items);
    return response;
  }

  private JsonObject handleGetMyItems(JsonObject d) {
    JsonObject response = new JsonObject();
    int sellerId = d.get("sellerId").getAsInt();
    JsonArray items = itemService.getItemsBySeller(sellerId);
    response.addProperty("status", "SUCCESS");
    response.add("items", items);
    return response;
  }

  private JsonObject handleDeleteItem(JsonObject d) {
    JsonObject response = new JsonObject();
    int itemId = d.get("itemId").getAsInt();
    boolean ok = itemService.deleteItem(itemId);
    response.addProperty("status",  ok ? "SUCCESS" : "FAIL");
    response.addProperty("message", ok ? "Xóa thành công!" : "Xóa thất bại!");
    return response;
  }

  // ── Auction Handlers ──────────────────────────────────────────────────────

  private JsonObject handleCreateAuction(JsonObject d) {
    int    itemId     = d.get("itemId").getAsInt();
    int    sellerId   = d.get("sellerId").getAsInt();
    double startPrice = d.get("startPrice").getAsDouble();
    double minStep    = d.get("minStep").getAsDouble();
    String endTime    = d.get("endTime").getAsString();
    return auctionService.createAuction(itemId, sellerId, startPrice, minStep, endTime);
  }

  private JsonObject handleGetActiveAuctions() {
    JsonObject response = new JsonObject();
    JsonArray auctions = auctionService.getActiveAuctions();
    response.addProperty("status", "SUCCESS");
    response.add("auctions", auctions);
    return response;
  }

  private JsonObject handleGetAuctionDetail(JsonObject d) {
    JsonObject response = new JsonObject();
    int auctionId = d.get("auctionId").getAsInt();
    JsonObject auction = auctionService.getAuctionDetail(auctionId);
    if (auction != null) {
      response.addProperty("status", "SUCCESS");
      response.add("auction", auction);
    } else {
      response.addProperty("status",  "FAIL");
      response.addProperty("message", "Không tìm thấy phiên đấu giá!");
    }
    return response;
  }

  /**
   * PLACE_BID đi qua AuctionManager (có ReentrantLock per auction)
   * thay vì gọi thẳng AuctionService — đây là điểm khác biệt chính.
   */
  private JsonObject handlePlaceBid(JsonObject d) {
    int    auctionId = d.get("auctionId").getAsInt();
    int    bidderId  = d.get("bidderId").getAsInt();
    double amount    = d.get("amount").getAsDouble();
    return auctionManager.placeBid(auctionId, bidderId, amount);
  }

  private JsonObject handleGetBidHistory(JsonObject d) {
    JsonObject response = new JsonObject();
    int auctionId = d.get("auctionId").getAsInt();
    JsonArray history = auctionService.getBidHistory(auctionId);
    response.addProperty("status", "SUCCESS");
    response.add("history", history);
    return response;
  }

  private JsonObject handleGetMyAuctions(JsonObject d) {
    JsonObject response = new JsonObject();
    int sellerId = d.get("sellerId").getAsInt();
    JsonArray auctions = auctionService.getAuctionsBySeller(sellerId);
    response.addProperty("status", "SUCCESS");
    response.add("auctions", auctions);
    return response;
  }

  private JsonObject handleEndAuction(JsonObject d) {
    JsonObject response = new JsonObject();
    int auctionId = d.get("auctionId").getAsInt();
    boolean ok = auctionService.endAuction(auctionId);
    if (ok) {
      // Giải phóng lock để tránh memory leak
      auctionManager.removeLock(auctionId);
      response.addProperty("status",  "SUCCESS");
      response.addProperty("message", "Kết thúc phiên thành công!");
    } else {
      response.addProperty("status",  "FAIL");
      response.addProperty("message", "Thất bại!");
    }
    return response;
  }

  // ── Util ─────────────────────────────────────────────────────────────────

  private JsonObject error(String message) {
    JsonObject r = new JsonObject();
    r.addProperty("status",  "ERROR");
    r.addProperty("message", message);
    return r;
  }
}