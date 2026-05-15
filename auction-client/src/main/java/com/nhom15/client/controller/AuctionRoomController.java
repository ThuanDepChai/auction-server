package com.nhom15.client.controller;

import com.google.gson.JsonObject;
import com.nhom15.client.command.GetAuctionDetailCommand;
import com.nhom15.client.command.ServerCommand;
import com.nhom15.client.controller.bidding.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;

import java.net.URL;
import java.util.ResourceBundle;

public class AuctionRoomController implements Initializable {

  // ── FXML Nodes (Mapping từ file FXML bạn gửi) ────────────────────────
  @FXML private AnchorPane rootPane;
  @FXML private Pane paneBackground;
  @FXML private ImageView imgProduct;
  @FXML private Label lblImgPlaceholder;
  @FXML private Label lblProductName, lblProductDesc;

  @FXML private Label lblCountdown, lblSoftClose, lblCurrentBid, lblBidStatus;
  @FXML private StackPane timerPane;
  @FXML private Circle dotPulse;

  @FXML private TextField txtCustomBid;
  @FXML private Button btnQuickBid1, btnQuickBid2, btnQuickBid3, btnConfirmBid;

  // ── Sub-Controllers ──────────────────────────────────────────────────
  private ProductPanelController productPanel;
  private ManualBidController manualBid;
  private PriceCountdownController priceCountdown;
  private BidHistoryController bidHistory;
  private AutoBidController autoBid;
  private Runnable onBack;
  private AuctionState state = new AuctionState();

  @Override
  public void initialize(URL location, ResourceBundle resources) {
    initSubControllers();
    // Giả sử lấy auctionId từ Session hoặc qua hàm setAuctionId
  }

  /**
   * Bước 1: Khởi tạo và "bơm" (Inject) các Node từ FXML vào Controller con
   */
  private void initSubControllers() {
    // 1. Product Panel
    productPanel = new ProductPanelController();
    productPanel.setNodes(imgProduct, lblImgPlaceholder, lblProductName,
            null, null, lblProductDesc, null, null, null, null, null, null);

    // 2. Manual Bid (Điều khiển đặt giá)
    manualBid = new ManualBidController();
    manualBid.setNodes(txtCustomBid, null, null, lblBidStatus, btnConfirmBid,
            btnQuickBid1, btnQuickBid2, btnQuickBid3, null, null, null, null);
    manualBid.initialize();

    // 3. Price & Countdown (Vùng đếm ngược giữa màn hình)
    priceCountdown = new PriceCountdownController();
    priceCountdown.setNodes(lblCurrentBid, null, null, null, lblCountdown,
            null, null, null, null, lblSoftClose, null, null);
    priceCountdown.initialize();

    // 4. Các controller khác (tùy biến thêm nếu bạn nhúng thêm FXML con)
    bidHistory = new BidHistoryController();
    autoBid = new AutoBidController();
  }

  /**
   * Bước 2: Load dữ liệu từ Server và phân phối cho các con
   */
  public void setAuctionId(int id) {
    state.setAuctionId(id);
    loadAuctionData(id);
  }
  public void setOnBack(Runnable onBack) {
    this.onBack = onBack;
  }

  private void loadAuctionData(int id) {
    new GetAuctionDetailCommand(id).executeAsync(res -> {
      if (!ServerCommand.isSuccess(res)) return;
      JsonObject auction = res.getAsJsonObject("auction");

      Platform.runLater(() -> {
        // Cập nhật State chung
        state.setCurrentPrice(dbl(auction, "currentPrice", 0));
        state.setMinStep(dbl(auction, "minStep", 50000));
        state.setEndTime(str(auction, "endTime", ""));

        // Ra lệnh cho các Controller con hiển thị dữ liệu
        productPanel.populate(auction);

        manualBid.setup(state, newPrice -> {
          // Callback khi đặt giá thành công
          onBidSuccess(newPrice);
        }, null); // Bạn có thể truyền PollingController vào đây

        priceCountdown.updatePrice(state.getCurrentPrice(), 0);
        priceCountdown.startCountdown(state.getEndTime(), () -> {
          System.out.println("Phiên đấu giá kết thúc!");
        });
      });
    });
  }

  private void onBidSuccess(double newPrice) {
    // Khi có giá mới, cập nhật đồng loạt
    priceCountdown.updatePrice(newPrice, state.getCurrentPrice());
    state.setCurrentPrice(newPrice);
    manualBid.refreshLabels();
    // bidHistory.load(); // Refresh lịch sử nếu cần
  }

  @FXML
  private void handleCloseRoom() {
    if (onBack != null) {
      onBack.run();
    }
    com.nhom15.client.util.ViewManager.navigateTo(com.nhom15.client.util.ViewManager.Views.HOME);
  }

  // ── Helper Methods để tránh lỗi 'Cannot resolve symbol' ───────────────
  private String str(JsonObject o, String key, String def) {
    return (o != null && o.has(key) && !o.get(key).isJsonNull()) ? o.get(key).getAsString() : def;
  }

  private double dbl(JsonObject o, String key, double def) {
    return (o != null && o.has(key)) ? o.get(key).getAsDouble() : def;
  }

  // Các hàm Action từ FXML gọi trực tiếp vào ManualBid cho gọn
  @FXML private void handleQuickBid1() { manualBid.handlePlaceBid(); }
  @FXML private void handleQuickBid2() { manualBid.handlePlaceBid(); }
  @FXML private void handleQuickBid3() { manualBid.handlePlaceBid(); }
  @FXML private void handleConfirmBid() { manualBid.handlePlaceBid(); }
}