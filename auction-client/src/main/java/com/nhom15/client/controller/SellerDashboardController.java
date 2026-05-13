package com.nhom15.client.controller;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.nhom15.client.command.SellerCommand;
import com.nhom15.client.util.CardFactory;
import com.nhom15.client.util.SessionManager;
import com.nhom15.client.util.ViewManager;
import java.io.File;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
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

  // ── Sidebar & Panels ─────────────────────────────────────────────────────
  @FXML
  private Label lblSellerName, lblPageTitle, lblDateTime;
  @FXML
  private Button btnMenuOverview, btnMenuItems, btnMenuAddItem, btnMenuAuctions, btnMenuCreateAuction;
  @FXML
  private VBox panelOverview, panelItems, panelAddItem, panelAuctions, panelCreateAuction;

  // ── Overview ─────────────────────────────────────────────────────────────
  @FXML
  private Label lblTotalItems, lblActiveAuctions, lblTotalSold, lblTotalRevenue;
  @FXML
  private VBox vboxRecentAuctions;

  // ── Add Item ─────────────────────────────────────────────────────────────
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

  // ── My Items & My Auctions ───────────────────────────────────────────────
  @FXML
  private FlowPane flowMyItems;
  @FXML
  private VBox vboxMyAuctions;

  // ── Create Auction ───────────────────────────────────────────────────────
  @FXML
  private ComboBox<String> cmbAuctionItem;
  @FXML
  private TextField txtAuctionStartPrice, txtAuctionMinStep, txtAuctionEndTime;
  @FXML
  private DatePicker dateAuctionEnd;
  @FXML
  private Label lblAuctionItemError, lblAuctionPriceError, lblAuctionStepError, lblAuctionTimeError, lblCreateAuctionStatus;

  // ── Internal ─────────────────────────────────────────────────────────────
  private String selectedImageBase64 = "";
  private String selectedImageExt = "jpg";
  private final Map<String, Integer> itemNameToId = new HashMap<>();
  private Timeline clockTimeline;

  @FXML
  public void initialize() {
    lblSellerName.setText(SessionManager.getUsername());

    // Bind errors
    Label[] errors = {lblItemNameError, lblItemCategoryError, lblItemPriceError,
        lblAuctionItemError, lblAuctionPriceError, lblAuctionStepError, lblAuctionTimeError};
    for (Label err : errors) {
      err.managedProperty().bind(err.visibleProperty());
    }

    cmbItemCategory.getItems()
        .addAll("Điện tử", "Thời trang", "Nhà cửa & Sân vườn", "Đồ sưu tầm", "Thể thao", "Khác");

    startClock();
    loadOverview();
  }

  private void startClock() {
    clockTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e ->
        lblDateTime.setText(
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy  HH:mm:ss")))
    ));
    clockTimeline.setCycleCount(Timeline.INDEFINITE);
    clockTimeline.play();
  }

  // ── Menu Navigation ──────────────────────────────────────────────────────
  @FXML
  private void handleMenuOverview(ActionEvent e) {
    showPanel(panelOverview, "📊  Tổng quan", btnMenuOverview);
    loadOverview();
  }

  @FXML
  private void handleMenuItems(ActionEvent e) {
    showPanel(panelItems, "📦  Sản phẩm của tôi", btnMenuItems);
    loadMyItems();
  }

  @FXML
  private void handleMenuAddItem(ActionEvent e) {
    showPanel(panelAddItem, "➕  Đăng sản phẩm", btnMenuAddItem);
  }

  @FXML
  private void handleMenuAuctions(ActionEvent e) {
    showPanel(panelAuctions, "🔨  Phiên đấu giá", btnMenuAuctions);
    loadMyAuctions();
  }

  @FXML
  private void handleMenuCreateAuction(ActionEvent e) {
    showPanel(panelCreateAuction, "⚡  Tạo phiên đấu giá", btnMenuCreateAuction);
    loadItemsForAuction();
  }

  private void showPanel(VBox target, String title, Button activeBtn) {
    lblPageTitle.setText(title.replaceAll("[^\\p{L}\\p{Z}]+", "").trim());
    VBox[] panels = {panelOverview, panelItems, panelAddItem, panelAuctions, panelCreateAuction};
    for (VBox p : panels) {
      p.setVisible(false);
      p.setManaged(false);
    }
    target.setVisible(true);
    target.setManaged(true);

    String normal = "-fx-background-color: transparent; -fx-text-fill: #555555; -fx-alignment: CENTER_LEFT; -fx-padding: 12 20 12 20; -fx-cursor: hand;";
    String active = "-fx-background-color: linear-gradient(to right, #4285F4, #9B72CB); -fx-text-fill: white; -fx-alignment: CENTER_LEFT; -fx-padding: 12 20 12 20; -fx-cursor: hand; -fx-font-weight: bold;";
    Button[] btns = {btnMenuOverview, btnMenuItems, btnMenuAddItem, btnMenuAuctions,
        btnMenuCreateAuction};
    for (Button b : btns) {
      b.setStyle(b == activeBtn ? active : normal);
    }
  }

  // ── Data Loading Logic ───────────────────────────────────────────────────
  private void loadOverview() {
    int sellerId = SessionManager.getUserId();

    SellerCommand.getMyItems(sellerId, resItems -> {
      int total = 0, sold = 0;
      if (resItems != null && "SUCCESS".equals(resItems.get("status").getAsString())) {
        JsonArray items = resItems.getAsJsonArray("items");
        total = items.size();
        for (int i = 0; i < items.size(); i++) {
          if ("SOLD".equals(items.get(i).getAsJsonObject().get("status").getAsString())) {
            sold++;
          }
        }
      }
      lblTotalItems.setText(String.valueOf(total));
      lblTotalSold.setText(String.valueOf(sold));
      lblTotalRevenue.setText("---");
    });

    SellerCommand.getMyAuctions(sellerId, resAuctions -> {
      int active = 0;
      vboxRecentAuctions.getChildren().clear();
      if (resAuctions != null && "SUCCESS".equals(resAuctions.get("status").getAsString())) {
        JsonArray auctions = resAuctions.getAsJsonArray("auctions");
        for (int i = 0; i < auctions.size(); i++) {
          JsonObject auc = auctions.get(i).getAsJsonObject();
          if ("ACTIVE".equals(auc.get("status").getAsString())) {
            active++;
          }
          if (i < 5) {
            vboxRecentAuctions.getChildren()
                .add(CardFactory.buildSellerAuctionRow(auc, this::handleEndAuction));
          }
        }
        if (auctions.size() == 0) {
          vboxRecentAuctions.getChildren()
              .add(CardFactory.buildEmptyLabel("Chưa có phiên đấu giá nào"));
        }
      }
      lblActiveAuctions.setText(String.valueOf(active));
    });
  }

  private void loadMyItems() {
    SellerCommand.getMyItems(SessionManager.getUserId(), response -> {
      flowMyItems.getChildren().clear();
      if (response == null || !"SUCCESS".equals(response.get("status").getAsString())
          || response.getAsJsonArray("items").size() == 0) {
        flowMyItems.getChildren().add(CardFactory.buildEmptyLabel("Chưa có sản phẩm nào"));
        return;
      }
      JsonArray items = response.getAsJsonArray("items");
      for (int i = 0; i < items.size(); i++) {
        flowMyItems.getChildren().add(
            CardFactory.buildSellerItemCard(items.get(i).getAsJsonObject(),
                this::handleDeleteItem));
      }
    });
  }

  private void loadItemsForAuction() {
    SellerCommand.getMyItems(SessionManager.getUserId(), response -> {
      cmbAuctionItem.getItems().clear();
      itemNameToId.clear();
      if (response == null || !"SUCCESS".equals(response.get("status").getAsString())) {
        return;
      }
      JsonArray items = response.getAsJsonArray("items");
      // Khai báo map trong phạm vi callback (trước vòng lặp)
      Map<String, Long> itemNameToPrice = new HashMap<>();
      for (int i = 0; i < items.size(); i++) {
        JsonObject item = items.get(i).getAsJsonObject();
        if ("AVAILABLE".equals(item.get("status").getAsString())) {
          String name = item.get("name").getAsString();
          cmbAuctionItem.getItems().add(name);
          itemNameToId.put(name, item.get("itemId").getAsInt());itemNameToPrice.put(name, (long) item.get("startPrice").getAsDouble());
          itemNameToPrice.put(name, (long) item.get("startPrice").getAsDouble());
        }
      }
      // ✅ Gán listener SAU vòng lặp
      cmbAuctionItem.setOnAction(e -> {
        String selected = cmbAuctionItem.getValue();
        if (selected != null && itemNameToPrice.containsKey(selected)) {
          txtAuctionStartPrice.setText(String.valueOf(itemNameToPrice.get(selected)));
        }
      });
    });
  }

  private void loadMyAuctions() {
    SellerCommand.getMyAuctions(SessionManager.getUserId(), response -> {
      vboxMyAuctions.getChildren().clear();
      if (response == null || !"SUCCESS".equals(response.get("status").getAsString())
          || response.getAsJsonArray("auctions").size() == 0) {
        vboxMyAuctions.getChildren().add(CardFactory.buildEmptyLabel("Chưa có phiên đấu giá nào"));
        return;
      }
      JsonArray auctions = response.getAsJsonArray("auctions");
      for (int i = 0; i < auctions.size(); i++) {
        vboxMyAuctions.getChildren().add(
            CardFactory.buildSellerAuctionRow(auctions.get(i).getAsJsonObject(),
                this::handleEndAuction));
      }
    });
  }

  // ── Actions ──────────────────────────────────────────────────────────────
  @FXML
  private void handlePickItemImage(javafx.scene.input.MouseEvent event) {
    pickImage();
  }

  @FXML
  private void handlePickItemImage(ActionEvent event) {
    pickImage();
  }

  private void pickImage() {
    FileChooser fc = new FileChooser();
    fc.setTitle("Chọn ảnh sản phẩm");
    fc.getExtensionFilters()
        .add(new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg"));
    File file = fc.showOpenDialog(txtItemName.getScene().getWindow());
    if (file == null) {
      return;
    }
    if (file.length() > 2 * 1024 * 1024) {
      showAlert("Ảnh không được vượt quá 2MB!");
      return;
    }

    try {
      byte[] bytes = Files.readAllBytes(file.toPath());
      selectedImageBase64 = Base64.getEncoder().encodeToString(bytes);
      selectedImageExt = file.getName().toLowerCase().endsWith(".png") ? "png" : "jpg";
      imgItemPreview.setImage(new Image(file.toURI().toString()));
      imgItemPreview.setVisible(true);
    } catch (Exception e) {
      showAlert("Không thể đọc file ảnh!");
    }
  }

  @FXML
  private void handleAddItem(ActionEvent event) {
    lblItemNameError.setVisible(false);
    lblItemCategoryError.setVisible(false);
    lblItemPriceError.setVisible(false);
    lblAddItemStatus.setText("");
    boolean hasError = false;

    String name = txtItemName.getText().trim(), desc = txtItemDescription.getText()
        .trim(), cat = cmbItemCategory.getValue();
    if (name.isEmpty()) {
      lblItemNameError.setText("Vui lòng nhập tên!");
      lblItemNameError.setVisible(true);
      hasError = true;
    }
    if (cat == null) {
      lblItemCategoryError.setText("Vui lòng chọn danh mục!");
      lblItemCategoryError.setVisible(true);
      hasError = true;
    }

    double price = 0;
    try {
      price = Double.parseDouble(txtItemStartPrice.getText().trim().replaceAll("[^0-9]", ""));
      if (price <= 0) {
        throw new Exception();
      }
    } catch (Exception e) {
      lblItemPriceError.setText("Giá không hợp lệ!");
      lblItemPriceError.setVisible(true);
      hasError = true;
    }

    if (hasError) {
      return;
    }

    JsonObject data = new JsonObject();
    data.addProperty("sellerId", SessionManager.getUserId());
    data.addProperty("name", name);
    data.addProperty("description", desc);
    data.addProperty("category", cat);
    data.addProperty("startPrice", price);
    data.addProperty("imageBase64", selectedImageBase64);
    data.addProperty("extension", selectedImageExt);

    SellerCommand.createItem(data, response -> {
      if (response != null && "SUCCESS".equals(response.get("status").getAsString())) {
        showStatus(lblAddItemStatus, "✓ Đăng sản phẩm thành công!", true);
        txtItemName.clear();
        txtItemDescription.clear();
        cmbItemCategory.setValue(null);
        txtItemStartPrice.clear();
        imgItemPreview.setVisible(false);
        selectedImageBase64 = "";
      } else {
        showStatus(lblAddItemStatus, "Thất bại!", false);
      }
    });
  }

  @FXML
  private void handleCreateAuction(ActionEvent event) {
    lblAuctionItemError.setVisible(false);
    lblAuctionPriceError.setVisible(false);
    lblAuctionStepError.setVisible(false);
    lblAuctionTimeError.setVisible(false);
    lblCreateAuctionStatus.setText("");
    boolean hasError = false;

    String itemName = cmbAuctionItem.getValue();
    if (itemName == null) {
      lblAuctionItemError.setText("Chọn sản phẩm!");
      lblAuctionItemError.setVisible(true);
      hasError = true;
    }

    double startPrice = 0, minStep = 0;
    try {
      startPrice = Double.parseDouble(
          txtAuctionStartPrice.getText().trim().replaceAll("[^0-9]", ""));
      if (startPrice <= 0) {
        throw new Exception();
      }
    } catch (Exception e) {
      lblAuctionPriceError.setVisible(true);
      hasError = true;
    }
    try {
      minStep = Double.parseDouble(txtAuctionMinStep.getText().trim().replaceAll("[^0-9]", ""));
      if (minStep <= 0) {
        throw new Exception();
      }
    } catch (Exception e) {
      lblAuctionStepError.setVisible(true);
      hasError = true;
    }

    String endTimeStr = "";
    try {
      if (dateAuctionEnd.getValue() == null || txtAuctionEndTime.getText().trim().isEmpty()) {
        throw new Exception("Chưa chọn ngày/giờ");
      }
      endTimeStr = dateAuctionEnd.getValue().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + " "
          + txtAuctionEndTime.getText().trim() + ":00";
      if (LocalDateTime.parse(endTimeStr, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
          .isBefore(LocalDateTime.now())) {
        throw new Exception();
      }
    } catch (Exception e) {
      lblAuctionTimeError.setVisible(true);
      hasError = true;
    }

    if (hasError) {
      return;
    }

    JsonObject data = new JsonObject();
    data.addProperty("itemId", itemNameToId.get(itemName));
    data.addProperty("sellerId", SessionManager.getUserId());
    data.addProperty("startPrice", startPrice);
    data.addProperty("minStep", minStep);
    data.addProperty("endTime", endTimeStr);

    SellerCommand.createAuction(data, response -> {
      if (response != null && "SUCCESS".equals(response.get("status").getAsString())) {
        showStatus(lblCreateAuctionStatus, "✓ Tạo phiên đấu giá thành công!", true);
        cmbAuctionItem.setValue(null);
        txtAuctionStartPrice.clear();
        txtAuctionMinStep.clear();
        dateAuctionEnd.setValue(null);
        txtAuctionEndTime.clear();
        loadItemsForAuction();
      } else {
        showStatus(lblCreateAuctionStatus, "Thất bại!", false);
      }
    });
  }

  private void handleDeleteItem(int itemId) {
    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Hành động này không thể hoàn tác.",
        ButtonType.OK, ButtonType.CANCEL);
    confirm.setTitle("Xác nhận xóa");
    confirm.setHeaderText("Bạn chắc chắn muốn xóa sản phẩm này?");
    confirm.showAndWait().ifPresent(result -> {
      if (result == ButtonType.OK) {
        SellerCommand.deleteItem(itemId, res -> {
          if (res != null && "SUCCESS".equals(res.get("status").getAsString())) {
            loadMyItems();
          } else {
            String msg = (res != null && res.has("message"))
                    ? res.get("message").getAsString() : "Xóa thất bại!";
            showAlert(msg);
          }
        });
      }
    });
  }

  private void handleEndAuction(int auctionId) {
    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Dừng phiên sớm? Sản phẩm sẽ về trạng thái sẵn bán.",
            ButtonType.OK, ButtonType.CANCEL);
    confirm.setTitle("Dừng phiên đấu giá");
    confirm.showAndWait().ifPresent(result -> {
      if (result == ButtonType.OK) {
        SellerCommand.cancelAuction(auctionId, res -> {
          if (res != null && "SUCCESS".equals(res.get("status").getAsString())) {
            loadMyAuctions();
            loadOverview();
          } else {
            showAlert("Dừng phiên thất bại!");
          }
        });
      }
    });
  }

  // ── Routing ──────────────────────────────────────────────────────────────
  @FXML
  private void handleGoHome(ActionEvent event) {
    clockTimeline.stop();
    ViewManager.navigateTo(ViewManager.Views.HOME);
  }

  @FXML
  private void handleLogout(ActionEvent event) {
    clockTimeline.stop();
    SessionManager.logout();
    ViewManager.navigateTo(ViewManager.Views.LOGIN);
  }

  // ── Utils ────────────────────────────────────────────────────────────────
  private void showStatus(Label lbl, String msg, boolean success) {
    lbl.setText(msg);
    lbl.setTextFill(success ? javafx.scene.paint.Color.web("#4CAF50")
        : javafx.scene.paint.Color.web("#D96570"));
  }

  private void showAlert(String msg) {
    new Alert(Alert.AlertType.INFORMATION, msg).showAndWait();
  }
}