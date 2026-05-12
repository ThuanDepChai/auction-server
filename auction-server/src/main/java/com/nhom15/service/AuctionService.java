package com.nhom15.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.nhom15.dao.AuctionDAO;
import com.nhom15.dao.ItemDAO;
import com.nhom15.exception.AuctionClosedException;
import com.nhom15.exception.InvalidBidException;
import com.nhom15.network.AuctionManager;
import java.io.File;
import java.nio.file.Files;
import java.util.Base64;

public class AuctionService {

  private final AuctionDAO auctionDAO = new AuctionDAO();
  private final ItemDAO itemDAO = new ItemDAO();
  // Singleton — dùng chung toàn server, quản lý lock per auction
  private final AuctionManager auctionManager = AuctionManager.getInstance();

  /**
   * Tạo phiên đấu giá
   */
  public JsonObject createAuction(int itemId, int sellerId, double startPrice,
                                  double minStep, String endTime) {
    JsonObject result = new JsonObject();
    // Cập nhật status item → IN_AUCTION
    itemDAO.updateStatus(itemId, "IN_AUCTION");

    int auctionId = auctionDAO.createAuction(itemId, sellerId, startPrice, minStep, endTime);
    if (auctionId > 0) {
      result.addProperty("status", "SUCCESS");
      result.addProperty("auctionId", auctionId);
      result.addProperty("message", "Tạo phiên đấu giá thành công!");
    } else {
      result.addProperty("status", "FAIL");
      result.addProperty("message", "Tạo phiên đấu giá thất bại!");
    }
    return result;
  }

  /**
   * Lấy các phiên đang active kèm Base64 ảnh
   */
  public JsonArray getActiveAuctions() {
    JsonArray auctions = auctionDAO.getActiveAuctions(20);
    return attachImageBase64(auctions);
  }

  /**
   * Chi tiết 1 phiên kèm Base64 ảnh
   */
  public JsonObject getAuctionDetail(int auctionId) {
    JsonObject auction = auctionDAO.getAuctionById(auctionId);
    if (auction == null) {
      return null;
    }
    attachSingleImage(auction);
    return auction;
  }

  /**
   * Đặt giá — bắt buộc đi qua AuctionManager để đảm bảo concurrency an toàn.
   *
   * <p>Luồng: AuctionService → AuctionManager (ReentrantLock) → AuctionDAO (transaction + FOR UPDATE)
   * <p>Exception được bắt và chuyển thành JSON response với message rõ ràng cho client.
   */
  public JsonObject placeBid(int auctionId, int bidderId, double amount) {
    try {
      // Đi qua AuctionManager — có ReentrantLock bảo vệ, tránh race condition
      return auctionManager.placeBid(auctionId, bidderId, amount);

    } catch (InvalidBidException e) {
      // Giá không hợp lệ — thông báo rõ mức tối thiểu cho client
      JsonObject result = new JsonObject();
      result.addProperty("status", "FAIL");
      result.addProperty("message", e.getMessage());
      result.addProperty("minRequired", e.getMinRequired());
      return result;

    } catch (AuctionClosedException e) {
      // Phiên đã đóng — thông báo trạng thái hiện tại
      JsonObject result = new JsonObject();
      result.addProperty("status", "FAIL");
      result.addProperty("message", e.getMessage());
      result.addProperty("auctionStatus", e.getCurrentStatus());
      return result;

    } catch (Exception e) {
      // Lỗi hệ thống không mong muốn
      System.err.println("❌ [AuctionService] Lỗi placeBid: " + e.getMessage());
      JsonObject result = new JsonObject();
      result.addProperty("status", "ERROR");
      result.addProperty("message", "Lỗi hệ thống, vui lòng thử lại!");
      return result;
    }
  }

  /**
   * Lịch sử đặt giá
   */
  public JsonArray getBidHistory(int auctionId) {
    return auctionDAO.getBidHistory(auctionId);
  }

  /**
   * Auction của seller
   */
  public JsonArray getAuctionsBySeller(int sellerId) {
    JsonArray auctions = auctionDAO.getAuctionsBySeller(sellerId);
    return attachImageBase64(auctions);
  }

  /**
   * Kết thúc phiên — cập nhật trạng thái item → SOLD và dọn lock của phiên.
   */
  public boolean endAuction(int auctionId) {
    JsonObject auction = auctionDAO.getAuctionById(auctionId);
    if (auction != null) {
      itemDAO.updateStatus(auction.get("itemId").getAsInt(), "SOLD");
    }
    boolean ok = auctionDAO.endAuction(auctionId);
    if (ok) {
      // Dọn lock — tránh memory leak khi có nhiều phiên
      auctionManager.removeLock(auctionId);
    }
    return ok;
  }

  // ── Helper ───────────────────────────────────────────────────────────────

  private JsonArray attachImageBase64(JsonArray items) {
    for (int i = 0; i < items.size(); i++) {
      attachSingleImage(items.get(i).getAsJsonObject());
    }
    return items;
  }

  private void attachSingleImage(JsonObject obj) {
    String path = obj.has("imagePath") ? obj.get("imagePath").getAsString() : "";
    if (path != null && !path.isEmpty()) {
      try {
        File f = new File(path);
        if (f.exists()) {
          byte[] bytes = Files.readAllBytes(f.toPath());
          obj.addProperty("imageBase64", Base64.getEncoder().encodeToString(bytes));
          return;
        }
      } catch (Exception e) {
        System.err.println("Lỗi đọc ảnh: " + e.getMessage());
      }
    }
    obj.addProperty("imageBase64", "");
  }
}