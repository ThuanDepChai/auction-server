package com.nhom15.model.auction;

import com.nhom15.model.Entity;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Bản ghi giao dịch đặt giá — lưu vết mọi lượt bid trong hệ thống.
 *
 * <p><b>Lý do tạo class mới:</b><br>
 * Spec yêu cầu: "Lớp BidTransaction lưu vết mọi lượt đặt giá (Bidder ID, Item ID,
 * Price, Timestamp) để phục vụ việc tra cứu lịch sử và vẽ biểu đồ."
 *
 * <p>Class {@link Bid} cũ lưu trực tiếp đối tượng {@code Bidder} (không phải ID)
 * và không có {@code itemId} — không đủ dùng cho:
 * <ul>
 *   <li>Vẽ {@code LineChart} biến động giá theo thời gian</li>
 *   <li>Tra cứu lịch sử bid của 1 bidder trên nhiều phiên</li>
 *   <li>Thống kê giao dịch theo sản phẩm</li>
 * </ul>
 *
 * <p>{@code BidTransaction} kế thừa {@link Entity} (có {@code id} = bid_id từ DB),
 * lưu các khoá ngoại bằng ID nguyên thuỷ (int) để dễ serialize sang JSON / ánh xạ DB.
 *
 * <p><b>Ví dụ sử dụng cho LineChart JavaFX:</b>
 * <pre>{@code
 *   List<BidTransaction> history = bidTransactionDAO.getByAuction(auctionId);
 *   XYChart.Series<String, Number> series = new XYChart.Series<>();
 *   for (BidTransaction tx : history) {
 *       series.getData().add(
 *           new XYChart.Data<>(tx.getFormattedTime(), tx.getAmount())
 *       );
 *   }
 * }</pre>
 */
public class BidTransaction extends Entity {

  // ── Các trường định danh ─────────────────────────────────────────────────

  /** ID của người đặt giá (khoá ngoại → bảng user). */
  private final int bidderId;

  /** Tên hiển thị — lưu kèm để tránh JOIN khi render UI. */
  private final String bidderUsername;

  /** ID của phiên đấu giá (khoá ngoại → bảng auction). */
  private final int auctionId;

  /** ID của sản phẩm (khoá ngoại → bảng item) — dùng cho biểu đồ theo sản phẩm. */
  private final int itemId;

  /** Tên sản phẩm — lưu kèm để tránh JOIN khi render. */
  private final String itemName;

  // ── Dữ liệu giao dịch ────────────────────────────────────────────────────

  /** Số tiền đặt giá (VND). */
  private final double amount;

  /**
   * Thời điểm đặt giá (epoch milli) — dùng làm trục X cho LineChart.
   * Lấy từ {@code System.currentTimeMillis()} hoặc {@code bid_time} trong DB.
   */
  private final long timestamp;

  /** Đây là auto-bid (tự động) hay manual bid (thủ công). */
  private final boolean autoBid;

  // ── Formatter ────────────────────────────────────────────────────────────

  private static final DateTimeFormatter DISPLAY_FMT =
      DateTimeFormatter.ofPattern("HH:mm:ss dd/MM");

  // ── Constructors ──────────────────────────────────────────────────────────

  /**
   * Constructor đầy đủ — dùng khi tạo từ dữ liệu DB.
   *
   * @param id              bid_id từ DB (kế thừa Entity)
   * @param bidderId        user_id của người đặt
   * @param bidderUsername  username để hiển thị
   * @param auctionId       auction_id
   * @param itemId          item_id
   * @param itemName        tên sản phẩm
   * @param amount          giá đặt
   * @param timestamp       thời điểm (epoch milli)
   * @param autoBid         true nếu hệ thống tự đặt (auto-bid)
   */
  public BidTransaction(int id, int bidderId, String bidderUsername,
      int auctionId, int itemId, String itemName,
      double amount, long timestamp, boolean autoBid) {
    this.id              = id;
    this.bidderId        = bidderId;
    this.bidderUsername  = bidderUsername;
    this.auctionId       = auctionId;
    this.itemId          = itemId;
    this.itemName        = itemName;
    this.amount          = amount;
    this.timestamp       = timestamp;
    this.autoBid         = autoBid;
  }

  /**
   * Constructor rút gọn — dùng khi ghi nhận bid mới (chưa có bid_id).
   * {@code id} được set sau khi INSERT DB trả về generated key.
   */
  public BidTransaction(int bidderId, String bidderUsername,
      int auctionId, int itemId, String itemName,
      double amount, boolean autoBid) {
    this(-1, bidderId, bidderUsername, auctionId, itemId, itemName,
        amount, System.currentTimeMillis(), autoBid);
  }

  // ── Getters ───────────────────────────────────────────────────────────────

  public int    getBidderId()       { return bidderId; }
  public String getBidderUsername() { return bidderUsername; }
  public int    getAuctionId()      { return auctionId; }
  public int    getItemId()         { return itemId; }
  public String getItemName()       { return itemName; }
  public double getAmount()         { return amount; }
  public long   getTimestamp()      { return timestamp; }
  public boolean isAutoBid()        { return autoBid; }

  // ── Helpers cho biểu đồ ──────────────────────────────────────────────────

  /**
   * Trả về nhãn thời gian định dạng "HH:mm:ss dd/MM" — dùng làm trục X của LineChart.
   *
   * <pre>{@code
   *   series.getData().add(new XYChart.Data<>(tx.getFormattedTime(), tx.getAmount()));
   * }</pre>
   */
  public String getFormattedTime() {
    LocalDateTime ldt = LocalDateTime.ofInstant(
        Instant.ofEpochMilli(timestamp), ZoneId.systemDefault());
    return ldt.format(DISPLAY_FMT);
  }

  // ── Debug ─────────────────────────────────────────────────────────────────

  @Override
  public String toString() {
    return String.format("BidTransaction{id=%d, bidder='%s', item='%s', amount=%.0f, time=%s, auto=%b}",
        id, bidderUsername, itemName, amount, getFormattedTime(), autoBid);
  }
}
