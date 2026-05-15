package com.nhom15.client.controller;

import com.google.gson.JsonObject;
import com.nhom15.client.command.GetAuctionDetailCommand;
import com.nhom15.client.command.ServerCommand;
import com.nhom15.client.controller.bidding.*;
import com.nhom15.client.util.SessionManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;

import java.net.URL;
import java.util.ResourceBundle;

public class AuctionRoomController implements Initializable {

  // ── FXML Nodes ───────────────────────────────────────────────────────
  @FXML private Pane paneBackground;
  @FXML private ImageView imgProduct;
  @FXML private Label lblImgPlaceholder, lblProductName, lblProductDesc;
  @FXML private Label lblCountdown, lblSoftClose, lblCurrentBid, lblBidStatus, lblViewerCount;
  @FXML private Button btnQuickBid1, btnQuickBid2, btnQuickBid3, btnConfirmBid;
  @FXML private TextField txtCustomBid;

  // ── Sub-Controllers ──────────────────────────────────────────────────
  private ProductPanelController productPanelController;
  private ManualBidController manualBidController;
  private PriceCountdownController priceCountdownController;
  private BidHistoryController bidHistoryController;
  private AutoBidController autoBidController;

  private final AuctionState state = new AuctionState();
  private Runnable onBack;

  @Override
  public void initialize(URL location, ResourceBundle resources) {
    setupControllers();
  }

  private void setupControllers() {
    // 1. Product Panel
    productPanelController = new ProductPanelController();
    productPanelController.setNodes(imgProduct, lblImgPlaceholder, lblProductName,
            null, null, lblProductDesc, null, null, null, null, null, null);

    // 2. Manual Bid
    manualBidController = new ManualBidController();
    manualBidController.setNodes(txtCustomBid, null, null, lblBidStatus, btnConfirmBid,
            btnQuickBid1, btnQuickBid2, btnQuickBid3, null, null, null, null);
    manualBidController.initialize();

    // 3. Price & Countdown
    priceCountdownController = new PriceCountdownController();
    priceCountdownController.setNodes(lblCurrentBid, null, null, null, lblCountdown,
            null, null, null, null, lblSoftClose, null, null);
    priceCountdownController.initialize();

    // 4. Auto Bid & History
    autoBidController = new AutoBidController();
    bidHistoryController = new BidHistoryController();
  }

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
        // Cập nhật State
        state.setCurrentPrice(dbl(auction, "currentPrice", 0));
        state.setMinStep(dbl(auction, "minStep", 50000));
        state.setEndTime(str(auction, "endTime", ""));

        // Gọi các hàm ĐÚNG TÊN trong sub-controller
        productPanelController.populate(auction); // Không dùng setData

        manualBidController.setup(state, newPrice -> {
          priceCountdownController.updatePrice(newPrice, state.getCurrentPrice());
          state.setCurrentPrice(newPrice);
          manualBidController.refreshLabels();
        }, null);

        priceCountdownController.updatePrice(state.getCurrentPrice(), 0);
        priceCountdownController.startCountdown(state.getEndTime(), () -> {
          System.out.println("Auction Ended");
        });
      });
    });
  }

  // FIX LỖI #handlePlaceBid TRONG FXML
  @FXML
  public void handlePlaceBid() {
    manualBidController.handlePlaceBid();
  }

  @FXML
  public void handleConfirmBid() {
    manualBidController.handlePlaceBid();
  }

  @FXML
  private void handleCloseRoom() {
    if (onBack != null) onBack.run();
    com.nhom15.client.util.ViewManager.navigateTo(com.nhom15.client.util.ViewManager.Views.HOME);
  }

  // ── Quickbid Handlers ────────────────────────────────────────────────
  @FXML private void handleQuickBid1() { manualBidController.handlePlaceBid(); }
  @FXML private void handleQuickBid2() { manualBidController.handlePlaceBid(); }
  @FXML private void handleQuickBid3() { manualBidController.handlePlaceBid(); }

  // Utility methods to avoid errors
  private String str(JsonObject o, String k, String d) { return (o.has(k) && !o.get(k).isJsonNull()) ? o.get(k).getAsString() : d; }
  private double dbl(JsonObject o, String k, double d) { return o.has(k) ? o.get(k).getAsDouble() : d; }
}