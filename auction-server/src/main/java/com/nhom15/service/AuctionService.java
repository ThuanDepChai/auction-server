package com.nhom15.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.nhom15.dao.AuctionDAO;
import com.nhom15.exception.AuctionClosedException;
import com.nhom15.exception.InvalidBidException;
import com.nhom15.network.AuctionManager;
import com.nhom15.network.AuctionRoomBroadcaster;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class AuctionService {

  private final AuctionDAO auctionDAO = new AuctionDAO();
  private final AuctionManager auctionManager = AuctionManager.getInstance();
  private static final boolean LATENCY_DEBUG = Boolean.getBoolean("auction.latency.debug");

  /**
   * FIX PERF: CachedThreadPool thay vì SingleThreadExecutor.
   *
   * Lý do lag ~1 giây trước đây:
   *  - SingleThreadExecutor chỉ có 1 thread xử lý broadcast tuần tự.
   *  - Mỗi subscriber được ghi qua out.println() (blocking TCP write).
   *  - Nếu bất kỳ subscriber nào có TCP send buffer đầy (mạng chậm / proxy),
   *    out.println() bị block → toàn bộ broadcast chain bị trễ cho đến khi
   *    buffer được tiêu thụ — có thể mất 200ms–1s tùy RTT của client đó.
   *  - Với SingleThreadExecutor, độ trễ này cộng dồn qua từng subscriber.
   *
   * Fix: CachedThreadPool → mỗi lần broadcastAsync() submit 1 task độc lập,
   * các phòng đấu giá khác nhau không chờ nhau.
   * Đồng thời AuctionRoomBroadcaster.broadcast() được tách thành per-subscriber
   * tasks để không subscriber nào block subscriber khác.
   */
  private static final ExecutorService BROADCAST_EXECUTOR =
      Executors.newCachedThreadPool(new ThreadFactory() {
        private final AtomicInteger idx = new AtomicInteger(0);
        @Override public Thread newThread(Runnable r) {
          Thread t = new Thread(r, "auction-bcast-" + idx.getAndIncrement());
          t.setDaemon(true);
          return t;
        }
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
      long startedAt = System.currentTimeMillis();
      JsonObject result = auctionManager.placeBid(auctionId, bidderId, amount);
      long serverProcessMs = System.currentTimeMillis() - startedAt;
      result.addProperty("serverProcessMs", serverProcessMs);
      if (LATENCY_DEBUG) {
        System.out.printf("[Latency] PLACE_BID auction=%d bidder=%d serverProcess=%dms%n",
            auctionId, bidderId, serverProcessMs);
      }

      // Map endTime → newEndTime cho client cập nhật bộ đếm ngược anti-sniping
      if (result.has("endTime") && !result.has("newEndTime")) {
        result.addProperty("newEndTime", result.get("endTime").getAsString());
      }

      // FIX 2: Broadcast ASYNC
      broadcastAsync(auctionId, result);
      broadcastSnapshotAsync(auctionId);

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

  private void broadcastSnapshotAsync(int auctionId) {
    BROADCAST_EXECUTOR.submit(() -> {
      try {
        JsonObject detail = auctionDAO.getRealtimeSnapshot(auctionId);
        if (detail != null) {
          AuctionRoomBroadcaster.INSTANCE.broadcast(auctionId, buildEnvelope(auctionId, detail));
        }
      } catch (Exception ex) {
        System.err.println("âš ï¸ [Broadcast snapshot] auction #" + auctionId + ": " + ex.getMessage());
      }
    });
  }

  /**
   * Đóng gói { "action":"AUCTION_UPDATE", "data":{...} } từ snapshot.
   *
   * FIX: Thêm trường "serverTime" = thời điểm gửi theo đồng hồ server.
   * Client dùng serverTime để tính clock offset (delta = serverTime - localNow)
   * và trừng phần delta khi tính second remaining:
   *   secondsLeft = ChronoUnit.SECONDS.between(LocalDateTime.now().plusSeconds(offsetSec), endTime)
   * Điều này đảm bảo mọi client đếu đếm ngược đồng bộ theo đồng hồ server,
   * dù đồng hồ máy client lệch vài giây hay khác múi giờ.
   */
  private static final DateTimeFormatter DT_FMT =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  private static JsonObject buildEnvelope(int auctionId, JsonObject src) {
    JsonObject data = new JsonObject();
    data.addProperty("auctionId", auctionId);
    copyDbl(src, data, "currentPrice");
    copyStr(src, data, "endTime");
    if (src.has("auctionStatus") && !src.get("auctionStatus").isJsonNull()) {
      data.addProperty("status", src.get("auctionStatus").getAsString());
    } else {
      copyStr(src, data, "status");
    }
    copyStr(src, data, "leadingBidder");
    copyInt(src, data, "totalBids");
    // FIX: thêm thời gian server để client có thể tính clock-offset
    data.addProperty("serverTime", LocalDateTime.now().format(DT_FMT));

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
