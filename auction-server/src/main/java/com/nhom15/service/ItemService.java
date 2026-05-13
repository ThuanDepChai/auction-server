package com.nhom15.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.nhom15.dao.ItemDAO;
import java.io.File;
import java.nio.file.Files;
import java.util.Base64;

public class ItemService {

  private final ItemDAO itemDAO = new ItemDAO();

  /**
   * Đăng sản phẩm mới, lưu ảnh nếu có.
   *
   * <p>Ảnh được lưu vào thư mục item_images/ với đường dẫn TƯƠNG ĐỐI (relative path).
   * Lý do: đường dẫn tuyệt đối (absolute path) bị gắn cứng vào máy chủ hiện tại,
   * khi server khởi động lại từ thư mục khác hoặc chạy trên máy khác thì ảnh sẽ
   * không tìm thấy được. Relative path luôn được resolve từ working directory của server.
   */
  public JsonObject createItem(int sellerId, String name, String description,
                               String category, double startPrice,
                               String imageBase64, String extension) {
    JsonObject result = new JsonObject();

    String imagePath = "";
    if (imageBase64 != null && !imageBase64.isEmpty()) {
      try {
        String baseDir = System.getProperty("user.dir");
        File imgDir = new File(baseDir, "item_images");
        imgDir.mkdirs();

        byte[] bytes = Base64.getDecoder().decode(imageBase64);
        String fileName = "item_" + System.currentTimeMillis() + "." + extension;
        File imgFile = new File(imgDir, fileName);
        Files.write(imgFile.toPath(), bytes);

        // FIX: Lưu đường dẫn TƯƠNG ĐỐI thay vì tuyệt đối
        // Ví dụ: "item_images/item_1715000000000.jpg"
        imagePath = "item_images/" + fileName;

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