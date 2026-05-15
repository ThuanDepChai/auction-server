package com.nhom15.model.item;

/**
 * Sản phẩm loại phương tiện (ô tô, xe máy, …).
 *
 * <p><b>FIX:</b> Cập nhật constructor nhận {@code int id} thay {@code String id}.
 */
public class Vehicle extends Item {

  private int    year;
  private double mileage;

  /**
   * @param id           khoá chính từ DB
   * @param name         tên phương tiện
   * @param startingPrice giá khởi điểm
   * @param year         năm sản xuất
   * @param mileage      số km đã đi
   */
  public Vehicle(int id, String name, double startingPrice, int year, double mileage) {
    super(id, name, startingPrice);
    this.year    = year;
    this.mileage = mileage;
  }

  public int    getYear()    { return year; }
  public double getMileage() { return mileage; }

  public void setYear(int year)          { this.year    = year; }
  public void setMileage(double mileage) { this.mileage = mileage; }

  @Override
  public void displayItemInfo() {
    System.out.println("Phương tiện: " + getName()
        + " - Năm SX: " + year
        + " - Số km đã đi: " + mileage + " km"
        + " - Giá khởi điểm: " + getStartingPrice());
  }
}
