package com.nhom15.service;

import com.nhom15.model.auction.Auction;
import com.nhom15.model.item.Item;

public class SellerService {

  //thêm sản phẩm vào database
  public void addItem(Item item) {
    System.out.println("Thêm sản phẩm: " + item.getName());
    // logic thêm sản phẩm vào hệ thống
  }

  public void updateItem(Item item) {
    System.out.println("Cập nhật sản phẩm: " + item.getName());
    // logic cập nhật thông tin sản phẩm
  }

  public void removeItem(Item item) {
    System.out.println("Xóa sản phẩm: " + item.getName());
    // logic xóa sản phẩm khỏi hệ thống
  }

  public Auction createAuction(Item item, double startPrice, long startTime, long endTime) {
    Auction auction = new Auction(0, item, startPrice, startTime, endTime);
    System.out.println("Tạo phiên đấu giá cho sản phẩm: " + item.getName());
    return auction;
  }
}
