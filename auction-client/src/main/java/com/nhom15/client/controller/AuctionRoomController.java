package com.nhom15.client.controller;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.nhom15.client.command.GetAuctionDetailCommand;
import com.nhom15.client.command.GetBidHistoryCommand;
import com.nhom15.client.command.GetItemImageCommand;
import com.nhom15.client.command.PlaceBidCommand;
import com.nhom15.client.command.ServerCommand;
import com.nhom15.client.network.AuctionRealtimeSubscriber;
import com.nhom15.client.util.SessionManager;
import com.nhom15.client.util.ViewManager;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.io.ByteArrayInputStream;
import java.net.URL;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class AuctionRoomController implements Initializable {

  private static final DateTimeFormatter DT_FMT =
          DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
  private static final DateTimeFormatter CHART_TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");
  private static final int SOFT_CLOSE_SECONDS = 30;
  private static final int MAX_CHART_POINTS = 120;

  // ── FXML nodes ──────────────────────────────────────────────────
  @FXML private Label      lblCountdown;
  @FXML private Label      lblSoftClose;
  @FXML private Label      lblCurrentBid;
  @FXML private Label      lblBidStatus;
  @FXML private TextField  txtCustomBid;
  @FXML private Button     btnQuickBid1;
  @FXML private Button     btnQuickBid2;
  @FXML private Button     btnQuickBid3;
  @FXML private Button     btnConfirmBid;
  @FXML private Button     btnPlaceBid;
  @FXML private HBox       rowWinning;
  @FXML private Circle     dotPulse;
  @FXML private Pane       paneBackground;
  @FXML private VBox       historyContainer;
  @FXML private ImageView  imgProduct;
  @FXML private Label      lblImgPlaceholder;
  @FXML private Label      lblProductName;
  @FXML private Label      lblProductDesc;
  @FXML private Label      lblViewerCount;
  @FXML private Button     btnCloseRoom;
  @FXML private StackPane  timerPane;
  @FXML private LineChart<Number, Number> chartPriceLive;
  @FXML private NumberAxis axisChartX;
  @FXML private NumberAxis axisChartY;
  @FXML private Label      lblChartLastUpdate;

  // ── State ────────────────────────────────────────────────────────
  private int            auctionId     = 0;
  private double         currentBidUSD = 0;
  private double         minStep       = 500;
  private LocalDateTime  auctionEndTime = null;

  private Runnable       onBack;
  private Timeline       countdownTimeline;
  private Timeline       pulseTimeline;
  private Timeline       pollingTimeline;
  private AnimationTimer bgAnimationTimer;
  private MediaPlayer    gifPlayer;
  private AuctionRealtimeSubscriber realtimeSubscriber;
  private XYChart.Series<Number, Number> priceChartSeries;
  private Timeline                 chartTimeline;
  private int                      chartXSeq = 0;

  private final NumberFormat currencyFmt = NumberFormat.getNumberInstance(Locale.US);

  // ─────────────────────────────────────────────────────────────────
  //  INITIALIZE
  // ─────────────────────────────────────────────────────────────────
  @Override
  public void initialize(URL location, ResourceBundle resources) {
    currencyFmt.setGroupingUsed(true);
    currencyFmt.setMaximumFractionDigits(0);
    startPulseDot();
    startBackgroundAnimation();
    attachNumericFilter();
    playGifBackground();
    setupPriceChart();
  }

  // ─────────────────────────────────────────────────────────────────
  //  PUBLIC API
  // ─────────────────────────────────────────────────────────────────

  public void setAuctionId(int id) {
    stopPriceChartTimeline();
    chartXSeq = 0;
    if (priceChartSeries != null) {
      priceChartSeries.getData().clear();
    }
    this.auctionId = id;
    loadAuctionDetail();
    loadBidHistory();
    startPolling();
    startRealtimeWatch(id);
  }

  public void setOnBack(Runnable onBack) {
    this.onBack = onBack;
  }

  // ─────────────────────────────────────────────────────────────────
  //  GIF BACKGROUND — dùng MediaView vì JavaFX không animate GIF
  //  trong -fx-background-image
  // ─────────────────────────────────────────────────────────────────

  private void playGifBackground() {
    if (timerPane == null) return;
    try {
      URL gifUrl = getClass().getResource("/images/gif1.gif");
      if (gifUrl == null) {
        System.err.println("[AuctionRoom] gif1.gif không tìm thấy.");
        return;
      }
      Media media = new Media(gifUrl.toExternalForm());
      gifPlayer = new MediaPlayer(media);
      gifPlayer.setCycleCount(MediaPlayer.INDEFINITE);
      gifPlayer.setMute(true);

      MediaView mv = new MediaView(gifPlayer);
      mv.setFitWidth(370);
      mv.setFitHeight(116);
      mv.setPreserveRatio(false);
      mv.setMouseTransparent(true);

      Rectangle clip = new Rectangle(370, 116);
      clip.setArcWidth(36);
      clip.setArcHeight(36);
      mv.setClip(clip);

      timerPane.getChildren().add(0, mv);
      gifPlayer.play();
    } catch (Exception e) {
      System.err.println("[AuctionRoom] Lỗi khởi tạo GIF: " + e.getMessage());
    }
  }

  // ─────────────────────────────────────────────────────────────────
  //  LOAD FROM SERVER
  // ─────────────────────────────────────────────────────────────────

  private void loadAuctionDetail() {
    new GetAuctionDetailCommand(auctionId).executeAsync(res -> {
      if (!ServerCommand.isSuccess(res) || !res.has("auction")) return;
      JsonObject a = res.getAsJsonObject("auction");

      double price = dbl(a, "currentPrice", dbl(a, "startPrice", 0));
      double step  = dbl(a, "minStep", dbl(a, "bidStep", 500));
      String endStr = str(a, "endTime", null);

      Platform.runLater(() -> {
        currentBidUSD = price;
        minStep = step;
        refreshBidDisplay();
        setText(lblProductName, str(a, "name", "Sản phẩm đấu giá"));
        setText(lblProductDesc, str(a, "description", ""));
        updateQuickBidLabels();
        if (endStr != null) {
          try {
            auctionEndTime = LocalDateTime.parse(endStr, DT_FMT);
            startCountdown();
          } catch (Exception ex) {
            System.err.println("[AuctionRoom] endTime parse lỗi: " + endStr);
          }
        }
        seedPriceChartFromCurrentState();
      });

      String imageId = str(a, "imageId", str(a, "imagePath", str(a, "image", null)));
      if (imageId != null && !imageId.isBlank()) loadProductImage(imageId);
    });
  }

  private void loadProductImage(String imageId) {
    new GetItemImageCommand(imageId).executeAsync(res -> {
      Platform.runLater(() -> {
        if (!ServerCommand.isSuccess(res) || !res.has("imageBase64")) return;
        try {
          String b64 = res.get("imageBase64").getAsString();
          if (b64 == null || b64.isBlank()) return;
          byte[] bytes = Base64.getDecoder().decode(b64.trim());
          Image img = new Image(new ByteArrayInputStream(bytes));
          if (!img.isError() && imgProduct != null) {
            imgProduct.setImage(img);
            imgProduct.setVisible(true);
            if (lblImgPlaceholder != null) lblImgPlaceholder.setVisible(false);
          }
        } catch (Exception e) {
          System.err.println("[AuctionRoom] Lỗi decode ảnh: " + e.getMessage());
        }
      });
    });
  }

  private void loadBidHistory() {
    if (auctionId == 0) return;
    new GetBidHistoryCommand(auctionId).executeAsync(res -> {
      if (res == null || !res.has("history")) return;
      JsonArray history = res.getAsJsonArray("history");
      Platform.runLater(() -> rebuildHistoryUI(history));
    });
  }

  private void rebuildHistoryUI(JsonArray history) {
    if (historyContainer == null) return;
    // Xóa các bid row cũ, giữ 2 node đầu (card-header + col-header)
    while (historyContainer.getChildren().size() > 2) {
      historyContainer.getChildren().remove(2);
    }
    if (history == null || history.size() == 0) return;

    List<JsonObject> items = new ArrayList<>();
    for (int i = 0; i < history.size(); i++) items.add(history.get(i).getAsJsonObject());
    Collections.reverse(items); // mới nhất lên đầu

    String myUser = SessionManager.getUsername();
    for (int i = 0; i < Math.min(items.size(), 8); i++) {
      JsonObject bid = items.get(i);
      double amount = dbl(bid, "amount", dbl(bid, "bidAmount", 0));
      String user   = str(bid, "username", str(bid, "bidderUsername", "***"));
      String time   = str(bid, "bidTime", str(bid, "createdAt", "--:--:--"));
      String alias  = maskUsername(user, myUser);
      String timeDisplay = time.length() >= 19 ? time.substring(11, 19) : time;
      historyContainer.getChildren().add(buildBidRow(timeDisplay, alias, amount, i == 0));
    }

    // Đồng bộ giá cao nhất
    if (!items.isEmpty()) {
      double topPrice = dbl(items.get(0), "amount", dbl(items.get(0), "bidAmount", 0));
      if (topPrice > currentBidUSD) {
        currentBidUSD = topPrice;
        refreshBidDisplay();
        appendPriceChartPoint(topPrice);
      }
    }
  }

  private String maskUsername(String username, String myUsername) {
    if (username == null || username.length() < 2) return "***";
    if (username.equals(myUsername)) return "Bạn";
    return Character.toUpperCase(username.charAt(0))
            + "***"
            + username.charAt(username.length() - 1);
  }

  // ─────────────────────────────────────────────────────────────────
  //  COUNTDOWN (endTime từ server)
  // ─────────────────────────────────────────────────────────────────

  private void startCountdown() {
    if (countdownTimeline != null) countdownTimeline.stop();
    countdownTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> tickTimer()));
    countdownTimeline.setCycleCount(Timeline.INDEFINITE);
    countdownTimeline.play();
    tickTimer();
  }

  private void tickTimer() {
    if (auctionEndTime == null) return;
    LocalDateTime now = LocalDateTime.now();
    if (now.isAfter(auctionEndTime)) { onAuctionEnd(); return; }
    long total   = ChronoUnit.SECONDS.between(now, auctionEndTime);
    long hours   = total / 3600;
    long minutes = (total % 3600) / 60;
    long seconds = total % 60;
    setText(lblCountdown, String.format("%02d:%02d:%02d", hours, minutes, seconds));
    if (total <= 10) animateTimerWarning();
  }

  private void applySoftClose() {
    if (auctionEndTime == null) return;
    long remaining = ChronoUnit.SECONDS.between(LocalDateTime.now(), auctionEndTime);
    if (remaining < SOFT_CLOSE_SECONDS) {
      auctionEndTime = LocalDateTime.now().plusSeconds(SOFT_CLOSE_SECONDS);
      setText(lblSoftClose, "Luật Soft Close — Đã gia hạn thêm " + SOFT_CLOSE_SECONDS + " giây!");
      animateSoftCloseNotice();
      PauseTransition revert = new PauseTransition(Duration.seconds(4));
      revert.setOnFinished(e -> setText(lblSoftClose, "Luật Soft Close — Đếm ngược 30 giây"));
      revert.play();
    }
  }

  private void animateTimerWarning() {
    if (lblCountdown == null) return;
    ScaleTransition st = new ScaleTransition(Duration.millis(200), lblCountdown);
    st.setFromX(1.0); st.setToX(1.06); st.setFromY(1.0); st.setToY(1.06);
    st.setAutoReverse(true); st.setCycleCount(2); st.play();
  }

  private void animateSoftCloseNotice() {
    if (lblSoftClose == null) return;
    FadeTransition ft = new FadeTransition(Duration.millis(300), lblSoftClose);
    ft.setFromValue(0.4); ft.setToValue(1.0); ft.play();
  }

  private void onAuctionEnd() {
    if (countdownTimeline != null) countdownTimeline.stop();
    if (pollingTimeline   != null) pollingTimeline.stop();
    stopPriceChartTimeline();
    setText(lblCountdown, "00:00:00");
    setText(lblBidStatus, "⏹ Phiên đấu giá kết thúc");
    if (lblBidStatus != null)
      lblBidStatus.setStyle("-fx-text-fill: #D96570; -fx-font-size: 12.5; -fx-font-weight: bold;");
    setButtonsDisabled(true);
  }

  // ─────────────────────────────────────────────────────────────────
  //  POLLING mỗi 5s
  // ─────────────────────────────────────────────────────────────────

  private void startPolling() {
    pollingTimeline = new Timeline(new KeyFrame(Duration.seconds(5), e -> poll()));
    pollingTimeline.setCycleCount(Timeline.INDEFINITE);
    pollingTimeline.play();
  }

  private void poll() {
    if (auctionId == 0) return;
    new GetAuctionDetailCommand(auctionId).executeAsync(res -> {
      if (!ServerCommand.isSuccess(res) || !res.has("auction")) return;
      JsonObject a = res.getAsJsonObject("auction");
      double price = dbl(a, "currentPrice", 0);
      int viewers  = (int) dbl(a, "viewerCount", dbl(a, "activeViewers", dbl(a, "viewers", 0)));

      Platform.runLater(() -> {
        if (price > currentBidUSD) {
          currentBidUSD = price;
          refreshBidDisplay();
          animateBidUpdate();
          applySoftClose();
          appendPriceChartPoint(price);
          loadBidHistory();
        }
        if (viewers > 0 && lblViewerCount != null)
          lblViewerCount.setText("\uD83D\uDC65  " + viewers + " người đang xem");
      });
    });
  }

  // ─────────────────────────────────────────────────────────────────
  //  REALTIME SUBSCRIBE
  // ─────────────────────────────────────────────────────────────────

  private void startRealtimeWatch(int id) {
    try {
      realtimeSubscriber = new AuctionRealtimeSubscriber();
      realtimeSubscriber.start(id, envelope -> {
        if (envelope == null) {
          return;
        }
        // Server gửi { "action":"AUCTION_UPDATE", "data": { "currentPrice", ... } }
        JsonObject d = (envelope.has("data") && envelope.get("data").isJsonObject())
                ? envelope.getAsJsonObject("data") : envelope;
        double newPrice = dbl(d, "currentPrice", 0);
        if (newPrice > currentBidUSD) {
          currentBidUSD = newPrice;
          refreshBidDisplay();
          animateBidUpdate();
          applySoftClose();
          appendPriceChartPoint(newPrice);
          loadBidHistory();
        }
      });
    } catch (Exception e) {
      System.err.println("[AuctionRoom] Realtime lỗi: " + e.getMessage());
    }
  }

  private void stopRealtimeWatch() {
    if (realtimeSubscriber != null) {
      try { realtimeSubscriber.stop(); } catch (Exception ignored) {}
    }
  }

  // ─────────────────────────────────────────────────────────────────
  //  BID HANDLERS
  // ─────────────────────────────────────────────────────────────────

  private void submitBid(double amount) {
    if (!SessionManager.isLoggedIn()) {
      showAlert("Chưa đăng nhập", "Bạn cần đăng nhập để đặt giá."); return;
    }
    double minNext = currentBidUSD + minStep;
    if (amount < minNext) {
      showAlert("Giá không hợp lệ",
              String.format("Giá tối thiểu: %s (hiện tại %s + bước %s).",
                      formatUSD(minNext), formatUSD(currentBidUSD), formatUSD(minStep)));
      return;
    }
    new PlaceBidCommand(auctionId, SessionManager.getUserId(), amount).executeAsync(res -> {
      Platform.runLater(() -> {
        if (ServerCommand.isSuccess(res)) {
          double shown = (res != null && res.has("currentPrice"))
                  ? res.get("currentPrice").getAsDouble() : amount;
          currentBidUSD = shown;
          refreshBidDisplay(); applySoftClose(); animateBidUpdate();
          appendPriceChartPoint(shown);
          loadBidHistory();
          if (txtCustomBid != null) txtCustomBid.clear();
        } else {
          String msg = (res != null && res.has("message"))
                  ? res.get("message").getAsString() : "Đặt giá thất bại.";
          showAlert("Lỗi đặt giá", msg);
        }
      });
    });
  }

  @FXML private void handleQuickBid1() { submitBid(currentBidUSD + minStep); }
  @FXML private void handleQuickBid2() { submitBid(currentBidUSD + minStep * 2); }
  @FXML private void handleQuickBid3() { submitBid(currentBidUSD + minStep * 5); }

  @FXML private void handleConfirmBid() {
    if (txtCustomBid == null) return;
    String raw = txtCustomBid.getText().trim().replaceAll("[,$]", "");
    if (raw.isEmpty()) { shakeNode(txtCustomBid); return; }
    try { submitBid(Double.parseDouble(raw)); }
    catch (NumberFormatException e) {
      shakeNode(txtCustomBid); showAlert("Sai định dạng", "Chỉ nhập số (ví dụ: 500000).");
    }
  }

  @FXML private void handlePlaceBid() { submitBid(currentBidUSD + minStep); }

  @FXML
  private void handleCloseRoom() {
    cleanup();
    if (onBack != null) onBack.run();
    else ViewManager.navigateTo(ViewManager.Views.HOME);
  }

  // ─────────────────────────────────────────────────────────────────
  //  DISPLAY
  // ─────────────────────────────────────────────────────────────────

  private void refreshBidDisplay() { setText(lblCurrentBid, formatUSD(currentBidUSD)); }

  private void updateQuickBidLabels() {
    if (btnQuickBid1 != null) btnQuickBid1.setText("+" + formatUSD(minStep));
    if (btnQuickBid2 != null) btnQuickBid2.setText("+" + formatUSD(minStep * 2));
    if (btnQuickBid3 != null) btnQuickBid3.setText("+" + formatUSD(minStep * 5));
  }

  private void animateBidUpdate() {
    if (lblCurrentBid == null) return;
    ScaleTransition st = new ScaleTransition(Duration.millis(120), lblCurrentBid);
    st.setFromX(1.0); st.setToX(1.12); st.setFromY(1.0); st.setToY(1.12);
    st.setAutoReverse(true); st.setCycleCount(2); st.play();
    FadeTransition ft = new FadeTransition(Duration.millis(300), lblCurrentBid);
    ft.setFromValue(0.5); ft.setToValue(1.0); ft.play();
  }

  private HBox buildBidRow(String time, String user, double amount, boolean isWinning) {
    String clr = isWinning ? "#2E7D32" : "#606368";
    String bg  = isWinning ? "-fx-background-color: #E8F5E9;" : "";
    String fw  = isWinning ? "-fx-font-weight: bold;" : "";
    double pad = isWinning ? 11 : 9;
    HBox row = new HBox();
    row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
    row.setStyle(bg);
    row.setPadding(new Insets(pad, 18, pad, 18));
    row.getChildren().addAll(
            styledLabel(time,            clr, 12.5, 88,  fw),
            styledLabel(user,            clr, 12.5, 100, fw),
            styledLabel(formatUSD(amount), clr, 12.5, -1, fw)
    );
    if (isWinning) {
      FadeTransition ft = new FadeTransition(Duration.millis(400), row);
      ft.setFromValue(0); ft.setToValue(1); ft.play();
    }
    return row;
  }

  private Label styledLabel(String text, String color, double size, double w, String extra) {
    Label lbl = new Label(text);
    lbl.setStyle(String.format("-fx-text-fill: %s; -fx-font-size: %.1f; %s", color, size, extra));
    if (w > 0) lbl.setPrefWidth(w);
    return lbl;
  }

  // ─────────────────────────────────────────────────────────────────
  //  PULSE DOT
  // ─────────────────────────────────────────────────────────────────

  private void startPulseDot() {
    if (dotPulse == null) return;
    pulseTimeline = new Timeline(
            new KeyFrame(Duration.ZERO,       new KeyValue(dotPulse.opacityProperty(), 1.0)),
            new KeyFrame(Duration.millis(800), new KeyValue(dotPulse.opacityProperty(), 0.15))
    );
    pulseTimeline.setAutoReverse(true);
    pulseTimeline.setCycleCount(Timeline.INDEFINITE);
    pulseTimeline.play();
  }

  // ─────────────────────────────────────────────────────────────────
  //  ANIMATED BG
  // ─────────────────────────────────────────────────────────────────

  private static final int     BG_CIRCLES = 6;
  private final double[]       cx = new double[BG_CIRCLES];
  private final double[]       cy = new double[BG_CIRCLES];
  private final double[]       vx = new double[BG_CIRCLES];
  private final double[]       vy = new double[BG_CIRCLES];
  private final double[]       cr = new double[BG_CIRCLES];
  private final Circle[]  bgCircles = new Circle[BG_CIRCLES];
  private static final Color[] BG_COLORS = {
          Color.web("#4285F4",0.06), Color.web("#9B72CB",0.05), Color.web("#D96570",0.04),
          Color.web("#4285F4",0.05), Color.web("#9B72CB",0.06), Color.web("#D96570",0.05),
  };

  private void startBackgroundAnimation() {
    if (paneBackground == null) return;
    Random rng = new Random();
    for (int i = 0; i < BG_CIRCLES; i++) {
      cr[i] = 120 + rng.nextDouble() * 180;
      cx[i] = rng.nextDouble() * 1280; cy[i] = rng.nextDouble() * 900;
      vx[i] = (rng.nextDouble() - 0.5) * 0.4; vy[i] = (rng.nextDouble() - 0.5) * 0.4;
      bgCircles[i] = new Circle(cx[i], cy[i], cr[i], BG_COLORS[i]);
      paneBackground.getChildren().add(bgCircles[i]);
    }
    bgAnimationTimer = new AnimationTimer() {
      @Override public void handle(long now) {
        double W = paneBackground.getWidth()  > 0 ? paneBackground.getWidth()  : 1280;
        double H = paneBackground.getHeight() > 0 ? paneBackground.getHeight() : 900;
        for (int i = 0; i < BG_CIRCLES; i++) {
          cx[i] += vx[i]; cy[i] += vy[i];
          if (cx[i]-cr[i]<0 || cx[i]+cr[i]>W) vx[i]=-vx[i];
          if (cy[i]-cr[i]<0 || cy[i]+cr[i]>H) vy[i]=-vy[i];
          bgCircles[i].setCenterX(cx[i]); bgCircles[i].setCenterY(cy[i]);
        }
      }
    };
    bgAnimationTimer.start();
  }

  // ─────────────────────────────────────────────────────────────────
  //  CLEANUP
  // ─────────────────────────────────────────────────────────────────

  public void cleanup() {
    if (countdownTimeline != null) countdownTimeline.stop();
    if (pulseTimeline     != null) pulseTimeline.stop();
    if (pollingTimeline   != null) pollingTimeline.stop();
    stopPriceChartTimeline();
    if (bgAnimationTimer  != null) bgAnimationTimer.stop();
    if (gifPlayer         != null) gifPlayer.stop();
    stopRealtimeWatch();
  }

  // ─────────────────────────────────────────────────────────────────
  //  LIVE PRICE CHART (1s tick + điểm mới mỗi lần giá tăng)
  // ─────────────────────────────────────────────────────────────────

  private void setupPriceChart() {
    if (chartPriceLive == null) {
      return;
    }
    chartPriceLive.setAnimated(false);
    chartPriceLive.setLegendVisible(false);
    chartPriceLive.setCreateSymbols(true);
    priceChartSeries = new XYChart.Series<>();
    priceChartSeries.setName("Giá");
    chartPriceLive.getData().clear();
    chartPriceLive.getData().add(priceChartSeries);
    if (axisChartX != null) {
      axisChartX.setForceZeroInRange(false);
    }
    if (axisChartY != null) {
      axisChartY.setForceZeroInRange(false);
    }
  }

  /** Gọi sau khi đã có currentBidUSD từ server (load phiên). */
  private void seedPriceChartFromCurrentState() {
    if (priceChartSeries == null || chartPriceLive == null || auctionId == 0) {
      return;
    }
    if (!priceChartSeries.getData().isEmpty()) {
      startPriceChartTimeline();
      return;
    }
    appendPriceChartPoint(currentBidUSD);
    startPriceChartTimeline();
  }

  private void startPriceChartTimeline() {
    if (chartPriceLive == null || auctionId == 0) {
      return;
    }
    stopPriceChartTimeline();
    chartTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
      if (auctionId == 0 || priceChartSeries == null) {
        return;
      }
      appendPriceChartPoint(currentBidUSD);
    }));
    chartTimeline.setCycleCount(Timeline.INDEFINITE);
    chartTimeline.play();
  }

  private void stopPriceChartTimeline() {
    if (chartTimeline != null) {
      chartTimeline.stop();
      chartTimeline = null;
    }
  }

  private void appendPriceChartPoint(double y) {
    if (priceChartSeries == null || chartPriceLive == null || auctionId == 0) {
      return;
    }
    chartXSeq++;
    priceChartSeries.getData().add(new XYChart.Data<>(chartXSeq, y));
    while (priceChartSeries.getData().size() > MAX_CHART_POINTS) {
      priceChartSeries.getData().remove(0);
    }
    if (lblChartLastUpdate != null) {
      lblChartLastUpdate.setText(LocalDateTime.now().format(CHART_TIME_FMT));
    }
  }

  // ─────────────────────────────────────────────────────────────────
  //  UTIL
  // ─────────────────────────────────────────────────────────────────

  private void attachNumericFilter() {
    if (txtCustomBid == null) return;
    txtCustomBid.textProperty().addListener((obs, o, n) -> {
      if (!n.matches("[0-9]*")) txtCustomBid.setText(n.replaceAll("[^0-9]", ""));
    });
  }

  private String formatUSD(double amount) {
    return "$" + currencyFmt.format((long) amount) + " USD";
  }

  private void setButtonsDisabled(boolean d) {
    if (btnConfirmBid != null) btnConfirmBid.setDisable(d);
    if (btnPlaceBid   != null) btnPlaceBid.setDisable(d);
    if (btnQuickBid1  != null) btnQuickBid1.setDisable(d);
    if (btnQuickBid2  != null) btnQuickBid2.setDisable(d);
    if (btnQuickBid3  != null) btnQuickBid3.setDisable(d);
    if (txtCustomBid  != null) txtCustomBid.setDisable(d);
  }

  private void setText(Label lbl, String val) { if (lbl != null) lbl.setText(val); }

  private void shakeNode(javafx.scene.Node node) {
    TranslateTransition tt = new TranslateTransition(Duration.millis(60), node);
    tt.setByX(8); tt.setCycleCount(6); tt.setAutoReverse(true);
    tt.setOnFinished(e -> node.setTranslateX(0)); tt.play();
  }

  private void showAlert(String title, String msg) {
    Platform.runLater(() -> {
      Alert a = new Alert(Alert.AlertType.INFORMATION);
      a.setTitle(title); a.setHeaderText(null); a.setContentText(msg);
      a.getDialogPane().setStyle("-fx-background-color: white; -fx-font-size: 13;");
      a.showAndWait();
    });
  }

  private String str(JsonObject o, String key, String def) {
    return (o!=null && o.has(key) && !o.get(key).isJsonNull()) ? o.get(key).getAsString() : def;
  }
  private double dbl(JsonObject o, String key, double def) {
    return (o!=null && o.has(key) && !o.get(key).isJsonNull()) ? o.get(key).getAsDouble() : def;
  }
}
