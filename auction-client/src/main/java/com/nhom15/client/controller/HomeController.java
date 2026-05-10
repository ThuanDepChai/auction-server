package com.nhom15.client.controller;

import com.google.gson.JsonArray;
import com.nhom15.client.command.HomeCommand;
import com.nhom15.client.util.CardFactory;
import com.nhom15.client.util.SessionManager;
import com.nhom15.client.util.ViewManager;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;

import java.util.ArrayList;
import java.util.List;

public class HomeController {

    // ── FXML Fields ──────────────────────────────────────────────────────────
    @FXML private Label      lblUsername;
    @FXML private Label      lblAvatarInitial;
    @FXML private ImageView  imgAvatar;
    @FXML private VBox       userDropdown;
    @FXML private FlowPane   flowProducts;
    @FXML private FlowPane   flowAuctions;
    @FXML private Button     btnSellerDashboard;
    @FXML private TextField  txtSearch;
    @FXML private ComboBox<String> cmbCategory;
    @FXML private ScrollPane mainScrollPane;

    private final List<Timeline> countdownTimers = new ArrayList<>();

    // ── Khởi tạo ────────────────────────────────────────────────────────────
    @FXML
    public void initialize() {
        setupUserInfo();
        setupCategories();
        setupClickOutsideToCloseDropdown();
        loadData();
    }

    private void setupUserInfo() {
        // Nếu chưa đăng nhập, hiển thị giao diện Khách
        if (!SessionManager.isLoggedIn()) {
            lblUsername.setText("Khách");
            lblAvatarInitial.setText("K");
            btnSellerDashboard.setVisible(false);
            btnSellerDashboard.setManaged(false);
            return;
        }

        // Lấy thông tin từ SessionManager (đã được bọc an toàn chống Null)
        String username = SessionManager.getUsername();
        lblUsername.setText((username == null || username.trim().isEmpty()) ? "Người dùng" : username);

        // Lấy chữ cái đầu tiên làm Avatar mặc định
        String initial = lblUsername.getText().substring(0, 1).toUpperCase();
        lblAvatarInitial.setText(initial);

        // Hiển thị nút Seller Dashboard dựa trên quyền
        boolean isSellerMode = SessionManager.isSeller() || SessionManager.isAdmin();
        btnSellerDashboard.setVisible(isSellerMode);
        btnSellerDashboard.setManaged(isSellerMode);

        // Tải ảnh đại diện từ Server
        HomeCommand.fetchUserAvatar(SessionManager.getUserId(), base64 -> {
            CardFactory.setAvatar(base64, imgAvatar, lblAvatarInitial);
        });
    }

    private void setupCategories() {
        if (cmbCategory != null) {
            cmbCategory.getItems().setAll(
                    "Tất cả danh mục", "Điện tử", "Thời trang",
                    "Nhà cửa & Sân vườn", "Đồ sưu tầm", "Thể thao"
            );
            cmbCategory.setValue("Tất cả danh mục");
        }
    }

    private void setupClickOutsideToCloseDropdown() {
        Platform.runLater(() -> {
            if (userDropdown == null || userDropdown.getScene() == null) return;
            Scene scene = userDropdown.getScene();
            scene.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_PRESSED, e -> {
                if (userDropdown.isVisible() && !userDropdown.localToScene(userDropdown.getBoundsInLocal()).contains(e.getSceneX(), e.getSceneY())) {
                    userDropdown.setVisible(false);
                    userDropdown.setManaged(false);
                }
            });
        });
    }

    // ── Load Dữ Liệu ────────────────────────────────────────────────────────
    private void loadData() {
        HomeCommand.fetchFeaturedProducts(
                items -> populateFlowPane(flowProducts, items, true),
                () -> flowProducts.getChildren().setAll(CardFactory.buildEmptyLabel("Chưa có sản phẩm nổi bật"))
        );

        HomeCommand.fetchActiveAuctions(
                auctions -> populateFlowPane(flowAuctions, auctions, false),
                () -> flowAuctions.getChildren().setAll(CardFactory.buildEmptyLabel("Chưa có phiên đấu giá nào"))
        );
    }

    private void populateFlowPane(FlowPane pane, JsonArray dataArray, boolean isProduct) {
        if (pane == null) return;
        pane.getChildren().clear();

        if (!isProduct) {
            countdownTimers.forEach(Timeline::stop);
        }

        if (dataArray == null || dataArray.isEmpty()) {
            pane.getChildren().add(CardFactory.buildEmptyLabel("Không có dữ liệu"));
            return;
        }

        for (int i = 0; i < dataArray.size(); i++) {
            if (isProduct) {
                pane.getChildren().add(CardFactory.buildProductCard(dataArray.get(i).getAsJsonObject(), this::handleGoToProductDetail));
            } else {
                pane.getChildren().add(CardFactory.buildAuctionCard(dataArray.get(i).getAsJsonObject(), countdownTimers, this::handleGoToBidding));
            }
        }
    }

    // ── Xử lý Navigation & Nút bấm ──────────────────────────────────────────
    @FXML
    private void handleUserMenu(ActionEvent event) {
        if (userDropdown != null) {
            boolean isVisible = !userDropdown.isVisible();
            userDropdown.setVisible(isVisible);
            userDropdown.setManaged(isVisible);
        }
    }

    @FXML private void handleProfile(ActionEvent event) { ViewManager.navigateTo(ViewManager.Views.PROFILE); }
    @FXML private void handleMyOrders(ActionEvent event) { ViewManager.navigateTo(ViewManager.Views.MY_ORDERS); }
    @FXML private void handleCart(ActionEvent event) { ViewManager.navigateTo(ViewManager.Views.CART); }

    @FXML
    private void handleLogout(ActionEvent event) {
        countdownTimers.forEach(Timeline::stop);
        SessionManager.logout();
        ViewManager.navigateTo(ViewManager.Views.LOGIN);
    }

    @FXML
    private void handleSellerDashboard(ActionEvent event) {
        if (!SessionManager.isSeller() && !SessionManager.isAdmin()) {
            showAlert("Bạn cần đăng ký tài khoản Seller để sử dụng chức năng này!");
            return;
        }
        ViewManager.navigateTo(ViewManager.Views.SELLER_DASHBOARD);
    }

    @FXML
    private void handleSearch(ActionEvent event) {
        String keyword = txtSearch != null ? txtSearch.getText().trim() : "";
        String category = (cmbCategory != null && !"Tất cả danh mục".equals(cmbCategory.getValue()))
                ? cmbCategory.getValue() : "";

        HomeCommand.searchProducts(keyword, category,
                items -> populateFlowPane(flowProducts, items, true),
                () -> flowProducts.getChildren().setAll(CardFactory.buildEmptyLabel("Không tìm thấy sản phẩm"))
        );
        if (mainScrollPane != null) mainScrollPane.setVvalue(0);
    }

    @FXML
    private void handleNotification(ActionEvent event) {
        showAlert("Chưa có thông báo mới.");
    }

    @FXML
    private void handleWallet(ActionEvent event) {
        if (userDropdown != null) userDropdown.setVisible(false);
        try {
            double balance = SessionManager.getBalance();
            showAlert(String.format("Số dư ví: %,d đ", (long) balance));
        } catch (Exception e) {
            showAlert("Không thể lấy số dư ví. Vui lòng thử lại.");
        }
    }

    @FXML
    private void handleSell(ActionEvent event) {
        if (!SessionManager.isSeller() && !SessionManager.isAdmin()) {
            showAlert("Bạn cần đăng ký tài khoản Seller để đăng bán!\nVào Trang cá nhân → Đăng ký bán hàng.");
            return;
        }
        ViewManager.navigateTo(ViewManager.Views.SELLER_DASHBOARD);
    }

    @FXML
    private void handleCategory(ActionEvent event) {
        Button src = (Button) event.getSource();
        String raw = src.getText().trim();

        int i = 0;
        while (i < raw.length()) {
            int cp = raw.codePointAt(i);
            if (Character.isLetter(cp)) break;
            i += Character.charCount(cp);
        }
        String category = raw.substring(i).trim();

        if (cmbCategory != null) cmbCategory.setValue(category);

        HomeCommand.searchProducts("", category,
                items -> populateFlowPane(flowProducts, items, true),
                () -> flowProducts.getChildren().setAll(CardFactory.buildEmptyLabel("Không tìm thấy sản phẩm"))
        );
    }

    // ── Callbacks (Điều hướng chi tiết) ─────────────────────────────────────
    private void handleGoToProductDetail(int productId) {
        countdownTimers.forEach(Timeline::stop);
        ViewManager.navigateTo("/view/product_detail.fxml", "Chi tiết sản phẩm", c -> {
            if (c instanceof ProductDetailController) ((ProductDetailController) c).setProductId(productId);
        });
    }

    private void handleGoToBidding(int auctionId) {
        countdownTimers.forEach(Timeline::stop);
        ViewManager.navigateTo(ViewManager.Views.BIDDING_ROOM, "Phòng đấu giá #" + auctionId, c -> {
            if (c instanceof BiddingRoomController) ((BiddingRoomController) c).setAuctionId(auctionId);
        });
    }

    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, msg);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    // (Stub Interfaces)
    public interface ProductDetailController { void setProductId(int productId); }
    public interface BiddingRoomController { void setAuctionId(int auctionId); }
}