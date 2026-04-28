package com.nhom15.client.controller;

import com.google.gson.JsonObject;
import com.nhom15.client.network.SocketClient;
import com.nhom15.client.util.FormValidator;
import javafx.animation.AnimationTimer;
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
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class RegisterController {

    @FXML private Pane bgAnimationPane; // Nền trang trí

    @FXML private TextField txtUsername;
    @FXML private TextField txtEmail;
    @FXML private PasswordField txtPassword;
    @FXML private PasswordField txtConfirmPassword;

    @FXML private Label lblUsernameError;
    @FXML private Label lblEmailError;
    @FXML private Label lblPasswordError;
    @FXML private Label lblConfirmError;

    @FXML private Button btnRegister;
    @FXML private Button btnBackToLogin;

    @FXML
    public void initialize() {
        // Ràng buộc Label lỗi để co giãn mượt mà
        lblUsernameError.managedProperty().bind(lblUsernameError.visibleProperty());
        lblEmailError.managedProperty().bind(lblEmailError.visibleProperty());
        lblPasswordError.managedProperty().bind(lblPasswordError.visibleProperty());
        lblConfirmError.managedProperty().bind(lblConfirmError.visibleProperty());

        // Kiểm tra Regex (bạn có thể điều chỉnh regex mạnh hơn nếu cần)
        FormValidator.bindRegex(txtUsername, lblUsernameError, "^.+$", "Vui lòng nhập tên đăng nhập!");
        FormValidator.bindRegex(txtEmail, lblEmailError, "^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$", "Email không hợp lệ!");
        FormValidator.bindRegex(txtPassword, lblPasswordError, "^.{6,}$", "Mật khẩu phải từ 6 ký tự trở lên!");

        try {
            Pane sharedBg = com.nhom15.client.util.BackgroundEngine.getSharedPane();

            // Kiểm tra và gỡ khỏi cha cũ một cách an toàn
            if (sharedBg.getParent() != null && sharedBg.getParent() instanceof Pane) {
                ((Pane) sharedBg.getParent()).getChildren().remove(sharedBg);
            }

            // Thêm vào lớp dưới cùng của bgAnimationPane
            if (bgAnimationPane != null) {
                bgAnimationPane.getChildren().add(0, sharedBg);

                // Ràng buộc kích thước để phủ kín màn hình
                sharedBg.prefWidthProperty().bind(bgAnimationPane.widthProperty());
                sharedBg.prefHeightProperty().bind(bgAnimationPane.heightProperty());
            }
        } catch (Exception e) {
            System.err.println("Lỗi khi load nền đồng bộ: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleRegister(ActionEvent event) {
        String username = txtUsername.getText();
        String email = txtEmail.getText();
        String password = txtPassword.getText();
        String confirmPassword = txtConfirmPassword.getText();

        // Xóa lỗi cũ ở ô xác nhận
        lblConfirmError.setVisible(false);

        // Kiểm tra trống và lỗi format
        if (username.trim().isEmpty() || email.trim().isEmpty() || password.trim().isEmpty() || confirmPassword.trim().isEmpty() ||
                lblUsernameError.isVisible() || lblEmailError.isVisible() || lblPasswordError.isVisible()) {

            if (username.trim().isEmpty()) lblUsernameError.setVisible(true);
            if (email.trim().isEmpty()) lblEmailError.setVisible(true);
            if (password.trim().isEmpty()) lblPasswordError.setVisible(true);
            if (confirmPassword.trim().isEmpty()) {
                lblConfirmError.setText("Vui lòng xác nhận mật khẩu!");
                lblConfirmError.setVisible(true);
            }
            return;
        }

        // Kiểm tra khớp mật khẩu
        if (!password.equals(confirmPassword)) {
            lblConfirmError.setText("Mật khẩu không khớp!");
            lblConfirmError.setVisible(true);
            return;
        }

        btnRegister.setDisable(true);
        btnRegister.setText("Đang xử lý...");

        JsonObject data = new JsonObject();
        data.addProperty("username", username);
        data.addProperty("email", email);
        data.addProperty("password", password);

        JsonObject requestJson = new JsonObject();
        requestJson.addProperty("action", "REGISTER");
        requestJson.add("data", data);

        new Thread(() -> {
            JsonObject responseJson = SocketClient.sendRequest(requestJson);

            javafx.application.Platform.runLater(() -> {
                btnRegister.setDisable(false);
                btnRegister.setText("XÁC NHẬN ĐĂNG KÝ");

                if (responseJson != null) {
                    String status = responseJson.get("status").getAsString();
                    String message = responseJson.get("message").getAsString();

                    if ("SUCCESS".equals(status)) {
                        showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đăng ký thành công! Vui lòng đăng nhập.");
                        handleBackToLogin(null); // Tự động quay về trang đăng nhập
                    } else {
                        showAlert(Alert.AlertType.ERROR, "Đăng ký thất bại", message);
                    }
                } else {
                    showAlert(Alert.AlertType.ERROR, "Lỗi kết nối", "Không thể kết nối đến Server!");
                }
            });
        }).start();
    }

    @FXML
    private void handleBackToLogin(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) btnBackToLogin.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Đăng nhập");
            stage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể mở màn hình đăng nhập!");
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