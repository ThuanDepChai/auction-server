package com.nhom15.dao;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.nhom15.util.DBConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class AuctionDAO {

  /**
   * Tạo phiên đấu giá mới
   */
  public int createAuction(int itemId, int sellerId, double startPrice,
                           double minStep, String endTime) {
    String sql =
            "INSERT INTO auction (item_id, seller_id, start_price, current_price, min_step, end_time) "
                    +
                    "VALUES (?, ?, ?, ?, ?, ?)";
    try (Connection conn = DBConnection.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
      ps.setInt(1, itemId);
      ps.setInt(2, sellerId);
      ps.setDouble(3, startPrice);
      ps.setDouble(4, startPrice); // current = start lúc đầu
      ps.setDouble(5, minStep);
      ps.setString(6, endTime);
      ps.executeUpdate();
      try (ResultSet rs = ps.getGeneratedKeys()) {
        if (rs.next()) {
          return rs.getInt(1);
        }
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return -1;
  }

  /**
   * Lấy các phiên đấu giá đang ACTIVE kèm thông tin item
   */
  public JsonArray getActiveAuctions(int limit) {
    String sql = "SELECT a.*, i.name, i.description, i.category, i.image_path, " +
            "u.username as seller_name FROM auction a " +
            "JOIN item i ON a.item_id = i.item_id " +
            "JOIN user u ON a.seller_id = u.user_id " +
            "WHERE a.status = 'ACTIVE' AND a.end_time > NOW() " +
            "ORDER BY a.end_time ASC LIMIT ?";
    JsonArray result = new JsonArray();
    try (Connection conn = DBConnection.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setInt(1, limit);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          result.add(mapRowToJson(rs));
        }
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return result;
  }

  /**
   * Lấy chi tiết 1 phiên đấu giá
   */
  public JsonObject getAuctionById(int auctionId) {
    String sql = "SELECT a.*, i.name, i.description, i.category, i.image_path, " +
            "u.username as seller_name FROM auction a " +
            "JOIN item i ON a.item_id = i.item_id " +
            "JOIN user u ON a.seller_id = u.user_id " +
            "WHERE a.auction_id = ?";
    try (Connection conn = DBConnection.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setInt(1, auctionId);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return mapRowToJson(rs);
        }
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return null;
  }

  /**
   * Đặt giá — cập nhật current_price nếu hợp lệ
   */
  public boolean placeBid(int auctionId, int bidderId, double amount) {
    String checkSql = "SELECT current_price, min_step, end_time, status, seller_id FROM auction WHERE auction_id = ? FOR UPDATE";
    String updateSql = "UPDATE auction SET current_price = ?, winner_id = ? WHERE auction_id = ?";
    String insertBid = "INSERT INTO bid (auction_id, bidder_id, amount) VALUES (?, ?, ?)";

    try (Connection conn = DBConnection.getConnection()) {
      conn.setAutoCommit(false);
      try {
        // Kiểm tra điều kiện
        double curPrice, minStep;
        String endTime, status;
        int sellerId;
        try (PreparedStatement ps = conn.prepareStatement(checkSql)) {
          ps.setInt(1, auctionId);
          try (ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) {
              return false;
            }
            curPrice = rs.getDouble("current_price");
            minStep = rs.getDouble("min_step");
            endTime = rs.getString("end_time");
            status = rs.getString("status");
            sellerId = rs.getInt("seller_id");
          }
        }

        if (!"ACTIVE".equals(status)) { return false; }
        if (bidderId == sellerId) { return false; }
        LocalDateTime endDateTime = LocalDateTime.parse(
                endTime,
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        );
        if (LocalDateTime.now().isAfter(endDateTime)) { return false; }
        if (amount < curPrice + minStep) {
          return false;
        }

        // Cập nhật giá
        try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
          ps.setDouble(1, amount);
          ps.setInt(2, bidderId);
          ps.setInt(3, auctionId);
          ps.executeUpdate();
        }

        // Lưu lịch sử bid
        try (PreparedStatement ps = conn.prepareStatement(insertBid)) {
          ps.setInt(1, auctionId);
          ps.setInt(2, bidderId);
          ps.setDouble(3, amount);
          ps.executeUpdate();
        }

        conn.commit();
        return true;
      } catch (Exception e) {
        conn.rollback();
        e.printStackTrace();
        return false;
      }
    } catch (SQLException e) {
      e.printStackTrace();
      return false;
    }
  }

  /**
   * Phiên bản nâng cao — ném custom exception thay vì trả boolean.
   * Được gọi từ AuctionManager (có ReentrantLock bảo vệ bên ngoài).
   *
   * @throws com.nhom15.exception.AuctionClosedException nếu phiên không còn ACTIVE
   * @throws com.nhom15.exception.InvalidBidException    nếu amount < currentPrice + minStep
   */
  public void placeBidOrThrow(int auctionId, int bidderId, double amount)
          throws com.nhom15.exception.AuctionClosedException,
          com.nhom15.exception.InvalidBidException {

    String checkSql =
            "SELECT current_price, min_step, status FROM auction WHERE auction_id = ? FOR UPDATE";
    String updateSql =
            "UPDATE auction SET current_price = ?, winner_id = ? WHERE auction_id = ?";
    String insertBid =
            "INSERT INTO bid (auction_id, bidder_id, amount) VALUES (?, ?, ?)";

    try (Connection conn = DBConnection.getConnection()) {
      conn.setAutoCommit(false);
      try {
        double curPrice;
        double minStep;
        String status;

        // FOR UPDATE — khoá dòng DB, ngăn concurrent read-modify-write
        try (PreparedStatement ps = conn.prepareStatement(checkSql)) {
          ps.setInt(1, auctionId);
          try (ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) {
              throw new com.nhom15.exception.AuctionClosedException(auctionId, "NOT_FOUND");
            }
            curPrice = rs.getDouble("current_price");
            minStep = rs.getDouble("min_step");
            status = rs.getString("status");
          }
        }

        // Kiểm tra trạng thái phiên
        if (!"ACTIVE".equals(status)) {
          conn.rollback();
          throw new com.nhom15.exception.AuctionClosedException(auctionId, status);
        }

        // Kiểm tra giá hợp lệ
        double minRequired = curPrice + minStep;
        if (amount < minRequired) {
          conn.rollback();
          throw new com.nhom15.exception.InvalidBidException(curPrice, minRequired);
        }

        // Cập nhật giá cao nhất
        try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
          ps.setDouble(1, amount);
          ps.setInt(2, bidderId);
          ps.setInt(3, auctionId);
          ps.executeUpdate();
        }

        // Ghi lịch sử bid
        try (PreparedStatement ps = conn.prepareStatement(insertBid)) {
          ps.setInt(1, auctionId);
          ps.setInt(2, bidderId);
          ps.setDouble(3, amount);
          ps.executeUpdate();
        }

        conn.commit();

      } catch (com.nhom15.exception.AuctionClosedException
               | com.nhom15.exception.InvalidBidException e) {
        // Re-throw custom exceptions sau khi rollback
        try { conn.rollback(); } catch (SQLException ignored) { }
        throw e;
      } catch (Exception e) {
        try { conn.rollback(); } catch (SQLException ignored) { }
        throw new RuntimeException("Lỗi DB khi đặt giá: " + e.getMessage(), e);
      }
    } catch (SQLException e) {
      throw new RuntimeException("Không lấy được connection: " + e.getMessage(), e);
    }
  }

  /**
   * Lịch sử đặt giá của 1 phiên
   */
  public JsonArray getBidHistory(int auctionId) {
    String sql = "SELECT b.*, u.username FROM bid b " +
            "JOIN user u ON b.bidder_id = u.user_id " +
            "WHERE b.auction_id = ? ORDER BY b.bid_time DESC LIMIT 20";
    JsonArray result = new JsonArray();
    try (Connection conn = DBConnection.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setInt(1, auctionId);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          JsonObject obj = new JsonObject();
          obj.addProperty("bidId", rs.getInt("bid_id"));
          obj.addProperty("username", rs.getString("username"));
          obj.addProperty("amount", rs.getDouble("amount"));
          obj.addProperty("bidTime", rs.getString("bid_time"));
          result.add(obj);
        }
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return result;
  }

  /**
   * Lấy auction theo seller
   */
  public JsonArray getAuctionsBySeller(int sellerId) {
    String sql = "SELECT a.*, i.name, i.description, i.category, i.image_path, " +
            "u.username as seller_name FROM auction a " +
            "JOIN item i ON a.item_id = i.item_id " +
            "JOIN user u ON a.seller_id = u.user_id " +
            "WHERE a.seller_id = ? ORDER BY a.start_time DESC";
    JsonArray result = new JsonArray();
    try (Connection conn = DBConnection.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setInt(1, sellerId);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          result.add(mapRowToJson(rs));
        }
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return result;
  }

  /**
   * Kết thúc phiên đấu giá
   */
  public boolean endAuction(int auctionId) {
    String sql = "UPDATE auction SET status = 'ENDED' WHERE auction_id = ?";
    try (Connection conn = DBConnection.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setInt(1, auctionId);
      return ps.executeUpdate() > 0;
    } catch (SQLException e) {
      e.printStackTrace();
      return false;
    }
  }

  // ── Helper ───────────────────────────────────────────────────────────────

  private JsonObject mapRowToJson(ResultSet rs) throws SQLException {
    JsonObject obj = new JsonObject();
    obj.addProperty("auctionId", rs.getInt("auction_id"));
    obj.addProperty("itemId", rs.getInt("item_id"));
    obj.addProperty("sellerId", rs.getInt("seller_id"));
    obj.addProperty("sellerName", rs.getString("seller_name"));
    obj.addProperty("name", rs.getString("name"));
    obj.addProperty("description",
            rs.getString("description") != null ? rs.getString("description") : "");
    obj.addProperty("category", rs.getString("category") != null ? rs.getString("category") : "");
    obj.addProperty("imagePath",
            rs.getString("image_path") != null ? rs.getString("image_path") : "");
    obj.addProperty("startPrice", rs.getDouble("start_price"));
    obj.addProperty("currentPrice", rs.getDouble("current_price"));
    obj.addProperty("minStep", rs.getDouble("min_step"));
    obj.addProperty("endTime", rs.getString("end_time"));
    obj.addProperty("status", rs.getString("status"));
    obj.addProperty("winnerId", rs.getInt("winner_id")); // lấy winnerId từ DB đưa vào JSON
    return obj;
  }
}