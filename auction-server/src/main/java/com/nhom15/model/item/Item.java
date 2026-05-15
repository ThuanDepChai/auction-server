package com.nhom15.model.item;

import com.nhom15.model.Entity;

/**
 * Lớp cha trừu tượng cho tất cả sản phẩm đấu giá.
 *
 * <p><b>FIX — Đồng nhất ID qua Entity:</b><br>
 * Code cũ khai báo {@code private final String id} riêng — không kế thừa từ {@link Entity}.
 * Điều này vi phạm yêu cầu "dùng lớp cha Entity chứa ID để quản lý đồng nhất":
 * <ul>
 *   <li>ID trong DB là kiểu {@code INT} (item_id), nhưng model dùng {@code String}
 *       → không ánh xạ được trực tiếp</li>
 *   <li>ItemFactory nhận {@code String id} nhưng truyền vào từ DAO là {@code int}
 *       → buộc phải ép kiểu thủ công, dễ lỗi</li>
 *   <li>Không kế thừa {@code createdAt} / {@code updatedAt} từ Entity</li>
 * </ul>
 *
 * <p>Fix: Item kế thừa Entity (int id, long createdAt, long updatedAt). Tất cả subclass
 * và ItemFactory được cập nhật để truyền {@code int id}.
 */
public abstract class Item extends Entity {

  private final String name;
  private final double startingPrice;
  private String description = "";
  private String category    = "";
  private String imagePath   = "";

  // ── Constructor ───────────────────────────────────────────────────────────

  /**
   * @param id           khoá chính từ DB (item_id)
   * @param name         tên sản phẩm
   * @param startingPrice giá khởi điểm
   */
  public Item(int id, String name, double startingPrice) {
    this.id           = id;          // thuộc tính protected của Entity
    this.name         = name;
    this.startingPrice = startingPrice;
  }

  // ── Abstract ──────────────────────────────────────────────────────────────

  /** In thông tin đặc trưng của từng loại sản phẩm con. */
  public abstract void displayItemInfo();

  // ── Getters (id thừa hưởng từ Entity) ───────────────────────────────────

  public String getName()           { return name; }
  public double getStartingPrice()  { return startingPrice; }
  public String getDescription()    { return description; }
  public String getCategory()       { return category; }
  public String getImagePath()      { return imagePath; }

  // ── Setters (metadata có thể thay đổi sau khi tạo) ───────────────────────

  public void setDescription(String description) { this.description = description; }
  public void setCategory(String category)       { this.category = category; }
  public void setImagePath(String imagePath)     { this.imagePath = imagePath; }
}
