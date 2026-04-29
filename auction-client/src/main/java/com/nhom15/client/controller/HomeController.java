package com.nhom15.client.controller;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.nhom15.client.network.SocketClient;
import com.nhom15.client.util.SessionManager;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class HomeController {

    @FXML private Label      lblUsername;
    @FXML private Label      lblAvatarInitial;
    @FXML private StackPane  avatarPane;
    @FXML private ImageView  imgAvatar;
    @FXML private VBox       userDropdown;
    @FXML private FlowPane   flowProducts;
    @FXML private FlowPane   flowAuctions;
    @FXML private Button     btnSellerDashboard;
    @FXML private TextField  txtSearch;
    @FXML private ComboBox<String> cmbCategory;
    @FXML private ScrollPane mainScrollPane;

    // Giữ danh sách Timeline đếm ngược để dừng khi rời màn hình
    private final List<Timeline> countdownTimers = new ArrayList<>();

    // ── Khởi tạo ────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        setupUserInfo();
        setupCategories();
        setupClickOutsideToCloseDropdown();
        loadData();
    }

    /** Hiển thị thông tin user trên navbar */
    private void setupUserInfo() {
        if (!SessionManager.isLoggedIn()) {
            lblUsername.setText("Khách");
            lblAvatarInitial.setText("K");
            return;
        }

        String username = SessionManager.getUsername();
        lblUsername.setText(username);
        lblAvatarInitial.setText(String.valueOf(username.charAt(0)).toUpperCase());

        // Ẩn "Quản lý bán hàng" nếu không phải Seller/Admin
        if (btnSellerDashboard != null) {
            btnSellerDashboard.setVisible(SessionManager.isSeller() || SessionManager.isAdmin());
            btnSellerDashboard.setManaged(SessionManager.isSeller() || SessionManager.isAdmin());
        }

        // Load avatar nếu có
        loadAvatarFromServer();
    }

    /** Load avatar từ server */
    private void loadAvatarFromServer() {
        if (SessionManager.getUserId() == 0) return;

        JsonObject data = new JsonObject();
        data.addProperty("userId", SessionManager.getUserId());

        JsonObject request = new JsonObject();
        request.addProperty("action", "GET_AVATAR");
        request.add("data", data);

        new Thread(() -> {
            JsonObject response = SocketClient.sendRequest(request);
            Platform.runLater(() -> {
                if (response == null) return;
                if ("SUCCESS".equals(response.get("status").getAsString())) {
                    String base64 = response.get("imageBase64").getAsString();
                    setAvatarFromBase64(base64);
                }
            });
        }).start();
    }

    /** Hiển thị ảnh avatar từ Base64 */
    private void setAvatarFromBase64(String base64) {
        try {
            byte[] bytes = Base64.getDecoder().decode(base64);
            Image image  = new Image(new ByteArrayInputStream(bytes));
            if (imgAvatar != null) {
                imgAvatar.setImage(image);
                imgAvatar.setVisible(true);
                lblAvatarInitial.setVisible(false);
            }
        } catch (Exception e) {
            System.err.println("Lỗi load avatar: " + e.getMessage());
        }
    }

    /** Đổ danh mục vào ComboBox tìm kiếm */
    private void setupCategories() {
        cmbCategory.getItems().addAll(
                "Tất cả danh mục",
                "Điện tử",
                "Thời trang",
                "Nhà cửa & Sân vườn",
                "Đồ sưu tầm",
                "Thể thao"
        );
        cmbCategory.setValue("Tất cả danh mục");
    }

    /** Đóng dropdown khi click ra ngoài */
    private void setupClickOutsideToCloseDropdown() {
        // Gắn vào scene sau khi scene đã sẵn sàng
        javafx.application.Platform.runLater(() -> {
            if (lblUsername.getScene() != null) {
                lblUsername.getScene().addEventFilter(
                        javafx.scene.input.MouseEvent.MOUSE_PRESSED, e -> {
                            if (userDropdown.isVisible()) {
                                if (!userDropdown.getBoundsInParent().contains(
                                        userDropdown.sceneToLocal(e.getSceneX(), e.getSceneY()))) {
                                    closeDropdown();
                                }
                            }
                        }
                );
            }
        });
    }

    // ── Load dữ liệu ────────────────────────────────────────────────────────

    private void loadData() {
        loadFeaturedProducts();
        loadActiveAuctions();
    }

    private void loadFeaturedProducts() {
        JsonObject request = new JsonObject();
        request.addProperty("action", "GET_FEATURED_PRODUCTS");

        new Thread(() -> {
            JsonObject response = SocketClient.sendRequest(request);
            Platform.runLater(() -> {
                flowProducts.getChildren().clear();
                if (response == null || !"SUCCESS".equals(response.get("status").getAsString())) {
                    flowProducts.getChildren().add(buildEmptyLabel("Chưa có sản phẩm nổi bật"));
                    return;
                }
                JsonArray items = response.getAsJsonArray("items");
                if (items == null || items.size() == 0) {
                    flowProducts.getChildren().add(buildEmptyLabel("Chưa có sản phẩm nổi bật"));
                    return;
                }
                for (int i = 0; i < items.size(); i++) {
                    JsonObject item = items.get(i).getAsJsonObject();
                    flowProducts.getChildren().add(buildProductCard(item));
                }
            });
        }).start();
    }

    private void loadActiveAuctions() {
        JsonObject request = new JsonObject();
        request.addProperty("action", "GET_ACTIVE_AUCTIONS");

        new Thread(() -> {
            JsonObject response = SocketClient.sendRequest(request);
            Platform.runLater(() -> {
                flowAuctions.getChildren().clear();
                // Dừng tất cả đồng hồ cũ
                countdownTimers.forEach(Timeline::stop);
                countdownTimers.clear();

                if (response == null || !"SUCCESS".equals(response.get("status").getAsString())) {
                    flowAuctions.getChildren().add(buildEmptyLabel("Chưa có phiên đấu giá nào"));
                    return;
                }
                JsonArray auctions = response.getAsJsonArray("auctions");
                if (auctions == null || auctions.size() == 0) {
                    flowAuctions.getChildren().add(buildEmptyLabel("Chưa có phiên đấu giá nào"));
                    return;
                }
                for (int i = 0; i < auctions.size(); i++) {
                    JsonObject auction = auctions.get(i).getAsJsonObject();
                    flowAuctions.getChildren().add(buildAuctionCard(auction));
                }
            });
        }).start();
    }

    // ── Tạo Card UI ─────────────────────────────────────────────────────────

    /** Card sản phẩm thông thường */
    private VBox buildProductCard(JsonObject item) {
        String name     = item.has("name")     ? item.get("name").getAsString()     : "Sản phẩm";
        String price    = item.has("price")    ? item.get("price").getAsString()    : "---";
        String sold     = item.has("sold")     ? item.get("sold").getAsString()     : "0";
        String imgBase64= item.has("imageBase64") ? item.get("imageBase64").getAsString() : null;

        VBox card = new VBox();
        card.setPrefWidth(210);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 10; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 10, 0, 0, 4); " +
                "-fx-cursor: hand;");

        // Ảnh sản phẩm
        StackPane imgContainer = new StackPane();
        imgContainer.setPrefHeight(160);
        imgContainer.setStyle("-fx-background-color: #F4F7FC; -fx-background-radius: 10 10 0 0;");

        if (imgBase64 != null && !imgBase64.isEmpty()) {
            try {
                byte[] bytes = Base64.getDecoder().decode(imgBase64);
                ImageView iv = new ImageView(new Image(new ByteArrayInputStream(bytes)));
                iv.setFitWidth(210);
                iv.setFitHeight(160);
                iv.setPreserveRatio(true);
                imgContainer.getChildren().add(iv);
            } catch (Exception e) {
                imgContainer.getChildren().add(buildImgPlaceholder());
            }
        } else {
            imgContainer.getChildren().add(buildImgPlaceholder());
        }

        // Thông tin
        VBox info = new VBox(5);
        info.setStyle("-fx-padding: 10 12 12 12;");

        Label lblName = new Label(name);
        lblName.setWrapText(true);
        lblName.setStyle("-fx-font-size: 13px;");

        Label lblPrice = new Label(formatPrice(price) + "đ");
        lblPrice.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #D96570;");

        Label lblSold = new Label("Đã bán " + sold);
        lblSold.setStyle("-fx-font-size: 11px; -fx-text-fill: #888888;");

        info.getChildren().addAll(lblName, lblPrice, lblSold);
        card.getChildren().addAll(imgContainer, info);

        // Hover effect
        card.setOnMouseEntered(e -> card.setStyle(card.getStyle() +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.18), 15, 0, 0, 6);"));
        card.setOnMouseExited(e -> card.setStyle(
                "-fx-background-color: white; -fx-background-radius: 10; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 10, 0, 0, 4); -fx-cursor: hand;"));

        return card;
    }

    /** Card đấu giá có đồng hồ đếm ngược */
    private VBox buildAuctionCard(JsonObject auction) {
        String name     = auction.has("name")        ? auction.get("name").getAsString()        : "Sản phẩm";
        String curPrice = auction.has("currentPrice") ? auction.get("currentPrice").getAsString() : "---";
        String endTime  = auction.has("endTime")      ? auction.get("endTime").getAsString()      : null;
        String imgBase64= auction.has("imageBase64")  ? auction.get("imageBase64").getAsString()  : null;
        int    auctionId= auction.has("auctionId")    ? auction.get("auctionId").getAsInt()       : -1;

        VBox card = new VBox();
        card.setPrefWidth(210);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 10; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 10, 0, 0, 4); -fx-cursor: hand;");

        // Ảnh
        StackPane imgContainer = new StackPane();
        imgContainer.setPrefHeight(150);
        imgContainer.setStyle("-fx-background-color: #F4F7FC; -fx-background-radius: 10 10 0 0;");

        if (imgBase64 != null && !imgBase64.isEmpty()) {
            try {
                byte[] bytes = Base64.getDecoder().decode(imgBase64);
                ImageView iv = new ImageView(new Image(new ByteArrayInputStream(bytes)));
                iv.setFitWidth(210);
                iv.setFitHeight(150);
                iv.setPreserveRatio(true);
                imgContainer.getChildren().add(iv);
            } catch (Exception e) {
                imgContainer.getChildren().add(buildImgPlaceholder());
            }
        } else {
            imgContainer.getChildren().add(buildImgPlaceholder());
        }

        // Thông tin
        VBox info = new VBox(5);
        info.setStyle("-fx-padding: 10 12 12 12;");

        Label lblName = new Label(name);
        lblName.setWrapText(true);
        lblName.setStyle("-fx-font-size: 13px;");

        Label lblPrice = new Label("Giá hiện tại: " + formatPrice(curPrice) + "đ");
        lblPrice.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #4285F4;");

        // Đồng hồ đếm ngược
        Label lblCountdown = new Label("⏰ --:--:--");
        lblCountdown.setStyle("-fx-font-size: 12px; -fx-text-fill: #D96570; -fx-font-weight: bold;");
        if (endTime != null) {
            startCountdown(lblCountdown, endTime);
        }

        // Nút đấu giá
        Button btnBid = new Button("Đấu giá ngay");
        btnBid.setMaxWidth(Double.MAX_VALUE);
        btnBid.setStyle("-fx-background-color: linear-gradient(to right, #4285F4, #9B72CB); " +
                "-fx-background-radius: 8; -fx-text-fill: white; " +
                "-fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 6 0 6 0;");
        btnBid.setOnAction(e -> handleGoToBidding(auctionId));

        info.getChildren().addAll(lblName, lblPrice, lblCountdown, btnBid);
        card.getChildren().addAll(imgContainer, info);

        return card;
    }

    /** Đồng hồ đếm ngược realtime */
    private void startCountdown(Label label, String endTimeStr) {
        try {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            LocalDateTime endTime = LocalDateTime.parse(endTimeStr, fmt);

            Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
                LocalDateTime now = LocalDateTime.now();
                if (now.isAfter(endTime)) {
                    label.setText("⏰ Đã kết thúc");
                    label.setStyle("-fx-font-size: 12px; -fx-text-fill: #888888;");
                    return;
                }
                long hours   = ChronoUnit.HOURS.between(now, endTime);
                long minutes = ChronoUnit.MINUTES.between(now, endTime) % 60;
                long seconds = ChronoUnit.SECONDS.between(now, endTime) % 60;
                label.setText(String.format("⏰ %02d:%02d:%02d", hours, minutes, seconds));

                // Đổi màu đỏ khi còn < 1 giờ
                if (hours < 1) {
                    label.setStyle("-fx-font-size: 12px; -fx-text-fill: #D96570; -fx-font-weight: bold;");
                }
            }));
            timeline.setCycleCount(Timeline.INDEFINITE);
            timeline.play();
            countdownTimers.add(timeline);

        } catch (Exception e) {
            label.setText("⏰ ---");
        }
    }

    /** Label placeholder khi không có data */
    private Label buildEmptyLabel(String text) {
        Label lbl = new Label(text);
        lbl.setStyle("-fx-text-fill: #AAAAAA; -fx-font-size: 14px; -fx-padding: 20;");
        return lbl;
    }

    /** Placeholder ảnh */
    private Label buildImgPlaceholder() {
        Label lbl = new Label("🖼");
        lbl.setStyle("-fx-font-size: 36px; -fx-text-fill: #CCCCCC;");
        return lbl;
    }

    // ── Dropdown user ────────────────────────────────────────────────────────

    @FXML
    private void handleUserMenu(ActionEvent event) {
        boolean showing = userDropdown.isVisible();
        userDropdown.setVisible(!showing);
        userDropdown.setManaged(!showing);
    }

    private void closeDropdown() {
        userDropdown.setVisible(false);
        userDropdown.setManaged(false);
    }

    // ── Tìm kiếm ────────────────────────────────────────────────────────────

    @FXML
    private void handleSearch(ActionEvent event) {
        String keyword  = txtSearch.getText().trim();
        String category = cmbCategory.getValue();
        if ("Tất cả danh mục".equals(category)) category = "";

        final String finalCategory = category;

        JsonObject data = new JsonObject();
        data.addProperty("keyword",  keyword);
        data.addProperty("category", finalCategory);

        JsonObject request = new JsonObject();
        request.addProperty("action", "SEARCH_PRODUCTS");
        request.add("data", data);

        final String cat = finalCategory;
        new Thread(() -> {
            JsonObject response = SocketClient.sendRequest(request);
            Platform.runLater(() -> {
                flowProducts.getChildren().clear();
                if (response == null || !"SUCCESS".equals(response.get("status").getAsString())) {
                    flowProducts.getChildren().add(buildEmptyLabel("Không tìm thấy sản phẩm phù hợp"));
                    return;
                }
                JsonArray items = response.getAsJsonArray("items");
                if (items == null || items.size() == 0) {
                    flowProducts.getChildren().add(buildEmptyLabel("Không tìm thấy sản phẩm phù hợp"));
                    return;
                }
                for (int i = 0; i < items.size(); i++) {
                    flowProducts.getChildren().add(buildProductCard(items.get(i).getAsJsonObject()));
                }
                // Scroll lên đầu để thấy kết quả
                mainScrollPane.setVvalue(0);
            });
        }).start();
    }

    @FXML
    private void handleCategory(ActionEvent event) {
        Button src = (Button) event.getSource();
        // Lấy tên danh mục, bỏ emoji ở đầu
        String raw = src.getText().trim();
        String category = raw.replaceAll("^[^a-zA-ZÀ-ỹ]+", "").trim();

        cmbCategory.setValue(category);

        JsonObject data = new JsonObject();
        data.addProperty("keyword",  "");
        data.addProperty("category", category);

        JsonObject request = new JsonObject();
        request.addProperty("action", "SEARCH_PRODUCTS");
        request.add("data", data);

        new Thread(() -> {
            JsonObject response = SocketClient.sendRequest(request);
            Platform.runLater(() -> {
                flowProducts.getChildren().clear();
                if (response == null || !"SUCCESS".equals(response.get("status").getAsString())) {
                    flowProducts.getChildren().add(buildEmptyLabel("Chưa có sản phẩm trong danh mục này"));
                    return;
                }
                JsonArray items = response.getAsJsonArray("items");
                if (items == null || items.size() == 0) {
                    flowProducts.getChildren().add(buildEmptyLabel("Chưa có sản phẩm trong danh mục này"));
                    return;
                }
                for (int i = 0; i < items.size(); i++) {
                    flowProducts.getChildren().add(buildProductCard(items.get(i).getAsJsonObject()));
                }
                mainScrollPane.setVvalue(0);
            });
        }).start();
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
        showAlert("Tính năng đang phát triển!");
    }

    @FXML
    private void handleSellerDashboard(ActionEvent event) {
        closeDropdown();
        if (!SessionManager.isSeller() && !SessionManager.isAdmin()) {
            showAlert("Bạn cần đăng ký tài khoản Seller để sử dụng chức năng này!\nVào Trang cá nhân → Đăng ký bán hàng.");
            return;
        }
        navigateTo("/view/seller_dashboard.fxml", "Quản lý bán hàng");
    }

    @FXML
    private void handleWallet(ActionEvent event) {
        closeDropdown();
        showAlert("Số dư ví: " + formatPrice(String.valueOf((long) SessionManager.getBalance())) + "đ");
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        closeDropdown();
        countdownTimers.forEach(Timeline::stop);
        SessionManager.logout();
        navigateTo("/view/login.fxml", "Đăng nhập");
    }

    @FXML
    private void handleSell(ActionEvent event) {
        if (!SessionManager.isSeller() && !SessionManager.isAdmin()) {
            showAlert("Bạn cần đăng ký tài khoản Seller để đăng bán!\nVào Trang cá nhân → Đăng ký bán hàng.");
            return;
        }
        navigateTo("/view/seller_dashboard.fxml", "Đăng bán sản phẩm");
    }

    @FXML
    private void handleNotification(ActionEvent event) {
        showAlert("Chưa có thông báo mới.");
    }

    @FXML
    private void handleCart(ActionEvent event) {
        showAlert("Giỏ hàng trống.");
    }

    private void handleGoToBidding(int auctionId) {
        // TODO: truyền auctionId sang BiddingRoom
        navigateTo("/view/bidding_room.fxml", "Phòng đấu giá");
    }

    // ── Tiện ích ─────────────────────────────────────────────────────────────

    private void navigateTo(String fxmlPath, String title) {
        try {
            countdownTimers.forEach(Timeline::stop);
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

    private String formatPrice(String raw) {
        try {
            long val = Long.parseLong(raw.replaceAll("[^0-9]", ""));
            return String.format("%,d", val);
        } catch (Exception e) {
            return raw;
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