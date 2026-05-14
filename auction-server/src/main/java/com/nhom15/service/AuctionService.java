package com.nhom15.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.nhom15.dao.AuctionDAO;
import com.nhom15.exception.AuctionClosedException;
import com.nhom15.exception.InvalidBidException;
import com.nhom15.network.AuctionManager;
import com.nhom15.network.AuctionRoomBroadcaster;

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
   *          → anti-sniping check → trigger auto-bid
   * <p>Sau khi thành công, lấy lại end_time mới nhất từ DB (có thể đã bị gia hạn bởi anti-sniping)
   * và đính kèm vào response để client cập nhật bộ đếm ngược.
   */
  public JsonObject placeBid(int auctionId, int bidderId, double amount) {
    try {
      JsonObject result = auctionManager.placeBid(auctionId, bidderId, amount);

      // Lấy end_time mới nhất — có thể đã bị gia hạn bởi anti-sniping
      JsonObject detail = auctionDAO.getAuctionById(auctionId);
      if (detail != null && detail.has("endTime")) {
        result.addProperty("newEndTime", detail.get("endTime").getAsString());
      }
      // Lấy currentPrice mới nhất từ DB (có thể đã bị auto-bid đẩy lên sau khi bid của bạn)
      if (detail != null && detail.has("currentPrice")) {
        result.addProperty("currentPrice", detail.get("currentPrice").getAsDouble());
      }
      if (detail != null && detail.has("leadingBidder")) {
        result.addProperty("leadingBidder", detail.get("leadingBidder").getAsString());
      }
      if (detail != null && detail.has("totalBids")) {
        result.addProperty("totalBids", detail.get("totalBids").getAsInt());
      }
      broadcastFromDetail(auctionId, detail);
      return result;

    } catch (InvalidBidException e) {
      JsonObject result = new JsonObject();
      result.addProperty("status", "FAIL");
      result.addProperty("message", e.getMessage());
      result.addProperty("minRequired", e.getMinRequired());
      return result;

    } catch (AuctionClosedException e) {
      JsonObject result = new JsonObject();
      result.addProperty("status", "FAIL");
      result.addProperty("message", e.getMessage());
      result.addProperty("auctionStatus", e.getCurrentStatus());
      return result;

    } catch (Exception e) {
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
      JsonObject detail = auctionDAO.getAuctionById(auctionId);
      broadcastFromDetail(auctionId, detail);
    }
    return ok;
  }

  /** Đẩy snapshot phiên tới mọi client đang SUBSCRIBE (cùng cổng TCP). */
  private void broadcastFromDetail(int auctionId, JsonObject detail) {
    if (detail == null) {
      return;
    }
    JsonObject data = new JsonObject();
    data.addProperty("auctionId", auctionId);
    if (detail.has("currentPrice")) {
      data.addProperty("currentPrice", detail.get("currentPrice").getAsDouble());
    }
    if (detail.has("endTime")) {
      data.addProperty("endTime", detail.get("endTime").getAsString());
    }
    if (detail.has("status")) {
      data.addProperty("status", detail.get("status").getAsString());
    }
    if (detail.has("leadingBidder")) {
      data.addProperty("leadingBidder", detail.get("leadingBidder").getAsString());
    }
    if (detail.has("totalBids")) {
      data.addProperty("totalBids", detail.get("totalBids").getAsInt());
    }
    JsonObject envelope = new JsonObject();
    envelope.addProperty("action", "AUCTION_UPDATE");
    envelope.add("data", data);
    AuctionRoomBroadcaster.INSTANCE.broadcast(auctionId, envelope);
  }
}