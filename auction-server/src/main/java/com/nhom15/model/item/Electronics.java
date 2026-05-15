package com.nhom15.model.item;

/**
 * Sản phẩm loại điện tử (điện thoại, laptop, …).
 *
 * <p><b>FIX:</b> Cập nhật constructor nhận {@code int id} thay {@code String id}.
 */
public class Electronics extends Item {

  private final String brand;

  /**
   * @param id           khoá chính từ DB
   * @param name         tên sản phẩm
   * @param startingPrice giá khởi điểm
   * @param brand        hãng sản xuất
   */
  public Electronics(int id, String name, double startingPrice, String brand) {
    super(id, name, startingPrice);
    this.brand = brand;
  }

  public String getBrand() { return brand; }

  @Override
  public void displayItemInfo() {
    System.out.println("Đồ điện tử: " + getName() + " - Hãng: " + brand
        + " - Giá khởi điểm: " + getStartingPrice());
  }
}
