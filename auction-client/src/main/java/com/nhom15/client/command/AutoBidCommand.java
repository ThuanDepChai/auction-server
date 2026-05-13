package com.nhom15.client.command;

import com.google.gson.JsonObject;

/**
 * AutoBidCommand — đăng ký / huỷ đấu giá tự động.
 *
 * <p>Server nhận action SET_AUTO_BID hoặc CANCEL_AUTO_BID và lưu cấu hình vào bảng auto_bids.
 * Khi có bid mới từ người khác, server tự động đặt giá thay user nếu currentPrice + minStep ≤
 * maxBid.
 */
public class AutoBidCommand extends ServerCommand {

    public enum Mode {
        SET,
        CANCEL
    }

    private final Mode mode;
    private final int auctionId;
    private final int bidderId;
    private final double maxBid;      // chỉ dùng khi mode = SET
    private final double increment;   // bước tăng tự động

    /** Constructor đặt auto-bid. */
    public AutoBidCommand(int auctionId, int bidderId, double maxBid, double increment) {
        this.mode = Mode.SET;
        this.auctionId = auctionId;
        this.bidderId = bidderId;
        this.maxBid = maxBid;
        this.increment = increment;
    }

    /** Constructor huỷ auto-bid. */
    public AutoBidCommand(int auctionId, int bidderId) {
        this.mode = Mode.CANCEL;
        this.auctionId = auctionId;
        this.bidderId = bidderId;
        this.maxBid = 0;
        this.increment = 0;
    }

    @Override
    protected JsonObject buildRequest() {
        JsonObject data = new JsonObject();
        data.addProperty("auctionId", auctionId);
        data.addProperty("bidderId", bidderId);

        if (mode == Mode.SET) {
            data.addProperty("maxBid", maxBid);
            data.addProperty("increment", increment);
        }

        JsonObject req = new JsonObject();
        req.addProperty("action", mode == Mode.SET ? "SET_AUTO_BID" : "CANCEL_AUTO_BID");
        req.add("data", data);
        return req;
    }
}