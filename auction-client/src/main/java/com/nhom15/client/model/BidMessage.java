package com.nhom15.client.model;

/**
 * DTO cho một lượt đặt giá — dùng hiển thị lịch sử bid trong BiddingRoomController. Map trực tiếp
 * với JSON server trả về từ GET_BID_HISTORY.
 */
public class BidMessage {

  private int bidId;
  private int auctionId;
  private String username;     // người đặt
  private double amount;       // giá đặt
  private String bidTime;      // thời điểm đặt

  public BidMessage() {
  }

  public BidMessage(int bidId, int auctionId, String username,
      double amount, String bidTime) {
    this.bidId = bidId;
    this.auctionId = auctionId;
    this.username = username;
    this.amount = amount;
    this.bidTime = bidTime;
  }

  // ── Getters & Setters ────────────────────────────────────────────────────

  public int getBidId() {
    return bidId;
  }

  public void setBidId(int v) {
    bidId = v;
  }

  public int getAuctionId() {
    return auctionId;
  }

  public void setAuctionId(int v) {
    auctionId = v;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String v) {
    username = v;
  }

  public double getAmount() {
    return amount;
  }

  public void setAmount(double v) {
    amount = v;
  }

  public String getBidTime() {
    return bidTime;
  }

  public void setBidTime(String v) {
    bidTime = v;
  }

  @Override
  public String toString() {
    return "BidMessage{username='" + username
        + "', amount=" + amount + ", bidTime='" + bidTime + "'}";
  }
}