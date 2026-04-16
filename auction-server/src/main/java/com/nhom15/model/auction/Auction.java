package com.nhom15.model.auction;

import com.nhom15.model.item.Item;
import com.nhom15.model.user.Bidder;

import java.util.ArrayList;
import java.util.List;

public class Auction {
    private int id;
    private Item item;
    private double startPrice;
    private double currentHighestBid;
    private Bidder highestBidder;
    private long startTime;
    private long endTime;
    private List<Bid> bids = new ArrayList<>();
    // Danh sách lưu các bids
    public List<Bid> getBids() {
        return bids;
    }
    //danh sách Observer
    private List<Observer> observers = new ArrayList<>();

    public Auction(int id, Item item, double startPrice, long startTime, long endTime) {
        this.id = id;
        this.item = item;
        this.startPrice = startPrice;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public int getId() {
        return id;
    }
    public Item getItem() {
        return item;
    }
    public double getStartPrice(){ return startPrice;}
    public long getStartTime(){ return startTime;}
    public long getEndTime() { return endTime;}
    public Bidder getHighestBidder() { return highestBidder;}

    // Thêm bids mới
    public void addBid(Bidder bidder, double amount) {
        Bid bid = new Bid(bidder, amount);
        bids.add(bid);
        System.out.println("Người dùng " + bidder.getUsername() + " vừa đặt giá: " + amount + " lúc " + bid.getTimestamp());
        String msg = "Sản phẩm " + item.getName() + " có giá mới: " + amount + " từ " + bidder.getUsername();
        notifyObservers("Có bid mới: " + amount + " từ " + bidder.getUsername()); // thêm thông báo ( observer)
    }

    public boolean isStarted() {
        return System.currentTimeMillis() >= startTime;
    }

    public boolean isEnded() {
        return System.currentTimeMillis() > endTime;
    }
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

    //  đăng ký người theo dõi
    public void registerObserver(Observer o) {
        if (!observers.contains(o)) {
            observers.add(o);
        }
    }

    //phát thông báo
    private void notifyObservers(String message) {
        for (Observer o : observers) {
            o.update(message);
        }
    }
    public void printInfo() {
        System.out.println("Auction #" + id + " | Item: " + item.getName() +
                " | Highest Bid: " + currentHighestBid +
                " | Bidder: " + (highestBidder != null ? highestBidder.getUsername() : "None"));
    }
}