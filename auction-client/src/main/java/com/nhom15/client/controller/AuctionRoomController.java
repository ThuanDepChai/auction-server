package com.nhom15.client.controller;

import com.google.gson.JsonObject;
import com.nhom15.client.command.GetAuctionDetailCommand;
import com.nhom15.client.command.ServerCommand;
import com.nhom15.client.controller.bidding.*;
import com.nhom15.client.network.AuctionRealtimeSubscriber;
import com.nhom15.client.util.SessionManager;
import com.nhom15.client.util.ViewManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * AuctionRoomController — controller chính của phòng đấu giá.
 *
 * Kiến trúc cập nhật realtime:
 *  ┌─────────────────────────────────────────────────────────────────┐
 *  │  AuctionRealtimeSubscriber  (TCP long-poll, FX-thread callback) │
 *  │         ↓ applyPushEnvelope()                                   │
 *  │  RealtimePollingController  (fallback poll 30s + notifyBid)     │
 *  │         ↓ onPriceChanged callback                               │
 *  │  applyPriceUpdate()  → UI labels + chart + history + labels     │
 *  └─────────────────────────────────────────────────────────────────┘
 *
 *  Sau khi PlaceBid thành công:
 *    ManualBidController → notifyBidResult(res) → giá cập nhật NGAY
 *    (không chờ polling 30s tiếp theo)
 */
public class AuctionRoomController implements Initializable {

  // ── FXML: Product panel ───────────────────────────────────────────────
  @FXML private Pane      paneBackground;
  @FXML private ImageView imgProduct;
  @FXML private Label     lblImgPlaceholder;
  @FXML private Label     lblProductName;
  @FXML private Label     lblProductDesc;
  @FXML private GridPane  gridSpecs;

  // ── FXML: Status ribbon ───────────────────────────────────────────────
  @FXML private Label lblViewerCount;

  // ── FXML: Timer + current bid ─────────────────────────────────────────
  @FXML private Label lblCountdown;
  @FXML private Label lblSoftClose;
  @FXML private Label lblCurrentBid;
  @FXML private Label lblBidStatus;

  // ── FXML: Manual bid controls ─────────────────────────────────────────
  @FXML private TextField txtCustomBid;
  @FXML private Button    btnQuickBid1;
  @FXML private Button    btnQuickBid2;
  @FXML private Button    btnQuickBid3;
  @FXML private Button    btnConfirmBid;
  @FXML private Button    btnPlaceBid;

  // ── FXML: Auto-Bid panel ──────────────────────────────────────────────
  @FXML private TextField txtMaxBid;
  @FXML private TextField txtIncrement;
  @FXML private Button    btnSetAutoBid;
  @FXML private Button    btnCancelAutoBid;
  @FXML private Label     lblAutoBidStatus;
  @FXML private Label     lblAutoBidInfo;

  // ── FXML: Live chart ──────────────────────────────────────────────────
  @FXML private LineChart<Number, Number> chartPriceLive;
  @FXML private NumberAxis axisChartX;
  @FXML private NumberAxis axisChartY;
  @FXML private Label      lblChartLastUpdate;

  // ── FXML: Dynamic bid history ─────────────────────────────────────────
  @FXML private VBox       vboxBidHistory;
  @FXML private ScrollPane scrollHistory;

  // ── Sub-Controllers ───────────────────────────────────────────────────
  private ProductPanelController   productPanelCtrl;
  private ManualBidController      manualBidCtrl;
  private PriceCountdownController countdownCtrl;
  private BidHistoryController     historyCtrl;
  private AutoBidController        autoBidCtrl;
  private RealtimePollingController pollingCtrl;

  /** Kết nối TCP dài hạn — nhận push AUCTION_UPDATE trên FX thread. */
  private AuctionRealtimeSubscriber subscriber;

  private final AuctionState state = new AuctionState();
  private Runnable onBack;

  // ── initialize ────────────────────────────────────────────────────────

  @Override
  public void initialize(URL url, ResourceBundle rb) {
    wireControllers();
  }

  private void wireControllers() {

    // 1. Product panel
    productPanelCtrl = new ProductPanelController();
    productPanelCtrl.setNodes(
        imgProduct, lblImgPlaceholder, lblProductName,
        null, null, lblProductDesc,
        null, null, null, null, null, null);
    productPanelCtrl.setGridSpecs(gridSpecs);

    // 2. Manual bid
    //    txtCustomBid → txtBidAmount (trường bên trong ManualBidController)
    //    btnConfirmBid → btnPlaceBid (nút bị disable + đổi text khi loading)
    manualBidCtrl = new ManualBidController();
    manualBidCtrl.setNodes(
        txtCustomBid,       // txtBidAmount
        null,               // lblMinBid
        null,               // lblBidError
        lblBidStatus,       // lblBidStatus
        btnConfirmBid,      // btnPlaceBid (disabled during loading)
        btnQuickBid1, btnQuickBid2, btnQuickBid3,
        null, null, null, null);
    manualBidCtrl.initialize();

    // 3. Countdown + price display
    //    lblCurrentBid → lblCurrentPrice
    //    lblSoftClose  → lblAntiSnipe
    countdownCtrl = new PriceCountdownController();
    countdownCtrl.setNodes(
        lblCurrentBid,      // lblCurrentPrice
        null,               // lblPriceChange
        null,               // lblLeader
        null,               // lblTotalBids
        lblCountdown,       // lblCountdown
        null,               // lblMyBudget
        null,               // progressBudget
        null,               // lblBudgetUsed
        null,               // lblStatus
        lblSoftClose,       // lblAntiSnipe
        null,               // progressTime
        lblChartLastUpdate); // lblLastUpdate
    countdownCtrl.initialize();

    // 4. Bid history + chart
    historyCtrl = new BidHistoryController();
    historyCtrl.setNodes(
        vboxBidHistory, scrollHistory,
        null, null, null,        // count / search / filter (optional)
        chartPriceLive, axisChartX, axisChartY,
        null, null, null, null,  // chart stats labels (optional)
        null, null);             // cmbChartType / toggleSmooth
    historyCtrl.initialize();        // tạo priceSeries và gắn vào chartPriceLive

    // 5. Auto-bid
    autoBidCtrl = new AutoBidController();
    autoBidCtrl.setNodes(
        txtMaxBid, txtIncrement,
        btnSetAutoBid, btnCancelAutoBid,
        lblAutoBidStatus, lblAutoBidInfo,
        null, null, null);
    autoBidCtrl.initialize();

    // 6. Realtime infrastructure
    pollingCtrl  = new RealtimePollingController();
    subscriber   = new AuctionRealtimeSubscriber();
  }

  // ── Public API ────────────────────────────────────────────────────────

  public void setAuctionId(int id) {
    state.setAuctionId(id);
    loadAuctionData(id);
  }

  public void setOnBack(Runnable r) { this.onBack = r; }

  // ── Data loading ──────────────────────────────────────────────────────

  private void loadAuctionData(int id) {
    new GetAuctionDetailCommand(id).executeAsync(res -> {
      if (!ServerCommand.isSuccess(res)) return;
      JsonObject auction = res.getAsJsonObject("auction");

      Platform.runLater(() -> {

        // Cập nhật state
        state.setCurrentPrice(dbl(auction, "currentPrice", 0));
        state.setMinStep(dbl(auction, "minStep", 50_000));
        state.setEndTime(str(auction, "endTime", ""));

        // Populate sản phẩm + giá ban đầu
        productPanelCtrl.populate(auction);
        countdownCtrl.updatePrice(state.getCurrentPrice(), 0);
        countdownCtrl.startCountdown(state.getEndTime(), this::onAuctionEnded);

        // Wire RealtimePollingController
        pollingCtrl.setup(
            state,
            this::onPriceChanged,
            (leader, total) -> {
              countdownCtrl.updateLeader(leader, SessionManager.getUsername());
              countdownCtrl.updateTotalBids(total);
            },
            this::onAuctionEnded);

        pollingCtrl.setOnEndTimeChanged(newEnd ->
            countdownCtrl.startCountdown(newEnd, this::onAuctionEnded));

        // Wire ManualBidController với pollingCtrl:
        //   Sau PlaceBid thành công → notifyBidResult(res) → giá thay đổi NGAY
        //   → onPriceChanged được gọi → applyPriceUpdate → chart + history + labels
        manualBidCtrl.setup(
            state,
            ignoredPrice -> manualBidCtrl.refreshLabels(),
            pollingCtrl);

        // Wire AutoBid
        autoBidCtrl.setup(state);
        autoBidCtrl.loadStatus();

        // Wire + load history (vẽ chart lần đầu)
        historyCtrl.setup(state);
        historyCtrl.load();

        // Bắt đầu subscribe realtime (TCP long-poll trên daemon thread)
        // Callback về FX thread thông qua Platform.runLater trong AuctionRealtimeSubscriber
        subscriber.start(id,
            envelope -> pollingCtrl.applyPushEnvelope(envelope));

        // Fallback polling 30s (tự hồi phục nếu lỡ push)
        pollingCtrl.start();
      });
    });
  }

  // ── Callbacks ─────────────────────────────────────────────────────────

  /**
   * Gọi bất cứ khi nào giá thay đổi (từ push, poll, hoặc ngay sau PlaceBid).
   * Luôn chạy trên FX thread.
   */
  private void onPriceChanged(double newPrice, double oldPrice) {
    if (Platform.isFxApplicationThread()) {
      applyPriceUpdate(newPrice, oldPrice);
    } else {
      Platform.runLater(() -> applyPriceUpdate(newPrice, oldPrice));
    }
  }

  /**
   * Cập nhật toàn bộ UI liên quan đến giá mới.
   * Phải gọi trên FX thread.
   */
  private void applyPriceUpdate(double newPrice, double oldPrice) {
    countdownCtrl.updatePrice(newPrice, oldPrice);   // nhãn giá + animation
    countdownCtrl.updateLastUpdateLabel();            // "Cập nhật: HH:mm:ss"
    historyCtrl.addChartPoint(newPrice);             // thêm điểm vào chart NGAY
    manualBidCtrl.refreshLabels();                    // cập nhật quick-bid text
    historyCtrl.load();                              // reload lịch sử từ server
  }

  private void onAuctionEnded() {
    if (Platform.isFxApplicationThread()) {
      doAuctionEnded();
    } else {
      Platform.runLater(this::doAuctionEnded);
    }
  }

  private void doAuctionEnded() {
    state.setAuctionEnded(true);
    manualBidCtrl.disableAll();
    autoBidCtrl.disableAll();
    pollingCtrl.stop();
    subscriber.stop();
    countdownCtrl.stopCountdown();
  }

  // ── FXML Handlers ─────────────────────────────────────────────────────

  /** "Đặt giá ngay" — secondary outline button. */
  @FXML
  public void handlePlaceBid() {
    manualBidCtrl.handlePlaceBid();
  }

  /** "Xác nhận đặt giá" — primary CTA button. */
  @FXML
  public void handleConfirmBid() {
    manualBidCtrl.handlePlaceBid();
  }

  /**
   * Quick-bid FXML handlers — các phương thức này bị ManualBidController.setupQuickBidButtons()
   * override bằng setOnAction(fillQuick) ngay sau khi setup() được gọi.
   * Giữ lại để tránh lỗi FXML binding (NoSuchMethodException) khi FXML load.
   */
  @FXML private void handleQuickBid1() { manualBidCtrl.handlePlaceBid(); }
  @FXML private void handleQuickBid2() { manualBidCtrl.handlePlaceBid(); }
  @FXML private void handleQuickBid3() { manualBidCtrl.handlePlaceBid(); }

  /** Bật Auto-Bid. */
  @FXML
  public void handleSetAutoBid() {
    autoBidCtrl.handleSetAutoBid();
  }

  /** Huỷ Auto-Bid. */
  @FXML
  public void handleCancelAutoBid() {
    autoBidCtrl.handleCancelAutoBid();
  }

  /** Đóng phòng — dừng kết nối + điều hướng về Home. */
  @FXML
  private void handleCloseRoom() {
    subscriber.stop();
    pollingCtrl.stop();
    countdownCtrl.stopCountdown();
    if (onBack != null) onBack.run();
    ViewManager.navigateTo(ViewManager.Views.HOME);
  }

  // ── Utilities ─────────────────────────────────────────────────────────

  private String str(JsonObject o, String k, String d) {
    return (o != null && o.has(k) && !o.get(k).isJsonNull()) ? o.get(k).getAsString() : d;
  }

  private double dbl(JsonObject o, String k, double d) {
    return (o != null && o.has(k)) ? o.get(k).getAsDouble() : d;
  }
}
