package com.nhom15.model.auction;

import java.util.ArrayList;
import java.util.List;

/**
 * AuctionRegistry — Singleton lưu danh sách phiên đấu giá trong bộ nhớ.
 *
 * <p>FIX: Đổi sang Eager Initialization để đảm bảo thread-safe.
 * Cách cũ dùng lazy init (if instance == null) không có synchronized
 * → trong môi trường multi-thread, 2 thread có thể tạo 2 instance khác nhau.
 *
 * <p>Eager init: JVM khởi tạo field static final ngay khi class được nạp,
 * quá trình này đã được JVM bảo đảm thread-safe mà không cần synchronized.
 */
public class AuctionRegistry {

  // FIX: Eager initialization — thread-safe, không cần synchronized
  private static final AuctionRegistry INSTANCE = new AuctionRegistry();

  // Danh sach phien dau gia
  private final List<Auction> auctions;

  private AuctionRegistry() {
    auctions = new ArrayList<>();
  }

  public static AuctionRegistry getInstance() {
    return INSTANCE;
  }

  // Them mot phien dau gia moi
  public void addAuction(Auction auction) {
    auctions.add(auction);
  }

  // Lay danh sach dau gia
  public List<Auction> getAuctions() {
    return auctions;
  }
}