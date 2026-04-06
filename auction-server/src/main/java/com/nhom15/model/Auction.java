package com.nhom15.model; // Nhớ check lại xem folder model của ông nằm ở đâu nhé
import com.nhom15.model.item.Item;
import com.nhom15.model.user.Bidder;

import java.util.ArrayList;
import java.util.List;

public class Auction {
    private int id;
    private Item item;
    private double startPrice;
    private long startTime;
    private long endTime;

    // Constructor này để khớp với dòng code của ông Thuận (dòng 29 trong ảnh)
    public Auction(int id, Item item, double startPrice, long startTime, long endTime) {
        this.id = id;
        this.item = item;
        this.startPrice = startPrice;
        this.startTime = startTime;
        this.endTime = endTime;
    }
    public int getId(){
        return id;
    }
    // Sau này ông sẽ viết thêm logic đấu giá ở đây
    public void startAuction() {
        System.out.println("Phiên đấu giá cho " + item.getName() + " bắt đầu!");
    }
    // Danh sách lưu các bid
    private List<Bid> bids = new ArrayList<>();
    public void addBid(Bidder bidder, double amount) {
        Bid bid = new Bid(bidder, amount);
        bids.add(bid);
        System.out.println("Người dùng " + bidder.getUsername() + " vừa đặt giá: " + amount + " lúc " + bid.getTimestamp());
    }
    public List<Bid> getBids() {
        return bids;
    }
    // Thêm bid mới
    // Lấy giá cao nhất
    public double getCurrentHighestBid() {
        double highest = startPrice;
        for (Bid bid : bids) {
            if (bid.getAmount() > highest) {
                highest = bid.getAmount();
            }
        }
        return highest;
    }
}