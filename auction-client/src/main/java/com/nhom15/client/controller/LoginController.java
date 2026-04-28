package com.nhom15.client.controller;

import com.google.gson.JsonObject;
import com.nhom15.client.network.SocketClient;
import com.nhom15.client.util.FormValidator;
import javafx.animation.FadeTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
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
import javafx.util.Duration;

import java.io.IOException;
import java.util.Random;

public class LoginController {

    // ID của Pane chứa hoạt ảnh nền
    @FXML private Pane bgAnimationPane;

    @FXML private TextField txtUsername;
    @FXML private TextField txtEmail;
    @FXML private PasswordField txtPassword;

    @FXML private Label lblUsernameError;
    @FXML private Label lblEmailError;
    @FXML private Label lblPasswordError;

    @FXML private Button btnLogin;
    @FXML private Button btnRegister;

    @FXML
    public void initialize() {
        // 1. Ràng buộc co giãn khoảng cách báo lỗi (ManagedProperty)
        lblUsernameError.managedProperty().bind(lblUsernameError.visibleProperty());
        lblEmailError.managedProperty().bind(lblEmailError.visibleProperty());
        lblPasswordError.managedProperty().bind(lblPasswordError.visibleProperty());

        // 2. Gắn bộ kiểm tra Regex (Dùng FormValidator như trước)
        FormValidator.bindRegex(txtUsername, lblUsernameError, "^.+$", "Vui lòng nhập tên đăng nhập!");
        FormValidator.bindRegex(txtEmail, lblEmailError, "^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$", "Email không hợp lệ!");
        FormValidator.bindRegex(txtPassword, lblPasswordError, "^.+$", "Vui lòng nhập mật khẩu!");

        // 3. Tạo hiệu ứng chuyển động nền
        createBackgroundAnimation();
    }

    /**
     * Tạo các hoạt ảnh nền tinh tế (vòng tròn trôi nổi)
     */
    private void createBackgroundAnimation() {
        int numberOfCircles = 6;
        Random random = new Random();

        // Kích thước Scene Full HD giả định cho vị trí ban đầu
        double assumedWidth = 1920.0;
        double assumedHeight = 1080.0;

        for (int i = 0; i < numberOfCircles; i++) {
            Circle circle = new Circle();

            // Màu sắc mờ, trong suốt cho nền
            Color baseColor;
            if (i % 3 == 0) baseColor = Color.web("#4285F4"); // Xanh Gemini
            else if (i % 3 == 1) baseColor = Color.web("#9B72CB"); // Tím
            else baseColor = Color.web("#D96570"); // Hồng

            // Thiết lập màu đặc (Color) cho vòng tròn
            circle.setFill(new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), 0.05 + random.nextDouble() * 0.1));
            circle.setRadius(50 + random.nextDouble() * 150);

            // Vị trí ban đầu ngẫu nhiên, tập trung xung quanh trung tâm
            double startX = assumedWidth * (0.3 + random.nextDouble() * 0.4);
            double startY = assumedHeight * (0.3 + random.nextDouble() * 0.4);
            circle.setLayoutX(startX);
            circle.setLayoutY(startY);

            // Hoạt ảnh trôi nổi (TranslateTransition)
            TranslateTransition translate = new TranslateTransition(Duration.seconds(20 + random.nextDouble() * 30), circle);
            translate.setByX(100 - random.nextDouble() * 200);
            translate.setByY(100 - random.nextDouble() * 200);
            translate.setCycleCount(Timeline.INDEFINITE);
            translate.setAutoReverse(true);
            translate.play();

            // --- PHẦN SỬA LỖI ---
            // Lấy màu hiện tại ra và ép kiểu về Color
            Color currentFillColor = (Color) circle.getFill();
            // Lấy độ mờ ban đầu từ đối tượng Color đã ép kiểu
            double initialOpacity = currentFillColor.getOpacity();

            // Hoạt ảnh xung opacity (FadeTransition)
            FadeTransition fade = new FadeTransition(Duration.seconds(5 + random.nextDouble() * 5), circle);
            fade.setFromValue(initialOpacity); // Dùng giá trị đã lấy được
            fade.setToValue(initialOpacity * 0.5);
            fade.setCycleCount(Timeline.INDEFINITE);
            fade.setAutoReverse(true);
            fade.play();
            // --------------------

            bgAnimationPane.getChildren().add(circle);
        }

        // Đảm bảo hoạt ảnh nền lấp đầy Pane khi Pane thay đổi kích thước
        bgAnimationPane.prefWidthProperty().bind(txtUsername.getScene().getWindow().widthProperty());
        bgAnimationPane.prefHeightProperty().bind(txtUsername.getScene().getWindow().heightProperty());
    }

    @FXML
    private void handleLogin(ActionEvent event) {
        String username = txtUsername.getText();
        String email = txtEmail.getText();
        String password = txtPassword.getText();

        if (username.trim().isEmpty() || email.trim().isEmpty() || password.trim().isEmpty() ||
                lblUsernameError.isVisible() || lblEmailError.isVisible() || lblPasswordError.isVisible()) {

            if (username.trim().isEmpty()) lblUsernameError.setVisible(true);
            if (email.trim().isEmpty()) lblEmailError.setVisible(true);
            if (password.trim().isEmpty()) lblPasswordError.setVisible(true);
            return;
        }

        btnLogin.setDisable(true);
        btnLogin.setText("Đang đăng nhập...");

        JsonObject data = new JsonObject();
        data.addProperty("username", username);
        data.addProperty("email", email);
        data.addProperty("password", password);

        JsonObject requestJson = new JsonObject();
        requestJson.addProperty("action", "LOGIN");
        requestJson.add("data", data);

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