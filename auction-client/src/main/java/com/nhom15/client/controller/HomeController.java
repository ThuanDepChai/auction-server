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
import javafx.scene.Node;
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

    // ── FXML fields ──────────────────────────────────────────────────────────
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

    // Đếm ngược — giữ tham chiếu để stop khi rời màn hình
    private final List<Timeline> countdownTimers = new ArrayList<>();

    // DateTimeFormatter dùng chung, thread-safe (immutable)
    private static final DateTimeFormatter DT_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

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
            safeSetText(lblUsername, "Khách");
            safeSetText(lblAvatarInitial, "K");
            hideSeller();
            return;
        }

        String username = SessionManager.getUsername();
        if (username == null || username.isEmpty()) username = "Người dùng";

        safeSetText(lblUsername, username);
        safeSetText(lblAvatarInitial, String.valueOf(username.charAt(0)).toUpperCase());

        hideSeller(); // ẩn trước, show lại nếu đúng role
        if (btnSellerDashboard != null && (SessionManager.isSeller() || SessionManager.isAdmin())) {
            btnSellerDashboard.setVisible(true);
            btnSellerDashboard.setManaged(true);
        }

        loadAvatarFromServer();
    }

    private void hideSeller() {
        if (btnSellerDashboard != null) {
            btnSellerDashboard.setVisible(false);
            btnSellerDashboard.setManaged(false);
        }
    }

    /** Null-safe setText */
    private void safeSetText(Label label, String text) {
        if (label != null) label.setText(text);
    }

    /** Load avatar từ server (background thread) */
    private void loadAvatarFromServer() {
        long userId = SessionManager.getUserId();
        if (userId == 0) return;

        JsonObject data = new JsonObject();
        data.addProperty("userId", userId);

        JsonObject request = new JsonObject();
        request.addProperty("action", "GET_AVATAR");
        request.add("data", data);

        new Thread(() -> {
            try {
                JsonObject response = SocketClient.sendRequest(request);
                Platform.runLater(() -> {
                    if (response == null) return;
                    String status = getStrSafe(response, "status", "");
                    if ("SUCCESS".equals(status)) {
                        String base64 = getStrSafe(response, "imageBase64", "");
                        if (!base64.isEmpty()) setAvatarFromBase64(base64);
                    }
                });
            } catch (Exception e) {
                System.err.println("[HomeController] loadAvatar error: " + e.getMessage());
            }
        }, "thread-load-avatar").start();
    }

    /** Hiển thị ảnh avatar từ Base64 */
    private void setAvatarFromBase64(String base64) {
        try {
            byte[] bytes = Base64.getDecoder().decode(base64);
            Image image  = new Image(new ByteArrayInputStream(bytes));
            if (imgAvatar != null && !image.isError()) {
                imgAvatar.setImage(image);
                imgAvatar.setVisible(true);
                if (lblAvatarInitial != null) lblAvatarInitial.setVisible(false);
            }
        } catch (Exception e) {
            System.err.println("[HomeController] setAvatar error: " + e.getMessage());
        }
    }

    /** Đổ danh mục vào ComboBox */
    private void setupCategories() {
        if (cmbCategory == null) return;
        cmbCategory.getItems().setAll(
                "Tất cả danh mục",
                "Điện tử",
                "Thời trang",
                "Nhà cửa & Sân vườn",
                "Đồ sưu tầm",
                "Thể thao"
        );
        cmbCategory.setValue("Tất cả danh mục");
    }

    /**
     * Đóng dropdown khi user click ra ngoài vùng dropdown.
     * Sử dụng SceneGraph bounds-in-scene để tránh sai lệch tọa độ.
     */
    private void setupClickOutsideToCloseDropdown() {
        Platform.runLater(() -> {
            Scene scene = getScene();
            if (scene == null) return;

            scene.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_PRESSED, e -> {
                if (userDropdown == null || !userDropdown.isVisible()) return;

                // Kiểm tra xem điểm click có nằm trong bounds của dropdown không
                javafx.geometry.Bounds bounds = userDropdown.localToScene(
                        userDropdown.getBoundsInLocal());
                if (!bounds.contains(e.getSceneX(), e.getSceneY())) {
                    closeDropdown();
                }
            });
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
            try {
                JsonObject response = SocketClient.sendRequest(request);
                Platform.runLater(() -> {
                    if (flowProducts == null) return;
                    flowProducts.getChildren().clear();
                    if (!isSuccess(response)) {
                        flowProducts.getChildren().add(buildEmptyLabel("Chưa có sản phẩm nổi bật"));
                        return;
                    }
                    JsonArray items = response.getAsJsonArray("items");
                    if (items == null || items.size() == 0) {
                        flowProducts.getChildren().add(buildEmptyLabel("Chưa có sản phẩm nổi bật"));
                        return;
                    }
                    for (int i = 0; i < items.size(); i++) {
                        flowProducts.getChildren().add(
                                buildProductCard(items.get(i).getAsJsonObject()));
                    }
                });
            } catch (Exception e) {
                System.err.println("[HomeController] loadFeaturedProducts: " + e.getMessage());
            }
        }, "thread-featured").start();
    }

    private void loadActiveAuctions() {
        JsonObject request = new JsonObject();
        request.addProperty("action", "GET_ACTIVE_AUCTIONS");

        new Thread(() -> {
            try {
                JsonObject response = SocketClient.sendRequest(request);
                Platform.runLater(() -> {
                    if (flowAuctions == null) return;
                    flowAuctions.getChildren().clear();
                    stopAllCountdowns();

                    if (!isSuccess(response)) {
                        flowAuctions.getChildren().add(buildEmptyLabel("Chưa có phiên đấu giá nào"));
                        return;
                    }
                    JsonArray auctions = response.getAsJsonArray("auctions");
                    if (auctions == null || auctions.size() == 0) {
                        flowAuctions.getChildren().add(buildEmptyLabel("Chưa có phiên đấu giá nào"));
                        return;
                    }
                    for (int i = 0; i < auctions.size(); i++) {
                        flowAuctions.getChildren().add(
                                buildAuctionCard(auctions.get(i).getAsJsonObject()));
                    }
                });
            } catch (Exception e) {
                System.err.println("[HomeController] loadActiveAuctions: " + e.getMessage());
            }
        }, "thread-auctions").start();
    }

    // ── Tạo Card UI ─────────────────────────────────────────────────────────

    /** Card sản phẩm thông thường */
    private VBox buildProductCard(JsonObject item) {
        String name      = getStrSafe(item, "name",     "Sản phẩm");
        String price     = getStrSafe(item, "price",    "0");
        String sold      = getStrSafe(item, "sold",     "0");
        int    productId = item.has("productId") ? item.get("productId").getAsInt() : -1;
        String imgBase64 = getStrSafe(item, "imageBase64", "");

        VBox card = new VBox();
        card.setPrefWidth(210);
        applyCardStyle(card, false);

        StackPane imgContainer = buildImageContainer(imgBase64, 160, "10 10 0 0");
        card.getChildren().add(imgContainer);

        VBox info = new VBox(5);
        info.setStyle("-fx-padding: 10 12 12 12;");

        Label lblName = new Label(name);
        lblName.setWrapText(true);
        lblName.setMaxWidth(186);
        lblName.setStyle("-fx-font-size: 13px; -fx-text-fill: #333333;");

        Label lblPrice = new Label(formatPrice(price) + "đ");
        lblPrice.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #D96570;");

        Label lblSold = new Label("Đã bán " + sold);
        lblSold.setStyle("-fx-font-size: 11px; -fx-text-fill: #888888;");

        info.getChildren().addAll(lblName, lblPrice, lblSold);
        card.getChildren().add(info);

        // Hover
        addHoverEffect(card);

        // Click → detail
        final int pid = productId;
        card.setOnMouseClicked(e -> handleGoToProductDetail(pid));

        return card;
    }

    /** Card đấu giá với đồng hồ đếm ngược */
    private VBox buildAuctionCard(JsonObject auction) {
        String name      = getStrSafe(auction, "name",         "Sản phẩm");
        String curPrice  = getStrSafe(auction, "currentPrice", "0");
        String endTime   = getStrSafe(auction, "endTime",      "");
        String imgBase64 = getStrSafe(auction, "imageBase64",  "");
        int    auctionId = auction.has("auctionId") ? auction.get("auctionId").getAsInt() : -1;

        VBox card = new VBox();
        card.setPrefWidth(210);
        applyCardStyle(card, false);

        StackPane imgContainer = buildImageContainer(imgBase64, 150, "10 10 0 0");
        card.getChildren().add(imgContainer);

        VBox info = new VBox(5);
        info.setStyle("-fx-padding: 10 12 12 12;");

        Label lblName = new Label(name);
        lblName.setWrapText(true);
        lblName.setMaxWidth(186);
        lblName.setStyle("-fx-font-size: 13px; -fx-text-fill: #333333;");

        Label lblPrice = new Label("Giá hiện tại: " + formatPrice(curPrice) + "đ");
        lblPrice.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #4285F4;");

        Label lblCountdown = new Label("⏰ --:--:--");
        lblCountdown.setStyle("-fx-font-size: 12px; -fx-text-fill: #D96570; -fx-font-weight: bold;");
        if (!endTime.isEmpty()) {
            startCountdown(lblCountdown, endTime);
        }

        Button btnBid = new Button("Đấu giá ngay");
        btnBid.setMaxWidth(Double.MAX_VALUE);
        btnBid.setStyle(
                "-fx-background-color: linear-gradient(to right, #4285F4, #9B72CB); " +
                        "-fx-background-radius: 8; -fx-text-fill: white; " +
                        "-fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 7 0 7 0;");
        final int aid = auctionId;
        btnBid.setOnAction(e -> handleGoToBidding(aid));

        info.getChildren().addAll(lblName, lblPrice, lblCountdown, btnBid);
        card.getChildren().add(info);

        return card;
    }

    // ── Helpers UI ──────────────────────────────────────────────────────────

    private void applyCardStyle(VBox card, boolean elevated) {
        String shadow = elevated
                ? "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.18), 15, 0, 0, 6);"
                : "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 10, 0, 0, 4);";
        card.setStyle(
                "-fx-background-color: white; -fx-background-radius: 10; " +
                        shadow + " -fx-cursor: hand;");
    }

    private void addHoverEffect(VBox card) {
        card.setOnMouseEntered(e -> applyCardStyle(card, true));
        card.setOnMouseExited(e  -> applyCardStyle(card, false));
    }

    /**
     * Tạo StackPane chứa ảnh sản phẩm từ Base64.
     * @param radius CSS border-radius string, ví dụ "10 10 0 0"
     */
    private StackPane buildImageContainer(String imgBase64, double height, String radius) {
        StackPane container = new StackPane();
        container.setPrefHeight(height);
        container.setStyle(
                "-fx-background-color: #F4F7FC; -fx-background-radius: " + radius + ";");

        if (imgBase64 != null && !imgBase64.isEmpty()) {
            try {
                byte[] bytes = Base64.getDecoder().decode(imgBase64);
                Image img = new Image(new ByteArrayInputStream(bytes));
                if (!img.isError()) {
                    ImageView iv = new ImageView(img);
                    iv.setFitWidth(210);
                    iv.setFitHeight(height);
                    iv.setPreserveRatio(true);
                    container.getChildren().add(iv);
                    return container;
                }
            } catch (Exception e) {
                // fall through to placeholder
            }
        }
        container.getChildren().add(buildImgPlaceholder());
        return container;
    }

    /** Đồng hồ đếm ngược realtime */
    private void startCountdown(Label label, String endTimeStr) {
        try {
            LocalDateTime endTime = LocalDateTime.parse(endTimeStr, DT_FMT);

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
                label.setStyle(
                        hours < 1
                                ? "-fx-font-size: 12px; -fx-text-fill: #D96570; -fx-font-weight: bold;"
                                : "-fx-font-size: 12px; -fx-text-fill: #E8A838; -fx-font-weight: bold;"
                );
            }));
            timeline.setCycleCount(Timeline.INDEFINITE);
            timeline.play();
            countdownTimers.add(timeline);

        } catch (Exception e) {
            System.err.println("[HomeController] countdown parse error: " + e.getMessage());
            label.setText("⏰ ---");
        }
    }

    private void stopAllCountdowns() {
        countdownTimers.forEach(Timeline::stop);
        countdownTimers.clear();
    }

    private Label buildEmptyLabel(String text) {
        Label lbl = new Label(text);
        lbl.setStyle("-fx-text-fill: #AAAAAA; -fx-font-size: 14px; -fx-padding: 20;");
        return lbl;
    }

    private Label buildImgPlaceholder() {
        Label lbl = new Label("🖼");
        lbl.setStyle("-fx-font-size: 36px; -fx-text-fill: #CCCCCC;");
        return lbl;
    }

    // ── Dropdown user ────────────────────────────────────────────────────────

    @FXML
    private void handleUserMenu(ActionEvent event) {
        if (userDropdown == null) return;
        boolean showing = userDropdown.isVisible();
        userDropdown.setVisible(!showing);
        userDropdown.setManaged(!showing);
    }

    private void closeDropdown() {
        if (userDropdown == null) return;
        userDropdown.setVisible(false);
        userDropdown.setManaged(false);
    }

    // ── Tìm kiếm ────────────────────────────────────────────────────────────

    @FXML
    private void handleSearch(ActionEvent event) {
        String keyword  = txtSearch  != null ? txtSearch.getText().trim() : "";
        String category = cmbCategory != null ? cmbCategory.getValue()   : "";
        if ("Tất cả danh mục".equals(category)) category = "";

        doSearch(keyword, category);
    }

    @FXML
    private void handleCategory(ActionEvent event) {
        Button src = (Button) event.getSource();
        // Tách tên danh mục: bỏ emoji + khoảng trắng đầu
        // Dùng codePoint để xử lý đúng Unicode supplementary (emoji)
        String raw = src.getText().trim();
        String category = stripLeadingEmoji(raw);

        if (cmbCategory != null) cmbCategory.setValue(category);
        doSearch("", category);
    }

    /**
     * Bỏ các ký tự không phải chữ cái (bao gồm emoji, số, dấu) ở đầu chuỗi.
     * Xử lý đúng với Unicode surrogate pairs.
     */
    private String stripLeadingEmoji(String text) {
        int i = 0;
        while (i < text.length()) {
            int cp = text.codePointAt(i);
            // Nếu là chữ cái (Latin hoặc Unicode letter) thì dừng
            if (Character.isLetter(cp)) break;
            i += Character.charCount(cp);
        }
        return text.substring(i).trim();
    }

    /** Gửi request tìm kiếm và cập nhật flowProducts */
    private void doSearch(String keyword, String category) {
        JsonObject data = new JsonObject();
        data.addProperty("keyword",  keyword);
        data.addProperty("category", category);

        JsonObject request = new JsonObject();
        request.addProperty("action", "SEARCH_PRODUCTS");
        request.add("data", data);

        new Thread(() -> {
            try {
                JsonObject response = SocketClient.sendRequest(request);
                Platform.runLater(() -> {
                    if (flowProducts == null) return;
                    flowProducts.getChildren().clear();
                    if (!isSuccess(response)) {
                        flowProducts.getChildren().add(
                                buildEmptyLabel("Không tìm thấy sản phẩm phù hợp"));
                        return;
                    }
                    JsonArray items = response.getAsJsonArray("items");
                    if (items == null || items.size() == 0) {
                        flowProducts.getChildren().add(
                                buildEmptyLabel("Không tìm thấy sản phẩm phù hợp"));
                        return;
                    }
                    for (int i = 0; i < items.size(); i++) {
                        flowProducts.getChildren().add(
                                buildProductCard(items.get(i).getAsJsonObject()));
                    }
                    if (mainScrollPane != null) mainScrollPane.setVvalue(0);
                });
            } catch (Exception e) {
                System.err.println("[HomeController] doSearch error: " + e.getMessage());
            }
        }, "thread-search").start();
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
        navigateTo("/view/my_orders.fxml", "Đơn hàng của tôi");
    }

    @FXML
    private void handleSellerDashboard(ActionEvent event) {
        closeDropdown();
        if (!SessionManager.isSeller() && !SessionManager.isAdmin()) {
            showAlert("Bạn cần đăng ký tài khoản Seller để sử dụng chức năng này!\n"
                    + "Vào Trang cá nhân → Đăng ký bán hàng.");
            return;
        }
        navigateTo("/view/seller_dashboard.fxml", "Quản lý bán hàng");
    }

    @FXML
    private void handleWallet(ActionEvent event) {
        closeDropdown();
        try {
            double balance = SessionManager.getBalance();
            showAlert("Số dư ví: " + formatPrice(String.valueOf((long) balance)) + "đ");
        } catch (Exception e) {
            showAlert("Không thể lấy số dư ví. Vui lòng thử lại.");
            System.err.println("[HomeController] handleWallet: " + e.getMessage());
        }
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        closeDropdown();
        stopAllCountdowns();
        SessionManager.logout();
        navigateTo("/view/login.fxml", "Đăng nhập");
    }

    @FXML
    private void handleSell(ActionEvent event) {
        if (!SessionManager.isSeller() && !SessionManager.isAdmin()) {
            showAlert("Bạn cần đăng ký tài khoản Seller để đăng bán!\n"
                    + "Vào Trang cá nhân → Đăng ký bán hàng.");
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
        navigateTo("/view/cart.fxml", "Giỏ hàng");
    }

    private void handleGoToProductDetail(int productId) {
        if (productId < 0) return;
        try {
            stopAllCountdowns();
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/view/product_detail.fxml"));
            Parent root = loader.load();

            // Truyền productId sang controller tiếp theo nếu có
            Object controller = loader.getController();
            if (controller instanceof ProductDetailController) {
                ((ProductDetailController) controller).setProductId(productId);
            }

            Stage stage = getCurrentStage();
            if (stage == null) return;
            stage.setScene(new Scene(root));
            stage.setTitle("Chi tiết sản phẩm");
            stage.centerOnScreen();
        } catch (IOException e) {
            System.err.println("[HomeController] productDetail nav: " + e.getMessage());
            // Màn hình detail chưa có — thông báo thân thiện thay vì crash
            showAlert("Không thể mở chi tiết sản phẩm.");
        }
    }

    private void handleGoToBidding(int auctionId) {
        if (auctionId < 0) {
            showAlert("Phiên đấu giá không hợp lệ.");
            return;
        }
        try {
            stopAllCountdowns();
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/view/bidding_room.fxml"));
            Parent root = loader.load();

            // Truyền auctionId sang BiddingRoomController
            Object controller = loader.getController();
            if (controller instanceof BiddingRoomController) {
                ((BiddingRoomController) controller).setAuctionId(auctionId);
            }

            Stage stage = getCurrentStage();
            if (stage == null) return;
            stage.setScene(new Scene(root));
            stage.setTitle("Phòng đấu giá #" + auctionId);
            stage.centerOnScreen();
        } catch (IOException e) {
            System.err.println("[HomeController] bidding nav: " + e.getMessage());
            showAlert("Không thể mở phòng đấu giá.");
        }
    }

    // ── Tiện ích ─────────────────────────────────────────────────────────────

    /**
     * Điều hướng an toàn: lấy Stage từ bất kỳ node nào đang attach vào scene.
     * Ưu tiên lblUsername, fallback sang các node khác.
     */
    private void navigateTo(String fxmlPath, String title) {
        try {
            stopAllCountdowns();
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();

            Stage stage = getCurrentStage();
            if (stage == null) {
                showAlert("Không thể điều hướng tới: " + title);
                return;
            }
            stage.setScene(new Scene(root));
            stage.setTitle(title);
            stage.centerOnScreen();
        } catch (IOException e) {
            System.err.println("[HomeController] navigateTo(" + fxmlPath + "): " + e.getMessage());
            showAlert("Không thể mở: " + title);
        }
    }

    /**
     * Lấy Stage từ các FXML node đã inject.
     * Tránh NPE khi node chưa attach vào scene.
     */
    private Stage getCurrentStage() {
        // Thử lần lượt các node có thể đã sẵn sàng
        Node[] candidates = {lblUsername, lblAvatarInitial, flowProducts, flowAuctions, txtSearch};
        for (Node n : candidates) {
            if (n != null && n.getScene() != null && n.getScene().getWindow() != null) {
                return (Stage) n.getScene().getWindow();
            }
        }
        return null;
    }

    /** Lấy Scene từ node bất kỳ */
    private Scene getScene() {
        Stage stage = getCurrentStage();
        return stage != null ? stage.getScene() : null;
    }

    /** Kiểm tra response có status SUCCESS không */
    private boolean isSuccess(JsonObject response) {
        if (response == null) return false;
        try {
            return "SUCCESS".equals(response.get("status").getAsString());
        } catch (Exception e) {
            return false;
        }
    }

    /** Lấy String an toàn từ JsonObject, trả về defaultValue nếu null/missing */
    private String getStrSafe(JsonObject obj, String key, String defaultValue) {
        try {
            if (obj == null || !obj.has(key) || obj.get(key).isJsonNull()) return defaultValue;
            return obj.get(key).getAsString();
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /** Format số thành dạng 1,000,000 */
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

    // ── Interface stubs (để tránh compile error nếu chưa có controller) ──────

    /**
     * Stub interface — xóa khi đã có class ProductDetailController thực.
     * Nếu đã có rồi thì xóa interface này đi.
     */
    interface ProductDetailController {
        void setProductId(int productId);
    }

    /**
     * Stub interface — xóa khi đã có class BiddingRoomController thực.
     */
    interface BiddingRoomController {
        void setAuctionId(int auctionId);
    }
}