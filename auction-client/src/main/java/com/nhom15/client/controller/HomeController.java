package com.nhom15.client.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class HomeController {

    @FXML
    private Label lblWelcome;

    // Phương thức để nhận dữ liệu từ màn hình Login truyền qua
    public void setUsername(String username) {
        lblWelcome.setText("Chào, " + username + "!");
    }

    @FXML
    public void initialize() {
        // Khởi tạo các dữ liệu sản phẩm ở đây
        System.out.println("Trang chủ đã sẵn sàng!");
    }
}
