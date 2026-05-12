package com.nhom15.network;

import com.google.gson.JsonObject;
import com.nhom15.dao.AuctionDAO;
import com.nhom15.exception.AuctionClosedException;
import com.nhom15.exception.InvalidBidException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * AuctionManager — Singleton quản lý concurrency cho toàn bộ phiên đấu giá.
 *
 * <p>Đảm bảo tính nhất quán khi nhiều client đặt giá đồng thời vào cùng 1 phiên
 * bằng 2 tầng bảo vệ:
 * <ol>
 *   <li>ReentrantLock (fair=true) tại tầng JVM — thread đến trước được phục vụ trước,
 *       không có race condition trong bộ nhớ.</li>
 *   <li>Transaction + SELECT FOR UPDATE tại tầng DB trong AuctionDAO — đảm bảo
 *       không lost update dù nhiều server instance chạy song song.</li>
 * </ol>
 */
public class AuctionManager {

  // ── Singleton (Eager initialization — thread-safe không cần synchronized) ──
  private static final AuctionManager INSTANCE = new AuctionManager();

  public static AuctionManager getInstance() {
    return INSTANCE;
  }

  private AuctionManager() {
  }

  // auction_id → lock riêng của từng phiên
  private final ConcurrentHashMap<Integer, ReentrantLock> auctionLocks = new ConcurrentHashMap<>();
  private final AuctionDAO auctionDAO = new AuctionDAO();

  // ── Lock management ───────────────────────────────────────────────────────

  /**
   * Lấy hoặc tạo lock cho phiên. computeIfAbsent là atomic — chỉ tạo 1 lock
   * duy nhất dù 100 thread gọi đồng thời.
   */
  private ReentrantLock getLock(int auctionId) {
    return auctionLocks.computeIfAbsent(auctionId, k -> new ReentrantLock(true));
    // fair=true: FIFO — thread nào đến trước được phục vụ trước, tránh starvation
  }

  // ── Core: Đặt giá có bảo vệ concurrency ──────────────────────────────────

  /**
   * Đặt giá an toàn — được gọi từ AuctionService.
   *
   * <p>Quy trình:
   * <ol>
   *   <li>Acquire lock của phiên này (blocking nếu thread khác đang giữ)</li>
   *   <li>Delegate xuống AuctionDAO (có transaction + kiểm tra hợp lệ trong DB)</li>
   *   <li>DAO ném exception nếu giá không hợp lệ hoặc phiên đã đóng</li>
   *   <li>Release lock trong finally — luôn chạy dù có exception</li>
   * </ol>
   *
   * @throws InvalidBidException    nếu amount < currentPrice + minStep
   * @throws AuctionClosedException nếu phiên không còn ACTIVE
   */
  public JsonObject placeBid(int auctionId, int bidderId, double amount)
          throws InvalidBidException, AuctionClosedException {

    ReentrantLock lock = getLock(auctionId);
    lock.lock();
    try {
      // Delegate xuống DAO — DAO tự kiểm tra và ném exception nếu sai
      auctionDAO.placeBidOrThrow(auctionId, bidderId, amount);

      // Nếu đến đây nghĩa là thành công
      JsonObject result = new JsonObject();
      result.addProperty("status", "SUCCESS");
      result.addProperty("message", "Đặt giá thành công!");
      result.addProperty("newPrice", amount);
      return result;

    } finally {
      lock.unlock(); // LUÔN chạy — kể cả khi ném exception
    }
  }

  /**
   * Dọn lock sau khi phiên kết thúc — tránh memory leak khi có nhiều phiên.
   * Gọi từ AuctionService.endAuction() sau khi END_AUCTION thành công.
   */
  public void removeLock(int auctionId) {
    auctionLocks.remove(auctionId);
  }
}