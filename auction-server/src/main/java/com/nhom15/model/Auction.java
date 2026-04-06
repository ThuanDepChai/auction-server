package com.nhom15.model;

import com.nhom15.model.item.Item;
import java.util.Date;

public class Auction {
    private int id;
    private Item item;
    private double startPrice;
    private long startTime;
    private long endTime;

    public Auction(int id, Item item, double startPrice, long startTime, long endTime) {
        this.id = id;
        this.item = item;
        this.startPrice = startPrice;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public void startAuction() {
        System.out.println("Phiên đấu giá cho " + item.getName() + " bắt đầu!");
    }
    public void addBid(com.nhom15.model.user.Bidder bidder, double amount) {
        System.out.println("Người dùng " + bidder + " vừa đặt giá: " + amount);
    }
}