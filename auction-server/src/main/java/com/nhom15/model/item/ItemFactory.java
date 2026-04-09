package com.nhom15.model.item;

import java.util.Map;

public class ItemFactory {
    public static Item createItem(String type, String id, String name, double price, Map<String, Object> attributes) {
        if ( type == null) return null ; // Nếu không có type s trả về null
        // XỬ LÝ TƯNG LOẠI SẢN PHẨM
        if ( type.equalsIgnoreCase("ART")){
            // Ép kiểu String cho thuộc tinnh của Art
            String artist = (String) attributes.get("artist");

            return new Art(id, name, price, artist);
        }
        else if (type.equalsIgnoreCase("VEHICLE")) {
            //Ép kiểu ( casting ) dữ liệu lấy từ Map:
            int year = (Integer) attributes.get("year");
            double mileage = (Double) attributes.get("mileage");

            return new Vehicle(id, name, price, year, mileage);
        }
    }

}

