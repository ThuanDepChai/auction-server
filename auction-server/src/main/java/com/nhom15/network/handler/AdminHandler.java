package com.nhom15.network.handler;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.nhom15.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * AdminHandler — xử lý các request từ Admin.
 * Gồm: lấy danh sách user, ban/unban user, lấy tất cả phiên đấu giá, hủy phiên đấu giá.
 */
public class AdminHandler {

  public JsonObject handle(JsonObject request) {
    String action = request.get("action").getAsString();
    return switch (action) {
      case "ADMIN_GET_ALL_USERS"    -> getAllUsers();
      case "ADMIN_BAN_USER"         -> banUser(request);
      case "ADMIN_GET_ALL_AUCTIONS" -> getAllAuctions();
      case "ADMIN_CANCEL_AUCTION"   -> cancelAuction(request);
      case "ADMIN_GET_STATS"        -> getStats();
      default -> error("Action Admin không được hỗ trợ: " + action);
    };
  }

  // ── 1. Lấy danh sách tất cả user ─────────────────────────────────────────
  private JsonObject getAllUsers() {
    String sql = "SELECT user_id, username, email, role, full_name, phone, balance, created_at FROM user ORDER BY created_at DESC";
    JsonArray arr = new JsonArray();
    try (Connection conn = DBConnection.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql);
        ResultSet rs = ps.executeQuery()) {
      while (rs.next()) {
        JsonObject u = new JsonObject();
        u.addProperty("user_id",    rs.getInt("user_id"));
        u.addProperty("username",   rs.getString("username"));
        u.addProperty("email",      rs.getString("email"));
        u.addProperty("role",       rs.getString("role"));
        u.addProperty("full_name",  rs.getString("full_name") != null ? rs.getString("full_name") : "");
        u.addProperty("phone",      rs.getString("phone") != null ? rs.getString("phone") : "");
        u.addProperty("balance",    rs.getDouble("balance"));
        u.addProperty("created_at", rs.getString("created_at") != null ? rs.getString("created_at") : "");
        arr.add(u);
      }
    } catch (SQLException e) {
      return error("Lỗi lấy danh sách user: " + e.getMessage());
    }
    JsonObject res = new JsonObject();
    res.addProperty("status", "OK");
    res.add("users", arr);
    return res;
  }

  // ── 2. Ban / Unban user (đổi role thành BANNED hoặc BIDDER) ──────────────
  private JsonObject banUser(JsonObject request) {
    if (!request.has("user_id") || !request.has("action_type")) {
      return error("Thiếu user_id hoặc action_type");
    }
    int userId = request.get("user_id").getAsInt();
    String actionType = request.get("action_type").getAsString(); // "BAN" hoặc "UNBAN"
    String newRole = actionType.equals("BAN") ? "BANNED" : "BIDDER";

    String sql = "UPDATE user SET role = ? WHERE user_id = ?";
    try (Connection conn = DBConnection.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, newRole);
      ps.setInt(2, userId);
      boolean ok = ps.executeUpdate() > 0;
      JsonObject res = new JsonObject();
      res.addProperty("status", ok ? "OK" : "ERROR");
      res.addProperty("message", ok ? "Cập nhật thành công" : "Không tìm thấy user");
      return res;
    } catch (SQLException e) {
      return error("Lỗi ban user: " + e.getMessage());
    }
  }

  // ── 3. Lấy tất cả phiên đấu giá ──────────────────────────────────────────
  private JsonObject getAllAuctions() {
    String sql = """
                SELECT a.auction_id, a.start_price, a.current_price, a.status, a.end_time,
                       i.name AS item_name, u.username AS seller_name
                FROM auction a
                JOIN item i ON a.item_id = i.item_id
                JOIN user u ON a.seller_id = u.user_id
                ORDER BY a.auction_id DESC
                """;
    JsonArray arr = new JsonArray();
    try (Connection conn = DBConnection.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql);
        ResultSet rs = ps.executeQuery()) {
      while (rs.next()) {
        JsonObject a = new JsonObject();
        a.addProperty("auction_id",     rs.getInt("auction_id"));
        a.addProperty("item_name",      rs.getString("item_name"));
        a.addProperty("seller_name",    rs.getString("seller_name"));
        a.addProperty("start_price",    rs.getDouble("start_price"));
        a.addProperty("current_price",  rs.getDouble("current_price"));
        a.addProperty("status",         rs.getString("status"));
        a.addProperty("end_time",       rs.getString("end_time") != null ? rs.getString("end_time") : "");
        arr.add(a);
      }
    } catch (SQLException e) {
      return error("Lỗi lấy danh sách phiên đấu giá: " + e.getMessage());
    }
    JsonObject res = new JsonObject();
    res.addProperty("status", "OK");
    res.add("auctions", arr);
    return res;
  }

  // ── 4. Hủy phiên đấu giá ─────────────────────────────────────────────────
  private JsonObject cancelAuction(JsonObject request) {
    if (!request.has("auction_id")) return error("Thiếu auction_id");
    int auctionId = request.get("auction_id").getAsInt();
    String sql = "UPDATE auction SET status = 'CANCELLED' WHERE auction_id = ?";
    try (Connection conn = DBConnection.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setInt(1, auctionId);
      boolean ok = ps.executeUpdate() > 0;
      JsonObject res = new JsonObject();
      res.addProperty("status", ok ? "OK" : "ERROR");
      res.addProperty("message", ok ? "Đã hủy phiên đấu giá" : "Không tìm thấy phiên đấu giá");
      return res;
    } catch (SQLException e) {
      return error("Lỗi hủy phiên đấu giá: " + e.getMessage());
    }
  }

  // ── 5. Thống kê tổng quan ─────────────────────────────────────────────────
  private JsonObject getStats() {
    try (Connection conn = DBConnection.getConnection()) {
      int totalUsers = queryCount(conn, "SELECT COUNT(*) FROM user");
      int totalAuctions = queryCount(conn, "SELECT COUNT(*) FROM auction");
      int activeAuctions = queryCount(conn, "SELECT COUNT(*) FROM auction WHERE status = 'ACTIVE'");
      int totalItems = queryCount(conn, "SELECT COUNT(*) FROM item");

      JsonObject res = new JsonObject();
      res.addProperty("status", "OK");
      res.addProperty("total_users", totalUsers);
      res.addProperty("total_auctions", totalAuctions);
      res.addProperty("active_auctions", activeAuctions);
      res.addProperty("total_items", totalItems);
      return res;
    } catch (SQLException e) {
      return error("Lỗi lấy thống kê: " + e.getMessage());
    }
  }

  private int queryCount(Connection conn, String sql) throws SQLException {
    try (PreparedStatement ps = conn.prepareStatement(sql);
        ResultSet rs = ps.executeQuery()) {
      return rs.next() ? rs.getInt(1) : 0;
    }
  }

  private JsonObject error(String message) {
    JsonObject r = new JsonObject();
    r.addProperty("status", "ERROR");
    r.addProperty("message", message);
    return r;
  }
}
