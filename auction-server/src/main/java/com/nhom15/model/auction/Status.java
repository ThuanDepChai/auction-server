package com.nhom15.model.auction;

/**
 * Trạng thái vòng đời của một phiên đấu giá.
 *
 * <p><b>FIX:</b> Enum cũ dùng {OPEN, RUNNING, FINISHED, PAID, CANCEL} nhưng DB và toàn bộ
 * handler/DAO dùng chuỗi "ACTIVE" / "ENDED". Hai tập giá trị hoàn toàn không khớp khiến
 * Status enum trở thành dead code — không thể so sánh với dữ liệu DB.
 *
 * <p>Enum mới căn chỉnh với DB:
 * <ul>
 *   <li>{@link #PENDING}   — phiên đã tạo nhưng chưa đến giờ bắt đầu</li>
 *   <li>{@link #ACTIVE}    — phiên đang diễn ra (cột {@code status = 'ACTIVE'} trong DB)</li>
 *   <li>{@link #ENDED}     — phiên đã kết thúc (cột {@code status = 'ENDED'})</li>
 *   <li>{@link #CANCELLED} — phiên bị huỷ</li>
 *   <li>{@link #PAID}      — người thắng đã thanh toán</li>
 * </ul>
 *
 * <p>Mỗi giá trị mang trường {@link #dbValue} để ánh xạ sang/từ chuỗi trong DB mà không
 * cần switch-case thủ công.
 */
public enum Status {

  /** Phiên đã tạo, chưa đến giờ bắt đầu. */
  PENDING("PENDING"),

  /** Phiên đang diễn ra — ứng với giá trị 'ACTIVE' trong DB. */
  ACTIVE("ACTIVE"),

  /** Phiên đã kết thúc — ứng với giá trị 'ENDED' trong DB. */
  ENDED("ENDED"),

  /** Phiên bị huỷ bởi seller hoặc admin. */
  CANCELLED("CANCELLED"),

  /** Người thắng đã hoàn tất thanh toán. */
  PAID("PAID");

  // ── Ánh xạ DB ────────────────────────────────────────────────────────────

  /** Giá trị chuỗi lưu trong cột status của bảng auction. */
  private final String dbValue;

  Status(String dbValue) {
    this.dbValue = dbValue;
  }

  /** Trả về chuỗi dùng trong câu SQL / JSON. */
  public String toDb() {
    return dbValue;
  }

  /**
   * Chuyển chuỗi từ DB sang enum.
   *
   * @param dbValue chuỗi lấy từ ResultSet
   * @return Status tương ứng, hoặc {@link #PENDING} nếu không nhận ra
   */
  public static Status fromDb(String dbValue) {
    if (dbValue == null) return PENDING;
    for (Status s : values()) {
      if (s.dbValue.equalsIgnoreCase(dbValue)) return s;
    }
    return PENDING; // fallback an toàn
  }

  // ── Kiểm tra nhanh ────────────────────────────────────────────────────────

  /** @return true nếu phiên đang nhận bid (ACTIVE). */
  public boolean isAcceptingBids() {
    return this == ACTIVE;
  }

  /** @return true nếu phiên đã kết thúc theo mọi nghĩa (ENDED hoặc PAID). */
  public boolean isFinished() {
    return this == ENDED || this == PAID;
  }
}
