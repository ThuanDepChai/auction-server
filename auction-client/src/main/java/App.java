package com.nhom15.client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class App extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            // ĐÃ FIX: Sửa đường dẫn thành "/view/Main.fxml"
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/Main.fxml"));
            Parent root = loader.load();

            Scene scene = new Scene(root, 1200, 700);

            primaryStage.setTitle("Hệ thống Đấu giá - Nhóm 15");
            primaryStage.setScene(scene);
            primaryStage.centerOnScreen();
            primaryStage.show();

        } catch (IOException e) {
            System.err.println("Lỗi không thể tải giao diện: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}