package com.nhom15.service;
import com.nhom15.model.auction.Auction;
import com.nhom15.model.auction.Bid;
import com.nhom15.model.item.Item;
import com.nhom15.model.user.Bidder;
import com.nhom15.model.user.Seller;

import java.util.List;

public class AuctionService {
    // Tạo phiên đấu giá mới
    public Auction createAuction(Seller seller, Item item, double startPrice, long startTime, long endTime) {
        Auction auction = new Auction(0, item, startPrice, startTime, endTime);
        System.out.println("Seller " + seller.getUsername() + " đã tạo phiên đấu giá cho sản phẩm: " + item.getName());
        return auction;
    }

    // Bidder đặt giá
    public void placeBid(Bidder bidder, Auction auction, double amount) {
        if (auction.isEnded()) {
            System.out.println("Phiên đấu giá #" + auction.getId() + " đã kết thúc, không thể đặt giá.");
            return;
        }
        if (amount <= 0) {
            System.out.println("Số tiền đặt phải lớn hơn 0!");
            return;
        }
        if (amount <= auction.getCurrentHighestBid()) {
            System.out.println("Số tiền đặt phải cao hơn giá hiện tại (" + auction.getCurrentHighestBid() + ")");
            return;
        }
        auction.addBid(bidder, amount);
        System.out.println("Bidder " + bidder.getUsername() + " đã đặt giá " + amount + " cho phiên #" + auction.getId());
    }

    // Kết thúc phiên đấu giá
    public void endAuction(Auction auction) {
        if (!auction.isEnded()) {
            System.out.println("Phiên đấu giá #" + auction.getId() + " chưa đến thời điểm kết thúc.");
            return;
        }
        Bidder winner = auction.getHighestBidder();
        if (winner != null) {
            System.out.println("Phiên đấu giá #" + auction.getId() + " đã kết thúc. Người thắng: " + winner.getUsername() +
                    " với giá " + auction.getCurrentHighestBid());
        } else {
            System.out.println("Phiên đấu giá #" + auction.getId() + " kết thúc mà không có người tham gia.");
        }
    }

    // Xem lịch sử bid của một phiên
    public void viewAuctionHistory(Auction auction) {
        System.out.println("Lịch sử đấu giá của phiên #" + auction.getId());
        List<Bid> bids = auction.getBids();
        if (bids.isEmpty()) {
            System.out.println(" - Chưa có lượt đặt giá nào.");
            return;
        }
        for (Bid bid : bids) {
            System.out.println(" - Bidder: " + bid.getBidder().getUsername() +
                    " | Giá: " + bid.getAmount() +
                    " | Thời điểm: " + bid.getTimestamp());
        }
    }
}
