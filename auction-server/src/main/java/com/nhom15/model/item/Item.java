package com.nhom15.model.item;

public abstract class Item {

  private final String id;
  private final String name;
  private final double startingPrice;

  public Item(String id, String name, double startingPrice) {
    this.id = id;
    this.name = name;
    this.startingPrice = startingPrice;
  }
  // Các hàm getter ,setter

  public String getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public double getStartingPrice() {
    return startingPrice;
  }

  public abstract void displayItemInfo();
}
