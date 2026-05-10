package com.nhom15.client.controller;

import com.google.gson.JsonObject;
import com.nhom15.client.network.SocketClient;
import com.nhom15.client.util.FormValidator;
import javafx.animation.PauseTransition;
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
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;

public class RegisterController {

    @FXML private Pane bgAnimationPane;

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

    private PauseTransition usernameCheckDelay;
    private boolean isUsernameAvailable = true;

    @FXML
    public void initialize() {
        lblUsernameError.managedProperty().bind(lblUsernameError.visibleProperty());
        lblEmailError.managedProperty().bind(lblEmailError.visibleProperty());
        lblPasswordError.managedProperty().bind(lblPasswordError.visibleProperty());
        lblConfirmError.managedProperty().bind(lblConfirmError.visibleProperty());

        FormValidator.bindRegex(txtEmail, lblEmailError,
                "^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$",
                "Email không hợp lệ!");
        FormValidator.bindRegex(txtPassword, lblPasswordError,
                "^(?=.*[A-Za-z])(?=.*\\d).{6,}$",
                "Mật khẩu phải từ 6 ký tự, bao gồm cả chữ lẫn số!");

        // Debounce 600ms sau khi ngừng gõ → gửi CHECK_USERNAME
        usernameCheckDelay = new PauseTransition(Duration.millis(600));
        usernameCheckDelay.setOnFinished(e -> checkUsernameAvailability());

        txtUsername.textProperty().addListener((obs, oldVal, newVal) -> {
            lblUsernameError.setVisible(false);
            isUsernameAvailable = true;
            usernameCheckDelay.stop();

            if (!newVal.trim().isEmpty()) {
                usernameCheckDelay.playFromStart();
            }
        });

        // Load background
        try {
            Pane sharedBg = com.nhom15.client.util.BackgroundEngine.getSharedPane();
            if (sharedBg.getParent() instanceof Pane) {
                ((Pane) sharedBg.getParent()).getChildren().remove(sharedBg);
            }
            if (bgAnimationPane != null) {
                bgAnimationPane.getChildren().add(0, sharedBg);
                sharedBg.prefWidthProperty().bind(bgAnimationPane.widthProperty());
                sharedBg.prefHeightProperty().bind(bgAnimationPane.heightProperty());
            }
        } catch (Exception e) {
            System.err.println("Lỗi khi load nền: " + e.getMessage());
        }
    }

    private void checkUsernameAvailability() {
        String username = txtUsername.getText().trim();
        if (username.isEmpty()) return;

        JsonObject data = new JsonObject();
        data.addProperty("username", username);

        JsonObject request = new JsonObject();
        request.addProperty("action", "CHECK_USERNAME");
        request.add("data", data);

        new Thread(() -> {
            JsonObject response = SocketClient.sendRequest(request);

            javafx.application.Platform.runLater(() -> {
                if (!txtUsername.getText().trim().equals(username)) return;
                if (response == null) return;

                String status = response.get("status").getAsString();
                if ("EXISTS".equals(status)) {
                    isUsernameAvailable = false;
                    lblUsernameError.setText("Tên đăng nhập đã tồn tại!");
                    lblUsernameError.setVisible(true);
                } else {
                    isUsernameAvailable = true;
                    lblUsernameError.setVisible(false);
                }
            });
        }).start();
    }

    @FXML
    private void handleRegister(ActionEvent event) {
        String username = txtUsername.getText().trim();
        String email    = txtEmail.getText().trim();
        String password = txtPassword.getText();
        String confirm  = txtConfirmPassword.getText();

        lblConfirmError.setVisible(false);
        boolean hasError = false;

        if (username.isEmpty()) {
            lblUsernameError.setText("Vui lòng nhập tên đăng nhập!");
            lblUsernameError.setVisible(true);
            hasError = true;
        }
        if (email.isEmpty()) {
            lblEmailError.setText("Vui lòng nhập email!");
            lblEmailError.setVisible(true);
            hasError = true;
        }
        if (password.isEmpty()) {
            lblPasswordError.setText("Vui lòng nhập mật khẩu!");
            lblPasswordError.setVisible(true);
            hasError = true;
        }
        if (confirm.isEmpty()) {
            lblConfirmError.setText("Vui lòng xác nhận mật khẩu!");
            lblConfirmError.setVisible(true);
            hasError = true;
        }

        if (hasError || !isUsernameAvailable
                || lblEmailError.isVisible()
                || lblPasswordError.isVisible()) {
            return;
        }

        if (!password.equals(confirm)) {
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

                if (responseJson == null) {
                    showAlert(Alert.AlertType.ERROR, "Lỗi kết nối", "Không thể kết nối đến Server!");
                    return;
                }

                String status  = responseJson.get("status").getAsString();
                String message = responseJson.get("message").getAsString();

                if ("SUCCESS".equals(status)) {
                    showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đăng ký thành công! Vui lòng đăng nhập.");
                    handleBackToLogin(null);
                } else {
                    isUsernameAvailable = false;
                    lblUsernameError.setText("Tên đăng nhập đã tồn tại!");
                    lblUsernameError.setVisible(true);
                }
            });
        }).start();
    }

    @FXML
    private void handleBackToLogin(ActionEvent event) {
        // Sử dụng ViewManager để trở về Login, giữ nguyên 100% kích thước cửa sổ hiện tại
        com.nhom15.client.util.ViewManager.navigateTo(com.nhom15.client.util.ViewManager.Views.LOGIN);
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}