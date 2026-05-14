package com.nhom15.dao;

import com.google.gson.JsonObject;
import com.nhom15.util.DBConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * AutoBidDAO — thao tác DB cho bảng auto_bid.
 *
 * <p>Bảng auto_bid có UNIQUE KEY (auction_id, bidder_id) nên dùng INSERT ... ON DUPLICATE KEY UPDATE
 * để upsert (tạo mới hoặc cập nhật nếu đã tồn tại).
 */
public class AutoBidDAO {

    /**
     * Tạo mới hoặc cập nhật cấu hình auto-bid.
     * Nếu bidder đã có record → cập nhật maxBid, increment và bật lại active = TRUE.
     *
     * @return true nếu thành công
     */
    public boolean upsertAutoBid(int auctionId, int bidderId, double maxBid, double increment) {
        String sql =
                "INSERT INTO auto_bid (auction_id, bidder_id, max_bid, increment_step, active) "
                        + "VALUES (?, ?, ?, ?, TRUE) "
                        + "ON DUPLICATE KEY UPDATE "
                        + "  max_bid = VALUES(max_bid), "
                        + "  increment_step = VALUES(increment_step), "
                        + "  active = TRUE";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, auctionId);
            ps.setInt(2, bidderId);
            ps.setDouble(3, maxBid);
            ps.setDouble(4, increment);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Huỷ auto-bid — đặt active = FALSE, giữ lại record để tra cứu lịch sử.
     *
     * @return true nếu cập nhật được (record tồn tại và đang active)
     */
    public boolean cancelAutoBid(int auctionId, int bidderId) {
        String sql = "UPDATE auto_bid SET active = FALSE "
                + "WHERE auction_id = ? AND bidder_id = ? AND active = TRUE";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, auctionId);
            ps.setInt(2, bidderId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Lấy cấu hình auto-bid hiện tại của 1 bidder trong 1 phiên.
     *
     * @return JsonObject { active, maxBid, increment } hoặc null nếu không có record
     */
    public JsonObject getAutoBid(int auctionId, int bidderId) {
        String sql = "SELECT active, max_bid, increment_step FROM auto_bid "
                + "WHERE auction_id = ? AND bidder_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, auctionId);
            ps.setInt(2, bidderId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    JsonObject obj = new JsonObject();
                    obj.addProperty("active",    rs.getBoolean("active"));
                    obj.addProperty("maxBid",    rs.getDouble("max_bid"));
                    obj.addProperty("increment", rs.getDouble("increment_step"));
                    return obj;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Lấy tất cả auto-bid đang active cho 1 phiên đấu giá.
     * Dùng bởi AuctionManager để trigger auto-bid sau mỗi lần có bid mới.
     *
     * <p>Thứ tự: maxBid DESC → đặt giá cao nhất được ưu tiên.
     * Nếu maxBid bằng nhau → người đăng ký auto-bid trước (auto_bid_id ASC) được ưu tiên.
     *
     * @return danh sách { bidderId, maxBid, increment } theo thứ tự ưu tiên
     */
    public java.util.List<JsonObject> getActiveAutoBids(int auctionId) {
        String sql = "SELECT bidder_id, max_bid, increment_step FROM auto_bid "
                + "WHERE auction_id = ? AND active = TRUE "
                + "ORDER BY max_bid DESC, auto_bid_id ASC";
        java.util.List<JsonObject> list = new java.util.ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, auctionId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    JsonObject obj = new JsonObject();
                    obj.addProperty("bidderId",  rs.getInt("bidder_id"));
                    obj.addProperty("maxBid",    rs.getDouble("max_bid"));
                    obj.addProperty("increment", rs.getDouble("increment_step"));
                    list.add(obj);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }
}