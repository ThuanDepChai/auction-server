package com.nhom15.client.controller;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.nhom15.client.command.LoginCommand;
import com.nhom15.client.command.ServerCommand;
import com.nhom15.client.model.UserDTO;
import com.nhom15.client.util.FormValidator;
import com.nhom15.client.util.SessionManager;
import com.nhom15.client.util.ViewManager;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.Pane;

public class LoginController {

    @FXML private Pane          bgAnimationPane;
    @FXML private TextField     txtUsername;
    @FXML private TextField     txtEmail;
    @FXML private PasswordField txtPassword;

    @FXML private Label         lblUsernameError;
    @FXML private Label         lblEmailError;
    @FXML private Label         lblPasswordError;

    @FXML private Button        btnLogin;
    @FXML private Button        btnRegister;

    @FXML
    public void initialize() {
        lblUsernameError.managedProperty().bind(lblUsernameError.visibleProperty());
        lblEmailError.managedProperty().bind(lblEmailError.visibleProperty());
        lblPasswordError.managedProperty().bind(lblPasswordError.visibleProperty());

        FormValidator.bindRegex(txtUsername, lblUsernameError, "^.+$", "Vui lòng nhập tên đăng nhập!");
        FormValidator.bindRegex(txtEmail, lblEmailError, "^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$", "Email không hợp lệ!");
        FormValidator.bindRegex(txtPassword, lblPasswordError, "^.+$", "Vui lòng nhập mật khẩu!");

        loadBackground();
    }

    private void loadBackground() {
        try {
            Pane sharedBg = com.nhom15.client.util.BackgroundEngine.getSharedPane();
            if (sharedBg.getParent() != null && sharedBg.getParent() instanceof Pane) {
                ((Pane) sharedBg.getParent()).getChildren().remove(sharedBg);
            }

            if (bgAnimationPane != null) {
                bgAnimationPane.getChildren().add(0, sharedBg);
                sharedBg.prefWidthProperty().bind(bgAnimationPane.widthProperty());
                sharedBg.prefHeightProperty().bind(bgAnimationPane.heightProperty());
            }
        } catch (Exception e) {
            System.err.println("[LoginController] Lỗi load nền: " + e.getMessage());
        }
    }

    @FXML
    private void handleLogin(ActionEvent event) {
        String username = txtUsername.getText().trim();
        String email    = txtEmail.getText().trim();
        String password = txtPassword.getText();

        boolean hasError = false;
        if (username.isEmpty()) { lblUsernameError.setVisible(true); hasError = true; }
        if (email.isEmpty())    { lblEmailError.setVisible(true);    hasError = true; }
        if (password.isEmpty()) { lblPasswordError.setVisible(true); hasError = true; }

        if (hasError || lblUsernameError.isVisible() || lblEmailError.isVisible() || lblPasswordError.isVisible()) {
            return;
        }

        setLoading(true);

        new LoginCommand(username, email, password).executeAsync(
                response -> Platform.runLater(() -> onLoginResponse(response)),
                ()       -> Platform.runLater(() -> {
                    setLoading(false);
                    showAlert(Alert.AlertType.ERROR, "Lỗi kết nối", "Không thể kết nối đến Server!");
                })
        );
    }

    private void onLoginResponse(JsonObject response) {
        setLoading(false);

        if (ServerCommand.isSuccess(response)) {
            try {
                // SỨC MẠNH CỦA CLEAN CODE Ở ĐÂY:
                // Biến đổi JsonObject "user" từ Server gửi về thẳng thành UserDTO
                Gson gson = new Gson();
                UserDTO user = gson.fromJson(response.getAsJsonObject("user"), UserDTO.class);

                // Fix: Server trả về "id" nhưng UserDTO map "user_id" → set thủ công
                if (user.getUserId() == 0 && response.has("userId")) {
                    user.setUserId(response.get("userId").getAsInt());
                }
                // Fix: Server trả về role ở ngoài response, không trong "user"
                if ((user.getRole() == null || user.getRole().isEmpty()) && response.has("role")) {
                    user.setRole(response.get("role").getAsString());
                }

                // Ném vào SessionManager (Đã được cập nhật)
                SessionManager.login(user);

                ViewManager.navigateTo(ViewManager.Views.HOME);
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Lỗi dữ liệu", "Không thể đọc dữ liệu từ máy chủ!");
                System.err.println("[LoginController] Lỗi Parse JSON: " + e.getMessage());
            }
        } else {
            String message = response.has("message") ? response.get("message").getAsString() : "Sai tài khoản hoặc mật khẩu!";
            showAlert(Alert.AlertType.ERROR, "Đăng nhập thất bại", message);
        }
    }

    @FXML
    private void handleRegister(ActionEvent event) {
        ViewManager.navigateTo(ViewManager.Views.REGISTER);
    }

    private void setLoading(boolean loading) {
        btnLogin.setDisable(loading);
        btnLogin.setText(loading ? "Đang đăng nhập..." : "ĐĂNG NHẬP");
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}