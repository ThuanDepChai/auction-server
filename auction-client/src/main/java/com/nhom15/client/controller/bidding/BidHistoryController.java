package com.nhom15.client.controller.bidding;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.nhom15.client.command.GetBidHistoryCommand;
import com.nhom15.client.command.ServerCommand;
import com.nhom15.client.util.SessionManager;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * BidHistoryController — TAB LỊCH SỬ + TAB BIỂU ĐỒ + LEADERBOARD panel bên phải.
 *
 * FIX:
 *  - buildLeaderboard(): tính rank từ bid history thực (gộp theo username, lấy max bid)
 *  - buildBidRow(): highlight row "của mình" đúng màu (bid-row-mine)
 *  - populateLeaderboard(): gọi sau load() để cập nhật panel leaderboard bên phải
 */
public class BidHistoryController {

    // ── Nodes — History tab ───────────────────────────────────────────────
    private VBox             vboxBidHistory;
    private ScrollPane       scrollHistory;
    private Label            lblHistoryCount;
    private TextField        txtHistorySearch;
    private ComboBox<String> cmbHistoryFilter;

    // ── Nodes — Chart tab ─────────────────────────────────────────────────
    private LineChart<Number, Number> priceChart;
    private NumberAxis       xAxis;
    private NumberAxis       yAxis;
    private Label            lblChartMax;
    private Label            lblChartMin;
    private Label            lblChartAvg;
    private Label            lblChartVolatility;
    private ComboBox<String> cmbChartType;
    private ToggleButton     toggleSmoothChart;

    // ── Nodes — Leaderboard panel (bên phải màn hình) ─────────────────────
    private VBox vboxLeaderboard;

    private AuctionState state;
    private XYChart.Series<Number, Number> priceSeries;
    private long chartTick    = 0;
    private int  lastHistSize = 0;
    /** Debounce reload lịch sử để cập nhật giá trước, tải lịch sử sau. */
    private boolean loadInFlight = false;
    private boolean reloadQueued = false;
    private PauseTransition reloadDebounce;

    // ── Inject ────────────────────────────────────────────────────────────

    public void setNodes(
        VBox vboxBidHistory, ScrollPane scrollHistory,
        Label lblHistoryCount, TextField txtHistorySearch,
        ComboBox<String> cmbHistoryFilter,
        LineChart<Number, Number> priceChart,
        NumberAxis xAxis, NumberAxis yAxis,
        Label lblChartMax, Label lblChartMin,
        Label lblChartAvg, Label lblChartVolatility,
        ComboBox<String> cmbChartType, ToggleButton toggleSmoothChart) {

        this.vboxBidHistory    = vboxBidHistory;
        this.scrollHistory     = scrollHistory;
        this.lblHistoryCount   = lblHistoryCount;
        this.txtHistorySearch  = txtHistorySearch;
        this.cmbHistoryFilter  = cmbHistoryFilter;
        this.priceChart        = priceChart;
        this.xAxis             = xAxis;
        this.yAxis             = yAxis;
        this.lblChartMax       = lblChartMax;
        this.lblChartMin       = lblChartMin;
        this.lblChartAvg       = lblChartAvg;
        this.lblChartVolatility = lblChartVolatility;
        this.cmbChartType      = cmbChartType;
        this.toggleSmoothChart = toggleSmoothChart;
    }

    /** Thêm node leaderboard riêng (panel bên phải của BiddingRoom). */
    public void setLeaderboardNode(VBox vboxLeaderboard) {
        this.vboxLeaderboard = vboxLeaderboard;
    }

    public void initialize() {
        setupChart();
    }

    public void setup(AuctionState state) {
        this.state = state;
    }

    // ── Public API ────────────────────────────────────────────────────────

    public void load() {
        if (state == null || state.getAuctionId() <= 0) {
            return;
        }
        if (loadInFlight) {
            reloadQueued = true;
            return;
        }

        loadInFlight = true;
        new GetBidHistoryCommand(state.getAuctionId()).executeAsync(
            res -> {
                loadInFlight = false;
                if (res != null && res.has("history")) {
                    JsonArray h = res.getAsJsonArray("history");
                    populateList(h);
                    populateLeaderboard(h);
                    if (h.size() != lastHistSize) {
                        lastHistSize = h.size();
                        rebuildChart(h);
                        updateChartStats(h);
                    }
                }
                runQueuedReloadIfNeeded();
            },
            () -> {
                loadInFlight = false;
                runQueuedReloadIfNeeded();
            });
    }

    public void loadSoon() {
        if (reloadDebounce == null) {
            reloadDebounce = new PauseTransition(Duration.millis(150));
            reloadDebounce.setOnFinished(e -> load());
        }
        reloadDebounce.playFromStart();
    }

    public void addChartPoint(double price) {
        if (priceSeries == null) return;
        chartTick++;
        priceSeries.getData().add(new XYChart.Data<>(chartTick, price));
        if (priceSeries.getData().size() > 60) priceSeries.getData().remove(0);
    }

    public void handleRefreshBids() {
        load();
    }

    private void runQueuedReloadIfNeeded() {
        if (reloadQueued) {
            reloadQueued = false;
            loadSoon();
        }
    }

    // ── Private: List ─────────────────────────────────────────────────────

    private void populateList(JsonArray history) {
        if (vboxBidHistory == null) return;
        vboxBidHistory.getChildren().clear();

        if (history.isEmpty()) {
            vboxBidHistory.getChildren().add(emptyLabel("📭 Chưa có lượt đặt giá nào"));
            if (lblHistoryCount != null) lblHistoryCount.setText("0");
            return;
        }
        if (lblHistoryCount != null) lblHistoryCount.setText(String.valueOf(history.size()));

        String filter = cmbHistoryFilter != null
            ? (cmbHistoryFilter.getValue() != null ? cmbHistoryFilter.getValue() : "Tất cả")
            : "Tất cả";
        String search = txtHistorySearch != null
            ? txtHistorySearch.getText().trim().toLowerCase() : "";

        String me = SessionManager.getUsername();

        for (int i = 0; i < history.size(); i++) {
            JsonObject bid = history.get(i).getAsJsonObject();
            String bidUser = str(bid, "username", "");

            // Filter
            if ("Của tôi".equals(filter)) {
                if (!bidUser.equals(me)) continue;
            } else if ("Auto-Bid".equals(filter)) {
                if (!bid.has("isAutoBid") || !bid.get("isAutoBid").getAsBoolean()) continue;
            } else if ("Top 5".equals(filter) && i >= 5) {
                break;
            }
            if (!search.isEmpty() && !bidUser.toLowerCase().contains(search)) continue;

            HBox row = buildBidRow(bid, i == 0, me);
            if (i == 0) {
                FadeTransition ft = new FadeTransition(Duration.millis(400), row);
                ft.setFromValue(0); ft.setToValue(1); ft.play();
            }
            vboxBidHistory.getChildren().add(row);
        }
        if (scrollHistory != null) Platform.runLater(() -> scrollHistory.setVvalue(0));
    }

    /**
     * FIX: Trước đây không phân biệt row "của mình" → dùng style CSS đúng.
     *  - i == 0 (cao nhất) → bid-row-top
     *  - username == mình   → bid-row-mine
     *  - còn lại            → bid-row-normal
     */
    private HBox buildBidRow(JsonObject bid, boolean isTop, String me) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);

        String bidUser = str(bid, "username", "");
        boolean isMine = (bidUser.equals(me));

        if (isTop) {
            row.getStyleClass().add("bid-row-top");
        } else if (isMine) {
            row.getStyleClass().add("bid-row-mine");
        } else {
            row.getStyleClass().add("bid-row-normal");
        }

        Label lblRank = new Label(isTop ? "🥇" : "•");
        lblRank.setStyle("-fx-font-size:" + (isTop ? "16" : "12") + "px;");

        Label lblUser = new Label(maskUsername(bidUser, isMine));
        lblUser.setStyle("-fx-font-weight:bold;-fx-font-size:13px;" +
            (isTop  ? "-fx-text-fill:#E65100;" :
                isMine ? "-fx-text-fill:#1D4ED8;" : "-fx-text-fill:#333;"));

        row.getChildren().addAll(lblRank, lblUser);

        if (isMine) {
            Label you = new Label(" Bạn ");
            you.setStyle("-fx-background-color:#4285F4;-fx-text-fill:white;" +
                "-fx-background-radius:4;-fx-font-size:9px;-fx-padding:1 4;");
            row.getChildren().add(you);
        }
        if (bid.has("isAutoBid") && bid.get("isAutoBid").getAsBoolean()) {
            Label auto = new Label(" 🤖 Auto ");
            auto.setStyle("-fx-background-color:#9C27B0;-fx-text-fill:white;" +
                "-fx-background-radius:4;-fx-font-size:9px;-fx-padding:1 4;");
            row.getChildren().add(auto);
        }

        Region gap = new Region(); HBox.setHgrow(gap, Priority.ALWAYS);
        row.getChildren().add(gap);

        double amount = bid.has("amount") ? bid.get("amount").getAsDouble() : 0;
        Label lblAmt  = new Label(String.format("%,.0fđ", amount));
        lblAmt.setStyle("-fx-font-weight:bold;-fx-text-fill:#D96570;-fx-font-size:14px;");
        Label lblTime = new Label(str(bid, "bidTime", ""));
        lblTime.setStyle("-fx-font-size:10px;-fx-text-fill:#AAAAAA;");
        VBox right = new VBox(2, lblAmt, lblTime);
        right.setAlignment(Pos.CENTER_RIGHT);
        row.getChildren().add(right);
        return row;
    }

    // ── Private: Leaderboard ─────────────────────────────────────────────

    /**
     * FIX: Tính rank từ bid history thực tế.
     * Gộp theo username → lấy max bid của mỗi người → sắp xếp giảm dần → hiển thị top 5.
     */
    private void populateLeaderboard(JsonArray history) {
        if (vboxLeaderboard == null) return;

        // Gộp: username → max bid
        Map<String, Double> maxBids = new LinkedHashMap<>();
        for (int i = 0; i < history.size(); i++) {
            JsonObject bid = history.get(i).getAsJsonObject();
            String user = str(bid, "username", "?");
            double amt  = bid.has("amount") ? bid.get("amount").getAsDouble() : 0;
            maxBids.merge(user, amt, Math::max);
        }

        // Sắp xếp giảm dần
        List<Map.Entry<String, Double>> sorted = new ArrayList<>(maxBids.entrySet());
        sorted.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

        vboxLeaderboard.getChildren().clear();
        if (sorted.isEmpty()) {
            Label empty = new Label("Chưa có dữ liệu...");
            empty.setStyle("-fx-text-fill:#9CA3AF;-fx-font-size:11px;");
            vboxLeaderboard.getChildren().add(empty);
            return;
        }

        String me = SessionManager.getUsername();
        String[] medals = {"🥇", "🥈", "🥉", "4.", "5."};
        String[] styleClasses = {
            "leader-row-1", "leader-row-2", "leader-row-3",
            "leader-row-2", "leader-row-2"
        };

        for (int i = 0; i < Math.min(5, sorted.size()); i++) {
            Map.Entry<String, Double> entry = sorted.get(i);
            String user  = entry.getKey();
            double price = entry.getValue();
            boolean isMine = (user.equals(me));

            HBox row = new HBox(8);
            row.setAlignment(Pos.CENTER_LEFT);
            row.getStyleClass().add(styleClasses[i]);

            Label lblMedal = new Label(medals[i]);
            lblMedal.setStyle("-fx-font-size:14px;");

            // Hiển thị tên đầy đủ nếu là mình, mask nếu là người khác
            Label lblName = new Label(isMine ? "Bạn" : maskUsername(user, false));
            lblName.setStyle("-fx-font-size:11px;-fx-font-weight:bold;" +
                (isMine ? "-fx-text-fill:#1D4ED8;" : "-fx-text-fill:#374151;"));

            Region gap = new Region(); HBox.setHgrow(gap, Priority.ALWAYS);

            Label lblPrice = new Label(String.format("%,.0fđ", price));
            lblPrice.setStyle("-fx-font-size:11px;-fx-font-weight:bold;-fx-text-fill:#D96570;");

            row.getChildren().addAll(lblMedal, lblName, gap, lblPrice);
            vboxLeaderboard.getChildren().add(row);
        }
    }

    // ── Private: Chart ────────────────────────────────────────────────────

    private void setupChart() {
        if (priceChart == null) return;
        priceSeries = new XYChart.Series<>();
        priceSeries.setName("Giá đấu");
        priceChart.getData().add(priceSeries);
        priceChart.setAnimated(false);
        priceChart.setLegendVisible(false);
        if (xAxis != null) xAxis.setAutoRanging(true);
        if (yAxis != null) yAxis.setAutoRanging(true);
    }

    private void rebuildChart(JsonArray history) {
        if (priceSeries == null) return;
        priceSeries.getData().clear();
        chartTick = 0;
        for (int i = history.size() - 1; i >= 0; i--) {
            JsonObject bid = history.get(i).getAsJsonObject();
            if (bid.has("amount")) {
                chartTick++;
                priceSeries.getData().add(
                    new XYChart.Data<>(chartTick, bid.get("amount").getAsDouble()));
            }
        }
    }

    private void updateChartStats(JsonArray history) {
        if (history.isEmpty()) return;
        double max = Double.MIN_VALUE, min = Double.MAX_VALUE, sum = 0;
        for (int i = 0; i < history.size(); i++) {
            double v = history.get(i).getAsJsonObject().get("amount").getAsDouble();
            if (v > max) max = v;
            if (v < min) min = v;
            sum += v;
        }
        double avg        = sum / history.size();
        double volatility = max - min;
        if (lblChartMax        != null) lblChartMax.setText(String.format("%,.0fđ", max));
        if (lblChartMin        != null) lblChartMin.setText(String.format("%,.0fđ", min));
        if (lblChartAvg        != null) lblChartAvg.setText(String.format("%,.0fđ", avg));
        if (lblChartVolatility != null) lblChartVolatility.setText(String.format("%,.0fđ", volatility));
    }

    // ── Utils ─────────────────────────────────────────────────────────────

    private Label emptyLabel(String msg) {
        Label l = new Label(msg);
        l.setStyle("-fx-text-fill:#AAAAAA;-fx-font-size:13px;-fx-padding:16;");
        return l;
    }

    /**
     * Mask username: hiển thị tên đầy đủ nếu là mình, ẩn giữa nếu là người khác.
     * "nguyen123" → "ng*****23"
     */
    private String maskUsername(String name, boolean isMine) {
        if (isMine || name == null || name.length() <= 3) return name;
        int show = Math.max(1, name.length() / 4);
        return name.substring(0, show)
            + "*".repeat(Math.max(0, name.length() - show - 1))
            + name.charAt(name.length() - 1);
    }

    private String str(JsonObject o, String key, String def) {
        return (o != null && o.has(key) && !o.get(key).isJsonNull())
            ? o.get(key).getAsString() : def;
    }
}
