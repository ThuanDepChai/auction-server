package com.nhom15.model.auction;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * AuctionRegistry — Singleton lưu danh sách phiên đấu giá trong bộ nhớ.
 *
 * <p><b>FIX — Thread-safety của danh sách phiên:</b><br>
 * Code cũ dùng {@code ArrayList} không đồng bộ. Trong môi trường multi-thread (mỗi client
 * có 1 thread), khi 2 thread gọi {@link #addAuction} + {@link #getAuctions} đồng thời,
 * có thể xảy ra {@link java.util.ConcurrentModificationException} hoặc dữ liệu bị hỏng.
 *
 * <p>Fix: dùng {@link CopyOnWriteArrayList} — tất cả thao tác đọc (vòng lặp, get) không
 * cần lock; ghi (add/remove) tạo bản copy mới. Phù hợp với pattern "đọc nhiều, ghi ít"
 * của danh sách phiên đấu giá.
 *
 * <p><b>Singleton:</b> Eager initialization — JVM đảm bảo thread-safe khi load class,
 * không cần {@code synchronized}.
 */
public class AuctionRegistry {

  // Eager init — thread-safe, không cần synchronized
  private static final AuctionRegistry INSTANCE = new AuctionRegistry();

  // FIX: CopyOnWriteArrayList thay ArrayList để tránh ConcurrentModificationException
  private final List<Auction> auctions = new CopyOnWriteArrayList<>();

  private AuctionRegistry() {}

  public static AuctionRegistry getInstance() {
    return INSTANCE;
  }

  // ── Mutate ────────────────────────────────────────────────────────────────

  /** Thêm phiên đấu giá mới — thread-safe. */
  public void addAuction(Auction auction) {
    if (auction == null) return;
    auctions.add(auction);
  }

  /** Xoá phiên đấu giá (ví dụ khi kết thúc) — thread-safe. */
  public void removeAuction(Auction auction) {
    auctions.remove(auction);
  }

  /** Xoá theo ID. */
  public void removeAuctionById(int auctionId) {
    auctions.removeIf(a -> a.getId() == auctionId);
  }

  // ── Query ─────────────────────────────────────────────────────────────────

  /**
   * Trả về danh sách không thể sửa đổi — caller chỉ đọc, không add/remove trực tiếp.
   * Vẫn thread-safe vì CopyOnWriteArrayList snapshot tại thời điểm gọi.
   */
  public List<Auction> getAuctions() {
    return Collections.unmodifiableList(auctions);
  }

  /** Tìm phiên theo ID. @return null nếu không tìm thấy. */
  public Auction findById(int auctionId) {
    for (Auction a : auctions) {
      if (a.getId() == auctionId) return a;
    }
    return null;
  }

  /** Trả về số phiên đang được lưu. */
  public int size() {
    return auctions.size();
  }
}
