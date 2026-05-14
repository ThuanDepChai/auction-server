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
                               List<String> imageBase64List, String extension) {
    JsonObject result = new JsonObject();

    String mainImagePath = "";
    List<String> subImagePaths = new ArrayList<>();

    if (imageBase64List != null && !imageBase64List.isEmpty()) {
      try {
        String baseDir = System.getProperty("user.dir");
        File imgDir = new File(baseDir, "item_images");
        imgDir.mkdirs();

        for (int i = 0; i < imageBase64List.size(); i++) {
          byte[] bytes = Base64.getDecoder().decode(imageBase64List.get(i));
          String fileName = "item_" + System.currentTimeMillis() + "_" + i + "." + extension;
          File imgFile = new File(imgDir, fileName);
          Files.write(imgFile.toPath(), bytes);

          String savedPath = "item_images/" + fileName;

          if (i == 0) {
            mainImagePath = savedPath; // Ảnh đầu tiên làm ảnh đại diện
          } else {
            subImagePaths.add(savedPath); // Các ảnh còn lại lưu vào danh sách phụ
          }
        }
      } catch (Exception e) {
        System.err.println("Lỗi lưu ảnh sản phẩm: " + e.getMessage());
      }
    }

    // Gọi hàm DAO mà ông vừa sửa ở bước trước
    int itemId = itemDAO.insertItem(sellerId, name, description, category, startPrice, mainImagePath, subImagePaths);

    if (itemId > 0) {
      result.addProperty("status", "SUCCESS");
      result.addProperty("itemId", itemId);
      result.addProperty("message", "Đăng sản phẩm thành công với " + imageBase64List.size() + " ảnh!");
    } else {
      result.addProperty("status", "FAIL");
      result.addProperty("message", "Đăng sản phẩm thất bại!");
    }
    return result;
  }

  // ... Các hàm bên dưới giữ nguyên
}