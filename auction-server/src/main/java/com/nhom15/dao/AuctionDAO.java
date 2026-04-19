package com.nhom15.dao;

import com.nhom15.util.DBConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AuctionDAO {
    // Lấy danh sách item đang đấu giá
    public List<String> getActiveAuctions() {
        List<String> auctions = new ArrayList<>();
        String sql = "SELECT item_id FROM auction"; //join với bảng item để lấy tên
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                auctions.add(String.valueOf(rs.getInt("item_id")));
            }
        } catch (SQLException e) {
            System.out.println("Lỗi lấy danh sách đấu giá: " + e.getMessage());
        }
        return auctions;
    }
}