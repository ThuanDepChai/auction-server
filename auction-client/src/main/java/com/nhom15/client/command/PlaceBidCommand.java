package com.nhom15.client.command;

import com.google.gson.JsonObject;

/**
 * PlaceBidCommand — đóng gói request đặt giá.
 */
public class PlaceBidCommand extends ServerCommand {

  private final int auctionId;
  private final int bidderId;
  private final double amount;

  public PlaceBidCommand(int auctionId, int bidderId, double amount) {
    this.auctionId = auctionId;
    this.bidderId = bidderId;
    this.amount = amount;
  }

  @Override
  protected JsonObject buildRequest() {
    JsonObject data = new JsonObject();
    data.addProperty("auctionId", auctionId);
    data.addProperty("bidderId", bidderId);
    data.addProperty("amount", amount);

    JsonObject req = new JsonObject();
    req.addProperty("action", "PLACE_BID");
    req.add("data", data);
    return req;
  }
}