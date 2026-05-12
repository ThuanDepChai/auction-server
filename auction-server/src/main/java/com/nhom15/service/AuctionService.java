package com.nhom15.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.nhom15.dao.AuctionDAO;
import com.nhom15.exception.AuctionClosedException;
import com.nhom15.exception.InvalidBidException;
import com.nhom15.network.AuctionManager;

public class AuctionService {

  private final AuctionDAO auctionDAO = new AuctionDAO();
  // Singleton — dùng chung toàn server, quản lý lock per auction
  private final AuctionManager auctionManager = AuctionManager.getInstance();

  /**
   * Tạo phiên đấu giá — ATOMIC: đổi item status + tạo auction trong cùng 1 transaction DB.
   * Nếu tạo auction thất bại, item KHÔNG bị đổi sang IN_AUCTION.
   */
  public JsonObject createAuction(int itemId, int sellerId, double startPrice,
                                  double minStep, String endTime) {
    JsonObject result = new JsonObject();

    // Gọi atomic method — 1 transaction duy nhất bao gồm cả 2 bước
    int auctionId = auctionDAO.createAuctionAtomic(itemId, sellerId, startPrice, minStep, endTime);

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
   * Lấy các phiên đang active — trả imagePath, client tự load ảnh qua GET_AVATAR nếu cần.
   */
  public JsonArray getActiveAuctions() {
    return auctionDAO.getActiveAuctions(20);
  }

  /**
   * Chi tiết 1 phiên — trả imagePath, không nhúng Base64 vào JSON.
   */
  public JsonObject getAuctionDetail(int auctionId) {
    return auctionDAO.getAuctionById(auctionId);
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
    return auctionDAO.getAuctionsBySeller(sellerId);
  }

  /**
   * Kết thúc phiên — ATOMIC: đổi auction status + item status trong cùng 1 transaction DB.
   * Sau đó dọn lock của phiên để tránh memory leak.
   */
  public boolean endAuction(int auctionId) {
    // Lấy itemId trước khi đóng để truyền vào atomic method
    JsonObject auction = auctionDAO.getAuctionById(auctionId);
    if (auction == null) {
      return false;
    }
    int itemId = auction.get("itemId").getAsInt();

    // Gọi atomic method — 1 transaction duy nhất bao gồm cả 2 bước
    boolean ok = auctionDAO.endAuctionAtomic(auctionId, itemId);
    if (ok) {
      // Dọn lock — tránh memory leak khi có nhiều phiên
      auctionManager.removeLock(auctionId);
    }
    return ok;
  }
}