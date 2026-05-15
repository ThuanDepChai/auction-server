package com.nhom15.network;

import com.google.gson.JsonObject;
import java.io.PrintWriter;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Phát sự kiện cập nhật phiên đấu giá tới mọi client đang SUBSCRIBE cùng {@code auctionId}.
 *
 * <p>Thread-safe; mỗi kết nối giữ một {@link PrintWriter} riêng.
 *
 * <p><b>FIX PERF — Per-subscriber parallel write:</b><br>
 * Code cũ: broadcast() ghi lần lượt (sequential) tới từng subscriber trên cùng 1 thread.
 * Nếu subscriber A có TCP send buffer đầy (mạng chậm / proxy RTT cao), out.println()
 * bị block → subscriber B, C, D... phải chờ → lag 200ms–1s cộng dồn.<br>
 * Fix: mỗi subscriber được ghi trên 1 task riêng (WRITE_EXECUTOR, CachedThreadPool).
 * Các client nhận update song song, không phụ thuộc vào tốc độ của nhau.
 */
public final class AuctionRoomBroadcaster {

  public static final AuctionRoomBroadcaster INSTANCE = new AuctionRoomBroadcaster();

  private final ConcurrentHashMap<Integer, CopyOnWriteArraySet<PrintWriter>> rooms =
      new ConcurrentHashMap<>();

  /**
   * Executor riêng để ghi socket tới từng subscriber song song.
   * CachedThreadPool: thread được tái dùng ngay; idle thread tự thu hồi sau 60s.
   * Với ≤ vài trăm subscriber đồng thời, số thread thực tế rất nhỏ.
   */
  private static final ExecutorService WRITE_EXECUTOR =
      Executors.newCachedThreadPool(new ThreadFactory() {
        private final AtomicInteger idx = new AtomicInteger(0);
        @Override public Thread newThread(Runnable r) {
          Thread t = new Thread(r, "bcast-write-" + idx.getAndIncrement());
          t.setDaemon(true);
          return t;
        }
      });

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
   * Gửi một dòng JSON tới tất cả subscriber của phiên — MỖI subscriber trên 1 task riêng.
   *
   * <p>Gọi từ BROADCAST_EXECUTOR trong AuctionService (không block request thread).
   * Mỗi subscriber nhận message trên task độc lập trong WRITE_EXECUTOR, không subscriber
   * nào block subscriber khác dù mạng của họ chậm đến mức nào.
   *
   * <p>Subscriber lỗi (PrintWriter.checkError) bị dọn ngay khỏi danh sách.
   */
  public void broadcast(int auctionId, JsonObject message) {
    CopyOnWriteArraySet<PrintWriter> subs = rooms.get(auctionId);
    if (subs == null || subs.isEmpty()) return;

    String line = message.toString();
    for (PrintWriter out : subs) {
      // FIX: mỗi subscriber nhận message trên task riêng — SONG SONG, không tuần tự
      WRITE_EXECUTOR.submit(() -> {
        synchronized (out) {
          out.println(line);
          out.flush();
          if (out.checkError()) {
            subs.remove(out);
          }
        }
      });
    }
  }
}