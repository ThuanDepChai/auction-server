package com.nhom15.client.command;

import com.google.gson.JsonObject;

/**
 * GetAutoBidStatusCommand — lấy cấu hình auto-bid hiện tại của user trong phiên đấu giá.
 * Server trả về: { status: "SUCCESS", autoBid: { maxBid, increment, active } }
 */
public class GetAutoBidStatusCommand extends ServerCommand {

    private final int auctionId;
    private final int bidderId;

    public GetAutoBidStatusCommand(int auctionId, int bidderId) {
        this.auctionId = auctionId;
        this.bidderId = bidderId;
    }

    @Override
    protected JsonObject buildRequest() {
        JsonObject data = new JsonObject();
        data.addProperty("auctionId", auctionId);
        data.addProperty("bidderId", bidderId);

        JsonObject req = new JsonObject();
        req.addProperty("action", "GET_AUTO_BID_STATUS");
        req.add("data", data);
        return req;
    }
}