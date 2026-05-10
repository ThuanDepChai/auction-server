package com.nhom15.client.command;

import com.google.gson.JsonObject;

public class GetAuctionDetailCommand extends ServerCommand {
    private final int auctionId;
    public GetAuctionDetailCommand(int auctionId) { this.auctionId = auctionId; }

    @Override
    protected JsonObject buildRequest() {
        JsonObject data = new JsonObject();
        data.addProperty("auctionId", auctionId);
        JsonObject req = new JsonObject();
        req.addProperty("action", "GET_AUCTION_DETAIL");
        req.add("data", data);
        return req;
    }
}