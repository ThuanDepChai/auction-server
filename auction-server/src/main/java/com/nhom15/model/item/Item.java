package com.nhom15.model.item;

import java.util.List;
import java.util.ArrayList;

public abstract class Item {

  private final String id;
  private final String name;
  private final double startingPrice;
  private List<String> subImagePaths = new ArrayList<>(); // Danh sách chứa các ảnh phụ

  public Item(String id, String name, double startingPrice) {
    this.id = id;
    this.name = name;
    this.startingPrice = startingPrice;
  }

  // --- Getter cho các thuộc tính final (không có Setter vì là final) ---
  public String getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public double getStartingPrice() {
    return startingPrice;
  }

  // --- Getter và Setter cho danh sách ảnh phụ ---
  public List<String> getSubImagePaths() {
    return subImagePaths;
  }

  public void setSubImagePaths(List<String> subImagePaths) {
    this.subImagePaths = subImagePaths;
  }

  // Phương thức hỗ trợ thêm nhanh 1 ảnh vào danh sách
  public void addSubImagePath(String path) {
    this.subImagePaths.add(path);
  }

  public abstract void displayItemInfo();
}