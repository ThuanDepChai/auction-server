package com.nhom15.client.controller.bidding;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import javafx.animation.KeyFrame;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.util.Duration;

/**
 * PriceCountdownController — quản lý vùng STATS giữa màn hình:
 * giá hiện tại, đồng hồ đếm ngược, anti-snipe, badge trạng thái.
 *
 * FIX: Không dùng @FXML nữa vì controller được khởi tạo bằng `new`.
 * Tất cả node được nhận qua setNodes().
 * initialize() được gọi thủ công từ BiddingRoomController.
 */
public class PriceCountdownController {

    private static final DateTimeFormatter DT_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private Label       lblCurrentPrice;
    private Label       lblPriceChange;
    private Label       lblLeader;
    private Label       lblTotalBids;
    private Label       lblCountdown;
    private Label       lblMyBudget;
    private ProgressBar progressBudget;
    private Label       lblBudgetUsed;
    private Label       lblStatus;
    private Label       lblAntiSnipe;
    private ProgressBar progressTime;
    private Label       lblLastUpdate;

    private Runnable onAuctionExpired;
    private Timeline countdownTimer;

    // ── Inject thủ công từ BiddingRoomController ──────────────────────────

    public void setNodes(
            Label lblCurrentPrice, Label lblPriceChange,
            Label lblLeader, Label lblTotalBids,
            Label lblCountdown, Label lblMyBudget,
            ProgressBar progressBudget, Label lblBudgetUsed,
            Label lblStatus, Label lblAntiSnipe,
            ProgressBar progressTime, Label lblLastUpdate) {

        this.lblCurrentPrice = lblCurrentPrice;
        this.lblPriceChange  = lblPriceChange;
        this.lblLeader       = lblLeader;
        this.lblTotalBids    = lblTotalBids;
        this.lblCountdown    = lblCountdown;
        this.lblMyBudget     = lblMyBudget;
        this.progressBudget  = progressBudget;
        this.lblBudgetUsed   = lblBudgetUsed;
        this.lblStatus       = lblStatus;
        this.lblAntiSnipe    = lblAntiSnipe;
        this.progressTime    = progressTime;
        this.lblLastUpdate   = lblLastUpdate;
    }

    // ── initialize() — gọi thủ công sau setNodes() ────────────────────────

    public void initialize() {
        if (lblAntiSnipe != null) {
            lblAntiSnipe.managedProperty().bind(lblAntiSnipe.visibleProperty());
            lblAntiSnipe.setVisible(false);
        }
    }

    // ── Public API ────────────────────────────────────────────────────────

    public void setOnAuctionExpired(Runnable cb) { this.onAuctionExpired = cb; }

    public void updatePrice(double newPrice, double oldPrice) {
        if (lblCurrentPrice == null) return;
        lblCurrentPrice.setText(String.format("%,.0fđ", newPrice));

        ScaleTransition st = new ScaleTransition(Duration.millis(250), lblCurrentPrice);
        st.setFromX(1.0); st.setFromY(1.0);
        st.setToX(1.18);  st.setToY(1.18);
        st.setAutoReverse(true); st.setCycleCount(2); st.play();
        lblCurrentPrice.setStyle("-fx-font-size:30px;-fx-font-weight:bold;-fx-text-fill:#E53935;");
        new Timeline(new KeyFrame(Duration.millis(700), e ->
                lblCurrentPrice.setStyle("-fx-font-size:30px;-fx-font-weight:bold;-fx-text-fill:#DB2777;")
        )).play();

        if (lblPriceChange != null && oldPrice > 0 && newPrice != oldPrice) {
            double diff = newPrice - oldPrice;
            lblPriceChange.setText(String.format("▲ +%,.0fđ", diff));
        }
    }

    public void updateLeader(String leader, String currentUsername) {
        if (lblLeader == null) return;
        if (leader == null || leader.isEmpty()) {
            lblLeader.setText("Chưa có ai dẫn đầu");
            lblLeader.setStyle("-fx-text-fill:#AAAAAA;-fx-font-size:11px;");
        } else if (leader.equals(currentUsername)) {
            lblLeader.setText("🏆 Bạn đang dẫn đầu!");
            lblLeader.setStyle("-fx-text-fill:#27AE60;-fx-font-weight:bold;-fx-font-size:13px;");
        } else {
            lblLeader.setText("🥇 Dẫn đầu: " + leader);
            lblLeader.setStyle("-fx-text-fill:#333;-fx-font-size:12px;");
        }
    }

    public void updateTotalBids(int count) {
        if (lblTotalBids != null) lblTotalBids.setText(count + " lượt đặt");
    }

    public void startCountdown(String endTimeStr, Runnable onExpiredCallback) {
        stopCountdown();
        this.onAuctionExpired = onExpiredCallback;
        try {
            LocalDateTime endTime = LocalDateTime.parse(endTimeStr, DT_FMT);
            countdownTimer = new Timeline(new KeyFrame(Duration.seconds(1),
                    e -> tickCountdown(endTime)));
            countdownTimer.setCycleCount(Timeline.INDEFINITE);
            countdownTimer.play();
        } catch (Exception e) {
            if (lblCountdown != null) lblCountdown.setText("---");
        }
    }

    public void stopCountdown() {
        if (countdownTimer != null) { countdownTimer.stop(); countdownTimer = null; }
    }

    public void updateStatusBadge(String status) {
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
        lblStatus.setStyle(style +
                "-fx-background-radius:20;-fx-padding:5 16 5 16;" +
                "-fx-font-weight:bold;-fx-font-size:11px;");
    }

    public void updateLastUpdateLabel() {
        if (lblLastUpdate != null)
            lblLastUpdate.setText("Cập nhật: " +
                    java.time.LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
    }

    // ── Private: countdown tick ───────────────────────────────────────────

    private void tickCountdown(LocalDateTime endTime) {
        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(endTime)) {
            if (lblCountdown != null) {
                lblCountdown.setText("⏰ Đã kết thúc");
                lblCountdown.setStyle("-fx-font-size:20px;-fx-font-weight:bold;-fx-text-fill:#E67E22;");
            }
            stopCountdown();
            if (onAuctionExpired != null) onAuctionExpired.run();
            return;
        }

        long totalSec = ChronoUnit.SECONDS.between(now, endTime);
        long h = totalSec / 3600, m = (totalSec % 3600) / 60, s = totalSec % 60;
        String txt = h > 0
                ? String.format("%02d:%02d:%02d", h, m, s)
                : String.format("%02d:%02d", m, s);
        if (lblCountdown != null) lblCountdown.setText(txt);

        if (totalSec <= 30) {
            applyCountdownStyle("-fx-font-size:26px;-fx-font-weight:bold;-fx-text-fill:#FF4444;-fx-font-family:'Courier New';");
            if (lblAntiSnipe != null) {
                lblAntiSnipe.setVisible(true);
                lblAntiSnipe.setText("⚡ Anti-Snipe: Bid mới gia hạn thêm " +
                        (totalSec < 10 ? "30" : "60") + " giây!");
            }
        } else if (totalSec <= 300) {
            applyCountdownStyle("-fx-font-size:26px;-fx-font-weight:bold;-fx-text-fill:#E67E22;-fx-font-family:'Courier New';");
            if (lblAntiSnipe != null) lblAntiSnipe.setVisible(false);
        } else {
            applyCountdownStyle("-fx-font-size:26px;-fx-font-weight:bold;-fx-text-fill:#2563EB;-fx-font-family:'Courier New';");
            if (lblAntiSnipe != null) lblAntiSnipe.setVisible(false);
        }
    }

    private void applyCountdownStyle(String style) {
        if (lblCountdown != null) lblCountdown.setStyle(style);
    }
}