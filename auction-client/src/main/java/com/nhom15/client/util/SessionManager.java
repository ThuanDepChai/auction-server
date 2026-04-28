package com.nhom15.client.util;

/**
 * Lưu thông tin người dùng hiện tại trong suốt phiên làm việc.
 * Dùng static để truy cập từ bất kỳ đâu mà không cần truyền tham số.
 */
public class SessionManager {

    private static String username;
    private static String email;
    private static int userId;
    private static String role; // "BUYER" | "SELLER" | "ADMIN"
    private static String avatarPath;
    private static double balance;

    // ── Đăng nhập ────────────────────────────────────────────────────────────

    /** Gọi sau khi server xác nhận đăng nhập thành công */
    public static void login(int userId, String username, String email,
                             String role, String avatarPath, double balance) {
        SessionManager.userId     = userId;
        SessionManager.username   = username;
        SessionManager.email      = email;
        SessionManager.role       = role;
        SessionManager.avatarPath = avatarPath;
        SessionManager.balance    = balance;
    }

    /** Gọi khi người dùng đăng xuất */
    public static void logout() {
        userId     = 0;
        username   = null;
        email      = null;
        role       = null;
        avatarPath = null;
        balance    = 0;
    }

    /** Kiểm tra đã đăng nhập chưa */
    public static boolean isLoggedIn() {
        return username != null && !username.isEmpty();
    }

    // ── Getter ───────────────────────────────────────────────────────────────

    public static int    getUserId()     { return userId; }
    public static String getUsername()   { return username; }
    public static String getEmail()      { return email; }
    public static String getRole()       { return role; }
    public static String getAvatarPath() { return avatarPath; }
    public static double getBalance()    { return balance; }

    // ── Tiện ích ─────────────────────────────────────────────────────────────

    public static boolean isSeller() { return "SELLER".equals(role); }
    public static boolean isBuyer()  { return "BIDDER".equals(role); }
    public static boolean isAdmin()  { return "ADMIN".equals(role); }

    /** Cập nhật số dư sau khi nạp tiền / đặt cọc */
    public static void updateBalance(double newBalance) {
        balance = newBalance;
    }

    /** Cập nhật avatar sau khi người dùng đổi ảnh */
    public static void updateAvatar(String newPath) {
        avatarPath = newPath;
    }

    /** Cập nhật role nếu người dùng được nâng cấp lên Seller */
    public static void updateRole(String newRole) {
        role = newRole;
    }
}