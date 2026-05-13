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
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

/**
 * BiddingRoomController — ORCHESTRATOR của màn hình đấu giá.
 *
 * Nhiệm vụ:
 *  1. Nhận auctionId và khởi tạo AuctionState chia sẻ.
 *  2. Inject AuctionState + callback vào từng sub-controller.
 *  3. Kết nối callback giữa các sub-controller.
 *  4. Xử lý top-bar: Back, Watchlist, Share, Chat.
 *
 * KHÔNG chứa logic nghiệp vụ — mọi logic đều trong sub-controller:
 *  - ProductPanelController   → panel sản phẩm
 *  - PriceCountdownController → giá + countdown + anti-snipe
 *  - ManualBidController      → tab đặt giá thủ công
 *  - AutoBidController        → tab auto-bid
 *  - BidHistoryController     → tab lịch sử + biểu đồ
 *  - RealtimePollingController → polling server 3 giây
 */
public class BiddingRoomController {

    // ── Top bar (chỉ những gì orchestrator thực sự cần) ──────────────────
    @FXML private Label   lblRoomId;
    @FXML private Button  btnWatchlist;
    @FXML private TabPane tabPane;

    // ── Chat (chưa tách sub-controller) ───────────────────────────────────
    @FXML private VBox       vboxChat;
    @FXML private ScrollPane scrollChat;
    @FXML private TextField  txtChatMessage;
    @FXML private Label      lblOnlineCount;
    @FXML private VBox       vboxLiveFeed;
    @FXML private VBox       vboxLeaderboard;
    @FXML private Label      lblUpdateTick;
    @FXML private VBox       vboxSimilar;

    // ── Sub-controllers (khai báo @FXML để JavaFX inject qua fx:id) ───────
    @FXML private ProductPanelController   productPanelController;
    @FXML private PriceCountdownController priceCountdownController;
    @FXML private ManualBidController      manualBidController;
    @FXML private AutoBidController        autoBidController;
    @FXML private BidHistoryController     bidHistoryController;

    // ── Không dùng @FXML vì không gắn với node cụ thể ────────────────────
    private final RealtimePollingController poller = new RealtimePollingController();
    private final AuctionState state = new AuctionState();
    private Runnable onBack;

    // ══════════════════════════════════════════════════════════════════════
    //  INITIALIZE
    // ══════════════════════════════════════════════════════════════════════

    @FXML
    public void initialize() {
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
        if (onBack != null) onBack.run();
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

    @FXML private void handleShare() {
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
    private void handleChatEmoji(javafx.event.ActionEvent e) {
        Button btn = (Button) e.getSource();
        System.out.println("[Emoji] " + btn.getText());
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
    // Lỗi ở đây :
    public void handlePlaceBid(ActionEvent actionEvent) {
    }

    public void handleSetAutoBid(ActionEvent actionEvent) {
    }

    public void handleCancelAutoBid(ActionEvent actionEvent) {
    }

    public void handleRefreshBids(ActionEvent actionEvent) {
    }
}
