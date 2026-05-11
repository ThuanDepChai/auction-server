package com.nhom15.service;

import com.nhom15.model.auction.Auction;
import com.nhom15.model.auction.Bid;
import com.nhom15.model.user.Bidder;

public class BidderService {

  // Chức năng của Bidder
  public void placeBid(Bidder bidder, Auction auction, double amount) {
    if (amount <= 0) {
      System.out.println("Số tiền đặt phải lớn hơn 0!");
      return;
    }
    if (auction.getCurrentHighestBid() >= amount) {
      System.out.println("Số tiền đặt phải cao hơn giá hiện tại");
      return;
    }
    auction.addBid(bidder, amount); // thêm lượt đấu giá
  }

  public void viewBidHistory(Bidder bidder, Auction auction) {
    System.out.println(
        "Lịch sử đấu giá của " + bidder.getUsername() + " trong phiên " + auction.getId());
    // logic hiển thị danh sách các bid đã đặt
    for (Bid bid : auction.getBids()) {
      if (bid.getBidder().equals(this)) {
        System.out.println(" - Giá: " + bid.getAmount() + " | Thời điểm: " + bid.getTimestamp());
      }
    }
  }

}
