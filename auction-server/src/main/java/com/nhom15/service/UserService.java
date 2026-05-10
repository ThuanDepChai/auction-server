package com.nhom15.service;

import com.nhom15.dao.UserDAO;
import com.nhom15.model.user.*;
import com.nhom15.util.PasswordUtil;

public class UserService {
    private UserDAO userDAO = new UserDAO();

    /** Kiểm tra username đã tồn tại chưa */
    public boolean isExists(String username) {
        return userDAO.isUsernameExists(username);
    }

    /** Hash mật khẩu và đăng ký */
    public boolean register(String username, String password, String email) {
        if (userDAO.isUsernameExists(username)) {
            return false;
        }
        String hashedPassword = PasswordUtil.hashPassword(password);
        return userDAO.registerUser(username, hashedPassword, email);
    }

    /** * XỬ LÝ ĐĂNG NHẬP (Gộp lại cho chuẩn)
     * Trả về Object User nếu thành công, trả về null nếu thất bại
     */
    public User login(String username, String inputPassword) {
        User user = userDAO.findByUsername(username);
        if (user == null) return null;

        // Kiểm tra password bằng BCrypt
        if (PasswordUtil.verifyPassword(inputPassword, user.getPasswordHash())) {
            return user; // Đúng mật khẩu thì trả về đối tượng User
        }
        return null; // Sai mật khẩu
    }

    /** Lấy thông tin User để làm Profile (Trả về Object, KHÔNG trả về JSON) */
    public User getProfile(int userId) {
        // Lưu ý: UserDAO của bạn cũng phải trả về User Object, không trả JSON
        return userDAO.findById(userId);
    }

    // --- Các hàm cập nhật (Giữ nguyên vì logic đã đúng) ---

    public boolean updateAvatar(int userId, String avatarPath) {
        return userDAO.updateAvatar(userId, avatarPath);
    }

    public boolean changePassword(int userId, String oldPassword, String newPassword) {
        User user = userDAO.findById(userId);
        if (user == null) return false;

        if (!PasswordUtil.verifyPassword(oldPassword, user.getPasswordHash())) {
            return false;
        }
        String newHashedPassword = PasswordUtil.hashPassword(newPassword);
        return userDAO.updatePassword(userId, newHashedPassword);
    }

    public boolean updateProfile(int userId, String fullName, String email, String phone) {
        return userDAO.updateProfile(userId, fullName, email, phone);
    }

    public boolean upgradeToSeller(int userId) {
        return userDAO.upgradeToSeller(userId);
    }

    public String getAvatarPath(int userId) {
        return userDAO.getAvatarPath(userId);
    }
}