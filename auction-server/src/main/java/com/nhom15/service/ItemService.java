package com.nhom15.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.nhom15.dao.ItemDAO;
import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class ItemService {

  private final ItemDAO itemDAO = new ItemDAO();

  /**
   * Đăng sản phẩm mới kèm danh sách nhiều ảnh.
   * imageBase64List: Danh sách các chuỗi ảnh Base64 gửi từ Client.
   */
  public JsonObject createItem(int sellerId, String name, String description,
                               String category, double startPrice,
                               List<String> imageBase64List, String extension, String extraInfo) {
    JsonObject result = new JsonObject();

    String mainImagePath = "";
    List<String> subImagePaths = new ArrayList<>();

    if (imageBase64List != null && !imageBase64List.isEmpty()) {
      try {
        String baseDir = System.getProperty("user.dir");
        File imgDir = new File(baseDir, "item_images");
        if (!imgDir.exists()) {
          imgDir.mkdirs();
        }

        for (int i = 0; i < imageBase64List.size(); i++) {
          byte[] bytes = Base64.getDecoder().decode(imageBase64List.get(i));
          // Thêm index i vào tên file để tránh trùng lặp khi lưu nhanh
          String fileName = "item_" + System.currentTimeMillis() + "_" + i + "." + extension;
          File imgFile = new File(imgDir, fileName);
          Files.write(imgFile.toPath(), bytes);

          String savedPath = "item_images/" + fileName;

          if (i == 0) {
            mainImagePath = savedPath; // Ảnh đầu tiên làm ảnh đại diện chính
          } else {
            subImagePaths.add(savedPath); // Các ảnh còn lại là ảnh phụ
          }
        }
      } catch (Exception e) {
        System.err.println("Lỗi lưu ảnh sản phẩm: " + e.getMessage());
      }
    }

    // Gọi hàm DAO đã nâng cấp để lưu vào bảng item
    int itemId = itemDAO.insertItem(sellerId, name, description, category, startPrice, mainImagePath, subImagePaths, extraInfo);

    if (itemId > 0) {
      result.addProperty("status", "SUCCESS");
      result.addProperty("itemId", itemId);
      result.addProperty("message", "Đăng sản phẩm thành công với " + (imageBase64List != null ? imageBase64List.size() : 0) + " ảnh!");
    } else {
      result.addProperty("status", "FAIL");
      result.addProperty("message", "Đăng sản phẩm thất bại!");
    }
    return result;
  }

  /**
   * Lấy sản phẩm nổi bật
   */
  public JsonArray getFeaturedItems() {
    return itemDAO.getFeaturedItems(20);
  }

  /**
   * Tìm kiếm sản phẩm
   */
  public JsonArray searchItems(String keyword, String category) {
    return itemDAO.searchItems(keyword, category);
  }

  /**
   * Lấy sản phẩm của seller
   */
  public JsonArray getItemsBySeller(int sellerId) {
    return itemDAO.getItemsBySeller(sellerId);
  }

  public boolean deleteItem(int itemId) {
    return itemDAO.deleteItem(itemId);
  }

  public boolean updateStatus(int itemId, String status) {
    return itemDAO.updateStatus(itemId, status);
  }
}