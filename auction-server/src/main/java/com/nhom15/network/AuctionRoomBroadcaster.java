package com.nhom15.network;

import com.google.gson.JsonObject;
import java.io.PrintWriter;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Phát sự kiện cập nhật phiên đấu giá tới mọi client đang SUBSCRIBE cùng {@code auctionId}.
 *
 * <p>Thread-safe; mỗi kết nối giữ một {@link PrintWriter} riêng.
 *
 * <p>FIX: {@code broadcast()} được gọi từ {@code BROADCAST_EXECUTOR} trong AuctionService
 * (không còn chạy trên request thread nữa), nên ngay cả khi 1 subscriber chậm thì cũng
 * không block response trả về cho bidder. Vẫn giữ {@code synchronized(out)} cho mỗi
 * PrintWriter để nhiều broadcast nếu xảy ra đồng thời không xen kẽ bytes.
 */
public final class AuctionRoomBroadcaster {

  public static final AuctionRoomBroadcaster INSTANCE = new AuctionRoomBroadcaster();

  private final ConcurrentHashMap<Integer, CopyOnWriteArraySet<PrintWriter>> rooms =
      new ConcurrentHashMap<>();

  private AuctionRoomBroadcaster() {}

  public void register(int auctionId, PrintWriter out) {
    rooms.computeIfAbsent(auctionId, k -> new CopyOnWriteArraySet<>()).add(out);
  }

  public void unregister(int auctionId, PrintWriter out) {
    CopyOnWriteArraySet<PrintWriter> subs = rooms.get(auctionId);
    if (subs == null) return;
    subs.remove(out);
    if (subs.isEmpty()) rooms.remove(auctionId, subs);
  }

  /**
   * Gửi một dòng JSON tới tất cả subscriber của phiên.
   *
   * <p>Chạy trên BROADCAST_EXECUTOR (daemon thread riêng) — không block request thread.
   * Subscriber lỗi (PrintWriter.checkError) bị dọn ngay khỏi danh sách.
   */
  public void broadcast(int auctionId, JsonObject message) {
    CopyOnWriteArraySet<PrintWriter> subs = rooms.get(auctionId);
    if (subs == null || subs.isEmpty()) return;

    String line = message.toString();
    for (PrintWriter out : subs) {
      synchronized (out) {
        out.println(line);
        if (out.checkError()) {
          subs.remove(out);
        }
      }
    }
  }
}