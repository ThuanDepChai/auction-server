package com.nhom15.client.command;

import com.google.gson.JsonObject;

public class GetBidHistoryCommand extends ServerCommand {

  private final int auctionId;

  public GetBidHistoryCommand(int auctionId) {
    this.auctionId = auctionId;
  }

  @Override
  protected JsonObject buildRequest() {
    JsonObject data = new JsonObject();
    data.addProperty("auctionId", auctionId);
    JsonObject req = new JsonObject();
    req.addProperty("action", "GET_BID_HISTORY");
    req.add("data", data);
    return req;
  }
}