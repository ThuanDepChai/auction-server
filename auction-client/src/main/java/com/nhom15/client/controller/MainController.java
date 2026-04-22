package com.nhom15.client.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;

import java.io.IOException;

public class MainController {

    @FXML
    private StackPane contentArea; // Nơi nhúng các màn hình con

    @FXML
    private Button btnHome;

    @FXML
    private Button btnProducts;

    @FXML
    private Button btnSettings;

    @FXML
    public void initialize() {
        // Mặc định khi mở MainLayout lên, có thể load sẵn màn hình Home
        // loadView("/com/nhom15/client/view/Home.fxml");
    }

    @FXML
    void showHomeView(ActionEvent event) {
        // Thay đường dẫn FXML tương ứng với màn hình Trang chủ của nhóm bạn
        loadView("/com/nhom15/client/view/Home.fxml");
    }

    @FXML
    void showProductView(ActionEvent event) {
        // Ví dụ load màn hình Quản lý Sản phẩm
        // loadView("/com/nhom15/client/view/Product.fxml");
        System.out.println("Chuyển sang màn hình Sản phẩm");
    }

    @FXML
    void showSettingsView(ActionEvent event) {
        System.out.println("Chuyển sang màn hình Cài đặt");
    }

    @FXML
    void handleLogout(ActionEvent event) {
        System.out.println("Thực hiện đăng xuất, quay lại màn Login...");
        // TODO: Viết code chuyển Stage hiện tại về Login.fxml
    }

    /**
     * Hàm dùng chung để load file FXML và nhúng vào StackPane (contentArea)
     */
    private void loadView(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Node view = loader.load();

            // Xóa nội dung cũ và thêm màn hình mới vào StackPane
            contentArea.getChildren().clear();
            contentArea.getChildren().add(view);

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Không thể load file FXML: " + fxmlPath);
        }
    }
}