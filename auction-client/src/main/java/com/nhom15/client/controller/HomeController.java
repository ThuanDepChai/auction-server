package com.nhom15.client.controller;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nhom15.client.command.HomeCommand;
import com.nhom15.client.util.CardFactory;
import com.nhom15.client.util.SessionManager;
import com.nhom15.client.util.ViewManager;
import javafx.scene.shape.Rectangle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;


public class HomeController {

  // ── FXML Fields ──────────────────────────────────────────────────────────
  @FXML private Label lblUsername;
  @FXML private Label lblAvatarInitial;
  @FXML private ImageView imgAvatar;
  @FXML private VBox userDropdown;
  @FXML private FlowPane flowProducts;
  @FXML private FlowPane flowAuctions;
  @FXML private Button btnSellerDashboard;
  @FXML private TextField txtSearch;
  @FXML private ComboBox<String> cmbCategory;
  @FXML private ComboBox<String> cmbPriceFilter;
  @FXML private ComboBox<String> cmbTypeFilter;
  @FXML private ComboBox<String> cmbSortFilter;
  @FXML private TextField txtYearFilter;
  @FXML private TextField txtBrandFilter;
  @FXML private ScrollPane mainScrollPane;
  @FXML private AnchorPane bannerContainer;
  @FXML private ImageView bannerImageView;
  private final List<Timeline> countdownTimers = new ArrayList<>();
  private JsonArray featuredProductsCache = new JsonArray();

  // ── Khởi tạo ────────────────────────────────────────────────────────────
  @FXML
  public void initialize() {
    setupUserInfo();
    setupCategories();
    setupProductFilters();
    setupClickOutsideToCloseDropdown();
    loadData();
    setupHeroBanner();
  }
  private void setupHeroBanner() {
    if (bannerContainer == null || bannerImageView == null) {
      return;
    }

    try {
      Image bannerImage = new Image(
          getClass().getResource("/images/anhbanner1.jpg").toExternalForm(), false);
      bannerImageView.setImage(bannerImage);
      bannerImageView.setPreserveRatio(true);
      bannerImageView.setManaged(false);
      bannerImageView.toBack();

      Rectangle clip = new Rectangle();
      clip.widthProperty().bind(bannerContainer.widthProperty());
      clip.heightProperty().bind(bannerContainer.heightProperty());
      clip.setArcWidth(18);
      clip.setArcHeight(18);
      bannerContainer.setClip(clip);

      Runnable updateCover = () -> updateImageCover(bannerImageView, bannerImage);
      bannerContainer.widthProperty().addListener((obs, oldVal, newVal) -> updateCover.run());
      bannerContainer.heightProperty().addListener((obs, oldVal, newVal) -> updateCover.run());
      bannerImage.progressProperty().addListener((obs, oldVal, newVal) -> updateCover.run());
      Platform.runLater(updateCover);
    } catch (Exception e) {
      System.err.println("Lỗi load banner Home: " + e.getMessage());
    }
  }

  private void updateImageCover(ImageView imageView, Image image) {
    double frameWidth = bannerContainer.getWidth();
    double frameHeight = bannerContainer.getHeight();
    double imageWidth = image.getWidth();
    double imageHeight = image.getHeight();

    if (frameWidth <= 0 || frameHeight <= 0 || imageWidth <= 0 || imageHeight <= 0) {
      return;
    }

    double frameRatio = frameWidth / frameHeight;
    double imageRatio = imageWidth / imageHeight;
    if (frameRatio > imageRatio) {
      imageView.setFitWidth(frameWidth);
      imageView.setFitHeight(0);
    } else {
      imageView.setFitWidth(0);
      imageView.setFitHeight(frameHeight);
    }

    double renderedWidth = frameRatio > imageRatio ? frameWidth : frameHeight * imageRatio;
    double renderedHeight = frameRatio > imageRatio ? frameWidth / imageRatio : frameHeight;
    imageView.setLayoutX((frameWidth - renderedWidth) / 2);
    imageView.setLayoutY((frameHeight - renderedHeight) / 2);
  }
  private void setupUserInfo() {
    if (!SessionManager.isLoggedIn()) {
      lblUsername.setText("Khách");
      lblAvatarInitial.setText("K");
      btnSellerDashboard.setVisible(false);
      btnSellerDashboard.setManaged(false);
      return;
    }

    String username = SessionManager.getUsername();
    lblUsername.setText((username == null || username.trim().isEmpty()) ? "Người dùng" : username);

    String initial = lblUsername.getText().substring(0, 1).toUpperCase();
    lblAvatarInitial.setText(initial);

    boolean isSellerMode = SessionManager.isSeller() || SessionManager.isAdmin();
    btnSellerDashboard.setVisible(isSellerMode);
    btnSellerDashboard.setManaged(isSellerMode);

    HomeCommand.fetchUserAvatar(SessionManager.getUserId(), base64 -> {
      CardFactory.setAvatar(base64, imgAvatar, lblAvatarInitial);
    });
  }

  private void setupCategories() {
    if (cmbCategory != null) {
      cmbCategory.getItems().setAll(
              "Tất cả danh mục", "Điện tử", "Thời trang",
              "Nhà cửa & Sân vườn", "Đồ sưu tầm", "Thể thao", "Xe cộ", "Nghệ thuật"
      );
      cmbCategory.setValue("Tất cả danh mục");
    }
  }

  private void setupProductFilters() {
    if (cmbPriceFilter != null) {
      cmbPriceFilter.getItems().setAll(
          "Tất cả giá", "Dưới 1 triệu", "1 - 5 triệu", "5 - 20 triệu",
          "20 - 100 triệu", "Trên 100 triệu"
      );
      cmbPriceFilter.setValue("Tất cả giá");
    }
    if (cmbTypeFilter != null) {
      cmbTypeFilter.getItems().setAll(
          "Tất cả mục", "Điện tử", "Thời trang", "Xe cộ", "Nghệ thuật",
          "Đồ sưu tầm", "Thể thao", "Nhà cửa & Sân vườn"
      );
      cmbTypeFilter.setValue("Tất cả mục");
    }
    if (cmbSortFilter != null) {
      cmbSortFilter.getItems().setAll(
          "Mặc định", "Giá thấp đến cao", "Giá cao đến thấp", "Tên A-Z"
      );
      cmbSortFilter.setValue("Mặc định");
    }
  }

  private void setupClickOutsideToCloseDropdown() {
    Platform.runLater(() -> {
      if (userDropdown == null || userDropdown.getScene() == null) return;
      Scene scene = userDropdown.getScene();
      scene.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_PRESSED, e -> {
        if (userDropdown.isVisible()
                && !userDropdown.localToScene(userDropdown.getBoundsInLocal())
                .contains(e.getSceneX(), e.getSceneY())) {
          userDropdown.setVisible(false);
          userDropdown.setManaged(false);
        }
      });
    });
  }

  // ── Load Dữ Liệu ────────────────────────────────────────────────────────
  private void loadData() {
    HomeCommand.fetchFeaturedProducts(
            items -> {
              featuredProductsCache = copyArray(items);
              applyProductFilters();
            },
            () -> flowProducts.getChildren()
                    .setAll(CardFactory.buildEmptyLabel("Chưa có sản phẩm nổi bật"))
    );

    HomeCommand.fetchActiveAuctions(
            auctions -> populateFlowPane(flowAuctions, auctions, false),
            () -> flowAuctions.getChildren()
                    .setAll(CardFactory.buildEmptyLabel("Chưa có phiên đấu giá nào"))
    );
  }

  private void populateFlowPane(FlowPane pane, JsonArray dataArray, boolean isProduct) {
    if (pane == null) return;
    pane.getChildren().clear();

    if (!isProduct) {
      countdownTimers.forEach(Timeline::stop);
    }

    if (dataArray == null || dataArray.isEmpty()) {
      pane.getChildren().add(CardFactory.buildEmptyLabel("Không có dữ liệu"));
      return;
    }

    for (int i = 0; i < dataArray.size(); i++) {
      if (isProduct) {
        pane.getChildren().add(CardFactory.buildProductCard(
                dataArray.get(i).getAsJsonObject(), this::handleGoToProductDetail));
      } else {
        pane.getChildren().add(CardFactory.buildAuctionCard(
                dataArray.get(i).getAsJsonObject(), countdownTimers, this::handleGoToBidding));
      }
    }
  }

  private void applyProductFilters() {
    if (flowProducts == null) {
      return;
    }

    List<JsonObject> filtered = new ArrayList<>();
    for (JsonElement element : featuredProductsCache) {
      if (element != null && element.isJsonObject()) {
        JsonObject item = element.getAsJsonObject();
        if (matchesProductFilters(item)) {
          filtered.add(item);
        }
      }
    }

    sortProducts(filtered);

    JsonArray result = new JsonArray();
    for (JsonObject item : filtered) {
      result.add(item);
    }
    populateFlowPane(flowProducts, result, true);
  }

  private boolean matchesProductFilters(JsonObject item) {
    return matchesPrice(item)
        && matchesCategoryFilter(item)
        && matchesYearFilter(item)
        && matchesBrandFilter(item);
  }

  private boolean matchesPrice(JsonObject item) {
    String selected = cmbPriceFilter == null ? null : cmbPriceFilter.getValue();
    if (selected == null || "Tất cả giá".equals(selected)) {
      return true;
    }

    double price = getNumber(item, "price", "startPrice", "currentPrice");
    return switch (selected) {
      case "Dưới 1 triệu" -> price < 1_000_000;
      case "1 - 5 triệu" -> price >= 1_000_000 && price <= 5_000_000;
      case "5 - 20 triệu" -> price >= 5_000_000 && price <= 20_000_000;
      case "20 - 100 triệu" -> price >= 20_000_000 && price <= 100_000_000;
      case "Trên 100 triệu" -> price > 100_000_000;
      default -> true;
    };
  }

  private boolean matchesCategoryFilter(JsonObject item) {
    String selected = cmbTypeFilter == null ? null : cmbTypeFilter.getValue();
    if (selected == null || "Tất cả mục".equals(selected)) {
      return true;
    }
    return normalize(getString(item, "category")).contains(normalize(selected));
  }

  private boolean matchesYearFilter(JsonObject item) {
    String expected = txtYearFilter == null ? "" : txtYearFilter.getText().trim();
    if (expected.isEmpty()) {
      return true;
    }

    String year = getExtraValue(item, "year");
    if (year.isEmpty()) {
      year = getString(item, "year");
    }
    return year.contains(expected);
  }

  private boolean matchesBrandFilter(JsonObject item) {
    String expected = txtBrandFilter == null ? "" : normalize(txtBrandFilter.getText());
    if (expected.isEmpty()) {
      return true;
    }

    String combined = getExtraValue(item, "brand") + " "
        + getExtraValue(item, "artist") + " "
        + getExtraValue(item, "sportType") + " "
        + getString(item, "brand") + " "
        + getString(item, "name");
    return normalize(combined).contains(expected);
  }

  private void sortProducts(List<JsonObject> items) {
    String selected = cmbSortFilter == null ? null : cmbSortFilter.getValue();
    if (selected == null || "Mặc định".equals(selected)) {
      return;
    }

    Comparator<JsonObject> byPrice = Comparator.comparingDouble(
        item -> getNumber(item, "price", "startPrice", "currentPrice"));
    switch (selected) {
      case "Giá thấp đến cao" -> items.sort(byPrice);
      case "Giá cao đến thấp" -> items.sort(byPrice.reversed());
      case "Tên A-Z" -> items.sort(Comparator.comparing(item -> normalize(getString(item, "name"))));
      default -> {
      }
    }
  }

  private JsonArray copyArray(JsonArray source) {
    JsonArray copy = new JsonArray();
    if (source == null) {
      return copy;
    }
    for (JsonElement element : source) {
      copy.add(element.deepCopy());
    }
    return copy;
  }

  private double getNumber(JsonObject object, String... keys) {
    for (String key : keys) {
      String raw = getString(object, key);
      if (!raw.isEmpty()) {
        try {
          return Double.parseDouble(raw.replaceAll("[^0-9.]", ""));
        } catch (Exception ignored) {
          return 0;
        }
      }
    }
    return 0;
  }

  private String getString(JsonObject object, String key) {
    if (object == null || !object.has(key) || object.get(key).isJsonNull()) {
      return "";
    }
    return object.get(key).getAsString();
  }

  private String getExtraValue(JsonObject item, String key) {
    String raw = getString(item, "extraInfo");
    if (raw.isEmpty()) {
      return "";
    }
    try {
      JsonElement parsed = JsonParser.parseString(raw);
      if (parsed.isJsonObject()) {
        return getString(parsed.getAsJsonObject(), key);
      }
    } catch (Exception ignored) {
      return "";
    }
    return "";
  }

  private String normalize(String value) {
    return value == null ? "" : value.trim().toLowerCase();
  }

  // ── Xử lý Navigation & Nút bấm ──────────────────────────────────────────
  @FXML
  private void handleUserMenu(ActionEvent event) {
    if (userDropdown != null) {
      boolean isVisible = !userDropdown.isVisible();
      userDropdown.setVisible(isVisible);
      userDropdown.setManaged(isVisible);
    }
  }

  @FXML
  private void handleProfile(ActionEvent event) {
    ViewManager.navigateTo(ViewManager.Views.PROFILE);
  }

  @FXML
  private void handleMyOrders(ActionEvent event) {
    ViewManager.navigateTo(ViewManager.Views.MY_ORDERS);
  }

  @FXML
  private void handleCart(ActionEvent event) {
    ViewManager.navigateTo(ViewManager.Views.CART);
  }

  @FXML
  private void handleLogout(ActionEvent event) {
    countdownTimers.forEach(Timeline::stop);
    SessionManager.logout();
    ViewManager.navigateTo(ViewManager.Views.LOGIN);
  }

  @FXML
  private void handleSellerDashboard(ActionEvent event) {
    if (!SessionManager.isSeller() && !SessionManager.isAdmin()) {
      showAlert("Bạn cần đăng ký tài khoản Seller để sử dụng chức năng này!");
      return;
    }
    ViewManager.navigateTo(ViewManager.Views.SELLER_DASHBOARD);
  }

  @FXML
  private void handleSearch(ActionEvent event) {
    String keyword  = txtSearch != null ? txtSearch.getText().trim() : "";
    String category = (cmbCategory != null && !"Tất cả danh mục".equals(cmbCategory.getValue()))
            ? cmbCategory.getValue() : "";

    HomeCommand.searchProducts(keyword, category,
            items -> {
              featuredProductsCache = copyArray(items);
              applyProductFilters();
            },
            () -> flowProducts.getChildren()
                    .setAll(CardFactory.buildEmptyLabel("Không tìm thấy sản phẩm"))
    );
    if (mainScrollPane != null) mainScrollPane.setVvalue(0);
  }

  @FXML
  private void handleNotification(ActionEvent event) {
    showAlert("Chưa có thông báo mới.");
  }

  @FXML
  private void handleWallet(ActionEvent event) {
    if (userDropdown != null) userDropdown.setVisible(false);
    try {
      double balance = SessionManager.getBalance();
      showAlert(String.format("Số dư ví: %,d đ", (long) balance));
    } catch (Exception e) {
      showAlert("Không thể lấy số dư ví. Vui lòng thử lại.");
    }
  }

  @FXML
  private void handleSell(ActionEvent event) {
    if (!SessionManager.isSeller() && !SessionManager.isAdmin()) {
      showAlert("Bạn cần đăng ký tài khoản Seller để đăng bán!\nVào Trang cá nhân → Đăng ký bán hàng.");
      return;
    }
    ViewManager.navigateTo(ViewManager.Views.SELLER_DASHBOARD);
  }

  @FXML
  private void handleCategory(ActionEvent event) {
    Button src = (Button) event.getSource();
    String raw = src.getText().trim();

    int i = 0;
    while (i < raw.length()) {
      int cp = raw.codePointAt(i);
      if (Character.isLetter(cp)) break;
      i += Character.charCount(cp);
    }
    String category = raw.substring(i).trim();

    if ("Tất cả danh mục".equals(category) || "Giao dịch hot".equals(category)) {
      category = "";
    }

    if (cmbCategory != null) {
      cmbCategory.setValue(category.isEmpty() ? "Tất cả danh mục" : category);
    }

    HomeCommand.searchProducts("", category,
            items -> {
              featuredProductsCache = copyArray(items);
              applyProductFilters();
            },
            () -> flowProducts.getChildren()
                    .setAll(CardFactory.buildEmptyLabel("Không tìm thấy sản phẩm"))
    );
  }

  @FXML
  private void handleApplyFilters(ActionEvent event) {
    applyProductFilters();
  }

  @FXML
  private void handleResetFilters(ActionEvent event) {
    if (cmbPriceFilter != null) {
      cmbPriceFilter.setValue("Tất cả giá");
    }
    if (cmbTypeFilter != null) {
      cmbTypeFilter.setValue("Tất cả mục");
    }
    if (cmbSortFilter != null) {
      cmbSortFilter.setValue("Mặc định");
    }
    if (txtYearFilter != null) {
      txtYearFilter.clear();
    }
    if (txtBrandFilter != null) {
      txtBrandFilter.clear();
    }
    applyProductFilters();
  }

  // ── Callbacks ────────────────────────────────────────────────────────────

  private void handleGoToProductDetail(int productId) {
    countdownTimers.forEach(Timeline::stop);
    ViewManager.navigateTo("/view/product_detail.fxml", "Chi tiết sản phẩm", c -> {
      // ProductDetailController không có trong dự án hiện tại — bỏ qua
    });
  }

  /**
   * FIX: Trước đây dùng inner interface BiddingRoomController (stub) nên
   * "c instanceof BiddingRoomController" luôn FALSE → setAuctionId() không bao giờ được gọi
   * → auctionId mãi là 0 → server trả NOT_FOUND.
   *
   * Sửa: Cast trực tiếp sang class thật
   * com.nhom15.client.controller.AuctionRoomController (cùng package).
   */
  private void handleGoToBidding(int auctionId) {
    countdownTimers.forEach(Timeline::stop);
    ViewManager.navigateTo(ViewManager.Views.AUCTION_ROOM,
            "Phòng đấu giá #" + auctionId,
            c -> {
              // FIX: Cast sang class thật thay vì inner interface stub
              if (c instanceof com.nhom15.client.controller.AuctionRoomController b) {
                b.setAuctionId(auctionId);
              }
            });
  }

  private void showAlert(String msg) {
    Alert alert = new Alert(Alert.AlertType.INFORMATION, msg);
    alert.setHeaderText(null);
    alert.showAndWait();
  }
}
