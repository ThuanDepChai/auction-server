package com.nhom15.client.controller;

import com.google.gson.JsonObject;
import com.nhom15.client.command.ProfileCommand;
import com.nhom15.client.util.SessionManager;
import com.nhom15.client.util.ViewManager;
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
    @FXML private javafx.scene.image.ImageView imgAvatar;

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

        loadProfileFromServer();
    }

    private void loadProfileFromServer() {
        ProfileCommand.fetchProfile(SessionManager.getUserId(), response -> {
            // --- THÊM DÒNG NÀY ĐỂ XEM SERVER TRẢ VỀ CÁI GÌ ---
            System.out.println("DEBUG CLIENT: Dữ liệu nhận từ Server: " + response);

            if (response != null && "SUCCESS".equals(response.get("status").getAsString())) {
                JsonObject p = response.getAsJsonObject("profile");

                // Ép cập nhật Role trực tiếp
                SessionManager.updateRole(p.get("role").getAsString());

                SessionManager.setProfileDetail(
                        p.has("fullName") && !p.get("fullName").isJsonNull() ? p.get("fullName").getAsString() : "",
                        p.has("phone") && !p.get("phone").isJsonNull() ? p.get("phone").getAsString() : "",
                        p.has("joinDate") && !p.get("joinDate").isJsonNull() ? p.get("joinDate").getAsString() : "---"
                );
                SessionManager.updateBalance(p.get("balance").getAsDouble());

                javafx.application.Platform.runLater(() -> loadProfileData());
            }
        });
    }

    private void loadProfileData() {
        String username = SessionManager.getUsername();
        String role     = SessionManager.getRole();
        String fullName = SessionManager.getFullName();

        lblAvatarInitial.setText(username != null ? String.valueOf(username.charAt(0)).toUpperCase() : "U");
        lblDisplayName.setText((fullName != null && !fullName.isEmpty()) ? fullName : username);

        lblRoleBadge.setText(role != null ? role : "BIDDER");
        if ("SELLER".equals(role)) {
            lblRoleBadge.setStyle("-fx-background-color: #FFF3E0; -fx-text-fill: #F57C00; -fx-background-radius: 10; -fx-padding: 3 12 3 12; -fx-font-weight: bold;");
        } else if ("ADMIN".equals(role)) {
            lblRoleBadge.setStyle("-fx-background-color: #FCE4EC; -fx-text-fill: #C62828; -fx-background-radius: 10; -fx-padding: 3 12 3 12; -fx-font-weight: bold;");
        }

        lblBalanceAmount.setText(String.format("%,.0fđ", SessionManager.getBalance()));
        lblJoinDate.setText(SessionManager.getJoinDate() != null ? SessionManager.getJoinDate() : "---");

        txtUsername.setText(username != null ? username : "");
        txtFullName.setText(fullName != null ? fullName : "");
        txtEmail.setText(SessionManager.getEmail() != null ? SessionManager.getEmail() : "");
        txtPhone.setText(SessionManager.getPhone() != null ? SessionManager.getPhone() : "");

        boolean hiddenUpgrade = "SELLER".equals(role) || "ADMIN".equals(role);
        cardUpgrade.setVisible(!hiddenUpgrade);
        cardUpgrade.setManaged(!hiddenUpgrade);

        // Gọi lại load Avatar nếu trong session đã lưu đường dẫn ảnh
        loadAvatar(SessionManager.getAvatarPath()); // Giả định SessionManager có hàm này
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

        ProfileCommand.updateProfile(SessionManager.getUserId(), fullName, email, phone, response -> {
            if (response == null) {
                showStatus(lblInfoStatus, "Lỗi kết nối!", false);
                return;
            }
            if ("SUCCESS".equals(response.get("status").getAsString())) {
                SessionManager.updateProfile(fullName, email, phone);
                lblDisplayName.setText(fullName.isEmpty() ? SessionManager.getUsername() : fullName);
                showStatus(lblInfoStatus, "✓ Lưu thành công!", true);
            } else {
                showStatus(lblInfoStatus, response.get("message").getAsString(), false);
            }
        });
    }

    // ── Đổi mật khẩu ────────────────────────────────────────────────────────
    @FXML
    private void handleChangePassword(ActionEvent event) {
        String oldPass     = txtOldPassword.getText();
        String newPass     = txtNewPassword.getText();
        String confirmPass = txtConfirmPassword.getText();

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

        ProfileCommand.changePassword(SessionManager.getUserId(), oldPass, newPass, response -> {
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
    }

    // ── Đổi ảnh đại diện ────────────────────────────────────────────────────
    private void setAvatarImage(javafx.scene.image.Image image) {
        imgAvatar.setImage(image);
        imgAvatar.setVisible(true);
        lblAvatarInitial.setVisible(false);
    }

    private void loadAvatar(String avatarPath) {
        if (avatarPath == null || avatarPath.isEmpty()) return;
        try {
            File file = new File(avatarPath);
            if (file.exists()) setAvatarImage(new javafx.scene.image.Image(file.toURI().toString()));
        } catch (Exception e) {
            System.err.println("Không load được avatar: " + e.getMessage());
        }
    }

    @FXML
    private void handleChangeAvatar(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Chọn ảnh đại diện");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg"));
        Stage stage = (Stage) txtUsername.getScene().getWindow();
        File file = fileChooser.showOpenDialog(stage);

        if (file == null) return;
        if (file.length() > 2 * 1024 * 1024) {
            showAlert("Ảnh không được vượt quá 2MB!");
            return;
        }
        openCropAvatarWindow(file);
    }

    private void openCropAvatarWindow(File file) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/CropAvatarView.fxml"));
            Parent root = loader.load();
            CropAvatarController cropController = loader.getController();

            cropController.initImage(file, base64Image -> uploadAvatarToServer(base64Image, file.getName()));

            Stage stage = new Stage();
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.initStyle(javafx.stage.StageStyle.UNDECORATED);
            stage.setScene(new Scene(root));
            stage.showAndWait();
        } catch (IOException e) {
            showStatus(lblInfoStatus, "Lỗi khi mở giao diện cắt", false);
        }
    }

    private void uploadAvatarToServer(String base64Image, String fileName) {
        try {
            showStatus(lblInfoStatus, "Đang tải ảnh lên...", true);
            String extension = fileName.toLowerCase().endsWith(".png") ? "png" : "jpg";

            // Hiển thị preview ngay lập tức
            byte[] imageBytes = java.util.Base64.getDecoder().decode(base64Image);
            setAvatarImage(new javafx.scene.image.Image(new java.io.ByteArrayInputStream(imageBytes)));

            ProfileCommand.updateAvatar(SessionManager.getUserId(), base64Image, extension, response -> {
                if (response == null) {
                    showStatus(lblInfoStatus, "Lỗi kết nối!", false);
                    return;
                }
                if ("SUCCESS".equals(response.get("status").getAsString())) {
                    String savedPath = response.get("avatarPath").getAsString();
                    SessionManager.updateAvatar(savedPath); // Cần đảm bảo hàm này có trong SessionManager
                    showStatus(lblInfoStatus, "✓ Cập nhật ảnh thành công!", true);
                } else {
                    showStatus(lblInfoStatus, "Lỗi: " + response.get("message").getAsString(), false);
                }
            });
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
                ProfileCommand.upgradeToSeller(SessionManager.getUserId(), success -> {
                    SessionManager.updateRole("SELLER");
                    lblRoleBadge.setText("SELLER");
                    lblRoleBadge.setStyle("-fx-background-color: #FFF3E0; -fx-text-fill: #F57C00; -fx-background-radius: 10; -fx-padding: 3 12 3 12; -fx-font-weight: bold;");
                    cardUpgrade.setVisible(false);
                    cardUpgrade.setManaged(false);
                    showAlert("🎉 Chúc mừng! Bạn đã trở thành Nhà bán hàng trên hệ thống.");
                }, () -> showAlert("Có lỗi xảy ra, vui lòng thử lại."));
            }
        });
    }

    // ── Điều hướng & Tiện ích ────────────────────────────────────────────────
    @FXML private void handleBack(ActionEvent event)    { ViewManager.navigateTo(ViewManager.Views.HOME); }
    @FXML private void handleGoHome(ActionEvent event)  { ViewManager.navigateTo(ViewManager.Views.HOME); }
    @FXML private void handleMyOrders(ActionEvent event){ showAlert("Tính năng đang phát triển!"); }
    @FXML private void handleSellerDashboard(ActionEvent event) { ViewManager.navigateTo(ViewManager.Views.SELLER_DASHBOARD); }
    @FXML private void handleLogout(ActionEvent event) {
        SessionManager.logout();
        ViewManager.navigateTo(ViewManager.Views.LOGIN);
    }

    private void showStatus(Label lbl, String msg, boolean success) {
        lbl.setText(msg);
        lbl.setTextFill(success ? javafx.scene.paint.Color.web("#4CAF50") : javafx.scene.paint.Color.web("#D96570"));
    }

    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Thông báo");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}