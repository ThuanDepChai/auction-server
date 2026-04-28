package com.nhom15.client.controller;

import com.google.gson.JsonObject;
import com.nhom15.client.network.SocketClient;
import com.nhom15.client.util.FormValidator;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

public class LoginController {

    @FXML private TextField txtUsername;
    @FXML private TextField txtEmail; // Đã thêm lại Email
    @FXML private PasswordField txtPassword;

    @FXML private Label lblUsernameError;
    @FXML private Label lblEmailError; // Thêm Label lỗi cho Email
    @FXML private Label lblPasswordError;

    @FXML private Button btnLogin;
    @FXML private Button btnRegister;

    @FXML
    public void initialize() {
        // 1. Phép thuật "co giãn" khoảng cách
        lblUsernameError.managedProperty().bind(lblUsernameError.visibleProperty());
        lblEmailError.managedProperty().bind(lblEmailError.visibleProperty());
        lblPasswordError.managedProperty().bind(lblPasswordError.visibleProperty());

        // 2. Gắn bộ kiểm tra Regex
        FormValidator.bindRegex(txtUsername, lblUsernameError, "^.+$", "Vui lòng nhập tên đăng nhập!");
        // Email thì check chuẩn định dạng
        FormValidator.bindRegex(txtEmail, lblEmailError, "^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$", "Email không hợp lệ (vd: abc@gmail.com)");
        FormValidator.bindRegex(txtPassword, lblPasswordError, "^.+$", "Vui lòng nhập mật khẩu!");
    }

    @FXML
    private void handleLogin(ActionEvent event) {
        String username = txtUsername.getText();
        String email = txtEmail.getText(); // Lấy dữ liệu Email
        String password = txtPassword.getText();

        // Kiểm tra xem có trường nào bị trống hoặc đang bị lỗi đỏ không
        if (username.trim().isEmpty() || email.trim().isEmpty() || password.trim().isEmpty() ||
                lblUsernameError.isVisible() || lblEmailError.isVisible() || lblPasswordError.isVisible()) {

            // Ép hiện lỗi nếu họ chưa gõ gì mà đã bấm nút
            if (username.trim().isEmpty()) lblUsernameError.setVisible(true);
            if (email.trim().isEmpty()) lblEmailError.setVisible(true);
            if (password.trim().isEmpty()) lblPasswordError.setVisible(true);
            return;
        }

        // Khóa nút tạo cảm giác mượt
        btnLogin.setDisable(true);
        btnLogin.setText("Đang đăng nhập...");

        JsonObject data = new JsonObject();
        data.addProperty("username", username);
        data.addProperty("email", email); // Gửi thêm Email lên Server
        data.addProperty("password", password);

        JsonObject requestJson = new JsonObject();
        requestJson.addProperty("action", "LOGIN");
        requestJson.add("data", data);

        // Chạy ngầm đi gửi mạng
        new Thread(() -> {
            JsonObject responseJson = SocketClient.sendRequest(requestJson);

            javafx.application.Platform.runLater(() -> {
                btnLogin.setDisable(false);
                btnLogin.setText("ĐĂNG NHẬP");

                if (responseJson != null) {
                    String status = responseJson.get("status").getAsString();
                    String message = responseJson.get("message").getAsString();

                    if ("SUCCESS".equals(status)) {
                        try {
                            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/Home.fxml"));
                            Parent root = loader.load();
                            Stage stage = (Stage) txtUsername.getScene().getWindow();
                            stage.setScene(new Scene(root));
                            stage.setTitle("Trang chủ");
                            stage.centerOnScreen();
                        } catch (Exception e) {
                            showAlert(Alert.AlertType.ERROR, "Lỗi giao diện", "Không thể tải Trang chủ!");
                        }
                    } else {
                        showAlert(Alert.AlertType.ERROR, "Đăng nhập thất bại", message);
                    }
                } else {
                    showAlert(Alert.AlertType.ERROR, "Lỗi kết nối", "Không thể kết nối đến Server!");
                }
            });
        }).start();
    }

    @FXML
    private void handleRegister(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/register.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) btnRegister.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Đăng ký tài khoản");
            stage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể mở màn hình đăng ký!");
        }
    }

    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}