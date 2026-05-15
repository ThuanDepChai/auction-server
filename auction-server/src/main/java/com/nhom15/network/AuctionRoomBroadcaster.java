package com.nhom15.network;

import com.google.gson.JsonObject;
import java.io.PrintWriter;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Phát sự kiện cập nhật phiên đấu giá tới mọi client đang SUBSCRIBE cùng {@code auctionId}.
 * Thread-safe; mỗi kết nối giữ một {@link PrintWriter} riêng.
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
    if (subs == null) {
      return;
    }
    subs.remove(out);
    if (subs.isEmpty()) {
      rooms.remove(auctionId, subs);
    }
  }

  /**
   * Gửi một dòng JSON (newline-terminated) tới tất cả subscriber của phiên.
   */
  public void broadcast(int auctionId, JsonObject message) {
    CopyOnWriteArraySet<PrintWriter> subs = rooms.get(auctionId);
    if (subs == null || subs.isEmpty()) {
      return;
    }
    String line = message.toString();
    for (PrintWriter out : subs) {
      synchronized (out) {
        out.println(line);
        out.flush();
        if (out.checkError()) {
          subs.remove(out);
        }
      }
    }
  }
}
