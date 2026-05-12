package com.nhom15.client.command;

import com.google.gson.JsonObject;

/**
 * GetItemImageCommand — lấy Base64 ảnh của 1 sản phẩm theo imagePath.
 *
 * <p>Được gọi lazy sau khi card đã render xong — tránh nhúng toàn bộ ảnh vào
 * list/detail response (gây lag 8MB+ mỗi lần load danh sách).
 */
public class GetItemImageCommand extends ServerCommand {

    private final String imagePath;

    public GetItemImageCommand(String imagePath) {
        this.imagePath = imagePath;
    }

    @Override
    protected JsonObject buildRequest() {
        JsonObject data = new JsonObject();
        data.addProperty("imagePath", imagePath);

        JsonObject req = new JsonObject();
        req.addProperty("action", "GET_ITEM_IMAGE");
        req.add("data", data);
        return req;
    }
}
