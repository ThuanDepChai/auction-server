package com.nhom15.service;

import com.nhom15.model.auction.Auction;
import com.nhom15.model.auction.Bid;
import com.nhom15.model.user.Bidder;

/**
 * BidderService — tầng nghiệp vụ OOP cho model Bidder.
 *
 * <p><b>Lưu ý phân tầng:</b> Class này thao tác trên các đối tượng domain
 * (Auction, Bidder) trong bộ nhớ, phục vụ demo OOP và unit-test logic thuần.
 * Việc đặt giá thực tế qua mạng được xử lý bởi {@link AuctionService}
 * → {@link com.nhom15.network.AuctionManager} → {@link com.nhom15.dao.AuctionDAO}.
 */
public class BidderService {

  /** Đặt giá vào phiên đấu giá trong bộ nhớ (dùng cho demo OOP / unit test). */
  public void placeBid(Bidder bidder, Auction auction, double amount) {
    if (amount <= 0) {
      System.out.println("Số tiền đặt phải lớn hơn 0!");
      return;
    }
    if (auction.getCurrentHighestBid() >= amount) {
      System.out.println("Số tiền đặt phải cao hơn giá hiện tại");
      return;
    }
    auction.addBid(bidder, amount);
  }

  public void viewBidHistory(Bidder bidder, Auction auction) {
    System.out.println(
            "Lịch sử đấu giá của " + bidder.getUsername() + " trong phiên " + auction.getId());
    for (Bid bid : auction.getBids()) {
      if (bid.getBidder().equals(bidder)) {
        System.out.println(" - Giá: " + bid.getAmount() + " | Thời điểm: " + bid.getTimestamp());
      }
    }
  }
}