package com.nhom15.client.controller;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.nhom15.client.model.UserDTO;
import com.nhom15.client.network.SocketClient;
import com.nhom15.client.util.FormValidator; // Nhớ import Class kiểm tra lúc nãy
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label; // Thêm Label
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

public class RegisterController {

    @FXML private TextField txtUsername;
    @FXML private TextField txtEmail;
    @FXML private PasswordField txtPassword;
    @FXML private PasswordField txtConfirmPassword;

    // THÊM 3 LABEL BÁO LỖI VÀO ĐÂY
    @FXML private Label lblEmailError;
    @FXML private Label lblPasswordError;
    @FXML private Label lblConfirmError;

    @FXML private Button btnRegister;
    @FXML private Button btnBackToLogin;

    @FXML
    public void initialize() {
        // 1. Phép thuật "co giãn" khoảng cách khi báo lỗi
        lblEmailError.managedProperty().bind(lblEmailError.visibleProperty());
        lblPasswordError.managedProperty().bind(lblPasswordError.visibleProperty());
        lblConfirmError.managedProperty().bind(lblConfirmError.visibleProperty());

        // 2. Gắn bộ kiểm tra Regex (tự động hiện lỗi sau 1.2s)
        String emailRegex = "^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$";
        FormValidator.bindRegex(txtEmail, lblEmailError, emailRegex, "Email không hợp lệ (vd: abc@gmail.com)");

        String passRegex = "^(?=.*\\d)(?=.*[a-zA-Z])(?=.*[!@#$%^&*]).{8,}$";
        FormValidator.bindRegex(txtPassword, lblPasswordError, passRegex, "Mật khẩu ≥8 ký tự, gồm số, chữ và ký tự đặc biệt!");

        FormValidator.bindMatch(txtPassword, txtConfirmPassword, lblConfirmError, "Mật khẩu xác nhận không khớp!");
    }

    @FXML
    private void handleRegister(ActionEvent event) {
        String username = txtUsername.getText();
        String email = txtEmail.getText();
        String password = txtPassword.getText();
        String confirmPassword = txtConfirmPassword.getText();

        // 1. Kiểm tra không được để trống (Cái này phải check trước tiên)
        if (username.trim().isEmpty() || email.trim().isEmpty() ||
                password.trim().isEmpty() || confirmPassword.trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Thiếu thông tin", "Vui lòng điền đầy đủ tất cả các trường!");
            return;
        }

        // 2. Kiểm tra xem có Label đỏ nào đang hiện không? Nếu CÓ thì KHÔNG CHO GỬI LÊN SERVER
        if (lblEmailError.isVisible() || lblPasswordError.isVisible() || lblConfirmError.isVisible()) {
            showAlert(Alert.AlertType.ERROR, "Lỗi dữ liệu", "Vui lòng sửa các lỗi đỏ trên màn hình trước khi đăng ký!");
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
                btnRegister.setText("XÁC NHẬN ĐĂNG KÝ");

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
}