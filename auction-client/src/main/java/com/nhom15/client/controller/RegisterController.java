package com.nhom15.client.controller;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nhom15.client.model.UserDTO;
import com.nhom15.client.network.SocketClient;
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class RegisterController {

    @FXML
    private TextField txtUsername;

    @FXML
    private TextField txtEmail;

    @FXML
    private PasswordField txtPassword;

    @FXML
    private PasswordField txtConfirmPassword;

    @FXML
    private Button btnRegister;

    @FXML
    private Button btnBackToLogin;

    @FXML
    private void handleRegister(ActionEvent event) {
        String username = txtUsername.getText();
        String email = txtEmail.getText();
        String password = txtPassword.getText();
        String confirmPassword = txtConfirmPassword.getText();

        // 1. Kiểm tra không được để trống
        if (username.trim().isEmpty() || email.trim().isEmpty() ||
                password.trim().isEmpty() || confirmPassword.trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Thiếu thông tin", "Vui lòng điền đầy đủ tất cả các trường!");
            return;
        }

        // 2. Kiểm tra định dạng email
        if (!isValidEmail(email)) {
            showAlert(Alert.AlertType.ERROR, "Lỗi email", "Email không đúng định dạng!");
            return;
        }

        // 3. Kiểm tra mật khẩu có khớp nhau không
        if (!password.equals(confirmPassword)) {
            showAlert(Alert.AlertType.ERROR, "Lỗi mật khẩu", "Mật khẩu xác nhận không khớp!");
            return;
        }

        // TẠO CẢM GIÁC MƯỢT: Khóa nút bấm lại và đổi chữ để người dùng biết app đang làm việc
        btnRegister.setDisable(true);
        btnRegister.setText("Đang xử lý...");

        // Đóng gói đối tượng User
        UserDTO newUser = new UserDTO(username, email, password);
        Gson gson = new Gson();

        // Tạo chuỗi JSON theo chuẩn giao thức đã quy ước
        JsonObject requestJson = new JsonObject();
        requestJson.addProperty("action", "REGISTER");
        requestJson.add("data", gson.toJsonTree(newUser));

        // THUÊ NHÂN VIÊN CHẠY NGẦM GỬI MẠNG ĐỂ KHÔNG BỊ ĐƠ GIAO DIỆN
        new Thread(() -> {
            // Lệnh gửi mạng này tốn thời gian nên để luồng ngầm chạy
            JsonObject responseJson = SocketClient.sendRequest(requestJson);

            // CẦM KẾT QUẢ VỀ BÁO LẠI CHO NHÂN VIÊN GIAO DIỆN (Bắt buộc)
            javafx.application.Platform.runLater(() -> {
                // Nhả nút ra, trả lại trạng thái ban đầu
                btnRegister.setDisable(false);
                btnRegister.setText("Đăng ký");

                // Xử lý phản hồi từ Server
                if (responseJson != null) {
                    String status = responseJson.get("status").getAsString();
                    String message = responseJson.get("message").getAsString();

                    if ("SUCCESS".equals(status)) {
                        showAlert(Alert.AlertType.INFORMATION, "Thành công", message);
                        goToLoginScreen(btnRegister);
                    } else {
                        showAlert(Alert.AlertType.ERROR, "Lỗi đăng ký", message);
                    }
                } else {
                    showAlert(Alert.AlertType.ERROR, "Lỗi kết nối", "Không thể kết nối đến Server! Vui lòng kiểm tra lại xem Server đã bật chưa.");
                }
            });
        }).start(); // Kích hoạt luồng ngầm chạy
    }

    @FXML
    private void handleBackToLogin(ActionEvent event) {
        // Nút "Quay lại"
        goToLoginScreen(btnBackToLogin);
    }

    // Hàm tiện ích để chuyển về màn Login
    private void goToLoginScreen(Button button) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) button.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Đăng nhập - Hệ thống Đấu giá");
            stage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Không thể load trang đăng nhập!");
        }
    }

    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    // Hàm kiểm tra định dạng email
    private boolean isValidEmail(String email) {
        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$";
        return email.matches(emailRegex);
    }
}