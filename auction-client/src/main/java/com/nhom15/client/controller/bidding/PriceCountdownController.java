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
 * PriceCountdownController — quản lý vùng STATS giữa màn hình.
 *
 * FIX:
 *  - updateLeader(): kiểm tra currentUsername null/empty trước khi so sánh
 *    → tránh trường hợp chưa login vẫn hiện "Bạn đang dẫn đầu"
 *  - updateBudget(): hiển thị số dư và % đã dùng cho lblMyBudget / progressBudget
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

    // thời điểm bắt đầu để tính progress bar thời gian
    private LocalDateTime auctionStartTime;
    private LocalDateTime auctionEndTime;

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

    // ── initialize() ─────────────────────────────────────────────────────

    public void initialize() {
        if (lblAntiSnipe != null) {
            lblAntiSnipe.managedProperty().bind(lblAntiSnipe.visibleProperty());
            lblAntiSnipe.setVisible(false);
        }
    }

    // ── Public API ────────────────────────────────────────────────────────

    public void setOnAuctionExpired(Runnable cb) { this.onAuctionExpired = cb; }

    /** Cập nhật giá hiện tại + animation. */
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
            lblPriceChange.setStyle(
                    "-fx-font-size:11px;-fx-text-fill:#10B981;" +
                            "-fx-background-color:#ECFDF5;-fx-background-radius:6;-fx-padding:2 8;");
        }
    }

    /**
     * FIX: Kiểm tra currentUsername trước khi so sánh.
     * Trước đây không check null/empty → user chưa login hoặc username rỗng
     * vẫn match → ai cũng thấy "Bạn đang dẫn đầu".
     */
    public void updateLeader(String leader, String currentUsername) {
        if (lblLeader == null) return;

        boolean leaderEmpty   = (leader == null || leader.trim().isEmpty());
        boolean userKnown     = (currentUsername != null && !currentUsername.trim().isEmpty());

        if (leaderEmpty) {
            lblLeader.setText("Chưa có ai dẫn đầu");
            lblLeader.setStyle("-fx-text-fill:#AAAAAA;-fx-font-size:11px;");
        } else if (userKnown && leader.equals(currentUsername)) {
            // Chỉ hiện "Bạn đang dẫn đầu" khi username thực sự khớp
            lblLeader.setText("🏆 Bạn đang dẫn đầu!");
            lblLeader.setStyle("-fx-text-fill:#27AE60;-fx-font-weight:bold;-fx-font-size:13px;");
        } else {
            // Ẩn bớt tên: chỉ hiện 2 ký tự đầu + ***
            String masked = maskUsername(leader);
            lblLeader.setText("🥇 Dẫn đầu: " + masked);
            lblLeader.setStyle("-fx-text-fill:#333;-fx-font-size:12px;");
        }
    }

    /** Hiển thị số dư ví và % ngân sách đã dùng so với giá hiện tại. */
    public void updateBudget(double balance, double currentPrice) {
        if (lblMyBudget != null)
            lblMyBudget.setText(String.format("%,.0fđ", balance));

        if (balance > 0 && currentPrice > 0) {
            double pct = Math.min(1.0, currentPrice / balance);
            if (progressBudget != null) progressBudget.setProgress(pct);
            if (lblBudgetUsed  != null)
                lblBudgetUsed.setText(String.format("%.0f%% ngân sách đã dùng", pct * 100));
        } else {
            if (progressBudget != null) progressBudget.setProgress(0);
            if (lblBudgetUsed  != null) lblBudgetUsed.setText("0% ngân sách đã dùng");
        }
    }

    public void updateTotalBids(int count) {
        if (lblTotalBids != null) lblTotalBids.setText(count + " lượt đặt");
    }

    public void startCountdown(String endTimeStr, Runnable onExpiredCallback) {
        stopCountdown();
        this.onAuctionExpired = onExpiredCallback;
        try {
            auctionEndTime   = LocalDateTime.parse(endTimeStr, DT_FMT);
            auctionStartTime = LocalDateTime.now();
            countdownTimer   = new Timeline(new KeyFrame(Duration.seconds(1),
                    e -> tickCountdown(auctionEndTime)));
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
        String label = switch (status) {
            case "ACTIVE", "RUNNING" -> "● ĐANG DIỄN RA";
            case "ENDED"             -> "■ ĐÃ KẾT THÚC";
            case "CANCELLED"         -> "✕ ĐÃ HUỶ";
            default                  -> status;
        };
        String style = switch (status) {
            case "ACTIVE", "RUNNING" ->
                    "-fx-background-color:linear-gradient(to right,#DCFCE7,#BBF7D0);-fx-text-fill:#166534;";
            case "ENDED" ->
                    "-fx-background-color:linear-gradient(to right,#FFF3E0,#FFECB3);-fx-text-fill:#92400E;";
            case "CANCELLED" ->
                    "-fx-background-color:linear-gradient(to right,#FFE4E6,#FED7D7);-fx-text-fill:#9F1239;";
            default ->
                    "-fx-background-color:#EEEEEE;-fx-text-fill:#888;";
        };
        lblStatus.setText(label);
        lblStatus.setStyle(style +
                "-fx-background-radius:20;-fx-padding:5 16 5 16;" +
                "-fx-font-weight:bold;-fx-font-size:11px;");
    }

    public void updateLastUpdateLabel() {
        if (lblLastUpdate != null)
            lblLastUpdate.setText("Cập nhật: " +
                    java.time.LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
    }

    /**
     * Hiển thị banner thông báo anti-sniping khi server gia hạn thời gian.
     * Gọi từ BiddingRoomController.onEndTimeChanged().
     */
    public void showAntiSnipeAlert() {
        if (lblAntiSnipe == null) return;
        lblAntiSnipe.setText("⚡ Phiên được gia hạn do có bid mới cuối giờ!");
        lblAntiSnipe.setStyle(
                "-fx-text-fill:#FFFFFF;-fx-background-color:#E67E22;" +
                        "-fx-background-radius:6;-fx-padding:4 12;-fx-font-weight:bold;");
        lblAntiSnipe.setVisible(true);
        // Tự ẩn sau 8 giây
        new Timeline(new KeyFrame(Duration.seconds(8),
                e -> lblAntiSnipe.setVisible(false))).play();
    }

    // ── Private ───────────────────────────────────────────────────────────

    private void tickCountdown(LocalDateTime endTime) {
        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(endTime)) {
            if (lblCountdown != null) {
                lblCountdown.setText("⏰ Đã kết thúc");
                lblCountdown.setStyle("-fx-font-size:20px;-fx-font-weight:bold;-fx-text-fill:#E67E22;");
            }
            if (progressTime != null) progressTime.setProgress(0);
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

        // Progress bar thời gian
        if (progressTime != null && auctionStartTime != null) {
            long totalDuration = ChronoUnit.SECONDS.between(auctionStartTime, endTime);
            double progress = totalDuration > 0 ? (double) totalSec / totalDuration : 0;
            progressTime.setProgress(Math.max(0, Math.min(1, progress)));
        }

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

    /**
     * Ẩn bớt username cho bảo mật: "nguyen123" → "ng*****23"
     */
    private String maskUsername(String name) {
        if (name == null || name.length() <= 3) return name;
        int show = Math.max(1, name.length() / 4);
        String prefix = name.substring(0, show);
        String suffix = name.substring(name.length() - 1);
        return prefix + "*".repeat(name.length() - show - 1) + suffix;
    }
}