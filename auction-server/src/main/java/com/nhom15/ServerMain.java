package com.nhom15;

import com.nhom15.model.user.User;
import com.nhom15.service.UserService;

public class ServerMain {

  public static void main(String[] args) {
    UserService userService = new UserService();

    // Ví dụ test đăng nhập
    String username = "ThuanDepChai";
    String password = "123"; // mật khẩu plain text người dùng nhập

    // Hứng kết quả bằng Object User thay vì boolean
    User loggedInUser = userService.login(username, password);

    // Kiểm tra xem user có khác null không
    if (loggedInUser != null) {
      System.out.println("Đăng nhập thành công!");
      System.out.println(
          "Xin chào: " + loggedInUser.getUsername() + " | Role: " + loggedInUser.getRole());
    } else {
      System.out.println("Sai tài khoản hoặc mật khẩu!");
    }
  }
}