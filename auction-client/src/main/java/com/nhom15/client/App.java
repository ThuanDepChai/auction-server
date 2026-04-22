package com.nhom15.client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

import static javafx.application.Application.launch;

public class App extends Application {

    // Bên trong file App.java (phần start)
    @Override
    public void start(Stage primaryStage) {
        try {
            // Đổi đường dẫn thành login.fxml
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/login.fxml"));
            Parent root = loader.load();

            // Không cần set cứng kích thước 1200x700 nữa để form đăng nhập gọn gàng
            Scene scene = new Scene(root);

            primaryStage.setTitle("Đăng nhập - Hệ thống Đấu giá");
            primaryStage.setScene(scene);
            primaryStage.centerOnScreen();
            primaryStage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        // Hàm launch() là bắt buộc để khởi động vòng đời của một app JavaFX
        launch(args);
    }
}