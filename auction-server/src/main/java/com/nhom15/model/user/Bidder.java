package com.nhom15.model.user;

import com.nhom15.model.Auction;
import com.nhom15.model.Bid;

public class Bidder extends User{
    // Constructor
    public Bidder(int id, String username, String password, String email) {
        super(id, username, password, email, UserRole.BIDDER);
    }

    @Override
    public void printInfo() {
        System.out.println("Bidder: " + username + " | Email: " + email);
    }

    // Chức năng của Bidder
    public void placeBid(Auction auction, double amount) {
        if (amount <= 0) {
            System.out.println("Số tiền đặt phải lớn hơn 0!");
            return;
        }
        if (auction.getCurrentHighestBid() >= amount) {
            System.out.println("Số tiền đặt phải cao hơn giá hiện tại");
            return;
        }
        auction.addBid(this, amount); // thêm lượt đấu giá
    }

    public void viewBidHistory(Auction auction) {
        System.out.println("Lịch sử đấu giá của " + username + " trong phiên " + auction.getId());
        // logic hiển thị danh sách các bid đã đặt
        for (Bid bid: auction.getBids()) {
            if (bid.getBidder().equals(this)) {
                System.out.println(" - Giá: " + bid.getAmount() + " | Thời điểm: " + bid.getTimestamp());
            }
        }
    }
}

