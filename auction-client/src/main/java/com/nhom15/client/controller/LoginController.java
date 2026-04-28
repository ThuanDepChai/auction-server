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

public class LoginController {

    @FXML private Pane bgAnimationPane; // Nền trang trí

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
        // Ràng buộc Label lỗi
        lblUsernameError.managedProperty().bind(lblUsernameError.visibleProperty());
        lblEmailError.managedProperty().bind(lblEmailError.visibleProperty());
        lblPasswordError.managedProperty().bind(lblPasswordError.visibleProperty());

        // Kiểm tra Regex
        FormValidator.bindRegex(txtUsername, lblUsernameError, "^.+$", "Vui lòng nhập tên đăng nhập!");
        FormValidator.bindRegex(txtEmail, lblEmailError, "^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$", "Email không hợp lệ!");
        FormValidator.bindRegex(txtPassword, lblPasswordError, "^.+$", "Vui lòng nhập mật khẩu!");

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