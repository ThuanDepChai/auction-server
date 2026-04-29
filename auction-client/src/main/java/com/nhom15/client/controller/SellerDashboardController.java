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
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class SellerDashboardController {

    // ── Sidebar ──────────────────────────────────────────────────────────────
    @FXML private Label   lblSellerName;
    @FXML private Label   lblPageTitle;
    @FXML private Label   lblDateTime;
    @FXML private Button  btnMenuOverview;
    @FXML private Button  btnMenuItems;
    @FXML private Button  btnMenuAddItem;
    @FXML private Button  btnMenuAuctions;
    @FXML private Button  btnMenuCreateAuction;

    // ── Panels ───────────────────────────────────────────────────────────────
    @FXML private VBox    panelOverview;
    @FXML private VBox    panelItems;
    @FXML private VBox    panelAddItem;
    @FXML private VBox    panelAuctions;
    @FXML private VBox    panelCreateAuction;

    // ── Overview ─────────────────────────────────────────────────────────────
    @FXML private Label   lblTotalItems;
    @FXML private Label   lblActiveAuctions;
    @FXML private Label   lblTotalSold;
    @FXML private Label   lblTotalRevenue;
    @FXML private VBox    vboxRecentAuctions;

    // ── Add Item ─────────────────────────────────────────────────────────────
    @FXML private TextField   txtItemName;
    @FXML private TextArea    txtItemDescription;
    @FXML private ComboBox<String> cmbItemCategory;
    @FXML private TextField   txtItemStartPrice;
    @FXML private Label       lblItemNameError;
    @FXML private Label       lblItemCategoryError;
    @FXML private Label       lblItemPriceError;
    @FXML private Label       lblAddItemStatus;
    @FXML private ImageView   imgItemPreview;
    @FXML private StackPane   imgPreviewPane;

    // ── My Items ─────────────────────────────────────────────────────────────
    @FXML private FlowPane    flowMyItems;

    // ── Create Auction ────────────────────────────────────────────────────────
    @FXML private ComboBox<String> cmbAuctionItem;
    @FXML private TextField   txtAuctionStartPrice;
    @FXML private TextField   txtAuctionMinStep;
    @FXML private DatePicker  dateAuctionEnd;
    @FXML private TextField   txtAuctionEndTime;
    @FXML private Label       lblAuctionItemError;
    @FXML private Label       lblAuctionPriceError;
    @FXML private Label       lblAuctionStepError;
    @FXML private Label       lblAuctionTimeError;
    @FXML private Label       lblCreateAuctionStatus;

    // ── My Auctions ──────────────────────────────────────────────────────────
    @FXML private VBox        vboxMyAuctions;

    // ── Internal ─────────────────────────────────────────────────────────────
    private String   selectedImageBase64 = "";
    private String   selectedImageExt    = "jpg";
    // Map itemName → itemId cho ComboBox tạo đấu giá
    private final Map<String, Integer> itemNameToId = new HashMap<>();
    private Timeline clockTimeline;

    // ── Init ─────────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        lblSellerName.setText(SessionManager.getUsername());

        // Bind managed
        lblItemNameError.managedProperty().bind(lblItemNameError.visibleProperty());
        lblItemCategoryError.managedProperty().bind(lblItemCategoryError.visibleProperty());
        lblItemPriceError.managedProperty().bind(lblItemPriceError.visibleProperty());
        lblAuctionItemError.managedProperty().bind(lblAuctionItemError.visibleProperty());
        lblAuctionPriceError.managedProperty().bind(lblAuctionPriceError.visibleProperty());
        lblAuctionStepError.managedProperty().bind(lblAuctionStepError.visibleProperty());
        lblAuctionTimeError.managedProperty().bind(lblAuctionTimeError.visibleProperty());

        // Danh mục
        cmbItemCategory.getItems().addAll(
                "Điện tử", "Thời trang", "Nhà cửa & Sân vườn",
                "Đồ sưu tầm", "Thể thao", "Khác"
        );

        // Đồng hồ realtime
        startClock();

        // Load dữ liệu tổng quan
        loadOverview();
    }

    // ── Đồng hồ ──────────────────────────────────────────────────────────────

    private void startClock() {
        clockTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            String now = LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("dd/MM/yyyy  HH:mm:ss"));
            lblDateTime.setText(now);
        }));
        clockTimeline.setCycleCount(Timeline.INDEFINITE);
        clockTimeline.play();
    }

    // ── Menu điều hướng ──────────────────────────────────────────────────────

    @FXML private void handleMenuOverview(ActionEvent e) {
        showPanel(panelOverview, "📊  Tổng quan", btnMenuOverview);
        loadOverview();
    }

    @FXML private void handleMenuItems(ActionEvent e) {
        showPanel(panelItems, "📦  Sản phẩm của tôi", btnMenuItems);
        loadMyItems();
    }

    @FXML private void handleMenuAddItem(ActionEvent e) {
        showPanel(panelAddItem, "➕  Đăng sản phẩm", btnMenuAddItem);
    }

    @FXML private void handleMenuAuctions(ActionEvent e) {
        showPanel(panelAuctions, "🔨  Phiên đấu giá", btnMenuAuctions);
        loadMyAuctions();
    }

    @FXML private void handleMenuCreateAuction(ActionEvent e) {
        showPanel(panelCreateAuction, "⚡  Tạo phiên đấu giá", btnMenuCreateAuction);
        loadItemsForAuction();
    }

    private void showPanel(VBox target, String title, Button activeBtn) {
        lblPageTitle.setText(title.replaceAll("[^a-zA-ZÀ-ỹ\\s]+", "").trim());

        // Ẩn tất cả panel
        for (VBox p : new VBox[]{panelOverview, panelItems, panelAddItem, panelAuctions, panelCreateAuction}) {
            p.setVisible(false);
            p.setManaged(false);
        }
        target.setVisible(true);
        target.setManaged(true);

        // Style menu
        String normal = "-fx-background-color: transparent; -fx-text-fill: #555555; " +
                "-fx-alignment: CENTER_LEFT; -fx-padding: 12 20 12 20; -fx-cursor: hand;";
        String active = "-fx-background-color: linear-gradient(to right, #4285F4, #9B72CB); " +
                "-fx-text-fill: white; -fx-alignment: CENTER_LEFT; " +
                "-fx-padding: 12 20 12 20; -fx-cursor: hand; -fx-font-weight: bold;";
        for (Button b : new Button[]{btnMenuOverview, btnMenuItems, btnMenuAddItem,
                btnMenuAuctions, btnMenuCreateAuction}) {
            b.setStyle(b == activeBtn ? active : normal);
        }
    }

    // ── Load tổng quan ────────────────────────────────────────────────────────

    private void loadOverview() {
        int sellerId = SessionManager.getUserId();

        new Thread(() -> {
            // Load items
            JsonObject reqItems = new JsonObject();
            reqItems.addProperty("action", "GET_MY_ITEMS");
            JsonObject dataItems = new JsonObject();
            dataItems.addProperty("sellerId", sellerId);
            reqItems.add("data", dataItems);
            JsonObject resItems = SocketClient.sendRequest(reqItems);

            // Load auctions
            JsonObject reqAuctions = new JsonObject();
            reqAuctions.addProperty("action", "GET_MY_AUCTIONS");
            JsonObject dataAuctions = new JsonObject();
            dataAuctions.addProperty("sellerId", sellerId);
            reqAuctions.add("data", dataAuctions);
            JsonObject resAuctions = SocketClient.sendRequest(reqAuctions);

            Platform.runLater(() -> {
                // Đếm stats
                int total = 0, active = 0, sold = 0;
                if (resItems != null && "SUCCESS".equals(resItems.get("status").getAsString())) {
                    JsonArray items = resItems.getAsJsonArray("items");
                    total = items.size();
                    for (int i = 0; i < items.size(); i++) {
                        if ("SOLD".equals(items.get(i).getAsJsonObject().get("status").getAsString())) sold++;
                    }
                }
                if (resAuctions != null && "SUCCESS".equals(resAuctions.get("status").getAsString())) {
                    JsonArray auctions = resAuctions.getAsJsonArray("auctions");
                    for (int i = 0; i < auctions.size(); i++) {
                        if ("ACTIVE".equals(auctions.get(i).getAsJsonObject().get("status").getAsString())) active++;
                    }
                    // Hiện recent auctions
                    vboxRecentAuctions.getChildren().clear();
                    for (int i = 0; i < Math.min(5, auctions.size()); i++) {
                        vboxRecentAuctions.getChildren().add(
                                buildAuctionRow(auctions.get(i).getAsJsonObject())
                        );
                    }
                    if (auctions.size() == 0) {
                        vboxRecentAuctions.getChildren().add(
                                buildEmptyLabel("Chưa có phiên đấu giá nào")
                        );
                    }
                }

                lblTotalItems.setText(String.valueOf(total));
                lblActiveAuctions.setText(String.valueOf(active));
                lblTotalSold.setText(String.valueOf(sold));
                lblTotalRevenue.setText("---");
            });
        }).start();
    }

    // ── Load sản phẩm của tôi ────────────────────────────────────────────────

    private void loadMyItems() {
        JsonObject data = new JsonObject();
        data.addProperty("sellerId", SessionManager.getUserId());
        JsonObject request = new JsonObject();
        request.addProperty("action", "GET_MY_ITEMS");
        request.add("data", data);

        new Thread(() -> {
            JsonObject response = SocketClient.sendRequest(request);
            Platform.runLater(() -> {
                flowMyItems.getChildren().clear();
                if (response == null || !"SUCCESS".equals(response.get("status").getAsString())) {
                    flowMyItems.getChildren().add(buildEmptyLabel("Chưa có sản phẩm nào"));
                    return;
                }
                JsonArray items = response.getAsJsonArray("items");
                if (items.size() == 0) {
                    flowMyItems.getChildren().add(buildEmptyLabel("Chưa có sản phẩm nào"));
                    return;
                }
                for (int i = 0; i < items.size(); i++) {
                    flowMyItems.getChildren().add(buildItemCard(items.get(i).getAsJsonObject()));
                }
            });
        }).start();
    }

    // ── Load sản phẩm vào ComboBox tạo đấu giá ──────────────────────────────

    private void loadItemsForAuction() {
        JsonObject data = new JsonObject();
        data.addProperty("sellerId", SessionManager.getUserId());
        JsonObject request = new JsonObject();
        request.addProperty("action", "GET_MY_ITEMS");
        request.add("data", data);

        new Thread(() -> {
            JsonObject response = SocketClient.sendRequest(request);
            Platform.runLater(() -> {
                cmbAuctionItem.getItems().clear();
                itemNameToId.clear();
                if (response == null || !"SUCCESS".equals(response.get("status").getAsString())) return;
                JsonArray items = response.getAsJsonArray("items");
                for (int i = 0; i < items.size(); i++) {
                    JsonObject item = items.get(i).getAsJsonObject();
                    if ("AVAILABLE".equals(item.get("status").getAsString())) {
                        String name = item.get("name").getAsString();
                        int    id   = item.get("itemId").getAsInt();
                        cmbAuctionItem.getItems().add(name);
                        itemNameToId.put(name, id);
                        // Điền giá khởi điểm từ sản phẩm
                        txtAuctionStartPrice.setText(
                                String.valueOf((long) item.get("startPrice").getAsDouble())
                        );
                    }
                }
            });
        }).start();
    }

    // ── Load phiên đấu giá của tôi ───────────────────────────────────────────

    private void loadMyAuctions() {
        JsonObject data = new JsonObject();
        data.addProperty("sellerId", SessionManager.getUserId());
        JsonObject request = new JsonObject();
        request.addProperty("action", "GET_MY_AUCTIONS");
        request.add("data", data);

        new Thread(() -> {
            JsonObject response = SocketClient.sendRequest(request);
            Platform.runLater(() -> {
                vboxMyAuctions.getChildren().clear();
                if (response == null || !"SUCCESS".equals(response.get("status").getAsString())) {
                    vboxMyAuctions.getChildren().add(buildEmptyLabel("Chưa có phiên đấu giá nào"));
                    return;
                }
                JsonArray auctions = response.getAsJsonArray("auctions");
                if (auctions.size() == 0) {
                    vboxMyAuctions.getChildren().add(buildEmptyLabel("Chưa có phiên đấu giá nào"));
                    return;
                }
                for (int i = 0; i < auctions.size(); i++) {
                    vboxMyAuctions.getChildren().add(
                            buildAuctionRow(auctions.get(i).getAsJsonObject())
                    );
                }
            });
        }).start();
    }

    // ── Xử lý đăng sản phẩm ─────────────────────────────────────────────────

    @FXML
    private void handlePickItemImage(javafx.scene.input.MouseEvent event) {
        pickImage();
    }

    @FXML
    private void handlePickItemImage(ActionEvent event) {
        pickImage();
    }

    private void pickImage() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Chọn ảnh sản phẩm");
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg")
        );
        Stage stage = (Stage) txtItemName.getScene().getWindow();
        File file = fc.showOpenDialog(stage);
        if (file == null) return;

        if (file.length() > 2 * 1024 * 1024) {
            showAlert("Ảnh không được vượt quá 2MB!");
            return;
        }

        try {
            byte[] bytes = Files.readAllBytes(file.toPath());
            selectedImageBase64 = Base64.getEncoder().encodeToString(bytes);
            selectedImageExt    = file.getName().toLowerCase().endsWith(".png") ? "png" : "jpg";

            Image img = new Image(file.toURI().toString());
            imgItemPreview.setImage(img);
            imgItemPreview.setVisible(true);
        } catch (Exception e) {
            showAlert("Không thể đọc file ảnh!");
        }
    }

    @FXML
    private void handleAddItem(ActionEvent event) {
        // Reset lỗi
        lblItemNameError.setVisible(false);
        lblItemCategoryError.setVisible(false);
        lblItemPriceError.setVisible(false);
        lblAddItemStatus.setText("");

        String name        = txtItemName.getText().trim();
        String description = txtItemDescription.getText().trim();
        String category    = cmbItemCategory.getValue();
        String priceStr    = txtItemStartPrice.getText().trim();

        boolean hasError = false;
        if (name.isEmpty()) {
            lblItemNameError.setText("Vui lòng nhập tên sản phẩm!");
            lblItemNameError.setVisible(true);
            hasError = true;
        }
        if (category == null) {
            lblItemCategoryError.setText("Vui lòng chọn danh mục!");
            lblItemCategoryError.setVisible(true);
            hasError = true;
        }
        double startPrice = 0;
        try {
            startPrice = Double.parseDouble(priceStr.replaceAll("[^0-9]", ""));
            if (startPrice <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            lblItemPriceError.setText("Giá không hợp lệ!");
            lblItemPriceError.setVisible(true);
            hasError = true;
        }
        if (hasError) return;

        final double finalPrice = startPrice;

        JsonObject data = new JsonObject();
        data.addProperty("sellerId",    SessionManager.getUserId());
        data.addProperty("name",        name);
        data.addProperty("description", description);
        data.addProperty("category",    category);
        data.addProperty("startPrice",  finalPrice);
        data.addProperty("imageBase64", selectedImageBase64);
        data.addProperty("extension",   selectedImageExt);

        JsonObject request = new JsonObject();
        request.addProperty("action", "CREATE_ITEM");
        request.add("data", data);

        new Thread(() -> {
            JsonObject response = SocketClient.sendRequest(request);
            Platform.runLater(() -> {
                if (response == null) {
                    showStatus(lblAddItemStatus, "Lỗi kết nối!", false);
                    return;
                }
                if ("SUCCESS".equals(response.get("status").getAsString())) {
                    showStatus(lblAddItemStatus, "✓ Đăng sản phẩm thành công!", true);
                    // Reset form
                    txtItemName.clear();
                    txtItemDescription.clear();
                    cmbItemCategory.setValue(null);
                    txtItemStartPrice.clear();
                    imgItemPreview.setVisible(false);
                    selectedImageBase64 = "";
                } else {
                    showStatus(lblAddItemStatus, "Thất bại: " + response.get("message").getAsString(), false);
                }
            });
        }).start();
    }

    // ── Xử lý tạo phiên đấu giá ─────────────────────────────────────────────

    @FXML
    private void handleCreateAuction(ActionEvent event) {
        // Reset lỗi
        lblAuctionItemError.setVisible(false);
        lblAuctionPriceError.setVisible(false);
        lblAuctionStepError.setVisible(false);
        lblAuctionTimeError.setVisible(false);
        lblCreateAuctionStatus.setText("");

        String itemName   = cmbAuctionItem.getValue();
        String priceStr   = txtAuctionStartPrice.getText().trim();
        String stepStr    = txtAuctionMinStep.getText().trim();
        LocalDate endDate = dateAuctionEnd.getValue();
        String   endHour  = txtAuctionEndTime.getText().trim();

        boolean hasError = false;

        if (itemName == null) {
            lblAuctionItemError.setText("Vui lòng chọn sản phẩm!");
            lblAuctionItemError.setVisible(true);
            hasError = true;
        }

        double startPrice = 0;
        try {
            startPrice = Double.parseDouble(priceStr.replaceAll("[^0-9]", ""));
            if (startPrice <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            lblAuctionPriceError.setText("Giá không hợp lệ!");
            lblAuctionPriceError.setVisible(true);
            hasError = true;
        }

        double minStep = 0;
        try {
            minStep = Double.parseDouble(stepStr.replaceAll("[^0-9]", ""));
            if (minStep <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            lblAuctionStepError.setText("Bước giá không hợp lệ!");
            lblAuctionStepError.setVisible(true);
            hasError = true;
        }

        String endTimeStr = "";
        try {
            if (endDate == null || endHour.isEmpty()) throw new Exception();
            endTimeStr = endDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + " " + endHour + ":00";
            // Kiểm tra endTime > now
            LocalDateTime endDT = LocalDateTime.parse(endTimeStr,
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            if (endDT.isBefore(LocalDateTime.now())) throw new Exception("Thời gian đã qua!");
        } catch (Exception e) {
            lblAuctionTimeError.setText("Thời gian kết thúc không hợp lệ!");
            lblAuctionTimeError.setVisible(true);
            hasError = true;
        }

        if (hasError) return;

        int itemId = itemNameToId.getOrDefault(itemName, -1);
        if (itemId == -1) {
            showStatus(lblCreateAuctionStatus, "Lỗi: không tìm thấy sản phẩm!", false);
            return;
        }

        final double fp = startPrice, ms = minStep;
        final String et = endTimeStr;

        JsonObject data = new JsonObject();
        data.addProperty("itemId",      itemId);
        data.addProperty("sellerId",    SessionManager.getUserId());
        data.addProperty("startPrice",  fp);
        data.addProperty("minStep",     ms);
        data.addProperty("endTime",     et);

        JsonObject request = new JsonObject();
        request.addProperty("action", "CREATE_AUCTION");
        request.add("data", data);

        new Thread(() -> {
            JsonObject response = SocketClient.sendRequest(request);
            Platform.runLater(() -> {
                if (response == null) {
                    showStatus(lblCreateAuctionStatus, "Lỗi kết nối!", false);
                    return;
                }
                if ("SUCCESS".equals(response.get("status").getAsString())) {
                    showStatus(lblCreateAuctionStatus, "✓ Tạo phiên đấu giá thành công!", true);
                    // Reset form
                    cmbAuctionItem.setValue(null);
                    txtAuctionStartPrice.clear();
                    txtAuctionMinStep.clear();
                    dateAuctionEnd.setValue(null);
                    txtAuctionEndTime.clear();
                    // Reload combobox
                    loadItemsForAuction();
                } else {
                    showStatus(lblCreateAuctionStatus, "Thất bại: " + response.get("message").getAsString(), false);
                }
            });
        }).start();
    }

    // ── Build UI components ───────────────────────────────────────────────────

    /** Card sản phẩm trong danh sách */
    private VBox buildItemCard(JsonObject item) {
        String name      = item.get("name").getAsString();
        String status    = item.get("status").getAsString();
        String price     = String.format("%,d", (long) item.get("startPrice").getAsDouble());
        String category  = item.get("category").getAsString();
        String imgBase64 = item.has("imageBase64") ? item.get("imageBase64").getAsString() : "";
        int    itemId    = item.get("itemId").getAsInt();

        VBox card = new VBox(8);
        card.setPrefWidth(200);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 10; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.07), 10, 0, 0, 4);");

        // Ảnh
        StackPane imgPane = new StackPane();
        imgPane.setPrefHeight(140);
        imgPane.setStyle("-fx-background-color: #F4F7FC; -fx-background-radius: 10 10 0 0;");
        if (!imgBase64.isEmpty()) {
            try {
                byte[] bytes = Base64.getDecoder().decode(imgBase64);
                ImageView iv = new ImageView(new Image(new ByteArrayInputStream(bytes)));
                iv.setFitWidth(200); iv.setFitHeight(140);
                iv.setPreserveRatio(true);
                imgPane.getChildren().add(iv);
            } catch (Exception e) {
                imgPane.getChildren().add(new Label("🖼"));
            }
        } else {
            Label ph = new Label("🖼");
            ph.setStyle("-fx-font-size: 30px; -fx-text-fill: #CCCCCC;");
            imgPane.getChildren().add(ph);
        }

        VBox info = new VBox(4);
        info.setStyle("-fx-padding: 8 10 10 10;");

        Label lblName = new Label(name);
        lblName.setWrapText(true);
        lblName.setStyle("-fx-font-size: 12px; -fx-font-weight: bold;");

        Label lblPrice = new Label(price + "đ");
        lblPrice.setStyle("-fx-font-size: 13px; -fx-text-fill: #D96570; -fx-font-weight: bold;");

        Label lblCat = new Label(category);
        lblCat.setStyle("-fx-font-size: 11px; -fx-text-fill: #888888;");

        // Badge status
        String statusColor = "AVAILABLE".equals(status) ? "#E8F5E9:#27AE60" :
                "IN_AUCTION".equals(status) ? "#EEF2FF:#4285F4" : "#FCE4EC:#C62828";
        String[] sc = statusColor.split(":");
        Label lblStatus = new Label(
                "AVAILABLE".equals(status) ? "Sẵn bán" :
                        "IN_AUCTION".equals(status) ? "Đang đấu giá" : "Đã bán"
        );
        lblStatus.setStyle("-fx-background-color: " + sc[0] + "; -fx-text-fill: " + sc[1] +
                "; -fx-background-radius: 8; -fx-padding: 2 8 2 8; -fx-font-size: 10px;");

        // Nút xóa (chỉ khi AVAILABLE)
        HBox actions = new HBox(5);
        if ("AVAILABLE".equals(status)) {
            Button btnDelete = new Button("🗑 Xóa");
            btnDelete.setStyle("-fx-background-color: #FCE4EC; -fx-text-fill: #C62828; " +
                    "-fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 4 10 4 10;");
            btnDelete.setOnAction(e -> handleDeleteItem(itemId));
            actions.getChildren().add(btnDelete);
        }

        info.getChildren().addAll(lblName, lblPrice, lblCat, lblStatus, actions);
        card.getChildren().addAll(imgPane, info);
        return card;
    }

    /** Row phiên đấu giá */
    private HBox buildAuctionRow(JsonObject auction) {
        String name     = auction.get("name").getAsString();
        String curPrice = String.format("%,d", (long) auction.get("currentPrice").getAsDouble());
        String endTime  = auction.get("endTime").getAsString();
        String status   = auction.get("status").getAsString();
        int    auctionId= auction.get("auctionId").getAsInt();

        HBox row = new HBox(15);
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        row.setStyle("-fx-background-color: #F9F9F9; -fx-background-radius: 8; -fx-padding: 12 15 12 15;");

        VBox info = new VBox(3);
        HBox.setHgrow(info, Priority.ALWAYS);

        Label lblName = new Label(name);
        lblName.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");

        Label lblPrice = new Label("Giá hiện tại: " + curPrice + "đ");
        lblPrice.setStyle("-fx-font-size: 12px; -fx-text-fill: #4285F4;");

        Label lblEnd = new Label("Kết thúc: " + endTime);
        lblEnd.setStyle("-fx-font-size: 11px; -fx-text-fill: #888888;");

        info.getChildren().addAll(lblName, lblPrice, lblEnd);

        // Status badge
        String sc = "ACTIVE".equals(status) ? "#E8F5E9:#27AE60" :
                "ENDED".equals(status)  ? "#F5F5F5:#888888" : "#FCE4EC:#C62828";
        String[] colors = sc.split(":");
        Label lblStatus = new Label("ACTIVE".equals(status) ? "Đang diễn ra" :
                "ENDED".equals(status)  ? "Đã kết thúc"  : "Đã hủy");
        lblStatus.setStyle("-fx-background-color: " + colors[0] + "; -fx-text-fill: " + colors[1] +
                "; -fx-background-radius: 10; -fx-padding: 4 12 4 12; " +
                "-fx-font-size: 11px; -fx-font-weight: bold;");

        // Nút kết thúc sớm
        if ("ACTIVE".equals(status)) {
            Button btnEnd = new Button("Kết thúc");
            btnEnd.setStyle("-fx-background-color: #FCE4EC; -fx-text-fill: #C62828; " +
                    "-fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 6 12 6 12;");
            btnEnd.setOnAction(e -> handleEndAuction(auctionId));
            row.getChildren().addAll(info, lblStatus, btnEnd);
        } else {
            row.getChildren().addAll(info, lblStatus);
        }

        return row;
    }

    // ── Xóa sản phẩm ─────────────────────────────────────────────────────────

    private void handleDeleteItem(int itemId) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Xác nhận xóa");
        confirm.setHeaderText("Bạn chắc chắn muốn xóa sản phẩm này?");
        confirm.setContentText("Hành động này không thể hoàn tác.");
        confirm.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                JsonObject data = new JsonObject();
                data.addProperty("itemId", itemId);
                JsonObject request = new JsonObject();
                request.addProperty("action", "DELETE_ITEM");
                request.add("data", data);

                new Thread(() -> {
                    JsonObject response = SocketClient.sendRequest(request);
                    Platform.runLater(() -> {
                        if (response != null && "SUCCESS".equals(response.get("status").getAsString())) {
                            loadMyItems();
                        } else {
                            showAlert("Xóa thất bại!");
                        }
                    });
                }).start();
            }
        });
    }

    // ── Kết thúc phiên đấu giá ────────────────────────────────────────────────

    private void handleEndAuction(int auctionId) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Kết thúc phiên");
        confirm.setHeaderText("Kết thúc phiên đấu giá sớm?");
        confirm.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                JsonObject data = new JsonObject();
                data.addProperty("auctionId", auctionId);
                JsonObject request = new JsonObject();
                request.addProperty("action", "END_AUCTION");
                request.add("data", data);

                new Thread(() -> {
                    JsonObject response = SocketClient.sendRequest(request);
                    Platform.runLater(() -> {
                        if (response != null && "SUCCESS".equals(response.get("status").getAsString())) {
                            loadMyAuctions();
                            loadOverview();
                        } else {
                            showAlert("Thất bại!");
                        }
                    });
                }).start();
            }
        });
    }

    // ── Điều hướng ───────────────────────────────────────────────────────────

    @FXML
    private void handleGoHome(ActionEvent event) {
        clockTimeline.stop();
        navigateTo("/view/Home.fxml", "Trang chủ");
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        clockTimeline.stop();
        SessionManager.logout();
        navigateTo("/view/login.fxml", "Đăng nhập");
    }

    // ── Tiện ích ─────────────────────────────────────────────────────────────

    private void navigateTo(String fxml, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            Parent root = loader.load();
            Stage stage = (Stage) lblSellerName.getScene().getWindow();
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

    private Label buildEmptyLabel(String text) {
        Label lbl = new Label(text);
        lbl.setStyle("-fx-text-fill: #AAAAAA; -fx-font-size: 13px; -fx-padding: 20;");
        return lbl;
    }

    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Thông báo");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}