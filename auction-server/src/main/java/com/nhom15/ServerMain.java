package com.nhom15;


import com.nhom15.dao.UserDAO;




import java.sql.Connection;
import java.sql.SQLException;


public class ServerMain {
    public static void main(String[] args) {
        UserDAO userDAO = new UserDAO();
        boolean user1 = userDAO.registerUser("TranDucThuan","123456","xyz@gmail.com");
        System.out.println("Đăng ký newUser: " + (user1 ? "Thành công" : "Thất bại"));


    }
}
