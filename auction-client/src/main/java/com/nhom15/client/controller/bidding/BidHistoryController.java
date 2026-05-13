package com.nhom15.client.controller.bidding;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.nhom15.client.command.GetBidHistoryCommand;
import com.nhom15.client.command.ServerCommand;
import com.nhom15.client.util.SessionManager;
import java.util.ArrayList;
import java.util.List;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * BidHistoryController — quản lý:
 * 1. TAB LỊCH SỬ: danh sách lượt đặt, filter, tìm kiếm
 * 2. TAB BIỂU ĐỒ: LineChart giá theo thời gian + stats (max, min, avg, volatility)
 *
 * Dùng GetBidHistoryCommand để tải dữ liệu.
 */
public class BidHistoryController {

    // ── FXML: History tab ─────────────────────────────────────────────────
    @FXML private VBox            vboxBidHistory;
    @FXML private ScrollPane      scrollHistory;
    @FXML private Label           lblHistoryCount;
    @FXML private TextField       txtHistorySearch;
    @FXML private ComboBox<String> cmbHistoryFilter;

    // ── FXML: Chart tab ───────────────────────────────────────────────────
    @FXML private LineChart<Number, Number> priceChart;
    @FXML private NumberAxis      xAxis;
    @FXML private NumberAxis      yAxis;
    @FXML private Label           lblChartMax;
    @FXML private Label           lblChartMin;
    @FXML private Label           lblChartAvg;
    @FXML private Label           lblChartVolatility;
    @FXML private ComboBox<String> cmbChartType;
    @FXML private ToggleButton    toggleSmoothChart;

    // ── State ─────────────────────────────────────────────────────────────
    private AuctionState state;
    private XYChart.Series<Number, Number> priceSeries;
    private long   chartTick     = 0;
    private int    lastHistSize  = 0;

    // ── Init ──────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        setupChart();
    }

    public void setup(AuctionState state) {
        this.state = state;
    }

    // ── Public API ────────────────────────────────────────────────────────

    /** Tải lịch sử từ server, cập nhật danh sách + chart nếu có thay đổi. */
    public void load() {
        new GetBidHistoryCommand(state.getAuctionId()).executeAsync(res -> {
            if (res == null || !res.has("history")) return;
            JsonArray h = res.getAsJsonArray("history");
            populateList(h);
            if (h.size() != lastHistSize) {
                lastHistSize = h.size();
                rebuildChart(h);
                updateChartStats(h);
            }
        });
    }

    /** Thêm 1 điểm giá mới vào chart (gọi sau mỗi bid mới từ polling). */
    public void addChartPoint(double price) {
        if (priceSeries == null) return;
        chartTick++;
        priceSeries.getData().add(new XYChart.Data<>(chartTick, price));
        if (priceSeries.getData().size() > 60) priceSeries.getData().remove(0);
    }

    // ── FXML handler ──────────────────────────────────────────────────────

    @FXML
    private void handleRefreshBids() {
        load();
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

        String filter = cmbHistoryFilter != null ? cmbHistoryFilter.getValue() : "Tất cả";
        String search = txtHistorySearch != null ? txtHistorySearch.getText().trim().toLowerCase() : "";

        for (int i = 0; i < history.size(); i++) {
            JsonObject bid = history.get(i).getAsJsonObject();

            // Filter
            if ("Của tôi".equals(filter)) {
                if (!str(bid, "username", "").equals(SessionManager.getUsername())) continue;
            } else if ("Auto-Bid".equals(filter)) {
                if (!bid.has("isAutoBid") || !bid.get("isAutoBid").getAsBoolean()) continue;
            } else if ("Top 5".equals(filter) && i >= 5) {
                break;
            }
            // Search
            if (!search.isEmpty() && !str(bid, "username", "").toLowerCase().contains(search)) continue;

            HBox row = buildBidRow(bid, i == 0);
            if (i == 0) {
                FadeTransition ft = new FadeTransition(Duration.millis(400), row);
                ft.setFromValue(0); ft.setToValue(1); ft.play();
            }
            vboxBidHistory.getChildren().add(row);
        }
        if (scrollHistory != null) Platform.runLater(() -> scrollHistory.setVvalue(0));
    }

    private HBox buildBidRow(JsonObject bid, boolean isTop) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-background-color:" + (isTop ? "#FFFDE7" : "#FAFAFA") +
            ";-fx-background-radius:8;-fx-padding:10 14;" +
            (isTop ? "-fx-border-color:#FFC107;-fx-border-width:0 0 0 4;" : ""));

        Label lblRank = new Label(isTop ? "🥇" : "•");
        lblRank.setStyle("-fx-font-size:" + (isTop ? "16" : "12") + "px;");
        Label lblUser = new Label(str(bid, "username", "?"));
        lblUser.setStyle("-fx-font-weight:bold;-fx-font-size:13px;" +
            (isTop ? "-fx-text-fill:#E65100;" : "-fx-text-fill:#333;"));
        row.getChildren().addAll(lblRank, lblUser);

        if (str(bid, "username", "").equals(SessionManager.getUsername())) {
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

        Label lblAmt  = new Label(String.format("%,.0fđ", bid.get("amount").getAsDouble()));
        lblAmt.setStyle("-fx-font-weight:bold;-fx-text-fill:#D96570;-fx-font-size:14px;");
        Label lblTime = new Label(str(bid, "bidTime", ""));
        lblTime.setStyle("-fx-font-size:10px;-fx-text-fill:#AAAAAA;");
        VBox right = new VBox(2, lblAmt, lblTime);
        right.setAlignment(Pos.CENTER_RIGHT);
        row.getChildren().add(right);
        return row;
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
        List<Double> prices = new ArrayList<>();
        for (int i = history.size() - 1; i >= 0; i--) {
            JsonObject bid = history.get(i).getAsJsonObject();
            if (bid.has("amount")) prices.add(bid.get("amount").getAsDouble());
        }
        for (double p : prices) {
            chartTick++;
            priceSeries.getData().add(new XYChart.Data<>(chartTick, p));
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

    private String str(JsonObject o, String key, String def) {
        return (o != null && o.has(key) && !o.get(key).isJsonNull())
            ? o.get(key).getAsString() : def;
    }
}
