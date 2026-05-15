package com.nhom15.model.item;

/**
 * Sản phẩm loại nghệ thuật (tranh, tượng, …).
 *
 * <p><b>FIX:</b> Cập nhật constructor nhận {@code int id} thay {@code String id}
 * để khớp với {@link Item} sau khi Item kế thừa Entity.
 */
public class Art extends Item {

  private final String artist;

  /**
   * @param id           khoá chính từ DB
   * @param name         tên tác phẩm
   * @param startingPrice giá khởi điểm
   * @param artist       tên tác giả / nghệ sĩ
   */
  public Art(int id, String name, double startingPrice, String artist) {
    super(id, name, startingPrice);
    this.artist = artist;
  }

  public String getArtist() { return artist; }

  @Override
  public void displayItemInfo() {
    System.out.println("Nghệ thuật: " + getName() + " - Tác giả: " + artist
        + " - Giá khởi điểm: " + getStartingPrice());
  }
}
