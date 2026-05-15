package com.nhom15.client.util;

import java.io.IOException;
import java.util.function.Consumer;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;

/**
 * ViewManager — quản lý điều hướng màn hình tập trung.
 */
public final class ViewManager {

  // ── Danh sách màn hình — đổi path FXML ở đây, không cần tìm trong controller ──
  public static final class Views {

    public static final String LOGIN = "/view/login.fxml";
    public static final String REGISTER = "/view/register.fxml";
    public static final String HOME = "/view/Home.fxml";
    public static final String PROFILE = "/view/profile.fxml";
    public static final String SELLER_DASHBOARD = "/view/seller_dashboard.fxml";
    public static final String BIDDING_ROOM = "/view/bidding_room.fxml";
    public static final String AUCTION_ROOM = "/view/Auctionroom.fxml";
    public static final String AUCTION_LIST = "/view/auction_list.fxml";
    public static final String MY_ORDERS = "/view/my_orders.fxml";
    public static final String CART = "/view/cart.fxml";
    public static final String ADMIN_DASHBOARD = "/view/admin_dashboard.fxml";

    private Views() {
    }
  }

  // ── Title mặc định theo path ─────────────────────────────────────────────
  private static final java.util.Map<String, String> DEFAULT_TITLES = java.util.Map.of(
          Views.LOGIN, "Đăng nhập",
          Views.REGISTER, "Đăng ký tài khoản",
          Views.HOME, "Trang chủ",
          Views.PROFILE, "Trang cá nhân",
          Views.SELLER_DASHBOARD, "Quản lý bán hàng",
          Views.BIDDING_ROOM, "Phòng đấu giá",
          Views.AUCTION_ROOM, "Phòng đấu giá",
          Views.AUCTION_LIST, "Danh sách đấu giá",
          Views.MY_ORDERS, "Đơn hàng của tôi"
  );

  // ── Singleton state ──────────────────────────────────────────────────────
  private static Stage primaryStage;

  private ViewManager() {
  }

  /**
   * Phải gọi 1 lần duy nhất trong App.start() trước mọi điều hướng.
   */
  public static void init(Stage stage) {
    primaryStage = stage;
  }

  public static Stage getStage() {
    return primaryStage;
  }

  // ── Navigate cơ bản ──────────────────────────────────────────────────────

  public static void navigateTo(String fxmlPath) {
    navigateTo(fxmlPath, DEFAULT_TITLES.getOrDefault(fxmlPath, "Auction App"), null);
  }

  public static void navigateTo(String fxmlPath, String title) {
    navigateTo(fxmlPath, title, null);
  }

  public static void navigateTo(String fxmlPath, Consumer<Object> controllerCallback) {
    navigateTo(fxmlPath,
            DEFAULT_TITLES.getOrDefault(fxmlPath, "Auction App"),
            controllerCallback);
  }

  /**
   * Core method — tất cả overload đều gọi về đây.
   */
  public static void navigateTo(String fxmlPath, String title,
                                Consumer<Object> controllerCallback) {
    if (primaryStage == null) {
      throw new IllegalStateException(
              "ViewManager chưa được init. Gọi ViewManager.init(stage) trong App.start()");
    }
    try {
      FXMLLoader loader = new FXMLLoader(
              ViewManager.class.getResource(fxmlPath));
      Parent root = loader.load();

      // Callback cho controller nếu cần truyền data
      if (controllerCallback != null) {
        Object controller = loader.getController();
        if (controller != null) {
          controllerCallback.accept(controller);
        }
      }
      //F11
      Scene currentScene = primaryStage.getScene();

      if (currentScene == null) {
        // Nếu là lần đầu tiên mở ứng dụng (chưa có Scene), ta tạo mới
        Scene newScene = new Scene(root);

        // Gắn sự kiện lắng nghe phím F11 để Full Screen
        newScene.setOnKeyPressed(event -> {
          if (event.getCode() == KeyCode.F11) {
            // Tắt / Bật chế độ Full Screen
            primaryStage.setFullScreen(!primaryStage.isFullScreen());

            // Lưu ý: Nếu bạn chỉ muốn phóng to (Maximize) chứ không muốn tràn viền mất Taskbar,
            // hãy xóa dòng setFullScreen ở trên và dùng dòng dưới đây:
            // primaryStage.setMaximized(!primaryStage.isMaximized());
          }
        });

        primaryStage.setScene(newScene);
        primaryStage.centerOnScreen();
      } else {
        // Nếu đã có Scene (tức là đang chuyển trang), ta chỉ thay đổi nội dung bên trong
        // Việc này giúp Form giữ nguyên vị trí và kích thước (kể cả khi đang Full Screen)
        currentScene.setRoot(root);
      }

      primaryStage.setTitle(title);

    } catch (IOException e) {
      System.err.println("[ViewManager] Không thể load: " + fxmlPath
              + " — " + e.getMessage());
      e.printStackTrace();
    }
  }

  // ── Load controller mà không đổi scene (dùng cho modal/dialog) ──────────

  @SuppressWarnings("unchecked")
  public static <T> T loadController(String fxmlPath) throws IOException {
    FXMLLoader loader = new FXMLLoader(
            ViewManager.class.getResource(fxmlPath));
    loader.load();
    return loader.getController();
  }
}