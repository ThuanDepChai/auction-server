package com.nhom15.client.util;

import com.google.gson.JsonObject;
import com.nhom15.client.command.GetItemImageCommand;
import com.nhom15.client.command.ServerCommand;
import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;
import java.util.function.Consumer;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

public class CardFactory {

  private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern(
          "yyyy-MM-dd HH:mm:ss");

  public static VBox buildProductCard(JsonObject item, Consumer<Integer> onClick) {
    String name = getStr(item, "name", "Sản phẩm");
    String price = getStr(item, "price", "0");
    String sold = getStr(item, "sold", "0");
    int productId = item.has("productId") ? item.get("productId").getAsInt() : -1;
    String imagePath = getStr(item, "imagePath", "");

    VBox card = createBaseCard(imagePath, 160);
    VBox info = new VBox(5);
    info.setStyle("-fx-padding: 10 12 12 12;");

    Label lblName = createLabel(name, "-fx-font-size: 13px; -fx-text-fill: #333333;");
    Label lblPrice = createLabel(formatPrice(price) + "đ",
            "-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #D96570;");
    Label lblSold = createLabel("Đã bán " + sold, "-fx-font-size: 11px; -fx-text-fill: #888888;");

    info.getChildren().addAll(lblName, lblPrice, lblSold);
    card.getChildren().add(info);
    card.setOnMouseClicked(e -> onClick.accept(productId));
    return card;
  }

  public static VBox buildAuctionCard(JsonObject auction, List<Timeline> activeTimers,
                                      Consumer<Integer> onBidClick) {
    String name = getStr(auction, "name", "Sản phẩm");
    String curPrice = getStr(auction, "currentPrice", "0");
    int auctionId = auction.has("auctionId") ? auction.get("auctionId").getAsInt() : -1;
    String imagePath = getStr(auction, "imagePath", "");

    VBox card = createBaseCard(imagePath, 150);
    VBox info = new VBox(5);
    info.setStyle("-fx-padding: 10 12 12 12;");

    Label lblName = createLabel(name, "-fx-font-size: 13px; -fx-text-fill: #333333;");
    Label lblPrice = createLabel("Giá hiện tại: " + formatPrice(curPrice) + "đ",
            "-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #4285F4;");

    Label lblCountdown = createLabel("⏰ --:--:--",
            "-fx-font-size: 12px; -fx-text-fill: #D96570; -fx-font-weight: bold;");
    setupCountdown(lblCountdown, getStr(auction, "endTime", ""), activeTimers);

    Button btnBid = new Button("Đấu giá ngay");
    btnBid.setMaxWidth(Double.MAX_VALUE);
    btnBid.setStyle(
            "-fx-background-color: linear-gradient(to right, #4285F4, #9B72CB); -fx-background-radius: 8; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 7 0 7 0;");
    btnBid.setOnAction(e -> onBidClick.accept(auctionId));

    info.getChildren().addAll(lblName, lblPrice, lblCountdown, btnBid);
    card.getChildren().add(info);
    return card;
  }

  public static Label buildEmptyLabel(String text) {
    Label lbl = new Label(text);
    lbl.setStyle("-fx-text-fill: #AAAAAA; -fx-font-size: 14px; -fx-padding: 20;");
    return lbl;
  }

  // --- Các hàm hỗ trợ nội bộ ---

  /**
   * Tạo card với placeholder ảnh. Nếu imagePath không rỗng, tự động gọi GET_ITEM_IMAGE
   * bất đồng bộ để load ảnh sau khi card đã hiển thị — tránh block UI.
   */
  private static VBox createBaseCard(String imagePath, double imgHeight) {
    VBox card = new VBox();
    card.setPrefWidth(210);
    card.setStyle(
            "-fx-background-color: white; -fx-background-radius: 10; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 10, 0, 0, 4); -fx-cursor: hand;");
    card.setOnMouseEntered(e -> card.setStyle(
            card.getStyle().replace("0.08", "0.18").replace("10, 0, 0, 4", "15, 0, 0, 6")));
    card.setOnMouseExited(e -> card.setStyle(
            card.getStyle().replace("0.18", "0.08").replace("15, 0, 0, 6", "10, 0, 0, 4")));

    StackPane imgContainer = new StackPane();
    imgContainer.setPrefHeight(imgHeight);
    imgContainer.setStyle("-fx-background-color: #F4F7FC; -fx-background-radius: 10 10 0 0;");

    Label placeholder = new Label("🖼");
    placeholder.setStyle("-fx-font-size: 36px; -fx-text-fill: #CCCCCC;");
    imgContainer.getChildren().add(placeholder);
    card.getChildren().add(imgContainer);

    // Lazy-load ảnh bất đồng bộ — card hiển thị ngay, ảnh điền vào sau
    if (imagePath != null && !imagePath.isEmpty()) {
      new GetItemImageCommand(imagePath).executeAsync(res -> {
        if (ServerCommand.isSuccess(res) && res.has("imageBase64")) {
          applyBase64ToContainer(imgContainer, res.get("imageBase64").getAsString(),
                  imgHeight, placeholder);
        }
      });
    }

    return card;
  }

  /**
   * Điền ảnh từ Base64 vào StackPane — chạy trên FX thread (đã được Platform.runLater bởi
   * executeAsync).
   */
  private static void applyBase64ToContainer(StackPane imgContainer, String base64,
                                             double imgHeight, Label placeholder) {
    try {
      if (base64 == null || base64.isEmpty()) {
        return;
      }
      Image img = new Image(new ByteArrayInputStream(Base64.getDecoder().decode(base64)));
      if (img.isError()) {
        return;
      }
      ImageView iv = new ImageView(img);
      iv.setFitWidth(210);
      iv.setFitHeight(imgHeight);
      iv.setPreserveRatio(true);
      imgContainer.getChildren().remove(placeholder);
      imgContainer.getChildren().add(iv);
    } catch (Exception ignored) {
      // Giữ placeholder nếu decode lỗi
    }
  }

  private static Label createLabel(String text, String style) {
    Label lbl = new Label(text);
    lbl.setWrapText(true);
    lbl.setMaxWidth(186);
    lbl.setStyle(style);
    return lbl;
  }

  private static void setupCountdown(Label label, String endTimeStr, List<Timeline> activeTimers) {
    try {
      LocalDateTime endTime = LocalDateTime.parse(endTimeStr, DT_FMT);
      Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(endTime)) {
          label.setText("⏰ Đã kết thúc");
          label.setStyle("-fx-font-size: 12px; -fx-text-fill: #888888;");
          return;
        }
        long hours = ChronoUnit.HOURS.between(now, endTime);
        long minutes = ChronoUnit.MINUTES.between(now, endTime) % 60;
        long seconds = ChronoUnit.SECONDS.between(now, endTime) % 60;
        label.setText(String.format("⏰ %02d:%02d:%02d", hours, minutes, seconds));
        label.setStyle(
                hours < 1 ? "-fx-font-size: 12px; -fx-text-fill: #D96570; -fx-font-weight: bold;"
                        : "-fx-font-size: 12px; -fx-text-fill: #E8A838; -fx-font-weight: bold;");
      }));
      timeline.setCycleCount(Timeline.INDEFINITE);
      timeline.play();
      activeTimers.add(timeline);
    } catch (Exception e) {
      label.setText("⏰ ---");
    }
  }

  private static String getStr(JsonObject obj, String key, String def) {
    return (obj != null && obj.has(key) && !obj.get(key).isJsonNull()) ? obj.get(key).getAsString()
            : def;
  }

  private static String formatPrice(String raw) {
    try {
      return String.format("%,d", Long.parseLong(raw.replaceAll("[^0-9]", "")));
    } catch (Exception e) {
      return raw;
    }
  }

  public static void setAvatar(String base64, ImageView imgAvatar, Label lblAvatarInitial) {
    try {
      if (base64 == null || base64.isEmpty()) {
        return;
      }

      byte[] bytes = Base64.getDecoder().decode(base64);
      Image image = new Image(new ByteArrayInputStream(bytes));

      if (imgAvatar != null && !image.isError()) {
        imgAvatar.setImage(image);
        imgAvatar.setVisible(true);
        if (lblAvatarInitial != null) {
          lblAvatarInitial.setVisible(false);
        }
      }
    } catch (Exception e) {
      System.err.println("[CardFactory] Lỗi hiển thị avatar: " + e.getMessage());
    }
  }

  public static VBox buildSellerItemCard(JsonObject item, Consumer<Integer> onDelete) {
    String name = item.get("name").getAsString();
    String status = item.get("status").getAsString();
    String price = String.format("%,d", (long) item.get("startPrice").getAsDouble());
    String category = item.get("category").getAsString();
    String imagePath = item.has("imagePath") && !item.get("imagePath").isJsonNull()
            ? item.get("imagePath").getAsString() : "";
    int itemId = item.get("itemId").getAsInt();

    VBox card = new VBox(8);
    card.setPrefWidth(200);
    card.setStyle(
            "-fx-background-color: white; -fx-background-radius: 10; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.07), 10, 0, 0, 4);");

    // ── Ảnh: placeholder trước, lazy-load sau ────────────────────────────
    StackPane imgPane = new StackPane();
    imgPane.setPrefHeight(140);
    imgPane.setStyle("-fx-background-color: #F4F7FC; -fx-background-radius: 10 10 0 0;");
    Label phLabel = new Label("🖼");
    phLabel.setStyle("-fx-font-size: 30px; -fx-text-fill: #CCCCCC;");
    imgPane.getChildren().add(phLabel);

    if (!imagePath.isEmpty()) {
      new GetItemImageCommand(imagePath).executeAsync(res -> {
        if (ServerCommand.isSuccess(res) && res.has("imageBase64")) {
          try {
            byte[] bytes = Base64.getDecoder().decode(res.get("imageBase64").getAsString());
            ImageView iv = new ImageView(new Image(new ByteArrayInputStream(bytes)));
            iv.setFitWidth(200);
            iv.setFitHeight(140);
            iv.setPreserveRatio(true);
            imgPane.getChildren().setAll(iv);
          } catch (Exception ignored) {
          }
        }
      });
    }

    VBox info = new VBox(4);
    info.setStyle("-fx-padding: 8 10 10 10;");
    Label lblName = new Label(name);
    lblName.setWrapText(true);
    lblName.setStyle("-fx-font-size: 12px; -fx-font-weight: bold;");
    Label lblPrice = new Label(price + "đ");
    lblPrice.setStyle("-fx-font-size: 13px; -fx-text-fill: #D96570; -fx-font-weight: bold;");
    Label lblCat = new Label(category);
    lblCat.setStyle("-fx-font-size: 11px; -fx-text-fill: #888888;");

    String statusColor = "AVAILABLE".equals(status) ? "#E8F5E9:#27AE60"
            : "IN_AUCTION".equals(status) ? "#EEF2FF:#4285F4" : "#FCE4EC:#C62828";
    String[] sc = statusColor.split(":");
    Label lblStatus = new Label("AVAILABLE".equals(status) ? "Sẵn bán"
            : "IN_AUCTION".equals(status) ? "Đang đấu giá" : "Đã bán");
    lblStatus.setStyle("-fx-background-color: " + sc[0] + "; -fx-text-fill: " + sc[1]
            + "; -fx-background-radius: 8; -fx-padding: 2 8 2 8; -fx-font-size: 10px;");

    HBox actions = new HBox(5);
    if ("AVAILABLE".equals(status)) {
      Button btnDelete = new Button("🗑 Xóa");
      btnDelete.setStyle(
              "-fx-background-color: #FCE4EC; -fx-text-fill: #C62828; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 4 10 4 10;");
      btnDelete.setOnAction(e -> onDelete.accept(itemId));
      actions.getChildren().add(btnDelete);
    }
    info.getChildren().addAll(lblName, lblPrice, lblCat, lblStatus, actions);
    card.getChildren().addAll(imgPane, info);
    return card;
  }

  public static HBox buildSellerAuctionRow(JsonObject auction, Consumer<Integer> onEnd) {
    String name = auction.get("name").getAsString();
    String curPrice = String.format("%,d", (long) auction.get("currentPrice").getAsDouble());
    String endTime = auction.get("endTime").getAsString();
    String status = auction.get("status").getAsString();
    int auctionId = auction.get("auctionId").getAsInt();

    HBox row = new HBox(15);
    row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
    row.setStyle(
            "-fx-background-color: #F9F9F9; -fx-background-radius: 8; -fx-padding: 12 15 12 15;");

    VBox info = new VBox(3);
    HBox.setHgrow(info, Priority.ALWAYS);
    Label lblName = new Label(name);
    lblName.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");
    Label lblPrice = new Label("Giá hiện tại: " + curPrice + "đ");
    lblPrice.setStyle("-fx-font-size: 12px; -fx-text-fill: #4285F4;");
    Label lblEnd = new Label("Kết thúc: " + endTime);
    lblEnd.setStyle("-fx-font-size: 11px; -fx-text-fill: #888888;");
    info.getChildren().addAll(lblName, lblPrice, lblEnd);

    String sc = "ACTIVE".equals(status) ? "#E8F5E9:#27AE60"
            : "ENDED".equals(status) ? "#F5F5F5:#888888" : "#FCE4EC:#C62828";
    String[] colors = sc.split(":");
    Label lblStatus = new Label("ACTIVE".equals(status) ? "Đang diễn ra"
            : "ENDED".equals(status) ? "Đã kết thúc" : "Đã hủy");
    lblStatus.setStyle("-fx-background-color: " + colors[0] + "; -fx-text-fill: " + colors[1]
            + "; -fx-background-radius: 10; -fx-padding: 4 12 4 12; -fx-font-size: 11px; -fx-font-weight: bold;");

    if ("ACTIVE".equals(status)) {
      Button btnEnd = new Button("Kết thúc");
      btnEnd.setStyle(
              "-fx-background-color: #FCE4EC; -fx-text-fill: #C62828; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 6 12 6 12;");
      btnEnd.setOnAction(e -> onEnd.accept(auctionId));
      row.getChildren().addAll(info, lblStatus, btnEnd);
    } else {
      row.getChildren().addAll(info, lblStatus);
    }
    return row;
  }
}
