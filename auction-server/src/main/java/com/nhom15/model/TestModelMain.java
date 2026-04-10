package com.nhom15.model;

import com.nhom15.model.item.Item;
import com.nhom15.model.item.ItemFactory;
import com.nhom15.model.item.SportCategory; // THÊM DÒNG NÀY ĐỂ HẾT LỖI

import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

public class TestModelMain {
    public static void main(String[] args) {
        System.out.println("========== BẮT ĐẦU KIỂM TRA TOÀN BỘ ITEM FACTORY ==========\n");

        // Danh sách để chứa các món đồ sau khi tạo
        List<Item> inventory = new ArrayList<>();

        // 1. TEST ART
        Map<String, Object> artAttrs = new HashMap<>();
        artAttrs.put("artist", "Leonardo da Vinci");
        inventory.add(ItemFactory.createItem("ART", "A01", "Mona Lisa", 1000000, artAttrs));

        // 2. TEST ELECTRONICS
        Map<String, Object> elecAttrs = new HashMap<>();
        elecAttrs.put("brand", "Samsung");
        inventory.add(ItemFactory.createItem("ELECTRONICS", "E01", "Galaxy S24", 1200, elecAttrs));

        // 3. TEST VEHICLE
        Map<String, Object> vehicleAttrs = new HashMap<>();
        vehicleAttrs.put("year", 2022);
        vehicleAttrs.put("mileage", 5000.5);
        inventory.add(ItemFactory.createItem("VEHICLE", "V01", "Honda Civic", 25000, vehicleAttrs));

        // 4. TEST FASHION
        Map<String, Object> fashionAttrs = new HashMap<>();
        fashionAttrs.put("size", "L");
        fashionAttrs.put("color", "Black");
        fashionAttrs.put("brand", "Nike");
        inventory.add(ItemFactory.createItem("FASHION", "F01", "Áo Hoodie", 50, fashionAttrs));

        // 5. TEST SPORTS
        Map<String, Object> sportAttrs = new HashMap<>();
        sportAttrs.put("sportType", SportCategory.CYCLING); // Đã có import nên sẽ không lỗi
        sportAttrs.put("condition", "Mới 100%");
        inventory.add(ItemFactory.createItem("SPORTS", "S01", "Xe đạp đua", 1500, sportAttrs));

        // 6. TEST TRƯỜNG HỢP SAI (Type không tồn tại)
        Item unknown = ItemFactory.createItem("GIA_DUNG", "X01", "Nồi cơm điện", 20, new HashMap<>());
        if (unknown == null) {
            System.out.println("[OK] Đã xử lý đúng: Trả về null khi type không hợp lệ.\n");
        }

        // DUYỆT QUA DANH SÁCH VÀ HIỂN THỊ
        System.out.println("--- CHI TIẾT CÁC SẢN PHẨM TRONG HỆ THỐNG ---");
        for (Item item : inventory) {
            if (item != null) {
                item.displayItemInfo();
            } else {
                System.out.println("[LỖI] Có một sản phẩm bị null trong danh sách!");
            }
        }

        System.out.println("\n========== KIỂM TRA HOÀN TẤT ==========");
    }
}