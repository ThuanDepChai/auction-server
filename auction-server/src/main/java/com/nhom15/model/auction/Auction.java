package com.nhom15.model.auction;

import com.nhom15.model.item.Item;
import com.nhom15.model.user.Bidder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * In-memory model của một phiên đấu giá.
 *
 * <p><b>FIX 1 — addBid() không cập nhật trường:</b><br>
 * Code cũ chỉ {@code bids.add(bid)} rồi {@code notifyObservers()} nhưng KHÔNG cập nhật
 * {@code currentHighestBid} và {@code highestBidder}. Hậu quả: {@code printInfo()} luôn in
 * giá 0 / bidder null, mặc dù đã có nhiều bid. Fix: cập nhật cả 2 trường trong addBid().
 *
 * <p><b>FIX 2 — endTime là final, anti-sniping không thể gia hạn:</b><br>
 * Code cũ khai báo {@code private final long endTime}. Khi anti-sniping cần gia hạn,
 * không có cách nào ghi đè giá trị. Fix: đổi sang non-final, thêm {@link #extendEndTime(long)}.
 *
 * <p><b>FIX 3 — Observer list không thread-safe:</b><br>
 * Code cũ dùng {@code ArrayList} — nếu 2 thread gọi {@code registerObserver} + {@code notifyObservers}
 * đồng thời có thể xảy ra {@code ConcurrentModificationException}. Fix: dùng {@code CopyOnWriteArrayList}.
 *
 * <p><b>FIX 4 — notifyObservers gửi delta thay vì toàn bộ chuỗi:</b><br>
 * Observer nhận đúng thông tin "đã thay đổi": giá mới và username của bidder dẫn đầu,
 * thay vì một chuỗi chung chung không phân biệt được loại sự kiện.
 */
public class Auction {

  private final int id;
  private final Item item;
  private final double startPrice;

  // FIX 2: bỏ final để anti-sniping có thể gia hạn
  private final long startTime;
  private long endTime;

  // FIX 1: 2 trường này trước đây không bao giờ được cập nhật trong addBid()
  private double currentHighestBid;
  private Bidder highestBidder;

  private final List<Bid> bids = new ArrayList<>();

  // FIX 3: dùng CopyOnWriteArrayList tránh ConcurrentModificationException
  private final List<Observer> observers = new CopyOnWriteArrayList<>();

  // ── Constructor ───────────────────────────────────────────────────────────

  public Auction(int id, Item item, double startPrice, long startTime, long endTime) {
    this.id             = id;
    this.item           = item;
    this.startPrice     = startPrice;
    this.startTime      = startTime;
    this.endTime        = endTime;
    // Khởi tạo giá ban đầu bằng startPrice
    this.currentHighestBid = startPrice;
  }

  // ── Bid logic ─────────────────────────────────────────────────────────────

  /**
   * Thêm bid mới vào phiên.
   *
   * <p><b>FIX 1:</b> Phương thức cũ chỉ add vào list rồi notify — KHÔNG cập nhật
   * {@code currentHighestBid} và {@code highestBidder}. Fix: cập nhật ngay khi amount hợp lệ.
   *
   * @param bidder người đặt giá
   * @param amount giá đặt — phải > currentHighestBid
   * @return true nếu bid hợp lệ và được chấp nhận
   */
  public boolean addBid(Bidder bidder, double amount) {
    if (amount <= currentHighestBid) {
      // Không chấp nhận giá không cao hơn hiện tại
      return false;
    }

    Bid bid = new Bid(bidder, amount);
    bids.add(bid);

    // FIX 1: Cập nhật trường giá và người dẫn đầu — trước đây bị bỏ sót
    currentHighestBid = amount;
    highestBidder     = bidder;

    System.out.printf("Người dùng %s vừa đặt giá: %.0f lúc %d%n",
        bidder.getUsername(), amount, bid.getTimestamp());

    // FIX 4: Notify với delta (giá mới + username) thay vì chuỗi chung
    notifyObservers(String.format("BID_UPDATE|auctionId=%d|price=%.0f|bidder=%s",
        id, amount, bidder.getUsername()));
    return true;
  }

  // ── Trạng thái phiên ─────────────────────────────────────────────────────

  /** @return true nếu phiên đã qua thời điểm bắt đầu. */
  public boolean isStarted() {
    return System.currentTimeMillis() >= startTime;
  }

  /** @return true nếu đã qua thời điểm kết thúc. */
  public boolean isEnded() {
    return System.currentTimeMillis() > endTime;
  }

  /** @return true nếu phiên đang chấp nhận bid (đã bắt đầu, chưa kết thúc). */
  public boolean isActive() {
    return isStarted() && !isEnded();
  }

  // ── Anti-sniping ──────────────────────────────────────────────────────────

  /**
   * Gia hạn thời gian kết thúc thêm {@code extraMillis} mili-giây.
   *
   * <p><b>FIX 2:</b> endTime trước đây là final, không thể gia hạn. Phương thức này
   * được gọi bởi anti-sniping logic khi có bid trong X giây cuối.
   *
   * @param extraMillis số mili-giây cộng thêm (dương)
   */
  public void extendEndTime(long extraMillis) {
    if (extraMillis <= 0) return;
    endTime += extraMillis;
    System.out.printf("⏱️ [AntiSnipe] Auction #%d gia hạn thêm %ds → endTime mới: %d%n",
        id, extraMillis / 1000, endTime);
    notifyObservers(String.format("TIME_EXTENDED|auctionId=%d|newEndTime=%d", id, endTime));
  }

  // ── Observer (GoF) ────────────────────────────────────────────────────────

  /**
   * Đăng ký observer — mỗi client/bidder quan tâm đến phiên này.
   * Dùng {@code contains()} để tránh đăng ký trùng.
   */
  public void registerObserver(Observer o) {
    if (!observers.contains(o)) {
      observers.add(o);
    }
  }

  /** Huỷ đăng ký khi client disconnect. */
  public void removeObserver(Observer o) {
    observers.remove(o);
  }

  /**
   * Phát thông báo tới toàn bộ observer.
   *
   * <p>FIX 3: Vì dùng CopyOnWriteArrayList, vòng lặp này an toàn ngay cả khi
   * thread khác gọi {@link #registerObserver} / {@link #removeObserver} đồng thời.
   */
  private void notifyObservers(String message) {
    for (Observer o : observers) {
      o.update(message);
    }
  }

  // ── Xác định người thắng ─────────────────────────────────────────────────

  /**
   * Xác định người thắng theo quy tắc: giá cao nhất, nếu bằng nhau → sớm nhất.
   *
   * <p>Phương thức này chỉ dùng cho in-memory model (test/demo). Trong luồng thực,
   * winner được xác định bởi DB (winner_id được cập nhật theo từng bid hợp lệ).
   *
   * @return {@link Bid} thắng cuộc, hoặc {@code null} nếu chưa có bid nào
   */
  public Bid determineWinner() {
    Bid winner = null;
    for (Bid bid : bids) {
      if (winner == null) {
        winner = bid;
      } else if (bid.getAmount() > winner.getAmount()) {
        // Giá cao hơn → thay thế
        winner = bid;
      } else if (bid.getAmount() == winner.getAmount()
          && bid.getTimestamp() < winner.getTimestamp()) {
        // Giá bằng nhau → ưu tiên người đặt trước (timestamp nhỏ hơn)
        winner = bid;
      }
    }
    return winner;
  }

  // ── Debug ─────────────────────────────────────────────────────────────────

  public void printInfo() {
    System.out.printf("Auction #%d | Item: %s | HighestBid: %.0f | Bidder: %s%n",
        id,
        item.getName(),
        currentHighestBid,
        highestBidder != null ? highestBidder.getUsername() : "None");
  }

  // ── Getters ───────────────────────────────────────────────────────────────

  public int getId()                  { return id; }
  public Item getItem()               { return item; }
  public double getStartPrice()       { return startPrice; }
  public long getStartTime()          { return startTime; }
  public long getEndTime()            { return endTime; }
  public Bidder getHighestBidder()    { return highestBidder; }
  public List<Bid> getBids()          { return bids; }

  /** Trả về giá cao nhất hiện tại — luôn chính xác sau FIX 1. */
  public double getCurrentHighestBid() { return currentHighestBid; }
}
