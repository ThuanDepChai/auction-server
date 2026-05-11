package com.nhom15.model.item;

public class Fashion extends Item {

  private String size;
  private String color;
  private String brand;

  public Fashion(String id, String name, double startingPrice, String size, String color,
      String brand) {
    super(id, name, startingPrice);
    this.size = size;
    this.color = color;
    this.brand = brand;
  }

  public String getSize() {
    return size;
  }

  public void setSize(String size) {
    this.size = size;
  }

  public String getColor() {
    return color;
  }

  public void setColor(String color) {
    this.color = color;
  }

  public String getBrand() {
    return brand;
  }

  public void setBrand(String brand) {
    this.brand = brand;
  }

  @Override
  public void displayItemInfo() {
    System.out.println(
        "Thời trang: " + getName() + " - Hãng: " + brand + " - Size: " + size + " - Màu: " + color);
  }
}