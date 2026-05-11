package com.nhom15.model.item;

public class Vehicle extends Item {

  private int year;
  private double mileage;

  public Vehicle(String id, String name, double startingPrice, int year, double mileage) {
    super(id, name, startingPrice); // Gọi constructor của lớp cha Item
    this.year = year;
    this.mileage = mileage;
  }

  // Getter và Setter cho các thuộc tính riêng
  public int getYear() {
    return year;
  }

  public void setYear(int year) {
    this.year = year;
  }

  public double getMileage() {
    return mileage;
  }

  public void setMileage(double mileage) {
    this.mileage = mileage;
  }

  // Ghi đè phương thức hiển thị thông tin
  @Override
  public void displayItemInfo() {
    System.out.println(
        "Phương tiện: " + getName() + " - Năm SX: " + year + " - Số km đã đi: " + mileage + "km");
  }
}