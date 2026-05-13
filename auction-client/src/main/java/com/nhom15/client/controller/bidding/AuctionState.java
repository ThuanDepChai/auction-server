package com.nhom15.client.controller.bidding;

/**
 * AuctionState — trạng thái chia sẻ giữa các sub-controller của BiddingRoom.
 *
 * Tất cả sub-controller đều giữ tham chiếu đến cùng 1 instance này,
 * đảm bảo currentPrice / minStep luôn nhất quán mà không cần truyền
 * tham số qua lại.
 */
public class AuctionState {

    private int    auctionId;
    private double currentPrice;
    private double minStep      = 50_000;
    private boolean autoBidActive = false;
    private boolean auctionEnded  = false;

    // ── Getters / Setters ────────────────────────────────────────────────

    public int getAuctionId()            { return auctionId; }
    public void setAuctionId(int id)     { this.auctionId = id; }

    public double getCurrentPrice()               { return currentPrice; }
    public void   setCurrentPrice(double p)       { this.currentPrice = p; }

    public double getMinStep()                    { return minStep; }
    public void   setMinStep(double s)            { this.minStep = s; }

    public boolean isAutoBidActive()              { return autoBidActive; }
    public void    setAutoBidActive(boolean v)    { this.autoBidActive = v; }

    public boolean isAuctionEnded()               { return auctionEnded; }
    public void    setAuctionEnded(boolean v)     { this.auctionEnded = v; }

    /** Giá tối thiểu hợp lệ cho lần đặt tiếp theo. */
    public double nextMinBid() {
        return currentPrice + minStep;
    }
}
