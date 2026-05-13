package com.nhom15.client.controller;

import com.google.gson.JsonObject;
import com.nhom15.client.command.GetAuctionDetailCommand;
import com.nhom15.client.command.ServerCommand;
import com.nhom15.client.controller.bidding.AuctionState;
import com.nhom15.client.controller.bidding.AutoBidController;
import com.nhom15.client.controller.bidding.BidHistoryController;
import com.nhom15.client.controller.bidding.ManualBidController;
import com.nhom15.client.controller.bidding.PriceCountdownController;
import com.nhom15.client.controller.bidding.ProductPanelController;
import com.nhom15.client.controller.bidding.RealtimePollingController;
import com.nhom15.client.util.SessionManager;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;

/**
 * BiddingRoomController — ORCHESTRATOR của màn hình đấu giá.
 *
 * FIX: FXML dùng layout phẳng (không có fx:include) nên JavaFX
 * KHÔNG thể inject sub-controller qua @FXML. Giải pháp: khai báo
 * sub-controllers bằng `new`, sau đó inject thủ công các FXML node
 * tương ứng vào từng sub-controller trong initialize().
 *
 * Các lỗi đã sửa:
 *  1. Bỏ @FXML trên các field sub-controller → khởi tạo bằng `new`
 *  2. Inject FXML nodes thủ công vào sub-controller qua setter
 *  3. Điền logic vào 4 method rỗng (handlePlaceBid, handleSetAutoBid,
 *     handleCancelAutoBid, handleRefreshBids) → delegate sang sub-controller
 *  4. handleBack: gọi ViewManager.navigateTo(HOME) khi onBack == null
 */
public class BiddingRoomController {

    // ══════════════════════════════════════════════════════════════════════
    //  FXML NODES — Top bar
    // ══════════════════════════════════════════════════════════════════════

    @FXML private Label    lblRoomId;
    @FXML private Button   btnWatchlist;
    @FXML private TabPane  tabPane;
    @FXML private Label    lblStatus;
    @FXML private Label    lblAntiSnipe;
    @FXML private ProgressBar progressTime;

    // ══════════════════════════════════════════════════════════════════════
    //  FXML NODES — Product panel (trái)
    // ══════════════════════════════════════════════════════════════════════

    @FXML private ImageView   imgProduct;
    @FXML private Label       lblImgPlaceholder;
    @FXML private Label       lblProductName;
    @FXML private Label       lblCategory;
    @FXML private Label       lblCondition;
    @FXML private Label       lblDescription;
    @FXML private Label       lblSeller;
    @FXML private Label       lblAvatarInitial;
    @FXML private Circle      avatarCircle;
    @FXML private Label       lblStartPrice;
    @FXML private ProgressBar progressReserve;
    @FXML private Label       lblReserveHint;

    // ══════════════════════════════════════════════════════════════════════
    //  FXML NODES — Price / Countdown (giữa, hàng stats)
    // ══════════════════════════════════════════════════════════════════════

    @FXML private Label       lblCurrentPrice;
    @FXML private Label       lblPriceChange;
    @FXML private Label       lblLeader;
    @FXML private Label       lblTotalBids;
    @FXML private Label       lblCountdown;
    @FXML private Label       lblMyBudget;
    @FXML private ProgressBar progressBudget;
    @FXML private Label       lblBudgetUsed;
    @FXML private Label       lblLastUpdate;

    // ══════════════════════════════════════════════════════════════════════
    //  FXML NODES — Tab Đặt giá thủ công
    // ══════════════════════════════════════════════════════════════════════

    @FXML private TextField  txtBidAmount;
    @FXML private Label      lblMinBid;
    @FXML private Label      lblBidError;
    @FXML private Label      lblBidStatus;
    @FXML private Button     btnPlaceBid;
    @FXML private Button     btnQuick1;
    @FXML private Button     btnQuick2;
    @FXML private Button     btnQuick3;
    @FXML private Button     btnQuick4;
    @FXML private Slider     bidSlider;
    @FXML private Label      lblSliderVal;
    @FXML private CheckBox   chkConfirmBid;

    // ══════════════════════════════════════════════════════════════════════
    //  FXML NODES — Tab Auto-Bid
    // ══════════════════════════════════════════════════════════════════════

    @FXML private TextField        txtMaxBid;
    @FXML private TextField        txtIncrement;
    @FXML private Button           btnSetAutoBid;
    @FXML private Button           btnCancelAutoBid;
    @FXML private Label            lblAutoBidStatus;
    @FXML private Label            lblAutoBidInfo;
    @FXML private ComboBox<String> cmbStrategy;
    @FXML private Slider           autoDelaySlider;
    @FXML private Label            lblAutoDelay;

    // ══════════════════════════════════════════════════════════════════════
    //  FXML NODES — Tab Lịch sử
    // ══════════════════════════════════════════════════════════════════════

    @FXML private VBox             vboxBidHistory;
    @FXML private ScrollPane       scrollHistory;
    @FXML private Label            lblHistoryCount;
    @FXML private TextField        txtHistorySearch;
    @FXML private ComboBox<String> cmbHistoryFilter;

    // ══════════════════════════════════════════════════════════════════════
    //  FXML NODES — Tab Biểu đồ
    // ══════════════════════════════════════════════════════════════════════

    @FXML private LineChart<Number, Number> priceChart;
    @FXML private NumberAxis     xAxis;
    @FXML private NumberAxis     yAxis;
    @FXML private Label          lblChartMax;
    @FXML private Label          lblChartMin;
    @FXML private Label          lblChartAvg;
    @FXML private Label          lblChartVolatility;
    @FXML private ComboBox<String> cmbChartType;
    @FXML private ToggleButton   toggleSmoothChart;

    // ══════════════════════════════════════════════════════════════════════
    //  FXML NODES — Chat
    // ══════════════════════════════════════════════════════════════════════

    @FXML private VBox       vboxChat;
    @FXML private ScrollPane scrollChat;
    @FXML private TextField  txtChatMessage;
    @FXML private Label      lblOnlineCount;
    @FXML private VBox       vboxLiveFeed;
    @FXML private VBox       vboxLeaderboard;
    @FXML private Label      lblUpdateTick;
    @FXML private VBox       vboxSimilar;

    // ══════════════════════════════════════════════════════════════════════
    //  SUB-CONTROLLERS — khởi tạo bằng `new`, inject nodes thủ công
    //  FIX: không dùng @FXML vì FXML không có fx:include
    // ══════════════════════════════════════════════════════════════════════

    private final ProductPanelController   productPanelController   = new ProductPanelController();
    private final PriceCountdownController priceCountdownController = new PriceCountdownController();
    private final ManualBidController      manualBidController      = new ManualBidController();
    private final AutoBidController        autoBidController        = new AutoBidController();
    private final BidHistoryController     bidHistoryController     = new BidHistoryController();
    private final RealtimePollingController poller                  = new RealtimePollingController();
    private final AuctionState             state                    = new AuctionState();

    private Runnable onBack;

    // ══════════════════════════════════════════════════════════════════════
    //  INITIALIZE
    // ══════════════════════════════════════════════════════════════════════

    @FXML
    public void initialize() {
        // 1. Inject FXML nodes vào từng sub-controller
        injectProductPanel();
        injectPriceCountdown();
        injectManualBid();
        injectAutoBid();
        injectBidHistory();

        // 2. Khởi tạo nội bộ các sub-controller (initialize không tự chạy vì không qua FXMLLoader)
        priceCountdownController.initialize();
        manualBidController.initialize();
        autoBidController.initialize();
        bidHistoryController.initialize();

        // 3. Setup với AuctionState + callbacks
        manualBidController.setup(state, this::onBidPlacedSuccessfully);
        autoBidController.setup(state);
        bidHistoryController.setup(state);
        priceCountdownController.setOnAuctionExpired(this::onAuctionEnded);

        poller.setup(
                state,
                this::onPriceChanged,
                this::onPollingUpdate,
                this::onAuctionEnded
        );
    }

    // ── Inject helpers ────────────────────────────────────────────────────

    private void injectProductPanel() {
        productPanelController.setNodes(
                imgProduct, lblImgPlaceholder,
                lblProductName, lblCategory, lblCondition,
                lblDescription, lblSeller, lblAvatarInitial,
                avatarCircle, lblStartPrice, progressReserve, lblReserveHint
        );
    }

    private void injectPriceCountdown() {
        priceCountdownController.setNodes(
                lblCurrentPrice, lblPriceChange, lblLeader, lblTotalBids,
                lblCountdown, lblMyBudget, progressBudget, lblBudgetUsed,
                lblStatus, lblAntiSnipe, progressTime, lblLastUpdate
        );
    }

    private void injectManualBid() {
        manualBidController.setNodes(
                txtBidAmount, lblMinBid, lblBidError, lblBidStatus,
                btnPlaceBid, btnQuick1, btnQuick2, btnQuick3, btnQuick4,
                bidSlider, lblSliderVal, chkConfirmBid
        );
    }

    private void injectAutoBid() {
        autoBidController.setNodes(
                txtMaxBid, txtIncrement, btnSetAutoBid, btnCancelAutoBid,
                lblAutoBidStatus, lblAutoBidInfo, cmbStrategy,
                autoDelaySlider, lblAutoDelay
        );
    }

    private void injectBidHistory() {
        bidHistoryController.setNodes(
                vboxBidHistory, scrollHistory, lblHistoryCount,
                txtHistorySearch, cmbHistoryFilter,
                priceChart, xAxis, yAxis,
                lblChartMax, lblChartMin, lblChartAvg, lblChartVolatility,
                cmbChartType, toggleSmoothChart
        );
    }

    // ══════════════════════════════════════════════════════════════════════
    //  ENTRY POINT
    // ══════════════════════════════════════════════════════════════════════

    public void setAuctionId(int id) {
        state.setAuctionId(id);
        if (lblRoomId != null) lblRoomId.setText("#" + id);

        loadDetailAndInit();
        bidHistoryController.load();
        autoBidController.loadStatus();
        poller.start();
    }

    public void setOnBack(Runnable r) { this.onBack = r; }

    // ══════════════════════════════════════════════════════════════════════
    //  LOAD
    // ══════════════════════════════════════════════════════════════════════

    private void loadDetailAndInit() {
        new GetAuctionDetailCommand(state.getAuctionId()).executeAsync(res -> {
            if (!ServerCommand.isSuccess(res) || !res.has("auction")) return;
            JsonObject a = res.getAsJsonObject("auction");

            state.setCurrentPrice(dbl(a, "currentPrice", dbl(a, "startPrice", 0)));
            state.setMinStep(dbl(a, "bidStep", 50_000));

            productPanelController.populate(a);
            priceCountdownController.updatePrice(state.getCurrentPrice(), 0);
            priceCountdownController.updateStatusBadge(str(a, "status", "ACTIVE"));

            String endTime = str(a, "endTime", null);
            if (endTime != null)
                priceCountdownController.startCountdown(endTime, this::onAuctionEnded);

            manualBidController.refreshLabels();
        });
    }

    // ══════════════════════════════════════════════════════════════════════
    //  CALLBACKS
    // ══════════════════════════════════════════════════════════════════════

    private void onBidPlacedSuccessfully(double amount) {
        double old = state.getCurrentPrice();
        state.setCurrentPrice(amount);
        priceCountdownController.updatePrice(amount, old);
        priceCountdownController.updateLeader(
                SessionManager.getUsername(), SessionManager.getUsername());
        bidHistoryController.addChartPoint(amount);
        bidHistoryController.load();
        manualBidController.refreshLabels();
    }

    private void onPriceChanged(double newPrice, double oldPrice) {
        priceCountdownController.updatePrice(newPrice, oldPrice);
        priceCountdownController.updateLastUpdateLabel();
        bidHistoryController.addChartPoint(newPrice);
        bidHistoryController.load();
        manualBidController.refreshLabels();
    }

    private void onPollingUpdate(String leader, int totalBids) {
        if (!leader.isEmpty())
            priceCountdownController.updateLeader(leader, SessionManager.getUsername());
        priceCountdownController.updateTotalBids(totalBids);
    }

    private void onAuctionEnded() {
        poller.stop();
        priceCountdownController.stopCountdown();
        priceCountdownController.updateStatusBadge("ENDED");
        manualBidController.disableAll();
        autoBidController.disableAll();
        state.setAuctionEnded(true);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  FXML HANDLERS — Top bar
    // ══════════════════════════════════════════════════════════════════════

    @FXML
    private void handleBack() {
        poller.stop();
        priceCountdownController.stopCountdown();
        if (onBack != null) {
            onBack.run();
        } else {
            // FIX: onBack thường không được set → fallback về Home
            com.nhom15.client.util.ViewManager.navigateTo(
                    com.nhom15.client.util.ViewManager.Views.HOME);
        }
    }

    @FXML
    private void handleToggleWatchlist() {
        if (btnWatchlist == null) return;
        boolean watching = btnWatchlist.getStyleClass().contains("watching");
        if (watching) {
            btnWatchlist.getStyleClass().remove("watching");
            btnWatchlist.setText("☆ Theo dõi");
        } else {
            btnWatchlist.getStyleClass().add("watching");
            btnWatchlist.setText("★ Đang theo dõi");
        }
    }

    @FXML
    private void handleShare() {
        System.out.println("Chia sẻ phiên: " + state.getAuctionId());
    }

    @FXML
    private void handleSendChat() {
        if (txtChatMessage == null) return;
        String msg = txtChatMessage.getText().trim();
        if (msg.isEmpty()) return;
        System.out.println("[Chat] " + SessionManager.getUsername() + ": " + msg);
        txtChatMessage.clear();
    }

    @FXML
    private void handleChatEmoji(ActionEvent e) {
        Button btn = (Button) e.getSource();
        System.out.println("[Emoji] " + btn.getText());
    }

    // ══════════════════════════════════════════════════════════════════════
    //  FXML HANDLERS — Delegate sang sub-controllers
    //  FIX: 4 method này trước đây bị để rỗng
    // ══════════════════════════════════════════════════════════════════════

    @FXML
    public void handlePlaceBid(ActionEvent actionEvent) {
        manualBidController.handlePlaceBid();
    }

    @FXML
    public void handleSetAutoBid(ActionEvent actionEvent) {
        autoBidController.handleSetAutoBid();
    }

    @FXML
    public void handleCancelAutoBid(ActionEvent actionEvent) {
        autoBidController.handleCancelAutoBid();
    }

    @FXML
    public void handleRefreshBids(ActionEvent actionEvent) {
        bidHistoryController.handleRefreshBids();
    }

    // ══════════════════════════════════════════════════════════════════════
    //  UTILS
    // ══════════════════════════════════════════════════════════════════════

    private String str(JsonObject o, String key, String def) {
        return (o != null && o.has(key) && !o.get(key).isJsonNull())
                ? o.get(key).getAsString() : def;
    }

    private double dbl(JsonObject o, String key, double def) {
        return (o != null && o.has(key)) ? o.get(key).getAsDouble() : def;
    }
}