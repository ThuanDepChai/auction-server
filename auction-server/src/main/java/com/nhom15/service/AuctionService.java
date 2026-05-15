package com.nhom15.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.nhom15.dao.AuctionDAO;
import com.nhom15.exception.AuctionClosedException;
import com.nhom15.exception.InvalidBidException;
import com.nhom15.network.AuctionManager;
import com.nhom15.network.AuctionRoomBroadcaster;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AuctionService {

  private final AuctionDAO auctionDAO = new AuctionDAO();
  private final AuctionManager auctionManager = AuctionManager.getInstance();

  /**
   * FIX: Executor riêng cho broadcast — 1 daemon thread đủ dùng.
   * Tách khỏi requestPool để request thread trả về response cho bidder ngay,
   * không phải block chờ socket write đến từng subscriber.
   */
  private static final ExecutorService BROADCAST_EXECUTOR =
      Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "auction-broadcast");
        t.setDaemon(true);
        return t;
      });

  /**
   * Tạo phiên đấu giá — ATOMIC: đổi item status + tạo auction trong cùng 1 transaction DB.
   */
  public JsonObject createAuction(int itemId, int sellerId, double startPrice,
      double minStep, String endTime) {
    JsonObject result = new JsonObject();
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

  public JsonArray getActiveAuctions() {
    return auctionDAO.getActiveAuctions(20);
  }

  public JsonObject getAuctionDetail(int auctionId) {
    return auctionDAO.getAuctionById(auctionId);
  }

  /**
   * Đặt giá — đi qua AuctionManager để đảm bảo concurrency an toàn.
   *
   * <p>FIX 1 — Bỏ query DB thừa: AuctionManager.placeBid() giờ trả về snapshot đầy đủ
   * (currentPrice, endTime, leadingBidder, totalBids) lấy TRONG LOCK sau khi toàn bộ
   * auto-bid chain chạy xong. AuctionService không cần gọi getAuctionById() nữa.
   *
   * <p>FIX 2 — Broadcast async: request thread submit envelope lên BROADCAST_EXECUTOR rồi
   * trả về response ngay, không block chờ socket write đến từng subscriber.
   */
  public JsonObject placeBid(int auctionId, int bidderId, double amount) {
    try {
      // Trả về: status, newPrice, currentPrice, endTime, leadingBidder, totalBids
      JsonObject result = auctionManager.placeBid(auctionId, bidderId, amount);

      // Map endTime → newEndTime cho client cập nhật bộ đếm ngược anti-sniping
      if (result.has("endTime") && !result.has("newEndTime")) {
        result.addProperty("newEndTime", result.get("endTime").getAsString());
      }

      // FIX 2: Broadcast ASYNC
      broadcastAsync(auctionId, result);

      return result;

    } catch (InvalidBidException e) {
      JsonObject r = new JsonObject();
      r.addProperty("status", "FAIL");
      r.addProperty("message", e.getMessage());
      r.addProperty("minRequired", e.getMinRequired());
      return r;
    } catch (AuctionClosedException e) {
      JsonObject r = new JsonObject();
      r.addProperty("status", "FAIL");
      r.addProperty("message", e.getMessage());
      r.addProperty("auctionStatus", e.getCurrentStatus());
      return r;
    } catch (Exception e) {
      System.err.println("❌ [AuctionService] Lỗi placeBid: " + e.getMessage());
      JsonObject r = new JsonObject();
      r.addProperty("status", "ERROR");
      r.addProperty("message", "Lỗi hệ thống, vui lòng thử lại!");
      return r;
    }
  }

  public JsonArray getBidHistory(int auctionId) {
    return auctionDAO.getBidHistory(auctionId);
  }

  public JsonArray getAuctionsBySeller(int sellerId) {
    return auctionDAO.getAuctionsBySeller(sellerId);
  }

  /**
   * Kết thúc phiên — ATOMIC. Broadcast trạng thái ENDED async.
   */
  public boolean endAuction(int auctionId) {
    JsonObject auction = auctionDAO.getAuctionById(auctionId);
    if (auction == null) return false;
    int itemId = auction.get("itemId").getAsInt();

    boolean ok = auctionDAO.endAuctionAtomic(auctionId, itemId);
    if (ok) {
      auctionManager.removeLock(auctionId);
      JsonObject detail = auctionDAO.getAuctionById(auctionId);
      if (detail != null) {
        broadcastAsync(auctionId, detail);
      }
    }
    return ok;
  }

  // ── Broadcast helpers ─────────────────────────────────────────────────────

  /**
   * Submit broadcast lên executor riêng. Request thread được giải phóng ngay lập tức.
   */
  private void broadcastAsync(int auctionId, JsonObject snapshot) {
    JsonObject envelope = buildEnvelope(auctionId, snapshot);
    BROADCAST_EXECUTOR.submit(() -> {
      try {
        AuctionRoomBroadcaster.INSTANCE.broadcast(auctionId, envelope);
      } catch (Exception ex) {
        System.err.println("⚠️ [Broadcast] auction #" + auctionId + ": " + ex.getMessage());
      }
    });
  }

  /** Đóng gói { "action":"AUCTION_UPDATE", "data":{...} } từ snapshot. */
  private static JsonObject buildEnvelope(int auctionId, JsonObject src) {
    JsonObject data = new JsonObject();
    data.addProperty("auctionId", auctionId);
    copyDbl(src, data, "currentPrice");
    copyStr(src, data, "endTime");
    copyStr(src, data, "status");
    copyStr(src, data, "leadingBidder");
    copyInt(src, data, "totalBids");

    JsonObject envelope = new JsonObject();
    envelope.addProperty("action", "AUCTION_UPDATE");
    envelope.add("data", data);
    return envelope;
  }

  private static void copyDbl(JsonObject s, JsonObject d, String k) {
    if (s.has(k) && !s.get(k).isJsonNull()) d.addProperty(k, s.get(k).getAsDouble());
  }

  private static void copyStr(JsonObject s, JsonObject d, String k) {
    if (s.has(k) && !s.get(k).isJsonNull()) d.addProperty(k, s.get(k).getAsString());
  }

  private static void copyInt(JsonObject s, JsonObject d, String k) {
    if (s.has(k) && !s.get(k).isJsonNull()) d.addProperty(k, s.get(k).getAsInt());
  }
}