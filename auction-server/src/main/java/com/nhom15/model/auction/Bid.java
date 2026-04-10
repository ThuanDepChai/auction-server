package com.nhom15.model.auction;
import com.nhom15.model.user.Bidder;

public class Bid {
    private Bidder bidder;
    private double amount;
    private long timestamp;
    public Bid (Bidder bidder,double amount) {
        this.bidder=bidder;
        this.amount=amount;
        this.timestamp= System.currentTimeMillis();
    }
    public Bidder getBidder() {
        return bidder;
    }
    public double getAmount() {
        return amount;
    }
    public long getTimestamp() {
        return timestamp;
    }
}
