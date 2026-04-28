package com.nhom15.service;

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
}