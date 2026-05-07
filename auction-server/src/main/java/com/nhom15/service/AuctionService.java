package com.nhom15.service;

import com.nhom15.dao.AuctionDAO;
import com.nhom15.dao.ItemDAO;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.File;
import java.nio.file.Files;
import java.util.Base64;

public class AuctionService {

    private final AuctionDAO auctionDAO = new AuctionDAO();
    private final ItemDAO    itemDAO    = new ItemDAO();

    /** Tạo phiên đấu giá */
    public JsonObject createAuction(int itemId, int sellerId, double startPrice,
                                    double minStep, String endTime) {
        JsonObject result = new JsonObject();
        // Cập nhật status item → IN_AUCTION
        itemDAO.updateStatus(itemId, "IN_AUCTION");

        int auctionId = auctionDAO.createAuction(itemId, sellerId, startPrice, minStep, endTime);
        if (auctionId > 0) {
            result.addProperty("status",    "SUCCESS");
            result.addProperty("auctionId", auctionId);
            result.addProperty("message",   "Tạo phiên đấu giá thành công!");
        } else {
            result.addProperty("status",  "FAIL");
            result.addProperty("message", "Tạo phiên đấu giá thất bại!");
        }
        return result;
    }

    /** Lấy các phiên đang active kèm Base64 ảnh */
    public JsonArray getActiveAuctions() {
        JsonArray auctions = auctionDAO.getActiveAuctions(20);
        return attachImageBase64(auctions);
    }

    /** Chi tiết 1 phiên kèm Base64 ảnh */
    public JsonObject getAuctionDetail(int auctionId) {
        JsonObject auction = auctionDAO.getAuctionById(auctionId);
        if (auction == null) return null;
        attachSingleImage(auction);
        return auction;
    }

    /** Đặt giá */
    public JsonObject placeBid(int auctionId, int bidderId, double amount) {
        JsonObject result = new JsonObject();
        boolean ok = auctionDAO.placeBid(auctionId, bidderId, amount);
        if (ok) {
            result.addProperty("status",  "SUCCESS");
            result.addProperty("message", "Đặt giá thành công!");
            result.addProperty("newPrice", amount);
        } else {
            result.addProperty("status",  "FAIL");
            result.addProperty("message", "Giá không hợp lệ hoặc phiên đã kết thúc!");
        }
        return result;
    }

    /** Lịch sử đặt giá */
    public JsonArray getBidHistory(int auctionId) {
        return auctionDAO.getBidHistory(auctionId);
    }

    /** Auction của seller */
    public JsonArray getAuctionsBySeller(int sellerId) {
        JsonArray auctions = auctionDAO.getAuctionsBySeller(sellerId);
        return attachImageBase64(auctions);
    }

    /** Kết thúc phiên */
    public boolean endAuction(int auctionId) {
        JsonObject auction = auctionDAO.getAuctionById(auctionId);
        if (auction != null) {
            itemDAO.updateStatus(auction.get("itemId").getAsInt(), "SOLD");
        }
        return auctionDAO.endAuction(auctionId);
    }

    // ── Helper ───────────────────────────────────────────────────────────────

    private JsonArray attachImageBase64(JsonArray items) {
        for (int i = 0; i < items.size(); i++) {
            attachSingleImage(items.get(i).getAsJsonObject());
        }
        return items;
    }

    private void attachSingleImage(JsonObject obj) {
        String path = obj.has("imagePath") ? obj.get("imagePath").getAsString() : "";
        if (path != null && !path.isEmpty()) {
            try {
                File f = new File(path);
                if (f.exists()) {
                    byte[] bytes = Files.readAllBytes(f.toPath());
                    obj.addProperty("imageBase64", Base64.getEncoder().encodeToString(bytes));
                    return;
                }
            } catch (Exception e) {
                System.err.println("Lỗi đọc ảnh: " + e.getMessage());
            }
        }
        obj.addProperty("imageBase64", "");
    }
}