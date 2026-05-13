package com.nhom15.service;

import com.google.gson.JsonObject;
import com.nhom15.dao.AutoBidDAO;

/**
 * AutoBidService — xử lý logic nghiệp vụ cho tính năng đấu giá tự động.
 *
 * <p>Luồng hoạt động:
 * <ol>
 *   <li>Bidder đăng ký auto-bid với maxBid (giá tối đa) và increment (bước tăng)</li>
 *   <li>Mỗi khi có bid mới từ người khác, AuctionManager gọi triggerAutoBids()
 *       để tự động đặt giá cho các bidder có auto-bid đang active</li>
 *   <li>Hệ thống tự đặt giá = currentPrice + increment, nếu vẫn ≤ maxBid</li>
 * </ol>
 */
public class AutoBidService {

    private final AutoBidDAO autoBidDAO = new AutoBidDAO();

    /**
     * Đăng ký hoặc cập nhật cấu hình auto-bid cho 1 bidder trong 1 phiên.
     */
    public JsonObject setAutoBid(int auctionId, int bidderId, double maxBid, double increment) {
        JsonObject result = new JsonObject();
        boolean ok = autoBidDAO.upsertAutoBid(auctionId, bidderId, maxBid, increment);
        if (ok) {
            result.addProperty("status", "SUCCESS");
            result.addProperty("message", "Kích hoạt auto-bid thành công!");
        } else {
            result.addProperty("status", "FAIL");
            result.addProperty("message", "Kích hoạt auto-bid thất bại!");
        }
        return result;
    }

    /**
     * Huỷ auto-bid — đặt active = FALSE thay vì xóa để giữ lịch sử.
     */
    public JsonObject cancelAutoBid(int auctionId, int bidderId) {
        JsonObject result = new JsonObject();
        boolean ok = autoBidDAO.cancelAutoBid(auctionId, bidderId);
        if (ok) {
            result.addProperty("status", "SUCCESS");
            result.addProperty("message", "Đã huỷ auto-bid!");
        } else {
            result.addProperty("status", "FAIL");
            result.addProperty("message", "Huỷ auto-bid thất bại hoặc chưa có cấu hình!");
        }
        return result;
    }

    /**
     * Lấy trạng thái auto-bid hiện tại.
     * Trả về: { status, autoBid: { active, maxBid, increment } }
     */
    public JsonObject getAutoBidStatus(int auctionId, int bidderId) {
        JsonObject result = new JsonObject();
        JsonObject autoBid = autoBidDAO.getAutoBid(auctionId, bidderId);
        result.addProperty("status", "SUCCESS");
        if (autoBid != null) {
            result.add("autoBid", autoBid);
        } else {
            // Không có bản ghi → trả về active = false
            JsonObject empty = new JsonObject();
            empty.addProperty("active", false);
            result.add("autoBid", empty);
        }
        return result;
    }
}