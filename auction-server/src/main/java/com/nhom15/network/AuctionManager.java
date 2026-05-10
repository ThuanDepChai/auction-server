package com.nhom15.network;

import com.nhom15.dao.AuctionDAO;
import com.google.gson.JsonObject;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Quản lý lock riêng cho từng phiên đấu giá.
 * Đảm bảo thread-safe khi nhiều client đặt giá đồng thời vào cùng 1 phiên.
 */
public class AuctionManager {

  // Singleton — dùng chung toàn server
  private static final AuctionManager INSTANCE = new AuctionManager();
  public static AuctionManager getInstance() { return INSTANCE; }
  private AuctionManager() {}

  // auction_id → lock riêng của phiên đó
  private final ConcurrentHashMap<Integer, ReentrantLock> auctionLocks = new ConcurrentHashMap<>();
  private final AuctionDAO auctionDAO = new AuctionDAO();

  /**
   * Lấy hoặc tạo lock cho phiên auction_id.
   * computeIfAbsent đảm bảo chỉ tạo 1 lock duy nhất dù nhiều thread cùng gọi.
   */
  private ReentrantLock getLock(int auctionId) {
    return auctionLocks.computeIfAbsent(auctionId, k -> new ReentrantLock(true));
    //                                                                      ^^^^
    //                                          fair=true: thread nào đến trước phục vụ trước
  }

  /**
   * Đặt giá có lock:
   *  1. Lock tầng application (ReentrantLock) — ngăn race condition trong JVM
   *  2. Bên trong DAO dùng SELECT ... FOR UPDATE — ngăn race condition ở DB
   */
  public JsonObject placeBid(int auctionId, int bidderId, double amount) {
    ReentrantLock lock = getLock(auctionId);
    lock.lock();
    try {
      boolean ok = auctionDAO.placeBid(auctionId, bidderId, amount);
      JsonObject result = new JsonObject();
      if (ok) {
        result.addProperty("status",   "SUCCESS");
        result.addProperty("message",  "Đặt giá thành công!");
        result.addProperty("newPrice", amount);
      } else {
        result.addProperty("status",  "FAIL");
        result.addProperty("message", "Giá không hợp lệ hoặc phiên đã kết thúc!");
      }
      return result;
    } finally {
      lock.unlock(); // luôn mở lock dù có exception
    }
  }

  /**
   * Xoá lock khi phiên đấu giá kết thúc để tránh leak memory.
   * Gọi từ AuctionHandler sau khi END_AUCTION thành công.
   */
  public void removeLock(int auctionId) {
    auctionLocks.remove(auctionId);
  }
}