package com.nhom15.dao;

import com.nhom15.util.DBConnection;
import java.sql.*;

public class BidDAO {
    // Thêm một lượt đặt giá mới
    public boolean placeBid(int auctionId, int userId, double amount) {
        String sql = "INSERT INTO bidtransaction (auction_id, user_id, bid_amount) VALUES (?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, auctionId);
            ps.setInt(2, userId);
            ps.setDouble(3, amount);
            ps.executeUpdate();
            System.out.println("Đặt giá thành công!");
            return true;
        } catch (SQLException e) {
            System.out.println("Lỗi đặt giá: " + e.getMessage());
            return false;
        }
    }
}