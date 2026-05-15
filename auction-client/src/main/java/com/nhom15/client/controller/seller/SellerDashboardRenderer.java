package com.nhom15.client.controller.seller;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.nhom15.client.util.CardFactory;
import java.util.function.Consumer;
import javafx.collections.FXCollections;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

public class SellerDashboardRenderer {

  private static final String MENU_NORMAL =
      "-fx-background-color: transparent; -fx-text-fill: #555555; -fx-alignment: CENTER_LEFT; "
          + "-fx-padding: 12 20 12 20; -fx-cursor: hand;";
  private static final String MENU_ACTIVE =
      "-fx-background-color: linear-gradient(to right, #4285F4, #9B72CB); -fx-text-fill: white; "
          + "-fx-alignment: CENTER_LEFT; -fx-padding: 12 20 12 20; -fx-cursor: hand; "
          + "-fx-font-weight: bold;";

  public void showPanel(Label title, VBox target, String text, Button activeButton,
      VBox[] panels, Button[] buttons) {
    title.setText(text.replaceAll("[^\\p{L}\\p{Z}]+", "").trim());
    for (VBox panel : panels) {
      panel.setVisible(false);
      panel.setManaged(false);
    }
    target.setVisible(true);
    target.setManaged(true);
    for (Button button : buttons) {
      button.setStyle(button == activeButton ? MENU_ACTIVE : MENU_NORMAL);
    }
  }

  public void configureComboBoxes(ComboBox<String> revenueFilter, ComboBox<String> itemFilter,
      ComboBox<String> auctionFilter, Runnable onAuctionFilterChanged, Runnable onRevenueChanged) {
    if (revenueFilter != null) {
      revenueFilter.setItems(FXCollections.observableArrayList("7 ngay", "30 ngay", "Thang nay"));
      revenueFilter.getSelectionModel().selectFirst();
      revenueFilter.setOnAction(e -> onRevenueChanged.run());
    }
    if (itemFilter != null) {
      itemFilter.setItems(FXCollections.observableArrayList("Tat ca", "San ban", "Dang dau gia", "Da ban"));
      itemFilter.getSelectionModel().selectFirst();
    }
    if (auctionFilter != null) {
      auctionFilter.setItems(FXCollections.observableArrayList(
          "Tat ca", "Dang dien ra", "Sap dien ra", "Da ket thuc", "Ban nhap"));
      auctionFilter.getSelectionModel().selectFirst();
      auctionFilter.setOnAction(e -> onAuctionFilterChanged.run());
    }
  }

  public String auctionFilterFromLabel(String value) {
    return switch (value == null ? "" : value) {
      case "Dang dien ra" -> "ACTIVE";
      case "Sap dien ra" -> "UPCOMING";
      case "Da ket thuc" -> "ENDED";
      case "Ban nhap" -> "DRAFT";
      default -> "ALL";
    };
  }

  public void renderItems(FlowPane container, JsonArray items, Consumer<Integer> onDelete) {
    container.getChildren().clear();
    if (items == null || items.size() == 0) {
      container.getChildren().add(CardFactory.buildEmptyLabel("Chua co san pham nao"));
      return;
    }
    for (int i = 0; i < items.size(); i++) {
      container.getChildren().add(CardFactory.buildSellerItemCard(items.get(i).getAsJsonObject(), onDelete));
    }
  }

  public void renderAuctions(VBox container, JsonArray auctions, String filter, Consumer<Integer> onEnd) {
    container.getChildren().clear();
    int rendered = 0;
    if (auctions != null) {
      for (int i = 0; i < auctions.size(); i++) {
        JsonObject auction = auctions.get(i).getAsJsonObject();
        if (!SellerDashboardStats.matchesAuctionFilter(auction, filter)) {
          continue;
        }
        container.getChildren().add(CardFactory.buildSellerAuctionRow(auction, onEnd));
        rendered++;
      }
    }
    if (rendered == 0) {
      container.getChildren().add(CardFactory.buildEmptyLabel("Khong co phien dau gia phu hop"));
    }
  }

  public void renderRecentAuctions(VBox container, JsonArray auctions, Consumer<Integer> onEnd) {
    container.getChildren().clear();
    if (auctions == null || auctions.size() == 0) {
      container.getChildren().add(CardFactory.buildEmptyLabel("Chua co phien dau gia nao"));
      return;
    }
    for (int i = 0; i < auctions.size() && i < 5; i++) {
      container.getChildren().add(CardFactory.buildSellerAuctionRow(auctions.get(i).getAsJsonObject(), onEnd));
    }
  }

  public void renderOrders(VBox container, JsonArray auctions) {
    container.getChildren().clear();
    int count = 0;
    if (auctions != null) {
      for (int i = 0; i < auctions.size(); i++) {
        JsonObject auction = auctions.get(i).getAsJsonObject();
        if ("ENDED".equals(SellerDashboardStats.getString(auction, "status", ""))) {
          container.getChildren().add(buildInfoRow(
              SellerDashboardStats.getString(auction, "itemName",
                  "Phien dau gia #" + SellerDashboardStats.getString(auction, "auctionId", "")),
              "Cho thanh toan",
              SellerDashboardStats.formatMoney(SellerDashboardStats.getDouble(auction, "currentPrice", 0))));
          count++;
        }
      }
    }
    if (count == 0) {
      container.getChildren().add(CardFactory.buildEmptyLabel("Chua co don hang nao can xu ly"));
    }
  }

  public void renderFinance(Label walletBalance, VBox transactions, JsonArray auctions) {
    double revenue = SellerDashboardStats.revenue(auctions);
    walletBalance.setText(SellerDashboardStats.formatMoney(revenue));
    transactions.getChildren().clear();
    if (revenue <= 0) {
      transactions.getChildren().add(CardFactory.buildEmptyLabel("Chua co giao dich nao"));
      return;
    }
    transactions.getChildren().add(buildInfoRow("Tien ban hang", "Da ghi nhan",
        SellerDashboardStats.formatMoney(revenue)));
    transactions.getChildren().add(buildInfoRow("Phi san tam tinh", "Commission 5%",
        "-" + SellerDashboardStats.formatMoney(revenue * 0.05)));
  }

  public void renderRevenueChart(LineChart<String, Number> chart, JsonArray auctions) {
    if (chart == null) {
      return;
    }
    double revenue = SellerDashboardStats.revenue(auctions);
    chart.getData().clear();
    XYChart.Series<String, Number> series = new XYChart.Series<>();
    series.getData().add(new XYChart.Data<>("T2", revenue * 0.15));
    series.getData().add(new XYChart.Data<>("T3", revenue * 0.25));
    series.getData().add(new XYChart.Data<>("T4", revenue * 0.35));
    series.getData().add(new XYChart.Data<>("T5", revenue * 0.55));
    series.getData().add(new XYChart.Data<>("T6", revenue));
    series.getData().add(new XYChart.Data<>("T7", revenue * 0.65));
    series.getData().add(new XYChart.Data<>("CN", revenue * 0.8));
    chart.getData().add(series);
  }

  public void selectAuctionFilterButton(Button activeButton, Button... buttons) {
    String active = "-fx-background-color: #4285F4; -fx-text-fill: white; -fx-background-radius: 20; "
        + "-fx-padding: 10 20 10 20; -fx-cursor: hand; -fx-font-weight: bold;";
    String normal = "-fx-background-color: #F4F7FC; -fx-text-fill: #666666; -fx-background-radius: 20; "
        + "-fx-padding: 10 20 10 20; -fx-cursor: hand;";
    for (Button button : buttons) {
      button.setStyle(button == activeButton ? active : normal);
    }
  }

  private VBox buildInfoRow(String title, String subtitle, String value) {
    VBox row = new VBox(4);
    row.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-padding: 14 16 14 16; "
        + "-fx-border-color: #EEEEEE; -fx-border-radius: 10;");
    Label lblTitle = new Label(title);
    lblTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: #222222;");
    Label lblSubtitle = new Label(subtitle + " - " + value);
    lblSubtitle.setStyle("-fx-text-fill: #666666;");
    row.getChildren().addAll(lblTitle, lblSubtitle);
    return row;
  }
}
