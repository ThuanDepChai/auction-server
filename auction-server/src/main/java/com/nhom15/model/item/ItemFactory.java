package com.nhom15.model.item;

import java.util.Map;

/**
 * Factory Method cho các loại sản phẩm.
 *
 * <p><b>FIX — id phải là {@code int} thay {@code String}:</b><br>
 * Code cũ nhận {@code String id} nhưng DB lưu {@code item_id INT}. Khi DAO lấy
 * {@code rs.getInt("item_id")} rồi ép sang String để truyền vào factory, mất đi
 * tính nhất quán và dễ gây lỗi parse. Fix: thay tham số thành {@code int id} để
 * ánh xạ trực tiếp từ ResultSet.
 *
 * <p>Subclass types được hỗ trợ: ART, ELECTRONICS, FASHION, VEHICLE, SPORTS.
 */
public class ItemFactory {

  /**
   * Tạo đối tượng Item con phù hợp với {@code type}.
   *
   * @param type       loại sản phẩm (không phân biệt hoa/thường): ART, ELECTRONICS,
   *                   FASHION, VEHICLE, SPORTS
   * @param id         khoá chính từ DB (item_id) — FIX: đổi từ String sang int
   * @param name       tên sản phẩm
   * @param price      giá khởi điểm
   * @param attributes các thuộc tính đặc trưng theo loại (xem tài liệu từng subclass)
   * @return Item con tương ứng, hoặc {@code null} nếu type không hỗ trợ
   */
  public static Item createItem(String type, int id, String name, double price,
      Map<String, Object> attributes) {
    if (type == null || type.isBlank()) return null;

    return switch (type.toUpperCase()) {

      case "ART" -> {
        String artist = (String) attributes.getOrDefault("artist", "Unknown");
        yield new Art(id, name, price, artist);
      }

      case "ELECTRONICS" -> {
        String brand = (String) attributes.getOrDefault("brand", "Unknown");
        yield new Electronics(id, name, price, brand);
      }

      case "FASHION" -> {
        String size  = (String) attributes.getOrDefault("size",  "");
        String color = (String) attributes.getOrDefault("color", "");
        String brand = (String) attributes.getOrDefault("brand", "");
        yield new Fashion(id, name, price, size, color, brand);
      }

      case "VEHICLE" -> {
        // FIX: ép kiểu an toàn — tránh ClassCastException nếu caller truyền sai kiểu
        int    year    = toInt(attributes.getOrDefault("year",    0));
        double mileage = toDouble(attributes.getOrDefault("mileage", 0.0));
        yield new Vehicle(id, name, price, year, mileage);
      }

      default -> {
        System.err.println("[ItemFactory] Loại sản phẩm không hỗ trợ: " + type);
        yield null;
      }
    };
  }

  // ── Helpers ép kiểu an toàn ───────────────────────────────────────────────

  private static int toInt(Object o) {
    if (o instanceof Number n) return n.intValue();
    try { return Integer.parseInt(String.valueOf(o)); } catch (Exception e) { return 0; }
  }

  private static double toDouble(Object o) {
    if (o instanceof Number n) return n.doubleValue();
    try { return Double.parseDouble(String.valueOf(o)); } catch (Exception e) { return 0.0; }
  }
}
