package com.nhom15.client.controller;

import com.google.gson.JsonObject;
import com.nhom15.client.command.AvatarCommand;
import com.nhom15.client.command.ProfileCommand;
import com.nhom15.client.command.ServerCommand;
import com.nhom15.client.util.SessionManager;
import com.nhom15.client.util.ViewManager;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

/**
 * Orchestrator: liên kết UI ↔ ProfileCommand / AvatarCommand / ViewManager.
 */
public class ProfileController {

  @FXML
  private Label lblAvatarInitial, lblDisplayName, lblRoleBadge;
  @FXML
  private Label lblBalanceAmount, lblJoinDate;
  @FXML
  private Label lblInfoStatus, lblPasswordStatus;
  @FXML
  private Label lblOldPassError, lblNewPassError, lblConfirmPassError;
  @FXML
  private VBox cardUpgrade;
  @FXML
  private ImageView imgAvatar;

  @FXML
  private TextField txtUsername, txtFullName, txtEmail, txtPhone;
  @FXML
  private PasswordField txtOldPassword, txtNewPassword, txtConfirmPassword;

  @FXML
  public void initialize() {
    for (Label l : new Label[]{lblOldPassError, lblNewPassError, lblConfirmPassError}) {
      l.managedProperty().bind(l.visibleProperty());
    }
    loadProfileFromServer();
  }

  private void loadProfileFromServer() {
    ProfileCommand.fetchProfile(SessionManager.getUserId(), response -> {
      if (ServerCommand.isSuccess(response)) {
        JsonObject p = response.getAsJsonObject("profile");
        SessionManager.updateRole(p.get("role").getAsString());
        SessionManager.setProfileDetail(
            safeGet(p, "fullName"), safeGet(p, "phone"), safeGet(p, "joinDate", "---"));
        SessionManager.updateBalance(p.get("balance").getAsDouble());
        // Fix: cập nhật email từ server vào session
        if (p.has("email") && !p.get("email").isJsonNull()) {
          SessionManager.updateProfile(
              safeGet(p, "fullName"),
              safeGet(p, "email"),
              safeGet(p, "phone")
          );
        }
      }
      fillForm();
    });
  }

  private void fillForm() {
    String username = SessionManager.getUsername();
    String fullName = SessionManager.getFullName();
    String role = SessionManager.getRole();

    lblAvatarInitial.setText(username != null
        ? String.valueOf(username.charAt(0)).toUpperCase() : "U");
    lblDisplayName.setText(notEmpty(fullName) ? fullName : username);
    lblRoleBadge.setText(role != null ? role : "BIDDER");
    applyRoleBadge(role);
    lblBalanceAmount.setText(String.format("%,.0fđ", SessionManager.getBalance()));
    lblJoinDate.setText(notEmpty(SessionManager.getJoinDate())
        ? SessionManager.getJoinDate() : "---");

    txtUsername.setText(safeStr(username));
    txtFullName.setText(safeStr(fullName));
    txtEmail.setText(safeStr(SessionManager.getEmail()));
    txtPhone.setText(safeStr(SessionManager.getPhone()));

    boolean isSeller = "SELLER".equals(role) || "ADMIN".equals(role);
    cardUpgrade.setVisible(!isSeller);
    cardUpgrade.setManaged(!isSeller);

    // Load avatar từ server qua base64 (tránh lỗi path khác máy)
    ProfileCommand.fetchAvatar(SessionManager.getUserId(), avatarResp -> {
      if (avatarResp != null && "SUCCESS".equals(avatarResp.get("status").getAsString())) {
        setAvatarImage(AvatarCommand.decodeImage(avatarResp.get("imageBase64").getAsString()));
      } else {
        // Fallback: thử load từ path local nếu có
        setAvatarImage(AvatarCommand.loadFromPath(SessionManager.getAvatarPath()));
      }
    });
  }

  @FXML
  private void handleSaveInfo(ActionEvent event) {
    String fullName = txtFullName.getText().trim();
    String email = txtEmail.getText().trim();
    String phone = txtPhone.getText().trim();

    if (!email.matches("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$")) {
      showStatus(lblInfoStatus, "Email không hợp lệ!", false);
      return;
    }
    ProfileCommand.updateProfile(SessionManager.getUserId(), fullName, email, phone, response -> {
      if (ServerCommand.isSuccess(response)) {
        SessionManager.updateProfile(fullName, email, phone);
        lblDisplayName.setText(notEmpty(fullName) ? fullName : SessionManager.getUsername());
        showStatus(lblInfoStatus, "✓ Lưu thành công!", true);
      } else {
        showStatus(lblInfoStatus, ServerCommand.getMessage(response, "Lưu thất bại!"), false);
      }
    });
  }

  @FXML
  private void handleChangePassword(ActionEvent event) {
    String oldPass = txtOldPassword.getText();
    String newPass = txtNewPassword.getText();
    String confirm = txtConfirmPassword.getText();

    lblOldPassError.setVisible(false);
    lblNewPassError.setVisible(false);
    lblConfirmPassError.setVisible(false);
    lblPasswordStatus.setText("");

    boolean err = false;
    if (oldPass.isEmpty()) {
      setLabel(lblOldPassError, "Vui lòng nhập mật khẩu hiện tại!");
      err = true;
    }
    if (!newPass.matches("^(?=.*[A-Za-z])(?=.*\\d).{6,}$")) {
      setLabel(lblNewPassError, "Mật khẩu phải từ 6 ký tự, có chữ và số!");
      err = true;
    }
    if (!newPass.equals(confirm)) {
      setLabel(lblConfirmPassError, "Mật khẩu xác nhận không khớp!");
      err = true;
    }
    if (err) {
      return;
    }

    ProfileCommand.changePassword(SessionManager.getUserId(), oldPass, newPass, response -> {
      if (ServerCommand.isSuccess(response)) {
        txtOldPassword.clear();
        txtNewPassword.clear();
        txtConfirmPassword.clear();
        showStatus(lblPasswordStatus, "✓ Đổi mật khẩu thành công!", true);
      } else {
        setLabel(lblOldPassError, "Mật khẩu hiện tại không đúng!");
      }
    });
  }

  @FXML
  private void handleChangeAvatar(ActionEvent event) {
    AvatarCommand.pickAndUpload(
        SessionManager.getUserId(),
        base64 -> setAvatarImage(AvatarCommand.decodeImage(base64)), // preview
        msg -> showStatus(lblInfoStatus, msg, msg.startsWith("✓")) // status
    );
  }

  @FXML
  private void handleUpgradeToSeller(ActionEvent event) {
    Alert dlg = new Alert(Alert.AlertType.CONFIRMATION);
    dlg.setTitle("Đăng ký bán hàng");
    dlg.setHeaderText("Bạn muốn trở thành Nhà bán hàng?");
    dlg.setContentText("Sau khi đăng ký, bạn có thể đăng sản phẩm và quản lý đơn hàng.");
    dlg.showAndWait().ifPresent(r -> {
      if (r != ButtonType.OK) {
        return;
      }
      ProfileCommand.upgradeToSeller(SessionManager.getUserId(),
          ok -> {
            SessionManager.updateRole("SELLER");
            lblRoleBadge.setText("SELLER");
            applyRoleBadge("SELLER");
            cardUpgrade.setVisible(false);
            cardUpgrade.setManaged(false);
            showAlert("🎉 Chúc mừng! Bạn đã trở thành Nhà bán hàng.");
          },
          () -> showAlert("Có lỗi xảy ra, vui lòng thử lại.")
      );
    });
  }

  @FXML
  private void handleBack(ActionEvent e) {
    ViewManager.navigateTo(ViewManager.Views.HOME);
  }

  @FXML
  private void handleGoHome(ActionEvent e) {
    ViewManager.navigateTo(ViewManager.Views.HOME);
  }

  @FXML
  private void handleSellerDashboard(ActionEvent e) {
    ViewManager.navigateTo(ViewManager.Views.SELLER_DASHBOARD);
  }

  @FXML
  private void handleMyOrders(ActionEvent e) {
    showAlert("Tính năng đang phát triển!");
  }

  @FXML
  private void handleLogout(ActionEvent e) {
    SessionManager.logout();
    ViewManager.navigateTo(ViewManager.Views.LOGIN);
  }

  // ── UI helpers — controller tự quản lý UI của mình ───────────────────────

  private void setAvatarImage(Image image) {
    if (image == null || image.isError()) {
      return;
    }
    imgAvatar.setImage(image);
    imgAvatar.setVisible(true);
    lblAvatarInitial.setVisible(false);
  }

  private void applyRoleBadge(String role) {
    String s = switch (role != null ? role : "") {
      case "SELLER" -> "-fx-background-color:#FFF3E0;-fx-text-fill:#F57C00;";
      case "ADMIN" -> "-fx-background-color:#FCE4EC;-fx-text-fill:#C62828;";
      default -> "-fx-background-color:#E8F5E9;-fx-text-fill:#388E3C;";
    };
    lblRoleBadge.setStyle(
        s + "-fx-background-radius:10;-fx-padding:3 12 3 12;-fx-font-weight:bold;");
  }

  private void showStatus(Label lbl, String msg, boolean ok) {
    lbl.setText(msg);
    lbl.setTextFill(ok ? Color.web("#4CAF50") : Color.web("#D96570"));
  }

  private void setLabel(Label lbl, String msg) {
    lbl.setText(msg);
    lbl.setVisible(true);
  }

  private void showAlert(String msg) {
    Alert a = new Alert(Alert.AlertType.INFORMATION);
    a.setTitle("Thông báo");
    a.setHeaderText(null);
    a.setContentText(msg);
    a.showAndWait();
  }

  private String safeStr(String s) {
    return s != null ? s : "";
  }

  private boolean notEmpty(String s) {
    return s != null && !s.isEmpty();
  }

  private String safeGet(JsonObject o, String k) {
    return safeGet(o, k, "");
  }

  private String safeGet(JsonObject o, String k, String def) {
    return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsString() : def;
  }
}