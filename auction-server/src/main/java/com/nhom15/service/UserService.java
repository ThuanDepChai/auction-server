package com.nhom15.service;

import com.google.gson.JsonObject;
import com.nhom15.dao.UserDAO;
import com.nhom15.model.user.*;
import com.nhom15.util.PasswordUtil;

public class UserService {
    private UserDAO userDAO = new UserDAO();

    /**
     * Kiểm tra username đã tồn tại chưa
     * Hàm này được AuctionServer gọi khi có request CHECK_USERNAME
     */
    public boolean isExists(String username) {
        return userDAO.isUsernameExists(username);
    }

    // Hash mật khẩu khi đăng ký để lưu vào UserDAO
    public boolean register(String username, String password, String email) {
        // Trước khi đăng ký, có thể check lại một lần nữa cho chắc
        if (userDAO.isUsernameExists(username)) {
            return false;
        }

        String hashedPassword = PasswordUtil.hashPassword(password);
        return userDAO.registerUser(username, hashedPassword, email);
    }

    // Đăng nhập: kiểm tra username và password
    public boolean login(String username, String inputPassword) {
        User user = userDAO.findByUsername(username);
        if (user == null) return false;

        // Kiểm tra password bằng BCrypt qua PasswordUtil
        // Lưu ý: Đảm bảo trong model User, hàm getPasswordHash() trả về đúng chuỗi đã hash
        return PasswordUtil.verifyPassword(inputPassword, user.getPasswordHash());
    }
    /** Trả về User object đầy đủ để server gửi về client */
    public User getUserForLogin(String username, String inputPassword) {
        User user = userDAO.findByUsername(username);
        if (user == null) return null;
        if (!PasswordUtil.verifyPassword(inputPassword, user.getPasswordHash())) return null;
        return user;
    }
    // Đăng xuất
    public void logout(User user) {
        if (user != null) {
            System.out.println("User " + user.getUsername() + " đã đăng xuất.");
        }
    }
    // Lấy thông tin
    public JsonObject getProfile(int userId) {
        return userDAO.getProfile(userId);
    }
    // Đổi Avatar
    public boolean updateAvatar(int userId, String avatarPath) {
        return userDAO.updateAvatar(userId, avatarPath);
    }
    // Đổi Password
    public boolean changePassword(int userId, String oldPassword, String newPassword) {
        // 1. Lấy thông tin user hiện tại từ Database thông qua ID
        User user = userDAO.findById(userId);
        if (user == null) {
            return false; // Không tìm thấy người dùng
        }

        // 2. Kiểm tra mật khẩu cũ có đúng không
        if (!PasswordUtil.verifyPassword(oldPassword, user.getPasswordHash())) {
            return false; // Sai mật khẩu cũ
        }

        // 3. Nếu đúng, tiến hành hash mật khẩu mới
        String newHashedPassword = PasswordUtil.hashPassword(newPassword);

        // 4. Gọi DAO để cập nhật mật khẩu mới vào Database
        return userDAO.updatePassword(userId, newHashedPassword);
    }
    // Cập nhật thông tin cá nhân
    public boolean updateProfile(int userId, String fullName, String email, String phone) {
        return userDAO.updateProfile(userId, fullName, email, phone);
    }

    // Nâng cấp tài khoản lên Seller
    public boolean upgradeToSeller(int userId) {
        return userDAO.upgradeToSeller(userId);
    }

    // Lấy đường dẫn ảnh đại diện
    public String getAvatarPath(int userId) {
        return userDAO.getAvatarPath(userId);
    }
}