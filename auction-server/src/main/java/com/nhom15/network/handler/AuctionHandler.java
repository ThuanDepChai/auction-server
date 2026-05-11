package com.nhom15.network.handler;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.nhom15.service.AuctionService;
import com.nhom15.service.ItemService;

/**
 * AuctionHandler — xử lý tất cả request liên quan đến đấu giá. Không chứa routing — chỉ chứa logic
 * của từng action.
 */
public class AuctionHandler {

  private final AuctionService auctionService = new AuctionService();
  private final ItemService itemService = new ItemService();

  public JsonObject handle(JsonObject request) {
    String action = request.get("action").getAsString();
    JsonObject d = request.has("data") ? request.getAsJsonObject("data") : new JsonObject();

    return switch (action) {
      case "GET_AUCTIONS",
           "GET_ACTIVE_AUCTIONS" -> handleGetAuctions();
      case "GET_AUCTION_DETAIL" -> handleGetAuctionDetail(d);
      case "CREATE_AUCTION" -> handleCreateAuction(d);
      case "PLACE_BID" -> handlePlaceBid(d);
      case "GET_BID_HISTORY" -> handleGetBidHistory(d);
      case "END_AUCTION" -> handleEndAuction(d);
      case "GET_MY_AUCTIONS" -> handleGetMyAuctions(d);
      case "CREATE_ITEM" -> handleCreateItem(d);
      case "GET_MY_ITEMS" -> handleGetMyItems(d);
      case "GET_FEATURED_PRODUCTS" -> handleGetFeaturedProducts();
      case "SEARCH_PRODUCTS" -> handleSearchProducts(d);
      case "DELETE_ITEM" -> handleDeleteItem(d);
      default -> error("AuctionHandler không hỗ trợ action: " + action);
    };
  }

  // ── Handlers ─────────────────────────────────────────────────────────────

  private JsonObject handleGetAuctions() {
    JsonArray auctions = auctionService.getActiveAuctions();
    JsonObject res = new JsonObject();
    res.addProperty("status", "SUCCESS");
    res.add("auctions", auctions);
    return res;
  }

  private JsonObject handleGetAuctionDetail(JsonObject d) {
    int auctionId = d.get("auctionId").getAsInt();
    JsonObject auction = auctionService.getAuctionDetail(auctionId);
    JsonObject res = new JsonObject();
    if (auction != null) {
      res.addProperty("status", "SUCCESS");
      res.add("auction", auction);
    } else {
      res.addProperty("status", "FAIL");
      res.addProperty("message", "Không tìm thấy phiên đấu giá!");
    }
    return res;
  }

  /**
   * Client gửi lên: itemId, sellerId, startPrice, minStep, endTime
   */
  private JsonObject handleCreateAuction(JsonObject d) {
    try {
      int itemId = d.get("itemId").getAsInt();
      int sellerId = d.get("sellerId").getAsInt();
      double startPrice = d.get("startPrice").getAsDouble();
      double minStep = d.get("minStep").getAsDouble();
      String endTime = d.get("endTime").getAsString();

      return auctionService.createAuction(itemId, sellerId, startPrice, minStep, endTime);

    } catch (Exception e) {
      return error("Lỗi tạo phiên đấu giá: " + e.getMessage());
    }
  }

  private JsonObject handlePlaceBid(JsonObject d) {
    int auctionId = d.get("auctionId").getAsInt();
    int bidderId = d.get("bidderId").getAsInt();
    double amount = d.get("amount").getAsDouble();
    return auctionService.placeBid(auctionId, bidderId, amount);
  }

  private JsonObject handleGetBidHistory(JsonObject d) {
    int auctionId = d.get("auctionId").getAsInt();
    JsonArray history = auctionService.getBidHistory(auctionId);
    JsonObject res = new JsonObject();
    res.addProperty("status", "SUCCESS");
    res.add("history", history);
    return res;
  }

  private JsonObject handleEndAuction(JsonObject d) {
    int auctionId = d.get("auctionId").getAsInt();
    boolean ok = auctionService.endAuction(auctionId);
    JsonObject res = new JsonObject();
    res.addProperty("status", ok ? "SUCCESS" : "FAIL");
    res.addProperty("message", ok ? "Kết thúc phiên đấu giá thành công!" : "Thất bại!");
    return res;
  }

  private JsonObject handleGetMyAuctions(JsonObject d) {
    int sellerId = d.get("sellerId").getAsInt();
    JsonArray auctions = auctionService.getAuctionsBySeller(sellerId);
    JsonObject res = new JsonObject();
    res.addProperty("status", "SUCCESS");
    res.add("auctions", auctions);
    return res;
  }

// ── Item Handlers (Dành cho Seller Dashboard) ─────────────────────────

  private JsonObject handleCreateItem(JsonObject d) {
    try {
      int sellerId = d.get("sellerId").getAsInt();
      String name = d.get("name").getAsString();
      String desc = d.has("description") ? d.get("description").getAsString() : "";
      String category = d.has("category") ? d.get("category").getAsString() : "";
      double startPrice = d.get("startPrice").getAsDouble();
      String imageBase64 = d.has("imageBase64") ? d.get("imageBase64").getAsString() : "";
      String extension = d.has("extension") ? d.get("extension").getAsString() : "jpg";

      return itemService.createItem(sellerId, name, desc, category, startPrice, imageBase64,
          extension);
    } catch (Exception e) {
      return error("Lỗi tạo sản phẩm: " + e.getMessage());
    }
  }

  private JsonObject handleGetMyItems(JsonObject d) {
    try {
      int sellerId = d.get("sellerId").getAsInt();
      JsonArray items = itemService.getItemsBySeller(sellerId);
      JsonObject res = new JsonObject();
      res.addProperty("status", "SUCCESS");
      res.add("items", items);
      return res;
    } catch (Exception e) {
      return error("Lỗi lấy danh sách sản phẩm: " + e.getMessage());
    }
  }

  private JsonObject handleDeleteItem(JsonObject d) {
    try {
      int itemId = d.get("itemId").getAsInt();
      boolean ok = itemService.deleteItem(itemId);
      JsonObject res = new JsonObject();
      res.addProperty("status", ok ? "SUCCESS" : "FAIL");
      res.addProperty("message", ok ? "Xóa sản phẩm thành công!" : "Xóa thất bại!");
      return res;
    } catch (Exception e) {
      return error("Lỗi xóa sản phẩm: " + e.getMessage());
    }
  }

// ── Lấy Sản phẩm cho trang chủ ────────────────────────────────────────

  private JsonObject handleGetFeaturedProducts() {
    try {
      JsonArray items = itemService.getFeaturedItems();
      JsonObject res = new JsonObject();
      res.addProperty("status", "SUCCESS");
      res.add("items", items);
      return res;
    } catch (Exception e) {
      return error("Lỗi lấy sản phẩm nổi bật: " + e.getMessage());
    }
  }

  private JsonObject handleSearchProducts(JsonObject d) {
    try {
      String keyword = d.has("keyword") ? d.get("keyword").getAsString() : "";
      String category = d.has("category") ? d.get("category").getAsString() : "";

      JsonArray items = itemService.searchItems(keyword, category);
      JsonObject res = new JsonObject();
      res.addProperty("status", "SUCCESS");
      res.add("items", items);
      return res;
    } catch (Exception e) {
      return error("Lỗi tìm kiếm sản phẩm: " + e.getMessage());
    }
  }

  // ── Util ─────────────────────────────────────────────────────────────────

  private JsonObject error(String message) {
    JsonObject r = new JsonObject();
    r.addProperty("status", "ERROR");
    r.addProperty("message", message);
    return r;
  }
}