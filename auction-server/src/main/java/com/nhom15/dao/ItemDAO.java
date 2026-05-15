package com.nhom15.dao;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.nhom15.util.DBConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class ItemDAO {

  /**
   * Thêm sản phẩm mới
   */
  public int insertItem(int sellerId, String name, String description,
                        String category, double startPrice, String imagePath, List<String> subImagePaths, String extraInfo) {
    String sqlItem =
            "INSERT INTO item (seller_id, name, description, category, start_price, image_path, extra_info) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?)";

    try (Connection conn = DBConnection.getConnection()) {
      conn.setAutoCommit(false);

      try (PreparedStatement psItem = conn.prepareStatement(sqlItem, Statement.RETURN_GENERATED_KEYS)) {
        psItem.setInt(1, sellerId);
        psItem.setString(2, name);
        psItem.setString(3, description);
        psItem.setString(4, category);
        psItem.setDouble(5, startPrice);
        psItem.setString(6, imagePath);
        psItem.setString(7, extraInfo);
        psItem.executeUpdate();

        int newItemId = -1;
        try (ResultSet rs = psItem.getGeneratedKeys()) {
          if (rs.next()) {
            newItemId = rs.getInt(1);
          }
        }

        if (newItemId != -1 && subImagePaths != null && !subImagePaths.isEmpty()) {
          String sqlImages = "INSERT INTO item_images (item_id, path) VALUES (?, ?)";
          try (PreparedStatement psImages = conn.prepareStatement(sqlImages)) {
            for (String path : subImagePaths) {
              psImages.setInt(1, newItemId);
              psImages.setString(2, path);
              psImages.addBatch();
            }
            psImages.executeBatch();
          }
        }

        conn.commit();
        return newItemId;
      } catch (SQLException e) {
        conn.rollback();
        e.printStackTrace();
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return -1;
  }

  /**
   * Lấy danh sách sản phẩm nổi bật (mới nhất, status AVAILABLE)
   */
  public JsonArray getFeaturedItems(int limit) {
    String sql = "SELECT i.*, u.username as seller_name FROM item i " +
            "JOIN user u ON i.seller_id = u.user_id " +
            "WHERE i.status = 'AVAILABLE' " +
            "ORDER BY i.created_at DESC LIMIT ?";
    return queryToJsonArray(sql, limit);
  }

  /**
   * Tìm kiếm sản phẩm theo từ khóa và danh mục
   */
  public JsonArray searchItems(String keyword, String category) {
    StringBuilder sql = new StringBuilder(
            "SELECT i.*, u.username as seller_name FROM item i " +
                    "JOIN user u ON i.seller_id = u.user_id WHERE i.status = 'AVAILABLE'"
    );
    List<Object> params = new ArrayList<>();

    if (keyword != null && !keyword.isEmpty()) {
      sql.append(" AND (i.name LIKE ? OR i.description LIKE ?)");
      params.add("%" + keyword + "%");
      params.add("%" + keyword + "%");
    }
    if (category != null && !category.isEmpty() && !"Tất cả danh mục".equals(category)) {
      sql.append(" AND i.category = ?");
      params.add(category);
    }
    sql.append(" ORDER BY i.created_at DESC LIMIT 50");

    JsonArray result = new JsonArray();
    try (Connection conn = DBConnection.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql.toString())) {
      for (int i = 0; i < params.size(); i++) {
        ps.setObject(i + 1, params.get(i));
      }
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
   * Lấy sản phẩm theo seller
   */
  public JsonArray getItemsBySeller(int sellerId) {
    String sql = "SELECT i.*, u.username as seller_name FROM item i " +
            "JOIN user u ON i.seller_id = u.user_id " +
            "WHERE i.seller_id = ? ORDER BY i.created_at DESC";
    return queryToJsonArray(sql, sellerId);
  }

  /**
   * Cập nhật image path
   */
  public boolean updateImagePath(int itemId, String imagePath) {
    String sql = "UPDATE item SET image_path = ? WHERE item_id = ?";
    try (Connection conn = DBConnection.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, imagePath);
      ps.setInt(2, itemId);
      return ps.executeUpdate() > 0;
    } catch (SQLException e) {
      e.printStackTrace();
      return false;
    }
  }

  /**
   * Cập nhật status
   */
  public boolean updateStatus(int itemId, String status) {
    String sql = "UPDATE item SET status = ? WHERE item_id = ?";
    try (Connection conn = DBConnection.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, status);
      ps.setInt(2, itemId);
      return ps.executeUpdate() > 0;
    } catch (SQLException e) {
      e.printStackTrace();
      return false;
    }
  }

  /**
   * Xóa sản phẩm
   */
  public boolean deleteItem(int itemId) {
    String sql = "DELETE FROM item WHERE item_id = ?";
    try (Connection conn = DBConnection.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setInt(1, itemId);
      return ps.executeUpdate() > 0;
    } catch (SQLException e) {
      e.printStackTrace();
      return false;
    }
  }

  // ── Helper ───────────────────────────────────────────────────────────────

  private JsonArray queryToJsonArray(String sql, Object param) {
    JsonArray result = new JsonArray();
    try (Connection conn = DBConnection.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setObject(1, param);
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

  private JsonObject mapRowToJson(ResultSet rs) throws SQLException {
    JsonObject obj = new JsonObject();
    obj.addProperty("itemId", rs.getInt("item_id"));
    obj.addProperty("sellerId", rs.getInt("seller_id"));
    obj.addProperty("sellerName", rs.getString("seller_name"));
    obj.addProperty("name", rs.getString("name"));
    obj.addProperty("description",
            rs.getString("description") != null ? rs.getString("description") : "");
    obj.addProperty("category", rs.getString("category") != null ? rs.getString("category") : "");
    obj.addProperty("startPrice", rs.getDouble("start_price"));
    obj.addProperty("imagePath",
            rs.getString("image_path") != null ? rs.getString("image_path") : "");
    obj.addProperty("extraInfo", rs.getString("extra_info") != null ? rs.getString("extra_info") : "");
    obj.addProperty("status", rs.getString("status"));
    obj.addProperty("createdAt", rs.getString("created_at"));
    return obj;
  }
}