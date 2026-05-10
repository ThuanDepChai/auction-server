package com.nhom15.client.command;

import com.google.gson.JsonObject;

public class GetActiveAuctionsCommand extends ServerCommand {

    @Override
    protected JsonObject buildRequest() {
        JsonObject req = new JsonObject();
        req.addProperty("action", "GET_AUCTIONS");
        return req;
    }
}