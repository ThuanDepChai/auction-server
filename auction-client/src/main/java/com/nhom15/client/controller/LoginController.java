package com.nhom15.client.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.io.IOException;

public class LoginController {

    // Liên kết với các thành phần giao diện bên FXML thông qua fx:id
    @FXML
    private TextField txtUsername;

    @FXML
    private TextField txtEmail;

    @FXML
    private PasswordField txtPassword;

    @FXML
    private Button btnLogin;

    @FXML
    private Button btnRegister;

    // Phương thức này được gọi tự động sau khi file FXML được tải xong
    @FXML
    public void initialize() {
        // Bạn có thể thiết lập các cài đặt ban đầu ở đây nếu cần
        // Ví dụ: Bắt sự kiện Enter để đăng nhập thay vì click chuột
    }

    // Xử lý sự kiện khi nhấn nút "ĐĂNG NHẬP"
    @FXML
    private void handleLogin(ActionEvent event) {
            String username = txtUsername.getText();
            String password = txtPassword.getText();

            // Kiểm tra rỗng
            if (username.trim().isEmpty() || password.trim().isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Thiếu thông tin", "Vui lòng nhập đầy đủ Tên đăng nhập và Mật khẩu!");
                return;
            }

            // 1. Đóng gói dữ liệu thành JSON theo đúng format Server đang chờ
            JsonObject data = new JsonObject();
            data.addProperty("username", username);
            data.addProperty("password", password);

            JsonObject requestJson = new JsonObject();
            requestJson.addProperty("action", "LOGIN"); // Báo cho Server biết đây là lệnh ĐĂNG NHẬP
            requestJson.add("data", data);

            // 2. GỌI SOCKET CLIENT ĐỂ GỬI ĐI (Giống hệt bên trang Đăng ký)
            // Nhớ import com.nhom15.client.network.SocketClient; ở đầu file nhé!
            JsonObject responseJson = com.nhom15.client.network.SocketClient.sendRequest(requestJson);

            // 3. Xử lý phản hồi từ Server
            if (responseJson != null) {
                String status = responseJson.get("status").getAsString();
                String message = responseJson.get("message").getAsString();

                if ("SUCCESS".equals(status)) {
                    showAlert(Alert.AlertType.INFORMATION, "Thành công", message);

                    // CHUYỂN SANG MÀN HÌNH TRANG CHỦ (HOME)
                    try {
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/Home.fxml"));
                        Parent root = loader.load();
                        Stage stage = (Stage) txtUsername.getScene().getWindow();
                        stage.setScene(new Scene(root));
                        stage.setTitle("Trang chủ - Hệ thống Đấu giá Nhóm 15");
                        stage.centerOnScreen();
                    } catch (Exception e) {
                        e.printStackTrace();
                        showAlert(Alert.AlertType.ERROR, "Lỗi giao diện", "Không thể tải được màn hình Trang chủ!");
                    }
                } else {
                    showAlert(Alert.AlertType.ERROR, "Thất bại", message);
                }
            } else {
                showAlert(Alert.AlertType.ERROR, "Lỗi kết nối", "Server không phản hồi. Hãy chắc chắn Server đang chạy!");
            }
        }

    // Xử lý sự kiện khi nhấn nút "TẠO TÀI KHOẢN MỚI"
    @FXML
    private void handleRegister(ActionEvent event) {
        try {
            // Tải giao diện Đăng ký
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/register.fxml"));
            Parent root = loader.load();

            // Lấy cửa sổ hiện tại và đổi Scene
            Stage stage = (Stage) btnRegister.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Đăng ký tài khoản");
            stage.centerOnScreen(); // Căn giữa màn hình

        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể mở màn hình đăng ký!");
        }
    }

    // Hàm tiện ích để hiển thị các hộp thoại thông báo (Alert Dialog)
    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null); // Tắt phần tiêu đề phụ cho gọn
        alert.setContentText(message);
        alert.showAndWait();
    }
}