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
        String email = txtEmail.getText();
        String password = txtPassword.getText();

        // Kiểm tra tính hợp lệ cơ bản
        if (username.trim().isEmpty() || password.trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Thiếu thông tin", "Vui lòng nhập đầy đủ Tên đăng nhập và Mật khẩu!");
            return;
        }

        // TODO: Viết logic kết nối Database hoặc kiểm tra tài khoản thực tế ở đây

        // Giả lập đăng nhập thành công
        if (username.equals("admin") && password.equals("123456")) {
            showAlert(Alert.AlertType.INFORMATION, "Thành công", "Chào mừng " + username + " đã quay trở lại Gemini!");
            // Code chuyển sang màn hình chính (Main Menu) sẽ nằm ở đây
        } else {
            showAlert(Alert.AlertType.ERROR, "Lỗi đăng nhập", "Tên đăng nhập hoặc mật khẩu không chính xác!");
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