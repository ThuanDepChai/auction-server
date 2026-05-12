package com.nhom15.exception;

/**
 * Ném khi người dùng cố đặt giá vào một phiên đấu giá đã kết thúc hoặc không còn ACTIVE.
 */
public class AuctionClosedException extends Exception {

    private final int auctionId;
    private final String currentStatus;

    public AuctionClosedException(int auctionId, String currentStatus) {
        super(String.format(
                "Phiên đấu giá #%d đã đóng (trạng thái: %s). Không thể đặt giá!",
                auctionId, currentStatus));
        this.auctionId = auctionId;
        this.currentStatus = currentStatus;
    }

    public int getAuctionId() {
        return auctionId;
    }

    public String getCurrentStatus() {
        return currentStatus;
    }
}