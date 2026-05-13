package com.nhom15.client.controller;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.nhom15.client.command.AutoBidCommand;
import com.nhom15.client.command.GetAuctionDetailCommand;
import com.nhom15.client.command.GetAutoBidStatusCommand;
import com.nhom15.client.command.GetBidHistoryCommand;
import com.nhom15.client.command.GetItemImageCommand;
import com.nhom15.client.command.PlaceBidCommand;
import com.nhom15.client.command.ServerCommand;
import com.nhom15.client.util.SessionManager;
import com.nhom15.client.util.ViewManager;
import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * BiddingRoomController — màn hình đấu giá nâng cao.
 * Tính năng: đặt giá thủ công, auto-bid, realtime polling 3s,
 * biểu đồ giá LineChart, anti-snipe indicator, animation khi giá thay đổi.
 */
public class BiddingRoomController {

  // ── TOP BAR ──────────────────────────────────────────────────────────────
  @FXML private Label lblTitle;
  @FXML private Label lblStatus;
  @FXML private Label lblAntiSnipe;

  // ── LEFT: sản phẩm ───────────────────────────────────────────────────────
  @FXML private ImageView imgProduct;
  @FXML private Label lblImgPlaceholder;
  @FXML private Label lblProductName;
  @FXML private Label lblCategory;
  @FXML private Label lblDescription;
  @FXML private Label lblSeller;
  @FXML private Label lblStartPrice;

  // ── RIGHT TOP: giá + đồng hồ ─────────────────────────────────────────────
  @FXML private Label lblCurrentPrice;
  @FXML private Label lblCountdown;
  @FXML private Label lblLeader;
  @FXML private Label lblTotalBids;

  // ── Tab Thủ công ─────────────────────────────────────────────────────────
  @FXML private TextField txtBidAmount;
  @FXML private Label lblMinBid;
  @FXML private Label lblBidError;
  @FXML private Label lblBidStatus;
  @FXML private Button btnPlaceBid;
  @FXML private Button btnQuick1;
  @FXML private Button btnQuick2;
  @FXML private Button btnQuick3;

  // ── Tab Auto-Bid ──────────────────────────────────────────────────────────
  @FXML private TextField txtMaxBid;
  @FXML private TextField txtIncrement;
  @FXML private Button btnSetAutoBid;
  @FXML private Button btnCancelAutoBid;
  @FXML private Label lblAutoBidStatus;
  @FXML private Label lblAutoBidInfo;

  // ── Tab Chart ────────────────────────────────────────────────────────────
  @FXML private LineChart<Number, Number> priceChart;
  @FXML private NumberAxis xAxis;
  @FXML private NumberAxis yAxis;

  // ── Lịch sử ──────────────────────────────────────────────────────────────
  @FXML private VBox vboxBidHistory;
  @FXML private ScrollPane scrollHistory;

  // ── State ─────────────────────────────────────────────────────────────────
  private int auctionId;
  private double currentPrice;
  private double minStep;
  private boolean autoBidActive = false;
  private Timeline countdownTimer;
  private Timeline realtimePoller;
  private long chartTickCounter = 0;
  private XYChart.Series<Number, Number> priceSeries;
  private Runnable onBack;
  private int lastHistorySize = 0;

  private static final DateTimeFormatter DT_FMT =
          DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  // ═════════════════════════════════════════════════════════════════════════
  //  INIT
  // ═════════════════════════════════════════════════════════════════════════

  @FXML
  public void initialize() {
    if (lblBidError != null) {
      lblBidError.managedProperty().bind(lblBidError.visibleProperty());
      lblBidError.setVisible(false);
    }
    if (lblBidStatus != null) {
      lblBidStatus.managedProperty().bind(lblBidStatus.visibleProperty());
      lblBidStatus.setVisible(false);
    }
    if (lblAutoBidStatus != null) {
      lblAutoBidStatus.managedProperty().bind(lblAutoBidStatus.visibleProperty());
      lblAutoBidStatus.setVisible(false);
    }
    if (lblAntiSnipe != null) {
      lblAntiSnipe.managedProperty().bind(lblAntiSnipe.visibleProperty());
      lblAntiSnipe.setVisible(false);
    }
    setupPriceChart();
    setupQuickBidButtons();
  }

  public void setAuctionId(int id) {
    this.auctionId = id;
    loadDetail();
    loadBidHistory();
    loadAutoBidStatus();
    startRealtimePolling();
  }

  public void setOnBack(Runnable r) { this.onBack = r; }

  // ═════════════════════════════════════════════════════════════════════════
  //  CHART
  // ═════════════════════════════════════════════════════════════════════════

  private void setupPriceChart() {
    if (priceChart == null) return;
    priceSeries = new XYChart.Series<>();
    priceSeries.setName("Giá đấu");
    priceChart.getData().add(priceSeries);
    priceChart.setAnimated(false);
    priceChart.setLegendVisible(false);
    if (xAxis != null) { xAxis.setAutoRanging(true); }
    if (yAxis != null) { yAxis.setAutoRanging(true); }
  }

  private void addChartPoint(double price) {
    if (priceSeries == null) return;
    chartTickCounter++;
    priceSeries.getData().add(new XYChart.Data<>(chartTickCounter, price));
    if (priceSeries.getData().size() > 60) priceSeries.getData().remove(0);
  }

  private void rebuildChart(JsonArray history) {
    if (priceSeries == null) return;
    priceSeries.getData().clear();
    chartTickCounter = 0;
    List<Double> prices = new ArrayList<>();
    for (int i = history.size() - 1; i >= 0; i--) {
      JsonObject bid = history.get(i).getAsJsonObject();
      if (bid.has("amount")) prices.add(bid.get("amount").getAsDouble());
    }
    for (double p : prices) {
      chartTickCounter++;
      priceSeries.getData().add(new XYChart.Data<>(chartTickCounter, p));
    }
  }

  // ═════════════════════════════════════════════════════════════════════════
  //  QUICK-BID
  // ═════════════════════════════════════════════════════════════════════════

  private void setupQuickBidButtons() {
    if (btnQuick1 == null) return;
    btnQuick1.setOnAction(e -> setQuickBid(1));
    btnQuick2.setOnAction(e -> setQuickBid(3));
    btnQuick3.setOnAction(e -> setQuickBid(5));
    updateQuickBidLabels();
  }

  private void updateQuickBidLabels() {
    if (btnQuick1 == null) return;
    btnQuick1.setText("+1 bước  " + formatShort(currentPrice + minStep));
    btnQuick2.setText("+3 bước  " + formatShort(currentPrice + minStep * 3));
    btnQuick3.setText("+5 bước  " + formatShort(currentPrice + minStep * 5));
  }

  private void setQuickBid(int mult) {
    if (txtBidAmount != null)
      txtBidAmount.setText(String.format("%.0f", currentPrice + minStep * mult));
  }

  private String formatShort(double amount) {
    if (amount >= 1_000_000_000) return String.format("%.1fT", amount / 1_000_000_000);
    if (amount >= 1_000_000) return String.format("%.0fM", amount / 1_000_000);
    if (amount >= 1000) return String.format("%.0fK", amount / 1000);
    return String.format("%.0f", amount);
  }

  // ═════════════════════════════════════════════════════════════════════════
  //  REALTIME POLLING
  // ═════════════════════════════════════════════════════════════════════════

  private void startRealtimePolling() {
    stopRealtimePolling();
    realtimePoller = new Timeline(new KeyFrame(Duration.seconds(3), e -> pollUpdate()));
    realtimePoller.setCycleCount(Timeline.INDEFINITE);
    realtimePoller.play();
  }

  private void stopRealtimePolling() {
    if (realtimePoller != null) realtimePoller.stop();
  }

  private void pollUpdate() {
    new GetAuctionDetailCommand(auctionId).executeAsync(res -> {
      if (!ServerCommand.isSuccess(res) || !res.has("auction")) return;
      JsonObject a = res.getAsJsonObject("auction");
      double newPrice = a.has("currentPrice") ? a.get("currentPrice").getAsDouble() : currentPrice;
      String newStatus = str(a, "status", "ACTIVE");

      if (newPrice != currentPrice) {
        double old = currentPrice;
        currentPrice = newPrice;
        updatePriceDisplay(newPrice, old);
        addChartPoint(newPrice);
        loadBidHistory();
      }

      if ("ENDED".equals(newStatus) || "CANCELLED".equals(newStatus)) {
        handleAuctionEnded();
      }

      String leader = str(a, "leadingBidder", "");
      if (!leader.isEmpty()) updateLeaderDisplay(leader);

      if (a.has("totalBids") && lblTotalBids != null)
        lblTotalBids.setText(a.get("totalBids").getAsInt() + " lượt đặt");
    });
  }

  // ═════════════════════════════════════════════════════════════════════════
  //  LOAD DATA
  // ═════════════════════════════════════════════════════════════════════════

  private void loadDetail() {
    new GetAuctionDetailCommand(auctionId).executeAsync(res -> {
      if (ServerCommand.isSuccess(res) && res.has("auction"))
        populateDetail(res.getAsJsonObject("auction"));
    });
  }

  private void loadBidHistory() {
    new GetBidHistoryCommand(auctionId).executeAsync(res -> {
      if (res != null && res.has("history")) {
        JsonArray h = res.getAsJsonArray("history");
        populateBidHistory(h);
        if (h.size() != lastHistorySize) {
          lastHistorySize = h.size();
          rebuildChart(h);
        }
      }
    });
  }

  private void loadAutoBidStatus() {
    if (!SessionManager.isBidder()) return;
    new GetAutoBidStatusCommand(auctionId, SessionManager.getUserId()).executeAsync(res -> {
      if (ServerCommand.isSuccess(res) && res.has("autoBid")) {
        JsonObject ab = res.getAsJsonObject("autoBid");
        autoBidActive = ab.has("active") && ab.get("active").getAsBoolean();
        updateAutoBidDisplay(autoBidActive, ab);
      }
    });
  }

  // ═════════════════════════════════════════════════════════════════════════
  //  POPULATE
  // ═════════════════════════════════════════════════════════════════════════

  private void populateDetail(JsonObject a) {
    if (lblTitle != null) lblTitle.setText("🏛️ Phòng đấu giá #" + auctionId);
    if (lblProductName != null) lblProductName.setText(str(a, "name", "---"));
    if (lblCategory != null) lblCategory.setText("📁 " + str(a, "category", "---"));
    if (lblDescription != null) lblDescription.setText(str(a, "description", "Không có mô tả."));
    if (lblSeller != null) lblSeller.setText("👤 " + str(a, "sellerName", "---"));
    if (lblStartPrice != null)
      lblStartPrice.setText("Giá khởi điểm: "
              + String.format("%,.0fđ", a.has("startPrice") ? a.get("startPrice").getAsDouble() : 0));

    currentPrice = a.has("currentPrice") ? a.get("currentPrice").getAsDouble() : 0;
    minStep = a.has("minStep") ? a.get("minStep").getAsDouble() : 0;
    if (lblCurrentPrice != null) lblCurrentPrice.setText(String.format("%,.0fđ", currentPrice));
    updateMinBidLabel();
    updateQuickBidLabels();
    addChartPoint(currentPrice);

    updateLeaderDisplay(str(a, "leadingBidder", ""));
    if (a.has("totalBids") && lblTotalBids != null)
      lblTotalBids.setText(a.get("totalBids").getAsInt() + " lượt đặt");

    String status = str(a, "status", "ACTIVE");
    updateStatusBadge(status);
    if ("ENDED".equals(status) || "CANCELLED".equals(status)) disableBidding();

    setupCountdown(str(a, "endTime", ""));

    String imagePath = str(a, "imagePath", "");
    if (!imagePath.isEmpty()) {
      loadProductImage(imagePath);
    } else {
      System.out.println("⚠️ [BiddingRoom] Sản phẩm không có imagePath trong dữ liệu auction.");
    }
  }

  private void populateBidHistory(JsonArray history) {
    if (vboxBidHistory == null) return;
    vboxBidHistory.getChildren().clear();
    if (history.size() == 0) {
      vboxBidHistory.getChildren().add(emptyLabel("📭 Chưa có lượt đặt giá nào"));
      return;
    }
    for (int i = 0; i < history.size(); i++) {
      HBox row = buildBidRow(history.get(i).getAsJsonObject(), i == 0);
      if (i == 0) {
        FadeTransition ft = new FadeTransition(Duration.millis(400), row);
        ft.setFromValue(0); ft.setToValue(1); ft.play();
      }
      vboxBidHistory.getChildren().add(row);
    }
    if (scrollHistory != null) Platform.runLater(() -> scrollHistory.setVvalue(0));
  }

  // ═════════════════════════════════════════════════════════════════════════
  //  UI HELPERS
  // ═════════════════════════════════════════════════════════════════════════

  private void updatePriceDisplay(double newPrice, double oldPrice) {
    if (lblCurrentPrice == null) return;
    lblCurrentPrice.setText(String.format("%,.0fđ", newPrice));
    updateMinBidLabel();
    updateQuickBidLabels();
    ScaleTransition st = new ScaleTransition(Duration.millis(250), lblCurrentPrice);
    st.setFromX(1.0); st.setFromY(1.0);
    st.setToX(1.18); st.setToY(1.18);
    st.setAutoReverse(true); st.setCycleCount(2); st.play();
    lblCurrentPrice.setStyle("-fx-font-size:32px;-fx-font-weight:bold;-fx-text-fill:#E53935;");
    new Timeline(new KeyFrame(Duration.millis(700),
            e -> lblCurrentPrice.setStyle(
                    "-fx-font-size:32px;-fx-font-weight:bold;-fx-text-fill:#D96570;"))).play();
  }

  private void updateLeaderDisplay(String leader) {
    if (lblLeader == null) return;
    if (leader == null || leader.isEmpty()) {
      lblLeader.setText("Chưa có ai dẫn đầu");
      lblLeader.setStyle("-fx-text-fill:#AAAAAA;-fx-font-size:12px;");
    } else if (leader.equals(SessionManager.getUsername())) {
      lblLeader.setText("🏆 Bạn đang dẫn đầu!");
      lblLeader.setStyle("-fx-text-fill:#27AE60;-fx-font-weight:bold;-fx-font-size:13px;");
    } else {
      lblLeader.setText("🥇 Dẫn đầu: " + leader);
      lblLeader.setStyle("-fx-text-fill:#333;-fx-font-size:12px;");
    }
  }

  private void updateMinBidLabel() {
    if (lblMinBid != null)
      lblMinBid.setText("Tối thiểu: " + String.format("%,.0fđ", currentPrice + minStep)
              + "  (bước: " + String.format("%,.0f", minStep) + "đ)");
  }

  private void updateStatusBadge(String status) {
    if (lblStatus == null) return;
    lblStatus.setText(status);
    String style = switch (status) {
      case "ACTIVE", "RUNNING" ->
              "-fx-background-color:#E8F5E9;-fx-text-fill:#27AE60;";
      case "ENDED" ->
              "-fx-background-color:#FFF3E0;-fx-text-fill:#E67E22;";
      case "CANCELLED" ->
              "-fx-background-color:#FFEBEE;-fx-text-fill:#E53935;";
      default ->
              "-fx-background-color:#EEEEEE;-fx-text-fill:#888;";
    };
    lblStatus.setStyle(style
            + "-fx-background-radius:10;-fx-padding:4 14 4 14;-fx-font-weight:bold;-fx-font-size:11px;");
  }

  private void handleAuctionEnded() {
    stopRealtimePolling();
    disableBidding();
    updateStatusBadge("ENDED");
    if (countdownTimer != null) countdownTimer.stop();
    if (lblCountdown != null) {
      lblCountdown.setText("⏰ Đã kết thúc");
      lblCountdown.setStyle("-fx-font-size:20px;-fx-font-weight:bold;-fx-text-fill:#E67E22;");
    }
  }

  private void disableBidding() {
    if (btnPlaceBid != null) btnPlaceBid.setDisable(true);
    if (txtBidAmount != null) txtBidAmount.setDisable(true);
    if (btnSetAutoBid != null) btnSetAutoBid.setDisable(true);
    if (btnQuick1 != null) { btnQuick1.setDisable(true); btnQuick2.setDisable(true); btnQuick3.setDisable(true); }
  }

  private void updateAutoBidDisplay(boolean active, JsonObject ab) {
    if (lblAutoBidInfo != null) {
      if (active) {
        double maxBid = ab.has("maxBid") ? ab.get("maxBid").getAsDouble() : 0;
        double inc = ab.has("increment") ? ab.get("increment").getAsDouble() : 0;
        lblAutoBidInfo.setText("✅ Auto-Bid đang BẬT\n"
                + "Giá tối đa: " + String.format("%,.0fđ", maxBid) + "\n"
                + "Bước tăng: " + String.format("%,.0fđ", inc));
        lblAutoBidInfo.setStyle("-fx-text-fill:#27AE60;-fx-font-size:12px;");
      } else {
        lblAutoBidInfo.setText("❌ Auto-Bid đang TẮT\nHệ thống sẽ không tự đặt giá thay bạn.");
        lblAutoBidInfo.setStyle("-fx-text-fill:#888;-fx-font-size:12px;");
      }
    }
    if (btnCancelAutoBid != null) btnCancelAutoBid.setDisable(!active);
  }

  // ═════════════════════════════════════════════════════════════════════════
  //  COUNTDOWN + ANTI-SNIPE
  // ═════════════════════════════════════════════════════════════════════════

  private void setupCountdown(String endTimeStr) {
    if (countdownTimer != null) countdownTimer.stop();
    try {
      LocalDateTime endTime = LocalDateTime.parse(endTimeStr, DT_FMT);
      countdownTimer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(endTime)) {
          if (lblCountdown != null) lblCountdown.setText("⏰ Đã kết thúc");
          disableBidding();
          countdownTimer.stop();
          return;
        }
        long totalSec = ChronoUnit.SECONDS.between(now, endTime);
        long h = totalSec / 3600, m = (totalSec % 3600) / 60, s = totalSec % 60;
        String txt = h > 0 ? String.format("%02d:%02d:%02d", h, m, s) : String.format("%02d:%02d", m, s);
        if (lblCountdown != null) lblCountdown.setText(txt);

        if (totalSec <= 30) {
          if (lblCountdown != null)
            lblCountdown.setStyle("-fx-font-size:22px;-fx-font-weight:bold;-fx-text-fill:#FF4444;");
          if (lblAntiSnipe != null) {
            lblAntiSnipe.setVisible(true);
            lblAntiSnipe.setText("⚡ Anti-Snipe: Bid mới sẽ gia hạn thêm " + (totalSec < 10 ? "30" : "60") + " giây!");
          }
        } else if (totalSec <= 300) {
          if (lblCountdown != null)
            lblCountdown.setStyle("-fx-font-size:22px;-fx-font-weight:bold;-fx-text-fill:#E67E22;");
          if (lblAntiSnipe != null) lblAntiSnipe.setVisible(false);
        } else {
          if (lblCountdown != null)
            lblCountdown.setStyle("-fx-font-size:22px;-fx-font-weight:bold;-fx-text-fill:#4285F4;");
          if (lblAntiSnipe != null) lblAntiSnipe.setVisible(false);
        }
      }));
      countdownTimer.setCycleCount(Timeline.INDEFINITE);
      countdownTimer.play();
    } catch (Exception e) {
      if (lblCountdown != null) lblCountdown.setText("---");
    }
  }

  // ═════════════════════════════════════════════════════════════════════════
  //  ACTIONS — Manual Bid
  // ═════════════════════════════════════════════════════════════════════════

  @FXML
  private void handlePlaceBid() {
    if (!SessionManager.isBidder()) { showBidError("⛔ Chỉ Bidder mới được phép đặt giá!"); return; }
    if (lblBidError != null) lblBidError.setVisible(false);
    if (lblBidStatus != null) lblBidStatus.setVisible(false);

    String raw = txtBidAmount.getText().trim().replaceAll("[^0-9]", "");
    if (raw.isEmpty()) { showBidError("⚠️ Vui lòng nhập số tiền!"); return; }

    double amount = Double.parseDouble(raw);
    if (amount < currentPrice + minStep) {
      showBidError(String.format("⚠️ Giá phải ít nhất %,.0fđ!", currentPrice + minStep));
      return;
    }

    if (btnPlaceBid != null) { btnPlaceBid.setDisable(true); btnPlaceBid.setText("⏳ Đang xử lý..."); }

    new PlaceBidCommand(auctionId, SessionManager.getUserId(), amount).executeAsync(
            res -> {
              if (btnPlaceBid != null) { btnPlaceBid.setDisable(false); btnPlaceBid.setText("🔨 ĐẶT GIÁ NGAY"); }
              if (ServerCommand.isSuccess(res)) {
                double old = currentPrice; currentPrice = amount;
                updatePriceDisplay(amount, old);
                addChartPoint(amount);
                if (txtBidAmount != null) txtBidAmount.clear();
                showBidSuccess("✓ Đặt giá thành công!");
                loadBidHistory();
                updateLeaderDisplay(SessionManager.getUsername());
              } else {
                showBidError(ServerCommand.getMessage(res, "❌ Đặt giá thất bại!"));
              }
            },
            () -> {
              if (btnPlaceBid != null) { btnPlaceBid.setDisable(false); btnPlaceBid.setText("🔨 ĐẶT GIÁ NGAY"); }
              showBidError("🔌 Lỗi kết nối đến server!");
            }
    );
  }

  // ═════════════════════════════════════════════════════════════════════════
  //  ACTIONS — Auto-Bid
  // ═════════════════════════════════════════════════════════════════════════

  @FXML
  private void handleSetAutoBid() {
    if (!SessionManager.isBidder()) { showAutoBidStatus("⛔ Chỉ Bidder mới được dùng Auto-Bid!", false); return; }
    String rawMax = txtMaxBid.getText().trim().replaceAll("[^0-9]", "");
    String rawInc = txtIncrement.getText().trim().replaceAll("[^0-9]", "");
    if (rawMax.isEmpty() || rawInc.isEmpty()) { showAutoBidStatus("⚠️ Nhập đầy đủ Giá tối đa và Bước giá!", false); return; }

    double maxBid = Double.parseDouble(rawMax);
    double increment = Double.parseDouble(rawInc);
    if (maxBid <= currentPrice) { showAutoBidStatus("⚠️ Giá tối đa phải > giá hiện tại " + String.format("%,.0fđ!", currentPrice), false); return; }
    if (increment < minStep) { showAutoBidStatus("⚠️ Bước giá tối thiểu " + String.format("%,.0fđ!", minStep), false); return; }

    if (btnSetAutoBid != null) btnSetAutoBid.setDisable(true);
    new AutoBidCommand(auctionId, SessionManager.getUserId(), maxBid, increment).executeAsync(
            res -> {
              if (btnSetAutoBid != null) btnSetAutoBid.setDisable(false);
              if (ServerCommand.isSuccess(res)) {
                autoBidActive = true;
                showAutoBidStatus("✅ Auto-Bid đã được kích hoạt!", true);
                JsonObject fakeAb = new JsonObject();
                fakeAb.addProperty("active", true);
                fakeAb.addProperty("maxBid", maxBid);
                fakeAb.addProperty("increment", increment);
                updateAutoBidDisplay(true, fakeAb);
                if (txtMaxBid != null) txtMaxBid.clear();
                if (txtIncrement != null) txtIncrement.clear();
              } else {
                showAutoBidStatus(ServerCommand.getMessage(res, "❌ Không thể kích hoạt Auto-Bid!"), false);
              }
            },
            () -> { if (btnSetAutoBid != null) btnSetAutoBid.setDisable(false); showAutoBidStatus("🔌 Lỗi kết nối!", false); }
    );
  }

  @FXML
  private void handleCancelAutoBid() {
    if (!autoBidActive) return;
    if (btnCancelAutoBid != null) btnCancelAutoBid.setDisable(true);
    new AutoBidCommand(auctionId, SessionManager.getUserId()).executeAsync(
            res -> {
              if (ServerCommand.isSuccess(res)) {
                autoBidActive = false;
                showAutoBidStatus("Auto-Bid đã bị huỷ.", false);
                JsonObject fakeAb = new JsonObject();
                fakeAb.addProperty("active", false);
                updateAutoBidDisplay(false, fakeAb);
              } else {
                if (btnCancelAutoBid != null) btnCancelAutoBid.setDisable(false);
                showAutoBidStatus(ServerCommand.getMessage(res, "❌ Không thể huỷ Auto-Bid!"), false);
              }
            },
            () -> { if (btnCancelAutoBid != null) btnCancelAutoBid.setDisable(false); showAutoBidStatus("🔌 Lỗi kết nối!", false); }
    );
  }

  @FXML private void handleRefreshBids() { loadBidHistory(); loadAutoBidStatus(); }

  @FXML
  private void handleBack() {
    stopRealtimePolling();
    if (countdownTimer != null) countdownTimer.stop();
    ViewManager.navigateTo(ViewManager.Views.HOME);
    if (onBack != null) onBack.run();
  }

  // ═════════════════════════════════════════════════════════════════════════
  //  BUILD BID ROW
  // ═════════════════════════════════════════════════════════════════════════

  private HBox buildBidRow(JsonObject bid, boolean isTop) {
    HBox row = new HBox(12);
    row.setAlignment(Pos.CENTER_LEFT);
    row.setStyle("-fx-background-color:" + (isTop ? "#FFFDE7" : "#FAFAFA")
            + ";-fx-background-radius:8;-fx-padding:10 14 10 14;"
            + (isTop ? "-fx-border-color:#FFC107;-fx-border-width:0 0 0 4;" : ""));

    Label lblRank = new Label(isTop ? "🥇" : "•");
    lblRank.setStyle("-fx-font-size:" + (isTop ? "16" : "12") + "px;");
    Label lblUser = new Label(str(bid, "username", "?"));
    lblUser.setStyle("-fx-font-weight:bold;-fx-font-size:13px;"
            + (isTop ? "-fx-text-fill:#E65100;" : "-fx-text-fill:#333;"));
    row.getChildren().addAll(lblRank, lblUser);

    if (str(bid, "username", "").equals(SessionManager.getUsername())) {
      Label you = new Label(" Bạn ");
      you.setStyle("-fx-background-color:#4285F4;-fx-text-fill:white;"
              + "-fx-background-radius:4;-fx-font-size:9px;-fx-padding:1 4 1 4;");
      row.getChildren().add(you);
    }
    // Badge auto-bid nếu có
    if (bid.has("isAutoBid") && bid.get("isAutoBid").getAsBoolean()) {
      Label auto = new Label(" 🤖 Auto ");
      auto.setStyle("-fx-background-color:#9C27B0;-fx-text-fill:white;"
              + "-fx-background-radius:4;-fx-font-size:9px;-fx-padding:1 4 1 4;");
      row.getChildren().add(auto);
    }

    Region gap = new Region(); HBox.setHgrow(gap, Priority.ALWAYS);
    row.getChildren().add(gap);

    Label lblAmt = new Label(String.format("%,.0fđ", bid.get("amount").getAsDouble()));
    lblAmt.setStyle("-fx-font-weight:bold;-fx-text-fill:#D96570;-fx-font-size:14px;");
    Label lblTime = new Label(str(bid, "bidTime", ""));
    lblTime.setStyle("-fx-font-size:10px;-fx-text-fill:#AAAAAA;");
    VBox right = new VBox(2, lblAmt, lblTime);
    right.setAlignment(Pos.CENTER_RIGHT);
    row.getChildren().add(right);
    return row;
  }

  // ═════════════════════════════════════════════════════════════════════════
  //  NOTIFICATION HELPERS
  // ═════════════════════════════════════════════════════════════════════════

  private void showBidError(String msg) {
    if (lblBidError == null) return;
    lblBidError.setText(msg); lblBidError.setVisible(true);
    if (lblBidStatus != null) lblBidStatus.setVisible(false);
    new Timeline(new KeyFrame(Duration.seconds(4), e -> lblBidError.setVisible(false))).play();
  }

  private void showBidSuccess(String msg) {
    if (lblBidStatus == null) return;
    lblBidStatus.setText(msg); lblBidStatus.setVisible(true);
    if (lblBidError != null) lblBidError.setVisible(false);
    new Timeline(new KeyFrame(Duration.seconds(3), e -> lblBidStatus.setVisible(false))).play();
  }

  private void showAutoBidStatus(String msg, boolean success) {
    if (lblAutoBidStatus == null) return;
    lblAutoBidStatus.setText(msg);
    lblAutoBidStatus.setStyle("-fx-font-size:12px;-fx-text-fill:"
            + (success ? "#27AE60" : "#D96570") + ";");
    lblAutoBidStatus.setVisible(true);
    new Timeline(new KeyFrame(Duration.seconds(4), e -> lblAutoBidStatus.setVisible(false))).play();
  }

  // ═════════════════════════════════════════════════════════════════════════
  //  UTILS
  // ═════════════════════════════════════════════════════════════════════════

  /**
   * Load ảnh sản phẩm từ server theo imagePath — có retry nếu thất bại lần đầu.
   */
  private void loadProductImage(String imagePath) {
    System.out.println("🖼️ [BiddingRoom] Đang load ảnh sản phẩm: " + imagePath);
    new GetItemImageCommand(imagePath).executeAsync(
        res -> {
          if (ServerCommand.isSuccess(res) && res.has("imageBase64")) {
            try {
              String base64 = res.get("imageBase64").getAsString();
              byte[] bytes = Base64.getDecoder().decode(base64);
              Image img = new Image(new ByteArrayInputStream(bytes));
              if (!img.isError()) {
                if (imgProduct != null) imgProduct.setImage(img);
                if (lblImgPlaceholder != null) lblImgPlaceholder.setVisible(false);
                System.out.println("✅ [BiddingRoom] Load ảnh thành công!");
              } else {
                System.err.println("❌ [BiddingRoom] Image bị lỗi sau khi decode.");
              }
            } catch (Exception e) {
              System.err.println("❌ [BiddingRoom] Lỗi decode Base64 ảnh: " + e.getMessage());
            }
          } else {
            String msg = ServerCommand.getMessage(res, "Không rõ lỗi");
            System.err.println("❌ [BiddingRoom] Server trả FAIL khi load ảnh: " + msg);
            // Retry 1 lần sau 2 giây (server có thể chưa sẵn sàng lúc đầu)
            new Timeline(new KeyFrame(Duration.seconds(2), e -> {
              System.out.println("🔄 [BiddingRoom] Retry load ảnh...");
              new GetItemImageCommand(imagePath).executeAsync(retryRes -> {
                if (ServerCommand.isSuccess(retryRes) && retryRes.has("imageBase64")) {
                  try {
                    byte[] bytes = Base64.getDecoder().decode(retryRes.get("imageBase64").getAsString());
                    Image img = new Image(new ByteArrayInputStream(bytes));
                    if (!img.isError()) {
                      if (imgProduct != null) imgProduct.setImage(img);
                      if (lblImgPlaceholder != null) lblImgPlaceholder.setVisible(false);
                      System.out.println("✅ [BiddingRoom] Retry load ảnh thành công!");
                    }
                  } catch (Exception ignored) {}
                } else {
                  System.err.println("❌ [BiddingRoom] Retry cũng thất bại. Giữ placeholder.");
                }
              });
            })).play();
          }
        },
        () -> System.err.println("🔌 [BiddingRoom] Lỗi kết nối khi load ảnh sản phẩm!")
    );
  }

  private Label emptyLabel(String msg) {
    Label l = new Label(msg);
    l.setStyle("-fx-text-fill:#AAAAAA;-fx-font-size:13px;-fx-padding:16;");
    return l;
  }

  private String str(JsonObject o, String key, String def) {
    return (o != null && o.has(key) && !o.get(key).isJsonNull()) ? o.get(key).getAsString() : def;
  }
}