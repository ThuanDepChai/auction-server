package com.nhom15.client.util;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * ImageCache — LRU cache lưu Base64 ảnh đã tải.
 *
 * <p><b>Vấn đề trước đây:</b> Mỗi lần render card (product / auction) đều gọi
 * GET_ITEM_IMAGE → tạo TCP connection mới → connect handshake ~200ms → download Base64.
 * Khi scroll hoặc navigate về trang chủ, toàn bộ ảnh bị request lại từ đầu.
 *
 * <p><b>Fix:</b> LRU cache giữ tối đa MAX_ENTRIES ảnh trong RAM.
 * Lần đầu tiên tải: gọi server bình thường, lưu kết quả vào cache.
 * Lần sau cùng imagePath: trả về Base64 từ cache ngay lập tức, không cần TCP.
 *
 * <p>Thread-safe: {@code synchronized} trên map — ảnh thường load trên background
 * thread của executeAsync, nhiều card có thể query cache đồng thời.
 */
public final class ImageCache {

    /** Số ảnh tối đa giữ trong RAM (~50 ảnh × ~200KB Base64 ≈ 10MB). */
    private static final int MAX_ENTRIES = 50;

    private static final Map<String, String> cache = new LinkedHashMap<>(MAX_ENTRIES, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
            return size() > MAX_ENTRIES;
        }
    };

    private ImageCache() {}

    /**
     * Lấy Base64 từ cache. Trả về {@code null} nếu chưa có.
     */
    public static synchronized String get(String imagePath) {
        return cache.get(imagePath);
    }

    /**
     * Lưu Base64 vào cache sau khi tải xong.
     *
     * @param imagePath key (đường dẫn ảnh)
     * @param base64    giá trị Base64 đã tải
     */
    public static synchronized void put(String imagePath, String base64) {
        if (imagePath != null && !imagePath.isBlank() && base64 != null && !base64.isBlank()) {
            cache.put(imagePath, base64);
        }
    }

    /** Số entry hiện tại trong cache (dùng để debug / metrics). */
    public static synchronized int size() {
        return cache.size();
    }

    /** Xóa toàn bộ cache (dùng khi logout / memory pressure). */
    public static synchronized void clear() {
        cache.clear();
    }
}
