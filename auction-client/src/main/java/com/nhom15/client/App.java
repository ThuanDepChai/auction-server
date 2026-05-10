package com.nhom15.client;

import com.nhom15.client.util.ViewManager; // Đừng quên import class này
import javafx.application.Application;
import javafx.stage.Stage;

public class App extends Application {

    @Override
    public void start(Stage primaryStage) {
        ViewManager.init(primaryStage);
        ViewManager.navigateTo(ViewManager.Views.LOGIN);

        // Nếu muốn app luôn mở to toàn màn hình ngay từ đầu:
        primaryStage.setMaximized(true);

        primaryStage.show();
    }

    public static void main(String[] args) {
        // Hàm launch() là bắt buộc để khởi động vòng đời của một app JavaFX
        launch(args);
    }
}