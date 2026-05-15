package com.nhom15.client.controller;

import com.google.gson.JsonObject;
import com.nhom15.client.command.ServerCommand;
import com.nhom15.client.controller.seller.SellerDashboardForms;
import com.nhom15.client.controller.seller.SellerDashboardRenderer;
import com.nhom15.client.controller.seller.SellerDashboardState;
import com.nhom15.client.controller.seller.SellerDashboardStats;
import com.nhom15.client.service.SellerDashboardService;
import com.nhom15.client.util.CardFactory;
import com.nhom15.client.util.SessionManager;
import com.nhom15.client.util.ViewManager;
import java.io.File;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.util.Duration;

public class SellerDashboardController {

  @FXML
  private Label lblSellerName, lblPageTitle, lblDateTime;
  @FXML
  private Button btnMenuOverview, btnMenuItems, btnMenuAddItem, btnMenuAuctions, btnMenuCreateAuction,
      btnMenuOrders, btnMenuFinance, btnMenuAnalytics, btnMenuSettings;
  @FXML
  private VBox panelOverview, panelItems, panelAddItem, panelAuctions, panelCreateAuction,
      panelOrders, panelFinance, panelAnalytics, panelSettings;

  @FXML
  private Label lblTotalItems, lblActiveAuctions, lblTotalSold, lblTotalRevenue, lblNotifications;
  @FXML
  private ComboBox<String> cmbRevenueTimeframe;
  @FXML
  private LineChart<String, Number> chartRevenue;
  @FXML
  private VBox vboxRecentAuctions;

  @FXML
  private TextField txtItemName, txtItemStartPrice;
  @FXML
  private TextArea txtItemDescription;
  @FXML
  private ComboBox<String> cmbItemCategory;
  @FXML
  private Label lblItemNameError, lblItemCategoryError, lblItemPriceError, lblAddItemStatus;
  @FXML
  private ImageView imgItemPreview;

  @FXML
  private VBox vboxExtraFashion, vboxExtraElectronics, vboxExtraVehicle, vboxExtraArt, vboxExtraSports;
  @FXML
  private TextField txtFashionBrand, txtFashionSize, txtFashionColor, txtElecBrand, txtElecSpecs;
  @FXML
  private TextField txtVehicleBrand, txtVehicleYear, txtVehicleMileage, txtArtArtist, txtArtYear,
      txtSportType, txtSportCondition;

  @FXML
  private FlowPane flowMyItems;
  @FXML
  private VBox vboxMyAuctions;
  @FXML
  private ComboBox<String> cmbFilterItems, cmbFilterAuctions;
  @FXML
  private Button btnAuctionActive, btnAuctionUpcoming, btnAuctionEnded, btnAuctionDraft;

  @FXML
  private ComboBox<String> cmbAuctionItem;
  @FXML
  private TextField txtAuctionStartPrice, txtAuctionMinStep, txtAuctionEndTime;
  @FXML
  private DatePicker dateAuctionEnd;
  @FXML
  private Label lblAuctionItemError, lblAuctionPriceError, lblAuctionStepError, lblAuctionTimeError,
      lblCreateAuctionStatus;

  @FXML
  private VBox vboxOrders, vboxTransactions;
  @FXML
  private Label lblWalletBalance;

  private final SellerDashboardService service = new SellerDashboardService();
  private final SellerDashboardRenderer renderer = new SellerDashboardRenderer();
  private final SellerDashboardState state = new SellerDashboardState();
  private Timeline clockTimeline;

  @FXML
  public void initialize() {
    lblSellerName.setText(SessionManager.getUsername());
    bindForms();
    configureControls();
    startClock();
    loadOverview();
  }

  private void bindForms() {
    SellerDashboardForms.bindErrorLabels(
        lblItemNameError, lblItemCategoryError, lblItemPriceError,
        lblAuctionItemError, lblAuctionPriceError, lblAuctionStepError, lblAuctionTimeError);
    SellerDashboardForms.configureCategoryExtras(
        cmbItemCategory, vboxExtraFashion, vboxExtraElectronics, vboxExtraVehicle, vboxExtraArt,
        vboxExtraSports);
  }

  private void configureControls() {
    cmbItemCategory.getItems().addAll(
        "Xe cộ", "Điện tử", "Thời trang", "Nghệ thuật", "Thể thao",
        "Nhà cửa & Sân vườn", "Đồ sưu tầm", "Khác");
    renderer.configureComboBoxes(
        cmbRevenueTimeframe,
        cmbFilterItems,
        cmbFilterAuctions,
        () -> {
          state.setAuctionFilter(renderer.auctionFilterFromLabel(cmbFilterAuctions.getValue()));
          renderAuctions();
        },
        () -> renderer.renderRevenueChart(chartRevenue, state.auctions()));
  }

  private void startClock() {
    clockTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e ->
        lblDateTime.setText(
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy  HH:mm:ss")))));
    clockTimeline.setCycleCount(Timeline.INDEFINITE);
    clockTimeline.play();
  }

  @FXML
  private void handleMenuOverview(ActionEvent event) {
    showPanel(panelOverview, "Tổng quan", btnMenuOverview);
    loadOverview();
  }

  @FXML
  private void handleMenuItems(ActionEvent event) {
    showPanel(panelItems, "Sản phẩm của tôi", btnMenuItems);
    loadMyItems();
  }

  @FXML
  private void handleMenuAddItem(ActionEvent event) {
    showPanel(panelAddItem, "Đăng sản phẩm", btnMenuAddItem);
  }

  @FXML
  private void handleMenuAuctions(ActionEvent event) {
    showPanel(panelAuctions, "Phiên đấu giá", btnMenuAuctions);
    loadMyAuctions();
  }

  @FXML
  private void handleMenuCreateAuction(ActionEvent event) {
    showPanel(panelCreateAuction, "Tạo phiên đấu giá", btnMenuCreateAuction);
    loadItemsForAuction();
  }

  @FXML
  private void handleMenuOrders(ActionEvent event) {
    showPanel(panelOrders, "Đơn hàng", btnMenuOrders);
    loadOrders();
  }

  @FXML
  private void handleMenuFinance(ActionEvent event) {
    showPanel(panelFinance, "Tài chính", btnMenuFinance);
    loadFinance();
  }

  @FXML
  private void handleMenuAnalytics(ActionEvent event) {
    showPanel(panelAnalytics, "Phân tích", btnMenuAnalytics);
    renderer.renderRevenueChart(chartRevenue, state.auctions());
  }

  @FXML
  private void handleMenuSettings(ActionEvent event) {
    showPanel(panelSettings, "Cài đặt", btnMenuSettings);
  }

  private void showPanel(VBox target, String title, Button activeButton) {
    renderer.showPanel(lblPageTitle, target, title, activeButton, panels(), menuButtons());
  }

  private void loadOverview() {
    int sellerId = SessionManager.getUserId();
    service.fetchItems(sellerId, items -> {
      state.setItems(items);
      lblTotalItems.setText(String.valueOf(items.size()));
      lblTotalSold.setText(String.valueOf(SellerDashboardStats.soldItemCount(items)));
    }, () -> {
      lblTotalItems.setText("0");
      lblTotalSold.setText("0");
    });

    service.fetchAuctions(sellerId, auctions -> {
      state.setAuctions(auctions);
      lblActiveAuctions.setText(String.valueOf(SellerDashboardStats.activeAuctionCount(auctions)));
      lblTotalRevenue.setText(SellerDashboardStats.formatMoney(SellerDashboardStats.revenue(auctions)));
      int endingSoon = SellerDashboardStats.endingSoonCount(auctions);
      lblNotifications.setText(endingSoon > 0
          ? "Bạn có " + endingSoon + " phiên đấu giá sắp kết thúc trong 24 giờ tới"
          : "Không có thông báo khẩn cấp mới");
      renderer.renderRecentAuctions(vboxRecentAuctions, auctions, this::handleEndAuction);
      renderer.renderRevenueChart(chartRevenue, auctions);
    }, () -> {
      lblActiveAuctions.setText("0");
      lblTotalRevenue.setText("0 d");
      vboxRecentAuctions.getChildren().setAll(CardFactory.buildEmptyLabel("Chưa có phiên đấu giá nào"));
    });
  }

  private void loadMyItems() {
    service.fetchItems(SessionManager.getUserId(), items -> {
      state.setItems(items);
      renderer.renderItems(flowMyItems, items, this::handleDeleteItem);
    }, () -> flowMyItems.getChildren().setAll(CardFactory.buildEmptyLabel("Chưa có sản phẩm nào")));
  }

  private void loadItemsForAuction() {
    service.fetchItems(SessionManager.getUserId(), items -> {
      state.setItems(items);
      indexAvailableItemsForAuction();
    }, () -> {
      cmbAuctionItem.getItems().clear();
      state.clearAuctionItemIndex();
    });
  }

  private void indexAvailableItemsForAuction() {
    cmbAuctionItem.getItems().clear();
    state.clearAuctionItemIndex();
    for (int i = 0; i < state.items().size(); i++) {
      JsonObject item = state.items().get(i).getAsJsonObject();
      if (!"AVAILABLE".equals(SellerDashboardStats.getString(item, "status", ""))) {
        continue;
      }
      String name = SellerDashboardStats.getString(item, "name", "");
      cmbAuctionItem.getItems().add(name);
      state.itemNameToId().put(name, (int) SellerDashboardStats.getDouble(item, "itemId", -1));
      state.itemNameToPrice().put(name, (long) SellerDashboardStats.getDouble(item, "startPrice", 0));
    }
    cmbAuctionItem.setOnAction(e -> {
      String selected = cmbAuctionItem.getValue();
      if (selected != null && state.itemNameToPrice().containsKey(selected)) {
        txtAuctionStartPrice.setText(String.valueOf(state.itemNameToPrice().get(selected)));
      }
    });
  }

  private void loadMyAuctions() {
    service.fetchAuctions(SessionManager.getUserId(), auctions -> {
      state.setAuctions(auctions);
      renderAuctions();
    }, () -> vboxMyAuctions.getChildren().setAll(CardFactory.buildEmptyLabel("Chưa có phiên đấu giá nào")));
  }

  private void loadOrders() {
    service.fetchAuctions(SessionManager.getUserId(), auctions -> {
      state.setAuctions(auctions);
      renderer.renderOrders(vboxOrders, auctions);
    }, () -> vboxOrders.getChildren().setAll(CardFactory.buildEmptyLabel("Chưa có đơn hàng nào cần xử lý")));
  }

  private void loadFinance() {
    service.fetchAuctions(SessionManager.getUserId(), auctions -> {
      state.setAuctions(auctions);
      renderer.renderFinance(lblWalletBalance, vboxTransactions, auctions);
    }, () -> renderer.renderFinance(lblWalletBalance, vboxTransactions, state.auctions()));
  }

  private void renderAuctions() {
    renderer.renderAuctions(vboxMyAuctions, state.auctions(), state.auctionFilter(), this::handleEndAuction);
  }

  @FXML
  private void filterActiveAuctions(ActionEvent event) {
    setAuctionFilter("ACTIVE", btnAuctionActive);
  }

  @FXML
  private void filterUpcomingAuctions(ActionEvent event) {
    setAuctionFilter("UPCOMING", btnAuctionUpcoming);
  }

  @FXML
  private void filterEndedAuctions(ActionEvent event) {
    setAuctionFilter("ENDED", btnAuctionEnded);
  }

  @FXML
  private void filterDraftAuctions(ActionEvent event) {
    setAuctionFilter("DRAFT", btnAuctionDraft);
  }

  private void setAuctionFilter(String filter, Button activeButton) {
    state.setAuctionFilter(filter);
    renderer.selectAuctionFilterButton(
        activeButton, btnAuctionActive, btnAuctionUpcoming, btnAuctionEnded, btnAuctionDraft);
    renderAuctions();
  }

  @FXML
  private void handlePickItemImage(javafx.scene.input.MouseEvent event) {
    pickImage();
  }

  @FXML
  private void handlePickItemImage(ActionEvent event) {
    pickImage();
  }

  private void pickImage() {
    FileChooser chooser = new FileChooser();
    chooser.setTitle("Chọn ảnh sản phẩm");
    chooser.getExtensionFilters()
        .add(new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg"));
    File file = chooser.showOpenDialog(txtItemName.getScene().getWindow());
    if (file == null) {
      return;
    }
    if (file.length() > 2 * 1024 * 1024) {
      showAlert("Ảnh không được vượt quá 2MB!");
      return;
    }
    try {
      state.setSelectedImageBase64(Base64.getEncoder().encodeToString(Files.readAllBytes(file.toPath())));
      state.setSelectedImageExt(file.getName().toLowerCase().endsWith(".png") ? "png" : "jpg");
      imgItemPreview.setImage(new Image(file.toURI().toString()));
      imgItemPreview.setVisible(true);
    } catch (Exception e) {
      showAlert("Không thể đọc file ảnh!");
    }
  }

  @FXML
  private void handleAddItem(ActionEvent event) {
    JsonObject payload = SellerDashboardForms.buildItemPayload(
        itemForm(), SessionManager.getUserId(), state.selectedImageBase64(), state.selectedImageExt());
    if (payload == null) {
      return;
    }
    service.createItem(payload, response -> {
      if (ServerCommand.isSuccess(response)) {
        showStatus(lblAddItemStatus, "Dang san pham thanh cong!", true);
        SellerDashboardForms.clearItemForm(itemForm());
        imgItemPreview.setVisible(false);
        state.setSelectedImageBase64("");
        loadMyItems();
      } else {
        showStatus(lblAddItemStatus, ServerCommand.getMessage(response, "Dang san pham that bai!"), false);
      }
    });
  }

  @FXML
  private void handleCreateAuction(ActionEvent event) {
    JsonObject payload = SellerDashboardForms.buildAuctionPayload(
        auctionForm(), SessionManager.getUserId(), state.itemNameToId());
    if (payload == null) {
      return;
    }
    service.createAuction(payload, response -> {
      if (ServerCommand.isSuccess(response)) {
        showStatus(lblCreateAuctionStatus, "Tao phien dau gia thanh cong!", true);
        SellerDashboardForms.clearAuctionForm(auctionForm());
        loadItemsForAuction();
      } else {
        showStatus(lblCreateAuctionStatus,
            ServerCommand.getMessage(response, "Tao phien dau gia that bai!"), false);
      }
    });
  }

  private void handleDeleteItem(int itemId) {
    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
        "Hành động này không thể hoàn tác.", ButtonType.OK, ButtonType.CANCEL);
    confirm.setTitle("Xác nhận xóa");
    confirm.setHeaderText("Bạn chắc chắn muốn xóa sản phẩm này?");
    confirm.showAndWait().ifPresent(result -> {
      if (result == ButtonType.OK) {
        service.deleteItem(itemId, response -> {
          if (ServerCommand.isSuccess(response)) {
            loadMyItems();
          } else {
            showAlert(ServerCommand.getMessage(response, "Xóa thất bại!"));
          }
        });
      }
    });
  }

  private void handleEndAuction(int auctionId) {
    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
        "Kết thúc phiên đấu giá sớm?", ButtonType.OK, ButtonType.CANCEL);
    confirm.setTitle("Kết thúc phiên");
    confirm.showAndWait().ifPresent(result -> {
      if (result == ButtonType.OK) {
        service.endAuction(auctionId, response -> {
          if (ServerCommand.isSuccess(response)) {
            loadMyAuctions();
            loadOverview();
          } else {
            showAlert(ServerCommand.getMessage(response, "Kết thúc phiên thất bại!"));
          }
        });
      }
    });
  }

  @FXML
  private void handleGoHome(ActionEvent event) {
    stopClock();
    ViewManager.navigateTo(ViewManager.Views.HOME);
  }

  @FXML
  private void handleLogout(ActionEvent event) {
    stopClock();
    SessionManager.logout();
    ViewManager.navigateTo(ViewManager.Views.LOGIN);
  }

  private void stopClock() {
    if (clockTimeline != null) {
      clockTimeline.stop();
    }
  }

  private VBox[] panels() {
    return new VBox[] {
        panelOverview, panelItems, panelAddItem, panelAuctions, panelCreateAuction,
        panelOrders, panelFinance, panelAnalytics, panelSettings
    };
  }

  private Button[] menuButtons() {
    return new Button[] {
        btnMenuOverview, btnMenuItems, btnMenuAddItem, btnMenuAuctions, btnMenuCreateAuction,
        btnMenuOrders, btnMenuFinance, btnMenuAnalytics, btnMenuSettings
    };
  }

  private SellerDashboardForms.ItemForm itemForm() {
    return new SellerDashboardForms.ItemForm(
        txtItemName, txtItemDescription, cmbItemCategory, txtItemStartPrice,
        lblItemNameError, lblItemCategoryError, lblItemPriceError, lblAddItemStatus,
        txtFashionBrand, txtFashionSize, txtFashionColor,
        txtElecBrand, txtElecSpecs,
        txtVehicleBrand, txtVehicleYear, txtVehicleMileage,
        txtArtArtist, txtArtYear,
        txtSportType, txtSportCondition);
  }

  private SellerDashboardForms.AuctionForm auctionForm() {
    return new SellerDashboardForms.AuctionForm(
        cmbAuctionItem, txtAuctionStartPrice, txtAuctionMinStep, dateAuctionEnd, txtAuctionEndTime,
        lblAuctionItemError, lblAuctionPriceError, lblAuctionStepError, lblAuctionTimeError,
        lblCreateAuctionStatus);
  }

  private void showStatus(Label label, String message, boolean success) {
    label.setText(message);
    label.setTextFill(success ? javafx.scene.paint.Color.web("#4CAF50")
        : javafx.scene.paint.Color.web("#D96570"));
  }

  private void showAlert(String message) {
    new Alert(Alert.AlertType.INFORMATION, message).showAndWait();
  }
}
