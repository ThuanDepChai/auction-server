package com.nhom15.client.controller;

import com.google.gson.JsonObject;
import com.nhom15.client.network.SocketClient;
import com.nhom15.client.util.SessionManager;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;

public class ProfileController {

    @FXML private Label lblAvatarInitial;
    @FXML private Label lblDisplayName;
    @FXML private Label lblRoleBadge;
    @FXML private Label lblBalanceAmount;
    @FXML private Label lblJoinDate;
    @FXML private Label lblInfoStatus;
    @FXML private Label lblPasswordStatus;
    @FXML private Label lblOldPassError;
    @FXML private Label lblNewPassError;
    @FXML private Label lblConfirmPassError;
    @FXML private VBox  cardUpgrade;

    @FXML private TextField     txtUsername;
    @FXML private TextField     txtFullName;
    @FXML private TextField     txtEmail;
    @FXML private TextField     txtPhone;
    @FXML private PasswordField txtOldPassword;
    @FXML private PasswordField txtNewPassword;
    @FXML private PasswordField txtConfirmPassword;

    @FXML
    public void initialize() {
        lblOldPassError.managedProperty().bind(lblOldPassError.visibleProperty());
        lblNewPassError.managedProperty().bind(lblNewPassError.visibleProperty());
        lblConfirmPassError.managedProperty().bind(lblConfirmPassError.visibleProperty());

        // Load thông tin đầy đủ từ server trước, sau đó mới fill form
        loadProfileFromServer();
    }

    private void loadProfileFromServer() {
        JsonObject data = new JsonObject();
        data.addProperty("userId", SessionManager.getUserId());

        JsonObject request = new JsonObject();
        request.addProperty("action", "GET_PROFILE");
        request.add("data", data);

        new Thread(() -> {
            JsonObject response = SocketClient.sendRequest(request);
            javafx.application.Platform.runLater(() -> {
                if (response != null && "SUCCESS".equals(response.get("status").getAsString())) {
                    JsonObject p = response.getAsJsonObject("profile");
                    // Lưu vào session
                    SessionManager.setProfileDetail(
                            p.get("fullName").getAsString(),
                            p.get("phone").getAsString(),
                            p.get("joinDate").getAsString()
                    );
                    SessionManager.updateBalance(p.get("balance").getAsDouble());
                }
                // Dù có lỗi hay không vẫn fill form với dữ liệu hiện có
                loadProfileData();
            });
        }).start();
    }

    private void loadProfileData() {
        String username = SessionManager.getUsername();
        String role     = SessionManager.getRole();

        // Avatar initial
        lblAvatarInitial.setText(username != null ?
                String.valueOf(username.charAt(0)).toUpperCase() : "U");

        // Tên hiển thị
        String fullName = SessionManager.getFullName();
        lblDisplayName.setText((fullName != null && !fullName.isEmpty()) ? fullName : username);

        // Role badge
        lblRoleBadge.setText(role != null ? role : "BIDDER");
        if ("SELLER".equals(role)) {
            lblRoleBadge.setStyle("-fx-background-color: #FFF3E0; -fx-text-fill: #F57C00; -fx-background-radius: 10; -fx-padding: 3 12 3 12; -fx-font-weight: bold;");
        } else if ("ADMIN".equals(role)) {
            lblRoleBadge.setStyle("-fx-background-color: #FCE4EC; -fx-text-fill: #C62828; -fx-background-radius: 10; -fx-padding: 3 12 3 12; -fx-font-weight: bold;");
        }

        // Số dư
        lblBalanceAmount.setText(String.format("%,.0fđ", SessionManager.getBalance()));

        // Ngày tham gia
        lblJoinDate.setText(SessionManager.getJoinDate() != null ?
                SessionManager.getJoinDate() : "---");

        // Điền form
        txtUsername.setText(username != null ? username : "");
        txtFullName.setText(fullName != null ? fullName : "");
        txtEmail.setText(SessionManager.getEmail() != null ? SessionManager.getEmail() : "");
        txtPhone.setText(SessionManager.getPhone() != null ? SessionManager.getPhone() : "");

        // Ẩn card nâng cấp nếu đã là Seller/Admin
        boolean hiddenUpgrade = "SELLER".equals(role) || "ADMIN".equals(role);
        cardUpgrade.setVisible(!hiddenUpgrade);
        cardUpgrade.setManaged(!hiddenUpgrade);
    }

    // ── Lưu thông tin cá nhân ────────────────────────────────────────────────

    @FXML
    private void handleSaveInfo(ActionEvent event) {
        String fullName = txtFullName.getText().trim();
        String email    = txtEmail.getText().trim();
        String phone    = txtPhone.getText().trim();

        if (email.isEmpty() || !email.matches("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$")) {
            showStatus(lblInfoStatus, "Email không hợp lệ!", false);
            return;
        }

        JsonObject data = new JsonObject();
        data.addProperty("userId",   SessionManager.getUserId());
        data.addProperty("fullName", fullName);
        data.addProperty("email",    email);
        data.addProperty("phone",    phone);

        JsonObject request = new JsonObject();
        request.addProperty("action", "UPDATE_PROFILE");
        request.add("data", data);

        new Thread(() -> {
            JsonObject response = SocketClient.sendRequest(request);
            javafx.application.Platform.runLater(() -> {
                if (response == null) {
                    showStatus(lblInfoStatus, "Lỗi kết nối!", false);
                    return;
                }
                if ("SUCCESS".equals(response.get("status").getAsString())) {
                    // Cập nhật session
                    SessionManager.updateProfile(fullName, email, phone);
                    lblDisplayName.setText(fullName.isEmpty() ?
                            SessionManager.getUsername() : fullName);
                    showStatus(lblInfoStatus, "✓ Lưu thành công!", true);
                } else {
                    showStatus(lblInfoStatus, response.get("message").getAsString(), false);
                }
            });
        }).start();
    }

    // ── Đổi mật khẩu ────────────────────────────────────────────────────────

    @FXML
    private void handleChangePassword(ActionEvent event) {
        String oldPass     = txtOldPassword.getText();
        String newPass     = txtNewPassword.getText();
        String confirmPass = txtConfirmPassword.getText();

        // Reset lỗi
        lblOldPassError.setVisible(false);
        lblNewPassError.setVisible(false);
        lblConfirmPassError.setVisible(false);
        lblPasswordStatus.setText("");

        boolean hasError = false;

        if (oldPass.isEmpty()) {
            lblOldPassError.setText("Vui lòng nhập mật khẩu hiện tại!");
            lblOldPassError.setVisible(true);
            hasError = true;
        }
        if (!newPass.matches("^(?=.*[A-Za-z])(?=.*\\d).{6,}$")) {
            lblNewPassError.setText("Mật khẩu phải từ 6 ký tự, có chữ và số!");
            lblNewPassError.setVisible(true);
            hasError = true;
        }
        if (!newPass.equals(confirmPass)) {
            lblConfirmPassError.setText("Mật khẩu xác nhận không khớp!");
            lblConfirmPassError.setVisible(true);
            hasError = true;
        }
        if (hasError) return;

        JsonObject data = new JsonObject();
        data.addProperty("userId",      SessionManager.getUserId());
        data.addProperty("oldPassword", oldPass);
        data.addProperty("newPassword", newPass);

        JsonObject request = new JsonObject();
        request.addProperty("action", "CHANGE_PASSWORD");
        request.add("data", data);

        new Thread(() -> {
            JsonObject response = SocketClient.sendRequest(request);
            javafx.application.Platform.runLater(() -> {
                if (response == null) {
                    showStatus(lblPasswordStatus, "Lỗi kết nối!", false);
                    return;
                }
                if ("SUCCESS".equals(response.get("status").getAsString())) {
                    txtOldPassword.clear();
                    txtNewPassword.clear();
                    txtConfirmPassword.clear();
                    showStatus(lblPasswordStatus, "✓ Đổi mật khẩu thành công!", true);
                } else {
                    lblOldPassError.setText("Mật khẩu hiện tại không đúng!");
                    lblOldPassError.setVisible(true);
                }
            });
        }).start();
    }


    // ── Đổi ảnh đại diện ────────────────────────────────────────────────────
    @FXML private javafx.scene.image.ImageView imgAvatar;

    /** Hiện ảnh thật, ẩn chữ cái đầu */
    private void setAvatarImage(javafx.scene.image.Image image) {
        imgAvatar.setImage(image);
        imgAvatar.setVisible(true);
        lblAvatarInitial.setVisible(false);
    }

    /** Gọi trong loadProfileData() nếu có avatarPath */
    private void loadAvatar(String avatarPath) {
        if (avatarPath == null || avatarPath.isEmpty()) return;
        try {
            java.io.File file = new java.io.File(avatarPath);
            if (file.exists()) {
                javafx.scene.image.Image image =
                        new javafx.scene.image.Image(file.toURI().toString());
                setAvatarImage(image);
            }
        } catch (Exception e) {
            System.err.println("Không load được avatar: " + e.getMessage());
        }
    }

    @FXML
    private void handleChangeAvatar(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Chọn ảnh đại diện");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg")
        );
        Stage stage = (Stage) txtUsername.getScene().getWindow();
        File file = fileChooser.showOpenDialog(stage);

        if (file == null) return;

        // Kiểm tra dung lượng tối đa 2MB
        if (file.length() > 2 * 1024 * 1024) {
            showAlert("Ảnh không được vượt quá 2MB!");
            return;
        }

        // Mở cửa sổ cắt ảnh
        openCropAvatarWindow(file);
    }

    private void openCropAvatarWindow(File file) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/CropAvatarView.fxml"));
            Parent root = loader.load();

            CropAvatarController cropController = loader.getController();

            // Khởi tạo ảnh và định nghĩa việc sẽ làm sau khi người dùng bấm "Cắt & Lưu"
            cropController.initImage(file, base64Image -> {
                // Gọi hàm gửi ảnh lên server
                uploadAvatarToServer(base64Image, file.getName());
            });

            Stage stage = new Stage();
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.initStyle(javafx.stage.StageStyle.UNDECORATED);
            stage.setScene(new Scene(root));
            stage.showAndWait();

        } catch (IOException e) {
            System.err.println("Lỗi khi mở cửa sổ cắt ảnh: " + e.getMessage());
            showStatus(lblInfoStatus, "Lỗi khi mở giao diện cắt", false);
        }
    }

    private void uploadAvatarToServer(String base64Image, String fileName) {
        try {
            showStatus(lblInfoStatus, "Đang tải ảnh lên...", true);
            String extension = fileName.toLowerCase().endsWith(".png") ? "png" : "jpg";

            // Hiện preview ngay lập tức từ chuỗi Base64 đã cắt
            byte[] imageBytes = java.util.Base64.getDecoder().decode(base64Image);
            java.io.ByteArrayInputStream bis = new java.io.ByteArrayInputStream(imageBytes);
            javafx.scene.image.Image previewImage = new javafx.scene.image.Image(bis);
            setAvatarImage(previewImage);

            // Gửi lên server
            JsonObject data = new JsonObject();
            data.addProperty("userId", SessionManager.getUserId());
            data.addProperty("imageBase64", base64Image);
            data.addProperty("extension", extension);

            JsonObject request = new JsonObject();
            request.addProperty("action", "UPDATE_AVATAR");
            request.add("data", data);

            new Thread(() -> {
                JsonObject response = SocketClient.sendRequest(request);
                javafx.application.Platform.runLater(() -> {
                    if (response == null) {
                        showStatus(lblInfoStatus, "Lỗi kết nối!", false);
                        return;
                    }
                    if ("SUCCESS".equals(response.get("status").getAsString())) {
                        String savedPath = response.get("avatarPath").getAsString();
                        SessionManager.updateAvatar(savedPath);
                        showStatus(lblInfoStatus, "✓ Cập nhật ảnh thành công!", true);
                    } else {
                        showStatus(lblInfoStatus, "Lỗi: " + response.get("message").getAsString(), false);
                    }
                });
            }).start();

        } catch (Exception e) {
            showStatus(lblInfoStatus, "Không thể xử lý ảnh!", false);
        }
    }

    // ── Nâng cấp lên Seller ──────────────────────────────────────────────────

    @FXML
    private void handleUpgradeToSeller(ActionEvent event) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Đăng ký bán hàng");
        confirm.setHeaderText("Bạn muốn trở thành Nhà bán hàng?");
        confirm.setContentText("Sau khi đăng ký, bạn có thể đăng sản phẩm lên Gemini và quản lý đơn hàng của mình.");
        confirm.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                JsonObject data = new JsonObject();
                data.addProperty("userId", SessionManager.getUserId());

                JsonObject request = new JsonObject();
                request.addProperty("action", "UPGRADE_TO_SELLER");
                request.add("data", data);

                new Thread(() -> {
                    JsonObject response = SocketClient.sendRequest(request);
                    javafx.application.Platform.runLater(() -> {
                        if (response != null &&
                                "SUCCESS".equals(response.get("status").getAsString())) {
                            SessionManager.updateRole("SELLER");
                            lblRoleBadge.setText("SELLER");
                            lblRoleBadge.setStyle(
                                    "-fx-background-color: #FFF3E0; -fx-text-fill: #F57C00; " +
                                            "-fx-background-radius: 10; -fx-padding: 3 12 3 12; -fx-font-weight: bold;"
                            );
                            cardUpgrade.setVisible(false);
                            cardUpgrade.setManaged(false);
                            showAlert("🎉 Chúc mừng! Bạn đã trở thành Nhà bán hàng trên Gemini.");
                        } else {
                            showAlert("Có lỗi xảy ra, vui lòng thử lại.");
                        }
                    });
                }).start();
            }
        });
    }

    // ── Điều hướng ───────────────────────────────────────────────────────────

    @FXML private void handleBack(ActionEvent event)    { navigateTo("/view/Home.fxml", "Trang chủ"); }
    @FXML private void handleGoHome(ActionEvent event)  { navigateTo("/view/Home.fxml", "Trang chủ"); }
    @FXML private void handleMyOrders(ActionEvent event){ showAlert("Tính năng đang phát triển!"); }

    @FXML
    private void handleSellerDashboard(ActionEvent event) {
        navigateTo("/view/seller_dashboard.fxml", "Quản lý bán hàng");
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        SessionManager.logout();
        navigateTo("/view/login.fxml", "Đăng nhập");
    }

    // ── Tiện ích ─────────────────────────────────────────────────────────────

    private void navigateTo(String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Stage stage = (Stage) txtUsername.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle(title);
            stage.centerOnScreen();
        } catch (IOException e) {
            showAlert("Không thể mở: " + title);
        }
    }

    private void showStatus(Label lbl, String msg, boolean success) {
        lbl.setText(msg);
        lbl.setTextFill(success ?
                javafx.scene.paint.Color.web("#4CAF50") :
                javafx.scene.paint.Color.web("#D96570"));
    }

    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Thông báo");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}