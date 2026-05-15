package com.nhom15.model.item;

/**
 * Sản phẩm loại thời trang (quần áo, giày, túi, …).
 *
 * <p><b>FIX:</b> Cập nhật constructor nhận {@code int id} thay {@code String id}.
 */
public class Fashion extends Item {

  private String size;
  private String color;
  private String brand;

  /**
   * @param id           khoá chính từ DB
   * @param name         tên sản phẩm
   * @param startingPrice giá khởi điểm
   * @param size         kích cỡ (S/M/L/XL …)
   * @param color        màu sắc
   * @param brand        hãng
   */
  public Fashion(int id, String name, double startingPrice,
      String size, String color, String brand) {
    super(id, name, startingPrice);
    this.size  = size;
    this.color = color;
    this.brand = brand;
  }

  public String getSize()  { return size; }
  public String getColor() { return color; }
  public String getBrand() { return brand; }

  public void setSize(String size)   { this.size  = size; }
  public void setColor(String color) { this.color = color; }
  public void setBrand(String brand) { this.brand = brand; }

  @Override
  public void displayItemInfo() {
    System.out.println("Thời trang: " + getName()
        + " - Hãng: " + brand
        + " - Size: " + size
        + " - Màu: " + color
        + " - Giá khởi điểm: " + getStartingPrice());
  }
}
