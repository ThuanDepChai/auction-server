package com.nhom15.network;

import com.google.gson.JsonObject;
import com.nhom15.dao.AuctionDAO;
import com.nhom15.dao.AutoBidDAO;
import com.nhom15.exception.AuctionClosedException;
import com.nhom15.exception.InvalidBidException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * AuctionManager — Singleton điều phối toàn bộ vòng đời đặt giá.
 *
 * <p><b>Trách nhiệm duy nhất (SRP):</b> Phối hợp các bước trong một lần đặt giá:
 * lock → validate+persist (DAO) → anti-sniping → trigger auto-bid → broadcast.
 *
 * <p><b>Hai tầng bảo vệ concurrency:</b>
 * <ol>
 *   <li>ReentrantLock(fair=true) tại JVM — FIFO, không starvation.</li>
 *   <li>Transaction + SELECT FOR UPDATE trong DAO — đảm bảo không lost-update
 *       dù chạy nhiều server instance.</li>
 * </ol>
 *
 * <p><b>Luồng khi có một bid mới:</b>
 * <pre>
 *   placeBid(auctionId, bidderId, amount)
 *     └─ acquireLock(auctionId)          // tầng 1: JVM lock
 *          └─ dao.placeBidOrThrow()      // tầng 2: DB transaction + FOR UPDATE
 *          └─ antiSnipingCheck()         // gia hạn nếu bid trong X giây cuối
 *          └─ triggerAutoBids()          // xử lý auto-bid của các bidder khác
 *               └─ [mỗi auto-bid hợp lệ] dao.placeBidOrThrow() (vẫn giữ lock)
 *               └─ triggerAutoBids()     // đệ quy tối đa MAX_AUTO_BID_ROUNDS lần
 *          └─ releaseLock()
 * </pre>
 */
public class AuctionManager {

  // ── Singleton ─────────────────────────────────────────────────────────────
  private static final AuctionManager INSTANCE = new AuctionManager();

  public static AuctionManager getInstance() { return INSTANCE; }

  private AuctionManager() {}

  // ── Cấu hình anti-sniping ─────────────────────────────────────────────────
  /** Nếu bid trong X giây cuối → gia hạn thêm Y giây. */
  private static final int ANTI_SNIPE_WINDOW_SEC   = 30;
  private static final int ANTI_SNIPE_EXTENSION_SEC = 60;

  /** Giới hạn vòng auto-bid đệ quy — tránh vòng lặp vô tận khi 2 auto-bid đấu nhau. */
  private static final int MAX_AUTO_BID_ROUNDS = 20;

  // ── Dependencies ──────────────────────────────────────────────────────────
  private final AuctionDAO  auctionDAO  = new AuctionDAO();
  private final AutoBidDAO  autoBidDAO  = new AutoBidDAO();

  // auction_id → lock riêng; computeIfAbsent là atomic
  private final ConcurrentHashMap<Integer, ReentrantLock> auctionLocks
          = new ConcurrentHashMap<>();

  // ── Public API ────────────────────────────────────────────────────────────

  /**
   * Đặt giá an toàn — entry point duy nhất cho mọi loại bid (manual + auto).
   *
   * <p>Toàn bộ pipeline (validate, persist, anti-snipe, auto-bid) chạy trong cùng
   * một lần giữ lock để đảm bảo tính nhất quán.
   *
   * @throws InvalidBidException    nếu amount &lt; currentPrice + minStep
   * @throws AuctionClosedException nếu phiên không còn ACTIVE
   */
  public JsonObject placeBid(int auctionId, int bidderId, double amount)
          throws InvalidBidException, AuctionClosedException {
    return placeBidInternal(auctionId, bidderId, amount, false, 0);
  }

  /** Dọn lock khi phiên kết thúc — tránh memory leak. */
  public void removeLock(int auctionId) {
    auctionLocks.remove(auctionId);
  }

  // ── Internal ──────────────────────────────────────────────────────────────

  /**
   * Phiên bản nội bộ — chỉ acquire lock ở round = 0 (bid gốc).
   * Các round auto-bid tiếp theo tái dùng lock đang giữ (vì cùng thread).
   * ReentrantLock cho phép reentrant nên không bị deadlock.
   */
  private JsonObject placeBidInternal(int auctionId, int bidderId, double amount,
                                      boolean isAutoBid, int round)
          throws InvalidBidException, AuctionClosedException {

    ReentrantLock lock = getLock(auctionId);
    lock.lock();
    try {
      // 1. Persist bid vào DB (validate + INSERT + UPDATE current_price)
      auctionDAO.placeBidOrThrow(auctionId, bidderId, amount);

      // 2. Anti-sniping — gia hạn thời gian nếu bid gần cuối phiên
      antiSnipingCheck(auctionId);

      // 3. Trigger auto-bid cho các bidder khác (nếu còn quota round)
      if (round < MAX_AUTO_BID_ROUNDS) {
        triggerAutoBids(auctionId, bidderId, amount, round);
      } else {
        System.out.println("⚠️ [AuctionManager] Auction #" + auctionId
                + " đạt giới hạn " + MAX_AUTO_BID_ROUNDS + " vòng auto-bid.");
      }

      // 4. Trả kết quả
      JsonObject result = new JsonObject();
      result.addProperty("status", "SUCCESS");
      result.addProperty("message", isAutoBid ? "Auto-bid thành công!" : "Đặt giá thành công!");
      result.addProperty("newPrice", amount);
      result.addProperty("isAutoBid", isAutoBid);
      return result;

    } finally {
      lock.unlock();
    }
  }

  /**
   * Sau khi có bid mới, kiểm tra tất cả auto-bid còn active trong phiên.
   * Loại trừ chính bidder vừa thắng (họ đã dẫn đầu rồi).
   *
   * <p><b>Logic chọn auto-bid thắng:</b> Chọn bidder có maxBid cao nhất;
   * nếu bằng nhau → ưu tiên người đăng ký auto-bid trước (ORDER BY trong DB).
   * Đặt giá = currentPrice + increment của người thắng, nhưng không vượt maxBid.
   *
   * <p>Nếu auto-bid thành công → đệ quy để xem có auto-bid nào khác cần phản ứng không.
   */
  private void triggerAutoBids(int auctionId, int lastBidderId, double currentPrice, int round) {
    List<JsonObject> activeBids = autoBidDAO.getActiveAutoBids(auctionId);
    if (activeBids.isEmpty()) return;

    // Chọn auto-bid ứng viên cao nhất (không phải người vừa đặt)
    JsonObject best = null;
    double bestMaxBid = 0;

    for (JsonObject ab : activeBids) {
      int    candidateId = ab.get("bidderId").getAsInt();
      double maxBid      = ab.get("maxBid").getAsDouble();

      if (candidateId == lastBidderId) continue; // đang dẫn đầu rồi, bỏ qua
      if (maxBid <= currentPrice)      continue; // không đủ tiền, bỏ qua

      if (best == null || maxBid > bestMaxBid) {
        best        = ab;
        bestMaxBid  = maxBid;
      }
    }

    if (best == null) return; // không có auto-bid nào đủ điều kiện

    int    autoBidderId = best.get("bidderId").getAsInt();
    double increment    = best.get("increment").getAsDouble();
    double proposedBid  = currentPrice + increment;

    // Không vượt quá maxBid của người đó
    if (proposedBid > bestMaxBid) {
      proposedBid = bestMaxBid;
    }

    // Đảm bảo đủ minStep (lấy từ DB qua DAO — tránh stale data)
    double minStep = auctionDAO.getMinStep(auctionId);
    if (proposedBid < currentPrice + minStep) {
      // maxBid không đủ để vượt minStep → tắt auto-bid của bidder này
      autoBidDAO.cancelAutoBid(auctionId, autoBidderId);
      System.out.println("ℹ️ [AutoBid] Bidder #" + autoBidderId
              + " hết quota (maxBid < currentPrice + minStep) → đã tắt.");
      return;
    }

    // Thực hiện auto-bid (reentrant vào cùng lock)
    try {
      System.out.printf("🤖 [AutoBid] Round %d — Bidder #%d đặt tự động %.0f cho Auction #%d%n",
              round + 1, autoBidderId, proposedBid, auctionId);
      placeBidInternal(auctionId, autoBidderId, proposedBid, true, round + 1);
    } catch (InvalidBidException | AuctionClosedException e) {
      // Phiên vừa đóng giữa chừng hoặc giá bị vượt — dừng lại, không lỗi
      System.out.println("ℹ️ [AutoBid] Dừng: " + e.getMessage());
    }
  }

  /**
   * Anti-sniping: nếu bid được đặt trong vòng ANTI_SNIPE_WINDOW_SEC giây cuối,
   * tự động gia hạn thêm ANTI_SNIPE_EXTENSION_SEC giây.
   *
   * <p>Ví dụ: kết thúc dự kiến 20:00:00, bid lúc 19:59:40 → gia hạn đến 20:01:00.
   */
  private void antiSnipingCheck(int auctionId) {
    try {
      long secondsLeft = auctionDAO.getSecondsUntilEnd(auctionId);
      if (secondsLeft >= 0 && secondsLeft <= ANTI_SNIPE_WINDOW_SEC) {
        auctionDAO.extendAuctionTime(auctionId, ANTI_SNIPE_EXTENSION_SEC);
        System.out.printf("⏱️ [AntiSnipe] Auction #%d gia hạn thêm %ds (còn %ds)%n",
                auctionId, ANTI_SNIPE_EXTENSION_SEC, secondsLeft);
      }
    } catch (Exception e) {
      // Anti-snipe không được làm hỏng bid chính
      System.err.println("⚠️ [AntiSnipe] Lỗi kiểm tra gia hạn: " + e.getMessage());
    }
  }

  /** Lấy hoặc tạo lock cho phiên (atomic, chỉ tạo 1 lock dù 100 thread gọi đồng thời). */
  private ReentrantLock getLock(int auctionId) {
    return auctionLocks.computeIfAbsent(auctionId, k -> new ReentrantLock(true));
  }
}