package com.nhom15.service;
import com.nhom15.model.user.*;
import com.nhom15.util.PasswordUtil;

public class UserService {
    // Đăng nhập: kiểm tra username và password
    public boolean login(User user, String inputUsername, String inputPassword) {
        if (user == null) return false;

        // kiểm tra username
        if (!user.getUsername().equals(inputUsername)) {
            return false;
        }

        // kiểm tra password bằng BCrypt qua PasswordUtil
        return PasswordUtil.verifyPassword(inputPassword, user.getPasswordHash());
    }

    // Đăng xuất
    public void logout(User user) {
        if (user != null) {
            System.out.println(user.getUsername() + " đã đăng xuất.");
        }
    }

    // Đăng ký: tạo User mới với mật khẩu được hash
    public User register(int id, String username, String plainPassword, String email, UserRole role) {
        String hashed = PasswordUtil.hashPassword(plainPassword);
        switch (role) {
            case BIDDER :
                return new Bidder(id, username, hashed, email);
            case SELLER:
                return new Seller(id, username, hashed, email);
            case ADMIN:
                return new Admin(id, username, hashed, email);
            default:
                throw new IllegalArgumentException("Role không hợp lệ: " + role);
        }
    }
}

