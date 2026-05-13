package com.nhom15.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.nhom15.dao.ItemDAO;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;

public class ItemService {

  private final ItemDAO itemDAO = new ItemDAO();

  /**
   * Đăng sản phẩm mới, lưu ảnh nếu có.
   * Client gửi Base64 khi tạo (upload 1 lần) — server lưu ra file, trả về imagePath.
   * Từ đó về sau client dùng GET_ITEM_IMAGE để lấy ảnh theo yêu cầu (lazy load).
   */
  public JsonObject createItem(int sellerId, String name, String description,
                               String category, double startPrice,
                               String imageBase64, String extension) {
    JsonObject result = new JsonObject();

    // Lưu ảnh ra file — chỉ xảy ra lúc tạo item, không lặp lại mỗi request
    String imagePath = "";
    if (imageBase64 != null && !imageBase64.isEmpty()) {
      try {
        // Dùng đường dẫn tuyệt đối để tránh lỗi khi working directory thay đổi
        String baseDir = System.getProperty("user.dir");
        File imgDir = new File(baseDir, "item_images");
        imgDir.mkdirs();
        byte[] bytes = Base64.getDecoder().decode(imageBase64);
        String fileName = "item_" + System.currentTimeMillis() + "." + extension;
        File imgFile = new File(imgDir, fileName);
        Files.write(imgFile.toPath(), bytes);
        imagePath = imgFile.getAbsolutePath();
      } catch (Exception e) {
        System.err.println("Lỗi lưu ảnh sản phẩm: " + e.getMessage());
      }
    }

    int itemId = itemDAO.insertItem(sellerId, name, description, category, startPrice, imagePath);
    if (itemId > 0) {
      result.addProperty("status", "SUCCESS");
      result.addProperty("itemId", itemId);
      result.addProperty("message", "Đăng sản phẩm thành công!");
    } else {
      result.addProperty("status", "FAIL");
      result.addProperty("message", "Đăng sản phẩm thất bại!");
    }
    return result;
  }

  /**
   * Lấy sản phẩm nổi bật — chỉ trả metadata + imagePath, không nhúng Base64.
   * Client dùng GET_ITEM_IMAGE để lazy-load ảnh từng card khi cần.
   */
  public JsonArray getFeaturedItems() {
    return itemDAO.getFeaturedItems(20);
  }

  /**
   * Tìm kiếm sản phẩm — chỉ trả metadata + imagePath.
   */
  public JsonArray searchItems(String keyword, String category) {
    return itemDAO.searchItems(keyword, category);
  }

  /**
   * Lấy sản phẩm của seller — chỉ trả metadata + imagePath.
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