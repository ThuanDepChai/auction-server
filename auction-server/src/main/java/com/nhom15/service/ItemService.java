package com.nhom15.service;

import com.nhom15.dao.ItemDAO;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;

public class ItemService {

    private final ItemDAO itemDAO = new ItemDAO();

    /** Đăng sản phẩm mới, lưu ảnh nếu có */
    public JsonObject createItem(int sellerId, String name, String description,
                                 String category, double startPrice,
                                 String imageBase64, String extension) {
        JsonObject result = new JsonObject();

        // Lưu ảnh trước
        String imagePath = "";
        if (imageBase64 != null && !imageBase64.isEmpty()) {
            try {
                new File("item_images").mkdirs();
                byte[] bytes = Base64.getDecoder().decode(imageBase64);
                imagePath = "item_images/item_" + System.currentTimeMillis() + "." + extension;
                Files.write(Paths.get(imagePath), bytes);
            } catch (Exception e) {
                System.err.println("Lỗi lưu ảnh sản phẩm: " + e.getMessage());
            }
        }

        int itemId = itemDAO.insertItem(sellerId, name, description, category, startPrice, imagePath);
        if (itemId > 0) {
            result.addProperty("status",  "SUCCESS");
            result.addProperty("itemId",  itemId);
            result.addProperty("message", "Đăng sản phẩm thành công!");
        } else {
            result.addProperty("status",  "FAIL");
            result.addProperty("message", "Đăng sản phẩm thất bại!");
        }
        return result;
    }

    /** Lấy sản phẩm nổi bật kèm Base64 ảnh */
    public JsonArray getFeaturedItems() {
        JsonArray items = itemDAO.getFeaturedItems(20);
        return attachImageBase64(items);
    }

    /** Tìm kiếm sản phẩm kèm Base64 ảnh */
    public JsonArray searchItems(String keyword, String category) {
        JsonArray items = itemDAO.searchItems(keyword, category);
        return attachImageBase64(items);
    }

    /** Lấy sản phẩm của seller kèm Base64 ảnh */
    public JsonArray getItemsBySeller(int sellerId) {
        JsonArray items = itemDAO.getItemsBySeller(sellerId);
        return attachImageBase64(items);
    }

    public boolean deleteItem(int itemId)                    { return itemDAO.deleteItem(itemId); }
    public boolean updateStatus(int itemId, String status)   { return itemDAO.updateStatus(itemId, status); }

    /** Đính kèm imageBase64 vào mỗi item */
    private JsonArray attachImageBase64(JsonArray items) {
        for (int i = 0; i < items.size(); i++) {
            JsonObject item = items.get(i).getAsJsonObject();
            String path = item.get("imagePath").getAsString();
            if (path != null && !path.isEmpty()) {
                try {
                    File f = new File(path);
                    if (f.exists()) {
                        byte[] bytes = Files.readAllBytes(f.toPath());
                        item.addProperty("imageBase64", Base64.getEncoder().encodeToString(bytes));
                    }
                } catch (Exception e) {
                    item.addProperty("imageBase64", "");
                }
            } else {
                item.addProperty("imageBase64", "");
            }
        }
        return items;
    }
}