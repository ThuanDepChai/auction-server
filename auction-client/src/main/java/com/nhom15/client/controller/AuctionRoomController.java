package com.nhom15.client.controller;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.net.URL;
import java.text.NumberFormat;
import java.util.*;

/**
 * ╔══════════════════════════════════════════════════════════════════╗
 * ║  AuctionRoomController — Gemini Live Auction Room               ║
 * ║  Handles:                                                       ║
 * ║   • Real-time countdown timer (with Soft Close rule)           ║
 * ║   • Current bid display & update                               ║
 * ║   • Quick bid buttons (+$500 / +$1,000 / +$2,500)             ║
 * ║   • Custom bid input validation                                 ║
 * ║   • Live bid history ledger                                     ║
 * ║   • Animated background (floating gradient circles)            ║
 * ║   • Pulsing LIVE dot animation                                  ║
 * ╚══════════════════════════════════════════════════════════════════╝
 *
 *  fx:id map (must match AuctionRoom.fxml exactly)
 *  ─────────────────────────────────────────────────
 *  lblCountdown    — HH:MM:SS countdown label
 *  lblSoftClose    — soft-close caption label
 *  lblCurrentBid   — current highest bid amount
 *  lblBidStatus    — "Mức giá sàn đã đạt" status
 *  txtCustomBid    — free-text custom bid TextField
 *  btnQuickBid1/2/3— +$500 / +$1,000 / +$2,500
 *  btnConfirmBid   — primary CTA
 *  btnPlaceBid     — secondary CTA
 *  rowWinning      — green-highlighted winning row HBox
 *  dotPulse        — red Circle pulse indicator
 *  paneBackground  — animated bg Pane
 *  historyContainer— VBox that holds live bid rows (optional fx:id)
 */
public class AuctionRoomController implements Initializable {

  // ── Constants ───────────────────────────────────────────────────
  private static final int    SOFT_CLOSE_SECONDS  = 30;   // restart timer after each new bid
  private static final double BID_INCREMENT       = 500;  // minimum increment (USD)
  private static final double INITIAL_TIMER_SECS  = 24;   // starting seconds on load
  private static final double DEPOSIT_AMOUNT      = 1_000;
  private static final Locale VN_LOCALE           = new Locale("vi", "VN");

  // ── FXML injected nodes ─────────────────────────────────────────
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

  // Optional — VBox that wraps ALL bid history rows inside the card.
  // If you give this fx:id in FXML, the controller will prepend new rows
  // dynamically. If not wired, history rows remain static.
  @FXML private VBox       historyContainer;

  // ── State ────────────────────────────────────────────────────────
  private double  currentBidUSD   = 15_800;
  private double  remainingSeconds = INITIAL_TIMER_SECS;
  private boolean userEligible    = true;   // deposit confirmed

  // Timers & animations
  private Timeline          countdownTimeline;
  private Timeline          pulseTimeline;
  private AnimationTimer    bgAnimationTimer;

  // Bid history (newest first)
  private final Deque<BidRecord> bidHistory = new ArrayDeque<>();

  // Number formatter  →  $15,800 USD
  private final NumberFormat currencyFmt = NumberFormat.getNumberInstance(Locale.US);

  // ─────────────────────────────────────────────────────────────────
  //  INITIALISE
  // ─────────────────────────────────────────────────────────────────
  @Override
  public void initialize(URL location, ResourceBundle resources) {
    currencyFmt.setGroupingUsed(true);
    currencyFmt.setMaximumFractionDigits(0);

    // Seed history with the static rows already in FXML
    seedBidHistory();

    // Start all live features
    startCountdown();
    startPulseDot();
    startBackgroundAnimation();

    // Refresh UI
    refreshBidDisplay();
    refreshTimerLabel();

    // Eligibility guard
    setUserEligible(userEligible);

    // Numeric-only filter on custom bid field
    attachNumericFilter();
  }

  // ─────────────────────────────────────────────────────────────────
  //  COUNTDOWN TIMER
  // ─────────────────────────────────────────────────────────────────

  /** Starts (or restarts) the real-time countdown. */
  private void startCountdown() {
    if (countdownTimeline != null) {
      countdownTimeline.stop();
    }

    countdownTimeline = new Timeline(
        new KeyFrame(Duration.seconds(1), e -> tickTimer())
    );
    countdownTimeline.setCycleCount(Timeline.INDEFINITE);
    countdownTimeline.play();
  }

  private void tickTimer() {
    if (remainingSeconds > 0) {
      remainingSeconds--;
      refreshTimerLabel();

      // Flash timer red when ≤ 10 s remain
      if (remainingSeconds <= 10) {
        animateTimerWarning();
      }
    } else {
      // Auction ended
      onAuctionEnd();
    }
  }

  /** Formats seconds → "HH:MM:SS" and updates the label. */
  private void refreshTimerLabel() {
    int total   = (int) remainingSeconds;
    int hours   = total / 3600;
    int minutes = (total % 3600) / 60;
    int seconds = total % 60;
    lblCountdown.setText(String.format("%02d:%02d:%02d", hours, minutes, seconds));
  }

  /**
   * Soft-Close rule: if a new bid arrives within the last SOFT_CLOSE_SECONDS,
   * reset the countdown to SOFT_CLOSE_SECONDS.
   */
  private void applySoftClose() {
    if (remainingSeconds < SOFT_CLOSE_SECONDS) {
      remainingSeconds = SOFT_CLOSE_SECONDS;
      lblSoftClose.setText("Luật Soft Close — Đã gia hạn thêm " + SOFT_CLOSE_SECONDS + " giây!");
      animateSoftCloseNotice();

      // Revert caption after 4 seconds
      PauseTransition revert = new PauseTransition(Duration.seconds(4));
      revert.setOnFinished(e ->
          lblSoftClose.setText("Luật Soft Close — Đếm ngược " + SOFT_CLOSE_SECONDS + " giây")
      );
      revert.play();
    }
  }

  /** Brief flash/scale on the timer label when ≤ 10 s remain. */
  private void animateTimerWarning() {
    ScaleTransition st = new ScaleTransition(Duration.millis(200), lblCountdown);
    st.setFromX(1.0); st.setToX(1.06);
    st.setFromY(1.0); st.setToY(1.06);
    st.setAutoReverse(true);
    st.setCycleCount(2);
    st.play();
  }

  private void animateSoftCloseNotice() {
    FadeTransition ft = new FadeTransition(Duration.millis(300), lblSoftClose);
    ft.setFromValue(0.4); ft.setToValue(1.0);
    ft.play();
  }

  private void onAuctionEnd() {
    countdownTimeline.stop();
    lblCountdown.setText("00:00:00");
    lblBidStatus.setText("⏹ Phiên đấu giá kết thúc");
    lblBidStatus.setStyle("-fx-text-fill: #D96570; -fx-font-size: 12.5; -fx-font-weight: bold;");
    setButtonsDisabled(true);
    showAlert("Phiên Đấu Giá Kết Thúc",
        "Người chiến thắng với giá " + formatUSD(currentBidUSD) + "!\nCảm ơn bạn đã tham gia.");
  }

  // ─────────────────────────────────────────────────────────────────
  //  PULSE DOT ANIMATION  (red circle ● blinks every 800 ms)
  // ─────────────────────────────────────────────────────────────────
  private void startPulseDot() {
    if (dotPulse == null) return;

    pulseTimeline = new Timeline(
        new KeyFrame(Duration.ZERO,
            new KeyValue(dotPulse.opacityProperty(), 1.0)),
        new KeyFrame(Duration.millis(800),
            new KeyValue(dotPulse.opacityProperty(), 0.15))
    );
    pulseTimeline.setAutoReverse(true);
    pulseTimeline.setCycleCount(Timeline.INDEFINITE);
    pulseTimeline.play();
  }

  // ─────────────────────────────────────────────────────────────────
  //  ANIMATED BACKGROUND  (floating translucent gradient circles)
  // ─────────────────────────────────────────────────────────────────
  private static final int   BG_CIRCLES = 6;
  private final double[]     cx  = new double[BG_CIRCLES];
  private final double[]     cy  = new double[BG_CIRCLES];
  private final double[]     vx  = new double[BG_CIRCLES];
  private final double[]     vy  = new double[BG_CIRCLES];
  private final double[]     cr  = new double[BG_CIRCLES];
  private final Circle[]     bgCircles = new Circle[BG_CIRCLES];

  private static final Color[] BG_COLORS = {
      Color.web("#4285F4", 0.06),
      Color.web("#9B72CB", 0.05),
      Color.web("#D96570", 0.04),
      Color.web("#4285F4", 0.05),
      Color.web("#9B72CB", 0.06),
      Color.web("#D96570", 0.05),
  };

  private void startBackgroundAnimation() {
    if (paneBackground == null) return;

    Random rng = new Random();
    double W = 1280, H = 900;

    for (int i = 0; i < BG_CIRCLES; i++) {
      cr[i] = 120 + rng.nextDouble() * 180;
      cx[i] = rng.nextDouble() * W;
      cy[i] = rng.nextDouble() * H;
      vx[i] = (rng.nextDouble() - 0.5) * 0.4;
      vy[i] = (rng.nextDouble() - 0.5) * 0.4;

      Circle c = new Circle(cx[i], cy[i], cr[i], BG_COLORS[i]);
      bgCircles[i] = c;
      paneBackground.getChildren().add(c);
    }

    bgAnimationTimer = new AnimationTimer() {
      @Override
      public void handle(long now) {
        double W = paneBackground.getWidth()  > 0 ? paneBackground.getWidth()  : 1280;
        double H = paneBackground.getHeight() > 0 ? paneBackground.getHeight() : 900;
        for (int i = 0; i < BG_CIRCLES; i++) {
          cx[i] += vx[i];
          cy[i] += vy[i];
          // Bounce off edges
          if (cx[i] - cr[i] < 0 || cx[i] + cr[i] > W) vx[i] = -vx[i];
          if (cy[i] - cr[i] < 0 || cy[i] + cr[i] > H) vy[i] = -vy[i];
          bgCircles[i].setCenterX(cx[i]);
          bgCircles[i].setCenterY(cy[i]);
        }
      }
    };
    bgAnimationTimer.start();
  }

  // ─────────────────────────────────────────────────────────────────
  //  BID LOGIC
  // ─────────────────────────────────────────────────────────────────

  /** Called by all bid paths after validation succeeds. */
  private void submitBid(double amount) {
    if (!userEligible) {
      showAlert("Không đủ điều kiện",
          "Bạn chưa đặt cọc $" + currencyFmt.format(DEPOSIT_AMOUNT) + " để tham gia đấu giá.");
      return;
    }

    double minNextBid = currentBidUSD + BID_INCREMENT;
    if (amount < minNextBid) {
      showAlert("Giá không hợp lệ",
          String.format("Giá đặt tối thiểu là %s (hiện tại %s + bước %s).",
              formatUSD(minNextBid),
              formatUSD(currentBidUSD),
              formatUSD(BID_INCREMENT)));
      return;
    }

    // Accept bid
    double prevBid = currentBidUSD;
    currentBidUSD = amount;

    // Soft-Close rule
    applySoftClose();

    // Update display
    refreshBidDisplay();

    // Prepend to history ledger
    String time = getCurrentTimeDisplay();
    BidRecord record = new BidRecord(time, "B***n", amount);
    bidHistory.addFirst(record);
    prependBidRow(record, true);

    // Animate the current-bid label
    animateBidUpdate();

    // Clear custom input
    txtCustomBid.clear();

    System.out.printf("[BID] %.0f → %.0f  (+%.0f)%n", prevBid, amount, amount - prevBid);
  }

  // ── Quick Bid Handlers ────────────────────────────────────────────

  @FXML
  private void handleQuickBid1() {
    submitBid(currentBidUSD + BID_INCREMENT);          // +$500
  }

  @FXML
  private void handleQuickBid2() {
    submitBid(currentBidUSD + BID_INCREMENT * 2);      // +$1,000
  }

  @FXML
  private void handleQuickBid3() {
    submitBid(currentBidUSD + BID_INCREMENT * 5);      // +$2,500
  }

  // ── Confirm (custom input) ────────────────────────────────────────

  @FXML
  private void handleConfirmBid() {
    String raw = txtCustomBid.getText().trim().replaceAll("[,$]", "");
    if (raw.isEmpty()) {
      shakeNode(txtCustomBid);
      showAlert("Chưa nhập giá", "Vui lòng nhập số tiền đặt giá vào ô bên trên.");
      return;
    }
    try {
      double amount = Double.parseDouble(raw);
      submitBid(amount);
    } catch (NumberFormatException e) {
      shakeNode(txtCustomBid);
      showAlert("Định dạng không hợp lệ", "Vui lòng chỉ nhập số (ví dụ: 16500).");
    }
  }

  // ── Place Bid (quick / secondary) ────────────────────────────────

  @FXML
  private void handlePlaceBid() {
    // "Đặt giá ngay" defaults to 1× increment
    submitBid(currentBidUSD + BID_INCREMENT);
  }

  // ─────────────────────────────────────────────────────────────────
  //  DISPLAY HELPERS
  // ─────────────────────────────────────────────────────────────────

  private void refreshBidDisplay() {
    lblCurrentBid.setText(formatUSD(currentBidUSD));
  }

  /** Scale-up animation on the current bid label after a new bid. */
  private void animateBidUpdate() {
    ScaleTransition st = new ScaleTransition(Duration.millis(120), lblCurrentBid);
    st.setFromX(1.0); st.setToX(1.12);
    st.setFromY(1.0); st.setToY(1.12);
    st.setAutoReverse(true);
    st.setCycleCount(2);
    st.play();

    FadeTransition ft = new FadeTransition(Duration.millis(300), lblCurrentBid);
    ft.setFromValue(0.5); ft.setToValue(1.0);
    ft.play();
  }

  /**
   * Dynamically prepends a new bid row to historyContainer (if wired).
   * If historyContainer is null, history remains static (FXML rows only).
   */
  private void prependBidRow(BidRecord record, boolean isWinning) {
    if (historyContainer == null) {
      // Fallback: update the winning row labels in-place
      updateWinningRowLabels(record);
      return;
    }

    HBox row = buildBidRow(record, isWinning);

    // Winning rows go right after the column-header row (index 1)
    // Non-winning rows go after the winning row (index 2)
    int insertIndex = isWinning ? 1 : 2;
    if (insertIndex > historyContainer.getChildren().size()) {
      insertIndex = historyContainer.getChildren().size();
    }
    historyContainer.getChildren().add(insertIndex, row);

    // Keep ledger to 8 visible rows (remove oldest)
    trimHistoryRows();

    // Fade-in the new row
    FadeTransition ft = new FadeTransition(Duration.millis(400), row);
    ft.setFromValue(0); ft.setToValue(1);
    ft.play();
  }

  /** Updates the static FXML rowWinning HBox labels with latest bid. */
  private void updateWinningRowLabels(BidRecord record) {
    if (rowWinning == null) return;
    List<javafx.scene.Node> nodes = rowWinning.getChildren();
    if (nodes.size() >= 3) {
      ((Label) nodes.get(0)).setText(record.time());
      ((Label) nodes.get(1)).setText(record.user());
      ((Label) nodes.get(2)).setText(formatUSD(record.amount()));
    }
  }

  /** Programmatically builds a bid history HBox row. */
  private HBox buildBidRow(BidRecord record, boolean isWinning) {
    String textColor   = isWinning ? "#2E7D32" : "#606368";
    String bgColor     = isWinning ? "-fx-background-color: #E8F5E9;" : "";
    String fontWeight  = isWinning ? "-fx-font-weight: bold;" : "";

    HBox row = new HBox();
    row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
    row.setStyle(bgColor);
    row.setPadding(new Insets(isWinning ? 11 : 9, 18, isWinning ? 11 : 9, 18));

    row.getChildren().addAll(
        styledLabel(record.time(),            textColor, 12.5, 88,  fontWeight),
        styledLabel(record.user(),            textColor, 12.5, 100, fontWeight),
        styledLabel(formatUSD(record.amount()), textColor, 12.5, -1, fontWeight)
    );
    return row;
  }

  private Label styledLabel(String text, String color, double fontSize,
      double prefW, String extra) {
    Label lbl = new Label(text);
    lbl.setStyle(String.format(
        "-fx-text-fill: %s; -fx-font-size: %.1f; %s", color, fontSize, extra));
    lbl.setFont(javafx.scene.text.Font.font(fontSize));
    if (prefW > 0) lbl.setPrefWidth(prefW);
    return lbl;
  }

  /**
   * Keeps the history container to a maximum of 8 data rows
   * (header + 8 bid rows + view-all button).
   */
  private void trimHistoryRows() {
    if (historyContainer == null) return;
    // Children layout: [cardHeader HBox] [colHeader HBox] [rows…] [viewAll Button]
    // We want max 8 bid rows → total children ≤ 2 + 8 + 1 = 11
    int max = 11;
    while (historyContainer.getChildren().size() > max) {
      // Remove second-to-last (before the "view all" button)
      int lastRow = historyContainer.getChildren().size() - 2;
      historyContainer.getChildren().remove(lastRow);
    }
  }

  // ─────────────────────────────────────────────────────────────────
  //  USER ELIGIBILITY
  // ─────────────────────────────────────────────────────────────────

  public void setUserEligible(boolean eligible) {
    this.userEligible = eligible;
    setButtonsDisabled(!eligible);
    if (!eligible) {
      lblBidStatus.setText("⚠ Chưa đặt cọc — không thể đặt giá");
      lblBidStatus.setStyle("-fx-text-fill: #D96570; -fx-font-size: 12.5; -fx-font-weight: bold;");
    }
  }

  private void setButtonsDisabled(boolean disable) {
    btnConfirmBid.setDisable(disable);
    btnPlaceBid.setDisable(disable);
    btnQuickBid1.setDisable(disable);
    btnQuickBid2.setDisable(disable);
    btnQuickBid3.setDisable(disable);
    txtCustomBid.setDisable(disable);
  }

  // ─────────────────────────────────────────────────────────────────
  //  BID HISTORY SEED  (mirrors the static rows already in FXML)
  // ─────────────────────────────────────────────────────────────────
  private void seedBidHistory() {
    bidHistory.addLast(new BidRecord("00:22:30", "N***A", 15_800));
    bidHistory.addLast(new BidRecord("00:22:39", "N***B", 17_000));
    bidHistory.addLast(new BidRecord("00:21:55", "T***C", 15_300));
    bidHistory.addLast(new BidRecord("00:21:10", "M***D", 14_800));
    bidHistory.addLast(new BidRecord("00:20:45", "P***E", 14_300));
  }

  // ─────────────────────────────────────────────────────────────────
  //  NUMERIC INPUT FILTER  (TextField only accepts digits)
  // ─────────────────────────────────────────────────────────────────
  private void attachNumericFilter() {
    txtCustomBid.textProperty().addListener((obs, oldVal, newVal) -> {
      if (!newVal.matches("[0-9]*")) {
        txtCustomBid.setText(newVal.replaceAll("[^0-9]", ""));
      }
    });
  }

  // ─────────────────────────────────────────────────────────────────
  //  UTILITY
  // ─────────────────────────────────────────────────────────────────

  /** Formats a double as "$15,800 USD". */
  private String formatUSD(double amount) {
    return "$" + currencyFmt.format((long) amount) + " USD";
  }

  /** Returns current time as "HH:MM:SS" string for bid history rows. */
  private String getCurrentTimeDisplay() {
    java.time.LocalTime now = java.time.LocalTime.now();
    return String.format("%02d:%02d:%02d", now.getHour(), now.getMinute(), now.getSecond());
  }

  /** Horizontal shake animation — signals invalid input. */
  private void shakeNode(javafx.scene.Node node) {
    TranslateTransition tt = new TranslateTransition(Duration.millis(60), node);
    tt.setFromX(0);
    tt.setByX(8);
    tt.setCycleCount(6);
    tt.setAutoReverse(true);
    tt.setOnFinished(e -> node.setTranslateX(0));
    tt.play();
  }

  /** Simple information alert in Vietnamese. */
  private void showAlert(String title, String message) {
    Platform.runLater(() -> {
      Alert alert = new Alert(Alert.AlertType.INFORMATION);
      alert.setTitle(title);
      alert.setHeaderText(null);
      alert.setContentText(message);

      // Apply Gemini styling to the dialog
      DialogPane dp = alert.getDialogPane();
      dp.setStyle("-fx-background-color: white; -fx-font-size: 13;");

      // Style the OK button
      dp.getButtonTypes().stream()
          .map(dp::lookupButton)
          .forEach(btn -> btn.setStyle(
              "-fx-background-color: #4285F4;" +
                  "-fx-text-fill: white;" +
                  "-fx-font-weight: bold;" +
                  "-fx-background-radius: 20;"
          ));

      alert.showAndWait();
    });
  }

  // ─────────────────────────────────────────────────────────────────
  //  PUBLIC API  (called by network layer / server messages)
  // ─────────────────────────────────────────────────────────────────

  /**
   * Called when a NEW BID arrives from the server (websocket / socket).
   * Must be invoked on the JavaFX Application Thread
   * (wrap with Platform.runLater if coming from a background thread).
   *
   * @param bidderAlias anonymised name (e.g. "K***Z")
   * @param amount      new highest bid in USD
   */
  public void onServerBidReceived(String bidderAlias, double amount) {
    Platform.runLater(() -> {
      if (amount <= currentBidUSD) return;  // stale / duplicate

      currentBidUSD = amount;
      refreshBidDisplay();
      applySoftClose();
      animateBidUpdate();

      String time = getCurrentTimeDisplay();
      BidRecord record = new BidRecord(time, bidderAlias, amount);
      bidHistory.addFirst(record);
      prependBidRow(record, true);
    });
  }

  /**
   * Called by the server to force-set remaining seconds
   * (e.g. after reconnect to sync with server clock).
   */
  public void syncTimerFromServer(double serverRemainingSeconds) {
    Platform.runLater(() -> {
      remainingSeconds = serverRemainingSeconds;
      refreshTimerLabel();
    });
  }

  /**
   * Marks the user's deposit as confirmed (called after deposit API response).
   */
  public void confirmDeposit() {
    Platform.runLater(() -> setUserEligible(true));
  }

  // ─────────────────────────────────────────────────────────────────
  //  CLEANUP  (call from owning stage onCloseRequest)
  // ─────────────────────────────────────────────────────────────────
  public void shutdown() {
    if (countdownTimeline  != null) countdownTimeline.stop();
    if (pulseTimeline      != null) pulseTimeline.stop();
    if (bgAnimationTimer   != null) bgAnimationTimer.stop();
  }

  // ─────────────────────────────────────────────────────────────────
  //  INNER RECORD  — lightweight bid entry
  // ─────────────────────────────────────────────────────────────────
  private record BidRecord(String time, String user, double amount) {}
}