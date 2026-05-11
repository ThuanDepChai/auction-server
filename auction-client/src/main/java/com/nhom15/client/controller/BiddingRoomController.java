package com.nhom15.client.controller;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.nhom15.client.command.*;
import com.nhom15.client.util.SessionManager;
import com.nhom15.client.util.ViewManager;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.layout.*;
import javafx.util.Duration;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

public class BiddingRoomController {

    @FXML private Label     lblTitle;
    @FXML private Label     lblStatus;
    @FXML private Label     lblProductName;
    @FXML private Label     lblCategory;
    @FXML private Label     lblDescription;
    @FXML private Label     lblSeller;
    @FXML private ImageView imgProduct;
    @FXML private Label     lblImgPlaceholder;
    @FXML private Label     lblCurrentPrice;
    @FXML private Label     lblCountdown;
    @FXML private TextField txtBidAmount;
    @FXML private Label     lblMinBid;
    @FXML private Label     lblBidError;
    @FXML private Label     lblBidStatus;
    @FXML private Button    btnPlaceBid;
    @FXML private VBox      vboxBidHistory;

    private int      auctionId;
    private double   currentPrice;
    private double   minStep;
    private Runnable onBack;
    private Timeline countdownTimer;

    private static final DateTimeFormatter DT_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ── Init ─────────────────────────────────────────────────────────────────

    @FXML public void initialize() {
        lblBidError.managedProperty().bind(lblBidError.visibleProperty());
        lblBidStatus.managedProperty().bind(lblBidStatus.visibleProperty());
    }

    public void setAuctionId(int id) {
        this.auctionId = id;
        loadDetail();
        loadBidHistory();
    }

    public void setOnBack(Runnable onBack) { this.onBack = onBack; }

    // ── Load ─────────────────────────────────────────────────────────────────

    private void loadDetail() {
        new GetAuctionDetailCommand(auctionId).executeAsync(
                res -> {
                    if (ServerCommand.isSuccess(res) && res.has("auction"))
                        populateDetail(res.getAsJsonObject("auction"));
                }
        );
    }

    private void loadBidHistory() {
        new GetBidHistoryCommand(auctionId).executeAsync(
                res -> {
                    if (res != null && res.has("history"))
                        populateBidHistory(res.getAsJsonArray("history"));
                }
        );
    }

    // ── Populate ─────────────────────────────────────────────────────────────

    private void populateDetail(JsonObject a) {
        lblTitle.setText("Phòng đấu giá #" + auctionId);
        lblProductName.setText(str(a, "name", "---"));
        lblCategory.setText("Danh mục: " + str(a, "category", "---"));
        lblDescription.setText(str(a, "description", ""));
        lblSeller.setText(str(a, "sellerName", "---"));

        currentPrice = a.has("currentPrice") ? a.get("currentPrice").getAsDouble() : 0;
        minStep      = a.has("minStep")       ? a.get("minStep").getAsDouble()       : 0;
        lblCurrentPrice.setText(String.format("%,.0fđ", currentPrice));
        lblMinBid.setText("Giá tối thiểu: " + String.format("%,.0f", currentPrice + minStep) + "đ");

        String status = str(a, "status", "ACTIVE");
        lblStatus.setText(status);
        if ("ENDED".equals(status)) {
            lblStatus.setStyle("-fx-background-color:#F5F5F5;-fx-text-fill:#888;-fx-background-radius:10;-fx-padding:4 14 4 14;-fx-font-weight:bold;-fx-font-size:12px;");
            btnPlaceBid.setDisable(true);
        }

        setupCountdown(str(a, "endTime", ""));

        String b64 = str(a, "imageBase64", "");
        if (!b64.isEmpty()) {
            try {
                Image img = new Image(new ByteArrayInputStream(Base64.getDecoder().decode(b64)));
                if (!img.isError()) { imgProduct.setImage(img); lblImgPlaceholder.setVisible(false); }
            } catch (Exception ignored) {}
        }
    }

    private void populateBidHistory(JsonArray history) {
        vboxBidHistory.getChildren().clear();
        if (history.size() == 0) {
            vboxBidHistory.getChildren().add(emptyLabel("Chưa có lượt đặt giá nào"));
            return;
        }
        for (int i = 0; i < history.size(); i++)
            vboxBidHistory.getChildren().add(buildBidRow(history.get(i).getAsJsonObject(), i == 0));
    }

    private HBox buildBidRow(JsonObject bid, boolean isTop) {
        HBox row = new HBox(12);
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        row.setStyle("-fx-background-color:" + (isTop ? "#EEF9FF" : "#FAFAFA")
                + ";-fx-background-radius:8;-fx-padding:8 12 8 12;");

        Label lblUser = new Label(str(bid, "username", "?"));
        lblUser.setStyle("-fx-font-weight:bold;-fx-font-size:13px;");
        Region gap = new Region(); HBox.setHgrow(gap, Priority.ALWAYS);

        Label lblAmt = new Label(String.format("%,.0fđ", bid.get("amount").getAsDouble()));
        lblAmt.setStyle("-fx-font-weight:bold;-fx-text-fill:#D96570;-fx-font-size:13px;");
        Label lblTime = new Label(str(bid, "bidTime", ""));
        lblTime.setStyle("-fx-font-size:10px;-fx-text-fill:#AAAAAA;");

        VBox right = new VBox(2, lblAmt, lblTime);
        right.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        row.getChildren().addAll(new Label(isTop ? "🥇" : "•"), lblUser, gap, right);
        return row;
    }

    // ── Countdown ────────────────────────────────────────────────────────────

    private void setupCountdown(String endTimeStr) {
        if (countdownTimer != null) countdownTimer.stop();
        try {
            LocalDateTime endTime = LocalDateTime.parse(endTimeStr, DT_FMT);
            countdownTimer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
                LocalDateTime now = LocalDateTime.now();
                if (now.isAfter(endTime)) {
                    lblCountdown.setText("Đã kết thúc");
                    btnPlaceBid.setDisable(true);
                    countdownTimer.stop();
                    return;
                }
                long h = ChronoUnit.HOURS.between(now, endTime);
                long m = ChronoUnit.MINUTES.between(now, endTime) % 60;
                long s = ChronoUnit.SECONDS.between(now, endTime) % 60;
                lblCountdown.setText(String.format("%02d:%02d:%02d", h, m, s));
                lblCountdown.setStyle("-fx-font-size:22px;-fx-font-weight:bold;-fx-text-fill:"
                        + (h < 1 ? "#D96570" : "#4285F4") + ";");
            }));
            countdownTimer.setCycleCount(Timeline.INDEFINITE);
            countdownTimer.play();
        } catch (Exception e) { lblCountdown.setText("---"); }
    }

    // ── Actions ──────────────────────────────────────────────────────────────

    @FXML private void handlePlaceBid() {
        lblBidError.setVisible(false);
        lblBidStatus.setVisible(false);

        String raw = txtBidAmount.getText().trim().replaceAll("[^0-9]", "");
        if (raw.isEmpty()) { showBidError("Vui lòng nhập số tiền!"); return; }

        double amount = Double.parseDouble(raw);
        if (amount < currentPrice + minStep) {
            showBidError(String.format("Giá phải ít nhất %,.0fđ!", currentPrice + minStep));
            return;
        }

        btnPlaceBid.setDisable(true);
        btnPlaceBid.setText("Đang đặt...");

        new PlaceBidCommand(auctionId, SessionManager.getUserId(), amount).executeAsync(
                res -> {
                    btnPlaceBid.setDisable(false);
                    btnPlaceBid.setText("🔨 ĐẶT GIÁ NGAY");
                    if (ServerCommand.isSuccess(res)) {
                        currentPrice = amount;
                        lblCurrentPrice.setText(String.format("%,.0fđ", currentPrice));
                        lblMinBid.setText("Giá tối thiểu: " + String.format("%,.0f", currentPrice + minStep) + "đ");
                        txtBidAmount.clear();
                        lblBidStatus.setText("✓ Đặt giá thành công!");
                        lblBidStatus.setVisible(true);
                        loadBidHistory();
                    } else {
                        showBidError(ServerCommand.getMessage(res, "Đặt giá thất bại!"));
                    }
                },
                () -> {
                    btnPlaceBid.setDisable(false);
                    btnPlaceBid.setText("🔨 ĐẶT GIÁ NGAY");
                    showBidError("Lỗi kết nối!");
                }
        );
    }

    @FXML private void handleRefreshBids() { loadBidHistory(); }

    @FXML private void handleBack() {
        if (countdownTimer != null) countdownTimer.stop();
        ViewManager.navigateTo(ViewManager.Views.HOME);
        if (onBack != null) onBack.run();
    }

    // ── Util ─────────────────────────────────────────────────────────────────

    private void showBidError(String msg) { lblBidError.setText(msg); lblBidError.setVisible(true); }

    private Label emptyLabel(String msg) {
        Label l = new Label(msg);
        l.setStyle("-fx-text-fill:#AAAAAA;-fx-font-size:13px;-fx-padding:10;");
        return l;
    }

    private String str(JsonObject o, String key, String def) {
        return (o != null && o.has(key) && !o.get(key).isJsonNull())
                ? o.get(key).getAsString() : def;
    }
}