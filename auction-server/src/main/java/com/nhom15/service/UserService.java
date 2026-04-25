package com.nhom15.service;
import com.nhom15.dao.UserDAO;
import com.nhom15.model.user.*;
import com.nhom15.util.PasswordUtil;

public class UserService {
    private UserDAO userDAO = new UserDAO();

    // Hash mật khẩu khi đăng ký để lưu vào UserDAO
    public boolean register(String username,String password, String email) {
        String hashedPassword = PasswordUtil.hashPassword(password);
        return userDAO.registerUser(username,hashedPassword,email);
    }
    // Đăng nhập: kiểm tra username và password
    public boolean login(String username, String inputPassword) {
        User user = userDAO.findByUsername(username);
        if (user == null ) return false;

        // kiểm tra password bằng BCrypt qua PasswordUtil
        return PasswordUtil.verifyPassword(inputPassword, user.getPasswordHash());
    }
    // Đăng xuất
    public void logout(User user) {
        if (user != null) {
            // Thêm vào các hành động sẽ có khi đăng xuất
        }
    }
}

