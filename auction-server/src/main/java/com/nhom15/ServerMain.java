package com.nhom15;


import com.nhom15.dao.UserDAO;
import com.nhom15.service.UserService;


import java.sql.Connection;
import java.sql.SQLException;


public class ServerMain {
    public static void main(String[] args) {
        UserService userService = new UserService();

        // Ví dụ test đăng nhập
        String username = "ThuanDepChai";
        String password = "123"; // mật khẩu plain text người dùng nhập

        boolean loginSuccess = userService.login(username, password);

        if (loginSuccess) {
            System.out.println("Đăng nhập thành công!");
        } else {
            System.out.println("Sai tài khoản hoặc mật khẩu!");
        }
    }
}

