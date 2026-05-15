package com.nhom15.client.controller;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.nhom15.client.command.ServerCommand;
import com.nhom15.client.network.SocketClient;
import com.nhom15.client.util.SessionManager;
import com.nhom15.client.util.ViewManager;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.text.NumberFormat;
import java.util.Locale;

public class AdminDashboardController {

  // ── Sidebar buttons ───────────────────────────────────────────────────────
  @FXML private Button btnMenuDashboard;
  @FXML private Button btnMenuUsers;
  @FXML private Button btnMenuAuctions;

  // ── Header ────────────────────────────────────────────────────────────────
  @FXML private Label lblPageTitle;
  @FXML private Label lblAdminName;

  // ── Panels ────────────────────────────────────────────────────────────────
  @FXML private VBox panelDashboard;
  @FXML private VBox panelUsers;
  @FXML private VBox panelAuctions;

  // ── Dashboard stats ───────────────────────────────────────────────────────
  @FXML private Label lblTotalUsers;
  @FXML private Label lblActiveAuctions;
  @FXML private Label lblTotalAuctions;
  @FXML private Label lblTotalItems;

  // ── User table ────────────────────────────────────────────────────────────
  @FXML private TableView<JsonObject> tableUsers;
  @FXML private TableColumn<JsonObject, String> colUserId;
  @FXML private TableColumn<JsonObject, String> colUsername;
  @FXML private TableColumn<JsonObject, String> colEmail;
  @FXML private TableColumn<JsonObject, String> colRole;
  @FXML private TableColumn<JsonObject, String> colBalance;
  @FXML private TableColumn<JsonObject, String> colAction;

  // ── Auction table ─────────────────────────────────────────────────────────
  @FXML private TableView<JsonObject> tableAuctions;
  @FXML private TableColumn<JsonObject, String> colAuctionId;
  @FXML private TableColumn<JsonObject, String> colItemName;
  @FXML private TableColumn<JsonObject, String> colSellerName;
  @FXML private TableColumn<JsonObject, String> colCurrentPrice;
  @FXML private TableColumn<JsonObject, String> colStatus;
  @FXML private TableColumn<JsonObject, String> colEndTime;
  @FXML private TableColumn<JsonObject, String> colAuctionAction;

  private final NumberFormat currencyFmt = NumberFormat.getNumberInstance(new Locale("vi", "VN"));

  // ─────────────────────────────────────────────────────────────────────────
  @FXML
  public void initialize() {
    lblAdminName.setText(SessionManager.getCurrentUser() != null
        ? SessionManager.getCurrentUser().getUsername() : "Admin");

    setupUserTable();
    setupAuctionTable();
    loadStats();
  }

  // ── Setup tables ──────────────────────────────────────────────────────────

  private void setupUserTable() {
    colUserId.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().get("user_id").getAsInt())));
    colUsername.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().get("username").getAsString()));
    colEmail.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().get("email").getAsString()));
    colRole.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().get("role").getAsString()));
    colBalance.setCellValueFactory(d -> new SimpleStringProperty(
        currencyFmt.format(d.getValue().get("balance").getAsDouble()) + "đ"));

    // Cột hành động: nút Ban/Unban
    colAction.setCellFactory(col -> new TableCell<>() {
      private final Button btn = new Button();
      {
        btn.setStyle("-fx-background-radius: 15; -fx-cursor: hand; -fx-font-size: 11px; -fx-padding: 4 12 4 12;");
        btn.setOnAction(e -> {
          JsonObject user = getTableView().getItems().get(getIndex());
          handleBanUser(user);
        });
      }

      @Override
      protected void updateItem(String item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || getTableView().getItems().get(getIndex()) == null) {
          setGraphic(null);
        } else {
          JsonObject user = getTableView().getItems().get(getIndex());
          String role = user.get("role").getAsString();
          if ("BANNED".equals(role)) {
            btn.setText("✅ Mở khóa");
            btn.setStyle(btn.getStyle() + "-fx-background-color: #27AE60; -fx-text-fill: white;");
          } else if ("ADMIN".equals(role)) {
            btn.setText("—");
            btn.setDisable(true);
          } else {
            btn.setText("🚫 Khóa");
            btn.setStyle(btn.getStyle() + "-fx-background-color: #D96570; -fx-text-fill: white;");
            btn.setDisable(false);
          }
          setGraphic(btn);
        }
      }
    });
  }

  private void setupAuctionTable() {
    colAuctionId.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().get("auction_id").getAsInt())));
    colItemName.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().get("item_name").getAsString()));
    colSellerName.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().get("seller_name").getAsString()));
    colCurrentPrice.setCellValueFactory(d -> new SimpleStringProperty(
        currencyFmt.format(d.getValue().get("current_price").getAsDouble()) + "đ"));
    colStatus.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().get("status").getAsString()));
    colEndTime.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().get("end_time").getAsString()));

    // Cột hành động: nút Hủy
    colAuctionAction.setCellFactory(col -> new TableCell<>() {
      private final Button btn = new Button("❌ Hủy");
      {
        btn.setStyle("-fx-background-color: #D96570; -fx-text-fill: white; -fx-background-radius: 15; -fx-cursor: hand; -fx-font-size: 11px; -fx-padding: 4 12 4 12;");
        btn.setOnAction(e -> {
          JsonObject auction = getTableView().getItems().get(getIndex());
          handleCancelAuction(auction);
        });
      }

      @Override
      protected void updateItem(String item, boolean empty) {
        super.updateItem(item, empty);
        if (empty) {
          setGraphic(null);
        } else {
          JsonObject auction = getTableView().getItems().get(getIndex());
          String status = auction.get("status").getAsString();
          btn.setDisable(!"ACTIVE".equals(status));
          setGraphic(btn);
        }
      }
    });
  }

  // ── Load data ─────────────────────────────────────────────────────────────

  private void loadStats() {
    new Thread(() -> {
      JsonObject req = new JsonObject();
      req.addProperty("action", "ADMIN_GET_STATS");
      JsonObject res = SocketClient.sendRequest(req);
      Platform.runLater(() -> {
        if (res != null && ServerCommand.isSuccess(res)) {
          lblTotalUsers.setText(String.valueOf(res.get("total_users").getAsInt()));
          lblActiveAuctions.setText(String.valueOf(res.get("active_auctions").getAsInt()));
          lblTotalAuctions.setText(String.valueOf(res.get("total_auctions").getAsInt()));
          lblTotalItems.setText(String.valueOf(res.get("total_items").getAsInt()));
        }
      });
    }).start();
  }

  private void loadUsers() {
    new Thread(() -> {
      JsonObject req = new JsonObject();
      req.addProperty("action", "ADMIN_GET_ALL_USERS");
      JsonObject res = SocketClient.sendRequest(req);
      Platform.runLater(() -> {
        if (res != null && ServerCommand.isSuccess(res)) {
          JsonArray arr = res.getAsJsonArray("users");
          ObservableList<JsonObject> list = FXCollections.observableArrayList();
          arr.forEach(e -> list.add(e.getAsJsonObject()));
          tableUsers.setItems(list);
        }
      });
    }).start();
  }

  private void loadAuctions() {
    new Thread(() -> {
      JsonObject req = new JsonObject();
      req.addProperty("action", "ADMIN_GET_ALL_AUCTIONS");
      JsonObject res = SocketClient.sendRequest(req);
      Platform.runLater(() -> {
        if (res != null && ServerCommand.isSuccess(res)) {
          JsonArray arr = res.getAsJsonArray("auctions");
          ObservableList<JsonObject> list = FXCollections.observableArrayList();
          arr.forEach(e -> list.add(e.getAsJsonObject()));
          tableAuctions.setItems(list);
        }
      });
    }).start();
  }

  // ── Actions ───────────────────────────────────────────────────────────────

  private void handleBanUser(JsonObject user) {
    String role = user.get("role").getAsString();
    String actionType = "BANNED".equals(role) ? "UNBAN" : "BAN";
    String username = user.get("username").getAsString();

    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
    confirm.setTitle("Xác nhận");
    confirm.setHeaderText(null);
    confirm.setContentText(("BAN".equals(actionType) ? "Khóa" : "Mở khóa") + " tài khoản " + username + "?");
    confirm.showAndWait().ifPresent(btn -> {
      if (btn == ButtonType.OK) {
        new Thread(() -> {
          JsonObject req = new JsonObject();
          req.addProperty("action", "ADMIN_BAN_USER");
          req.addProperty("user_id", user.get("user_id").getAsInt());
          req.addProperty("action_type", actionType);
          JsonObject res = SocketClient.sendRequest(req);
          Platform.runLater(() -> {
            if (res != null && ServerCommand.isSuccess(res)) {
              loadUsers(); // Reload bảng
            } else {
              showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể thực hiện thao tác!");
            }
          });
        }).start();
      }
    });
  }

  private void handleCancelAuction(JsonObject auction) {
    String itemName = auction.get("item_name").getAsString();
    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
    confirm.setTitle("Xác nhận hủy");
    confirm.setHeaderText(null);
    confirm.setContentText("Hủy phiên đấu giá \"" + itemName + "\"?");
    confirm.showAndWait().ifPresent(btn -> {
      if (btn == ButtonType.OK) {
        new Thread(() -> {
          JsonObject req = new JsonObject();
          req.addProperty("action", "ADMIN_CANCEL_AUCTION");
          req.addProperty("auction_id", auction.get("auction_id").getAsInt());
          JsonObject res = SocketClient.sendRequest(req);
          Platform.runLater(() -> {
            if (res != null && ServerCommand.isSuccess(res)) {
              loadAuctions();
            } else {
              showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể hủy phiên đấu giá!");
            }
          });
        }).start();
      }
    });
  }

  // ── Menu navigation ───────────────────────────────────────────────────────

  @FXML
  private void handleMenuDashboard() {
    showPanel(panelDashboard);
    setActiveMenu(btnMenuDashboard);
    lblPageTitle.setText("Tổng quan");
    loadStats();
  }

  @FXML
  private void handleMenuUsers() {
    showPanel(panelUsers);
    setActiveMenu(btnMenuUsers);
    lblPageTitle.setText("Quản lý User");
    loadUsers();
  }

  @FXML
  private void handleMenuAuctions() {
    showPanel(panelAuctions);
    setActiveMenu(btnMenuAuctions);
    lblPageTitle.setText("Quản lý Đấu giá");
    loadAuctions();
  }

  @FXML
  private void handleRefresh() {
    if (panelDashboard.isVisible()) loadStats();
    else if (panelUsers.isVisible()) loadUsers();
    else if (panelAuctions.isVisible()) loadAuctions();
  }

  @FXML
  private void handleLogout() {
    SessionManager.logout();
    ViewManager.navigateTo(ViewManager.Views.LOGIN);
  }

  // ── Helpers ───────────────────────────────────────────────────────────────

  private void showPanel(VBox target) {
    panelDashboard.setVisible(false); panelDashboard.setManaged(false);
    panelUsers.setVisible(false);     panelUsers.setManaged(false);
    panelAuctions.setVisible(false);  panelAuctions.setManaged(false);
    target.setVisible(true); target.setManaged(true);
  }

  private void setActiveMenu(Button active) {
    String normal = "-fx-background-color: transparent; -fx-text-fill: #555555; -fx-alignment: CENTER_LEFT; -fx-padding: 12 20 12 20; -fx-cursor: hand;";
    String activeStyle = "-fx-background-color: linear-gradient(to right, #D96570, #9B72CB); -fx-text-fill: white; -fx-alignment: CENTER_LEFT; -fx-padding: 12 20 12 20; -fx-cursor: hand; -fx-font-weight: bold;";
    btnMenuDashboard.setStyle(normal);
    btnMenuUsers.setStyle(normal);
    btnMenuAuctions.setStyle(normal);
    active.setStyle(activeStyle);
  }

  private void showAlert(Alert.AlertType type, String title, String msg) {
    Alert a = new Alert(type);
    a.setTitle(title);
    a.setHeaderText(null);
    a.setContentText(msg);
    a.showAndWait();
  }
}
