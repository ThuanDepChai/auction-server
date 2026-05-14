package com.nhom15.client.controller;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

public class EbayHomeController {

  @FXML
  private TextField searchField;

  @FXML
  private ComboBox<String> categoryCombo;

  @FXML
  private HBox futureCategoriesContainer;

  @FXML
  private HBox dealsContainer;

  @FXML
  private HBox trendingContainer;

  @FXML
  public void initialize() {
    // Cài đặt danh mục cho thanh tìm kiếm
    categoryCombo.getItems().addAll("All Categories", "Antiques", "Art", "Baby", "Books", "Business & Industrial");
    categoryCombo.getSelectionModel().selectFirst();

    // 1. Khởi tạo dữ liệu giả cho khu vực "The future in your hands"
    String[] futureCats = {"Laptops", "Computer parts", "Smartphones", "Enterprise networking", "Tablets and eBooks", "Storage and media"};
    for (String cat : futureCats) {
      futureCategoriesContainer.getChildren().add(createCategoryCard(cat, 180));
    }

    // 2. Khởi tạo dữ liệu giả cho khu vực "Today's Deals"
    String[][] deals = {
        {"SONY WH-CH720N/B Headphones", "₫1,579,777", "₫4,739,857"},
        {"Dyson V8 Origin Extra Cordless", "₫4,739,857", "₫11,060,017"},
        {"eufy SoloCam S3 Solar Wireless", "₫2,633,137", "₫5,266,537"},
        {"Apple iPhone 16 Pro Max", "₫22,759,950", ""},
        {"Bose SoundLink Max Portable", "₫5,740,812", "₫8,600,546"}
    };
    for (String[] deal : deals) {
      dealsContainer.getChildren().add(createDealCard(deal[0], deal[1], deal[2]));
    }

    // 3. Khởi tạo dữ liệu giả cho khu vực "Trending on eBay"
    String[] trendingCats = {"Tech", "Motors", "Luxury", "Collectibles and art", "Home and garden", "Trading cards"};
    for (String cat : trendingCats) {
      trendingContainer.getChildren().add(createCategoryCard(cat, 160));
    }
  }

  /**
   * Hàm hỗ trợ tạo Card danh mục (Hình vuông xám + Tên danh mục bên dưới)
   */
  private VBox createCategoryCard(String title, double size) {
    VBox card = new VBox(10);
    card.setAlignment(Pos.TOP_LEFT);

    // Khung chứa ảnh giả lập (vùng màu xám)
    VBox imagePlaceholder = new VBox();
    imagePlaceholder.setPrefSize(size, size);
    imagePlaceholder.setMinSize(size, size);
    imagePlaceholder.setStyle("-fx-background-color: #f1f1f1; -fx-background-radius: 10;");
    // Ghi chú: Nếu có ảnh thật, bạn thêm ImageView vào trong imagePlaceholder này

    Label lblTitle = new Label(title);
    lblTitle.setFont(Font.font("System", 14));
    lblTitle.setTextFill(Color.web("#333333"));
    lblTitle.setWrapText(true);
    lblTitle.setPrefWidth(size);

    card.getChildren().addAll(imagePlaceholder, lblTitle);
    return card;
  }

  /**
   * Hàm hỗ trợ tạo Card sản phẩm Today's Deals (Có ảnh, giá, giá gốc gạch ngang)
   */
  private VBox createDealCard(String title, String currentPrice, String oldPrice) {
    VBox card = new VBox(10);
    card.setAlignment(Pos.TOP_LEFT);
    card.setPrefWidth(220);

    // Khung chứa ảnh (có thể chèn nút trái tim Yêu thích ở góc trên phải)
    VBox imagePlaceholder = new VBox();
    imagePlaceholder.setPrefSize(220, 220);
    imagePlaceholder.setMinSize(220, 220);
    imagePlaceholder.setStyle("-fx-background-color: #f1f1f1; -fx-background-radius: 10;");

    Label lblTitle = new Label(title);
    lblTitle.setFont(Font.font("System", 14));
    lblTitle.setWrapText(true);
    lblTitle.setMaxWidth(220);

    HBox priceBox = new HBox(5);
    priceBox.setAlignment(Pos.BOTTOM_LEFT);

    Label lblCurrentPrice = new Label(currentPrice);
    lblCurrentPrice.setFont(Font.font("System", FontWeight.BOLD, 16));

    priceBox.getChildren().add(lblCurrentPrice);

    // Xử lý giá cũ (nếu có) để tạo hiệu ứng gạch ngang
    if (!oldPrice.isEmpty()) {
      Text txtOldPrice = new Text(oldPrice);
      txtOldPrice.setStrikethrough(true);
      txtOldPrice.setFill(Color.web("#777777"));
      priceBox.getChildren().add(txtOldPrice);
    }

    card.getChildren().addAll(imagePlaceholder, lblTitle, priceBox);
    return card;
  }
}