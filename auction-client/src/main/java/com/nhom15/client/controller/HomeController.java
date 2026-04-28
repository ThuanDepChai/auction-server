package com.nhom15.client.controller;

import com.nhom15.client.util.SessionManager;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;

public class HomeController {

    @FXML private Label lblUsername;
    @FXML private Label lblAvatarInitial;
    @FXML private VBox  userDropdown;
    @FXML private FlowPane flowProducts;
    @FXML private FlowPane flowAuctions;
    @FXML private Button btnSell;
    @FXML private Button btnSellerDashboard;

    @FXML
    public void initialize() {
        // Hiển thị tên + chữ cái đầu avatar
        if (SessionManager.isLoggedIn()) {
            String uname = SessionManager.getUsername();
            lblUsername.setText(uname);
            lblAvatarInitial.setText(String.valueOf(uname.charAt(0)).toUpperCase());
        } else {
            lblUsername.setText("Khách");
            lblAvatarInitial.setText("K");
        }

        // Ẩn "Quản lý bán hàng" nếu không phải Seller
        btnSellerDashboard.setVisible(SessionManager.isSeller());
        btnSellerDashboard.setManaged(SessionManager.isSeller());

        // Load sản phẩm (sau này kết nối server)
        loadProducts();
        loadAuctions();
    }

    // ── Dropdown user ────────────────────────────────────────────────────────

    @FXML
    private void handleUserMenu(ActionEvent event) {
        boolean showing = userDropdown.isVisible();
        userDropdown.setVisible(!showing);
        userDropdown.setManaged(!showing);
    }

    /** Đóng dropdown khi click ra ngoài — gọi từ BorderPane onMousePressed nếu cần */
    public void closeDropdown() {
        userDropdown.setVisible(false);
        userDropdown.setManaged(false);
    }

    // ── Điều hướng ───────────────────────────────────────────────────────────

    @FXML
    private void handleProfile(ActionEvent event) {
        closeDropdown();
        navigateTo("/view/profile.fxml", "Trang cá nhân");
    }

    @FXML
    private void handleMyOrders(ActionEvent event) {
        closeDropdown();
        // navigateTo("/view/my_orders.fxml", "Đơn hàng của tôi");
        showAlert("Tính năng đang phát triển!");
    }

    @FXML
    private void handleSellerDashboard(ActionEvent event) {
        closeDropdown();
        navigateTo("/view/seller_dashboard.fxml", "Quản lý bán hàng");
    }

    @FXML
    private void handleWallet(ActionEvent event) {
        closeDropdown();
        showAlert("Số dư ví: " + String.format("%,.0f", SessionManager.getBalance()) + "đ");
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        closeDropdown();
        SessionManager.logout();
        navigateTo("/view/login.fxml", "Đăng nhập");
    }

    @FXML
    private void handleSell(ActionEvent event) {
        navigateTo("/view/seller_dashboard.fxml", "Đăng bán sản phẩm");
    }

    @FXML
    private void handleSearch(ActionEvent event) {
        // TODO: gửi request tìm kiếm lên server
    }

    @FXML
    private void handleNotification(ActionEvent event) {
        showAlert("Chưa có thông báo mới.");
    }

    @FXML
    private void handleCart(ActionEvent event) {
        showAlert("Giỏ hàng trống.");
    }

    @FXML
    private void handleCategory(ActionEvent event) {
        // TODO: lọc sản phẩm theo danh mục
        Button src = (Button) event.getSource();
        System.out.println("Danh mục: " + src.getText());
    }

    // ── Load dữ liệu ─────────────────────────────────────────────────────────

    private void loadProducts() {
        // TODO: gửi GET_PRODUCTS lên server rồi render vào flowProducts
        flowProducts.getChildren().clear();
    }

    private void loadAuctions() {
        // TODO: gửi GET_AUCTIONS lên server rồi render vào flowAuctions
        flowAuctions.getChildren().clear();
    }

    // ── Tiện ích ─────────────────────────────────────────────────────────────

    private void navigateTo(String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Stage stage = (Stage) lblUsername.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle(title);
            stage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Không thể mở: " + title);
        }
    }

    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Thông báo");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}