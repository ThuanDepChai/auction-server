package com.nhom15.client.controller;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.nhom15.client.command.CheckUsernameCommand;
import com.nhom15.client.command.LoginCommand;
import com.nhom15.client.command.RegisterCommand;
import com.nhom15.client.command.ServerCommand;
import com.nhom15.client.model.UserDTO;
import com.nhom15.client.util.FormValidator;
import com.nhom15.client.util.SessionManager;
import com.nhom15.client.util.ViewManager;
import java.net.URL;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

public class LoginController {

  @FXML
  private StackPane rootPane;
  @FXML
  private BorderPane contentLayer;
  @FXML
  private StackPane modalLayer;
  @FXML
  private StackPane authModal;
  @FXML
  private VBox loginForm;
  @FXML
  private VBox registerForm;
  @FXML
  private Pane bgAnimationPane;
  @FXML
  private StackPane heroMediaSection;
  @FXML
  private MediaView heroVideoView;
  @FXML
  private StackPane bannerMediaSection;
  @FXML
  private ImageView bannerImageView;
  @FXML
  private StackPane closingMediaSection;
  @FXML
  private MediaView closingVideoView;

  @FXML
  private TextField txtUsername;
  @FXML
  private TextField txtEmail;
  @FXML
  private PasswordField txtPassword;

  @FXML
  private Label lblUsernameError;
  @FXML
  private Label lblEmailError;
  @FXML
  private Label lblPasswordError;

  @FXML
  private TextField regUsername;
  @FXML
  private TextField regEmail;
  @FXML
  private PasswordField regPassword;
  @FXML
  private PasswordField regConfirmPassword;

  @FXML
  private Label lblRegUsernameError;
  @FXML
  private Label lblRegEmailError;
  @FXML
  private Label lblRegPasswordError;
  @FXML
  private Label lblRegConfirmError;

  @FXML
  private Button btnLogin;
  @FXML
  private Button btnRegister;

  private PauseTransition usernameCheckDelay;
  private boolean isUsernameAvailable = true;
  private MediaPlayer heroPlayer;
  private MediaPlayer closingPlayer;
  private final Rectangle heroClip = new Rectangle();
  private final Rectangle bannerClip = new Rectangle();
  private final Rectangle closingClip = new Rectangle();

  @FXML
  public void initialize() {
    bindManagedToVisible(lblUsernameError, lblEmailError, lblPasswordError,
        lblRegUsernameError, lblRegEmailError, lblRegPasswordError, lblRegConfirmError);

    setupLoginValidation();
    setupRegisterValidation();
    setupBackground();
  }

  private void setupLoginValidation() {
    FormValidator.bindRegex(txtUsername, lblUsernameError, "^.+$",
        "Vui lòng nhập tên đăng nhập!");
    FormValidator.bindRegex(txtEmail, lblEmailError, "^[\\w-.]+@([\\w-]+\\.)+[\\w-]{2,4}$",
        "Email không hợp lệ!");
    FormValidator.bindRegex(txtPassword, lblPasswordError, "^.+$",
        "Vui lòng nhập mật khẩu!");
  }

  private void setupRegisterValidation() {
    FormValidator.bindRegex(regEmail, lblRegEmailError, "^[\\w-.]+@([\\w-]+\\.)+[\\w-]{2,4}$",
        "Email không hợp lệ!");
    FormValidator.bindRegex(regPassword, lblRegPasswordError,
        "^(?=.*[A-Za-z])(?=.*\\d).{6,}$",
        "Mật khẩu phải từ 6 ký tự, bao gồm cả chữ lẫn số!");

    usernameCheckDelay = new PauseTransition(Duration.millis(600));
    usernameCheckDelay.setOnFinished(e -> checkUsernameAsync());

    regUsername.textProperty().addListener((obs, oldVal, newVal) -> {
      lblRegUsernameError.setVisible(false);
      isUsernameAvailable = true;
      usernameCheckDelay.stop();
      if (!newVal.trim().isEmpty()) {
        usernameCheckDelay.playFromStart();
      }
    });
  }

  private void setupBackground() {
    setupResponsiveSectionHeights();
    setupCoverViewport(heroMediaSection, heroVideoView, heroClip);
    setupCoverViewport(bannerMediaSection, bannerImageView, bannerClip);
    setupCoverViewport(closingMediaSection, closingVideoView, closingClip);
    loadParticleFallback();
    loadBannerImage();
    heroPlayer = loadLoopingVideo(heroVideoView, "/video/videobanner1.mp4");
    closingPlayer = loadLoopingVideo(closingVideoView, "/video/videobanner2.mp4");
  }

  private void setupResponsiveSectionHeights() {
    if (rootPane == null) {
      return;
    }
    rootPane.heightProperty().addListener((obs, oldVal, newVal) -> updateSectionHeights());
    rootPane.widthProperty().addListener((obs, oldVal, newVal) -> {
      updateSectionHeights();
      updateAllCovers();
    });
    rootPane.sceneProperty().addListener((obs, oldScene, newScene) -> Platform.runLater(() -> {
      updateSectionHeights();
      updateAllCovers();
    }));
    Platform.runLater(() -> {
      updateSectionHeights();
      updateAllCovers();
    });
  }

  private void updateSectionHeights() {
    double rootHeight = rootPane == null ? 0 : rootPane.getHeight();
    if (rootHeight <= 0) {
      return;
    }

    setSectionHeight(heroMediaSection, clamp(rootHeight - 62, 560, 820));
    setSectionHeight(bannerMediaSection, clamp(rootHeight * 0.72, 460, 700));
    setSectionHeight(closingMediaSection, clamp(rootHeight * 0.70, 440, 680));
    updateAllCovers();
  }

  private void setSectionHeight(StackPane section, double height) {
    if (section == null) {
      return;
    }
    section.setMinHeight(height);
    section.setPrefHeight(height);
    section.setMaxHeight(height);
  }

  private double clamp(double value, double min, double max) {
    return Math.max(min, Math.min(max, value));
  }

  private void setupCoverViewport(StackPane viewport, Node mediaNode, Rectangle clip) {
    if (viewport == null || mediaNode == null) {
      return;
    }
    mediaNode.setManaged(false);
    viewport.setClip(clip);
    viewport.widthProperty().addListener((obs, oldVal, newVal) -> updateCover(viewport, mediaNode, clip));
    viewport.heightProperty().addListener((obs, oldVal, newVal) -> updateCover(viewport, mediaNode, clip));
  }

  private MediaPlayer loadLoopingVideo(MediaView mediaView, String resourcePath) {
    if (mediaView == null) {
      return null;
    }
    try {
      URL videoUrl = getClass().getResource(resourcePath);
      if (videoUrl == null) {
        return null;
      }

      MediaPlayer player = new MediaPlayer(new Media(videoUrl.toExternalForm()));
      player.setCycleCount(MediaPlayer.INDEFINITE);
      player.setMute(true);
      player.setAutoPlay(true);
      player.setOnError(() ->
          System.err.println("[LoginController] Video " + resourcePath + ": "
              + player.getError().getMessage()));
      player.setOnReady(() -> updateCoverForMediaView(mediaView));
      mediaView.setMediaPlayer(player);
      return player;
    } catch (Exception e) {
      System.err.println("[LoginController] Video " + resourcePath + ": " + e.getMessage());
      return null;
    }
  }

  private void loadBannerImage() {
    if (bannerImageView == null) {
      return;
    }
    try {
      URL imageUrl = getClass().getResource("/images/anhbanner1.jpg");
      if (imageUrl != null) {
        bannerImageView.setImage(new Image(imageUrl.toExternalForm()));
        bannerImageView.getImage().progressProperty().addListener((obs, oldVal, newVal) -> {
          if (newVal.doubleValue() >= 1) {
            updateCover(bannerMediaSection, bannerImageView, bannerClip);
          }
        });
        updateCover(bannerMediaSection, bannerImageView, bannerClip);
      }
    } catch (Exception e) {
      System.err.println("[LoginController] Banner image: " + e.getMessage());
    }
  }

  private void updateCoverForMediaView(MediaView mediaView) {
    if (mediaView == heroVideoView) {
      updateCover(heroMediaSection, heroVideoView, heroClip);
    } else if (mediaView == closingVideoView) {
      updateCover(closingMediaSection, closingVideoView, closingClip);
    }
  }

  private void updateAllCovers() {
    updateCover(heroMediaSection, heroVideoView, heroClip);
    updateCover(bannerMediaSection, bannerImageView, bannerClip);
    updateCover(closingMediaSection, closingVideoView, closingClip);
  }

  private void updateCover(StackPane viewport, Node mediaNode, Rectangle clip) {
    if (viewport == null || mediaNode == null || clip == null) {
      return;
    }

    double viewportWidth = viewport.getWidth();
    double viewportHeight = viewport.getHeight();
    if (viewportWidth <= 0 || viewportHeight <= 0) {
      return;
    }

    clip.setWidth(viewportWidth);
    clip.setHeight(viewportHeight);

    double mediaWidth = getMediaWidth(mediaNode);
    double mediaHeight = getMediaHeight(mediaNode);
    if (mediaWidth <= 0 || mediaHeight <= 0) {
      return;
    }

    double scale = Math.max(viewportWidth / mediaWidth, viewportHeight / mediaHeight);
    double targetWidth = Math.ceil(mediaWidth * scale);
    double targetHeight = Math.ceil(mediaHeight * scale);

    if (mediaNode instanceof MediaView mediaView) {
      mediaView.setPreserveRatio(true);
      mediaView.setFitWidth(targetWidth);
      mediaView.setFitHeight(targetHeight);
    } else if (mediaNode instanceof ImageView imageView) {
      imageView.setPreserveRatio(true);
      imageView.setFitWidth(targetWidth);
      imageView.setFitHeight(targetHeight);
    }

    mediaNode.setTranslateX((viewportWidth - targetWidth) / 2);
    mediaNode.setTranslateY((viewportHeight - targetHeight) / 2);
  }

  private double getMediaWidth(Node mediaNode) {
    if (mediaNode instanceof MediaView mediaView
        && mediaView.getMediaPlayer() != null
        && mediaView.getMediaPlayer().getMedia() != null) {
      return mediaView.getMediaPlayer().getMedia().getWidth();
    }
    if (mediaNode instanceof ImageView imageView && imageView.getImage() != null) {
      return imageView.getImage().getWidth();
    }
    return 0;
  }

  private double getMediaHeight(Node mediaNode) {
    if (mediaNode instanceof MediaView mediaView
        && mediaView.getMediaPlayer() != null
        && mediaView.getMediaPlayer().getMedia() != null) {
      return mediaView.getMediaPlayer().getMedia().getHeight();
    }
    if (mediaNode instanceof ImageView imageView && imageView.getImage() != null) {
      return imageView.getImage().getHeight();
    }
    return 0;
  }

  private void loadParticleFallback() {
    try {
      Pane sharedBg = com.nhom15.client.util.BackgroundEngine.getSharedPane();
      if (sharedBg.getParent() instanceof Pane parent) {
        parent.getChildren().remove(sharedBg);
      }

      if (bgAnimationPane != null) {
        bgAnimationPane.getChildren().add(0, sharedBg);
        sharedBg.prefWidthProperty().bind(bgAnimationPane.widthProperty());
        sharedBg.prefHeightProperty().bind(bgAnimationPane.heightProperty());
      }
    } catch (Exception e) {
      System.err.println("[LoginController] Background fallback: " + e.getMessage());
    }
  }

  @FXML
  private void handleShowLogin(ActionEvent event) {
    showLoginForm();
    openModal();
  }

  @FXML
  private void handleShowRegister(ActionEvent event) {
    showRegisterForm();
    openModal();
  }

  @FXML
  private void handleCloseModal(ActionEvent event) {
    closeModal();
  }

  @FXML
  private void handleModalBackdropClick(MouseEvent event) {
    closeModal();
  }

  @FXML
  private void consumeClick(MouseEvent event) {
    event.consume();
  }

  private void showLoginForm() {
    loginForm.setVisible(true);
    loginForm.setManaged(true);
    registerForm.setVisible(false);
    registerForm.setManaged(false);
  }

  private void showRegisterForm() {
    registerForm.setVisible(true);
    registerForm.setManaged(true);
    loginForm.setVisible(false);
    loginForm.setManaged(false);
  }

  private void openModal() {
    contentLayer.setEffect(new GaussianBlur(14));
    modalLayer.setManaged(true);
    modalLayer.setVisible(true);
    modalLayer.setOpacity(0);
    authModal.setScaleX(0.96);
    authModal.setScaleY(0.96);

    FadeTransition fade = new FadeTransition(Duration.millis(180), modalLayer);
    fade.setToValue(1);
    fade.play();

    ScaleTransition scale = new ScaleTransition(Duration.millis(180), authModal);
    scale.setToX(1);
    scale.setToY(1);
    scale.play();
  }

  private void closeModal() {
    FadeTransition fade = new FadeTransition(Duration.millis(140), modalLayer);
    fade.setToValue(0);
    fade.setOnFinished(e -> {
      modalLayer.setVisible(false);
      modalLayer.setManaged(false);
      contentLayer.setEffect(null);
    });
    fade.play();
  }

  @FXML
  private void handleLogin(ActionEvent event) {
    String username = txtUsername.getText().trim();
    String email = txtEmail.getText().trim();
    String password = txtPassword.getText();

    boolean hasError = false;
    if (username.isEmpty()) {
      lblUsernameError.setVisible(true);
      hasError = true;
    }
    if (email.isEmpty()) {
      lblEmailError.setVisible(true);
      hasError = true;
    }
    if (password.isEmpty()) {
      lblPasswordError.setVisible(true);
      hasError = true;
    }

    if (hasError || lblUsernameError.isVisible() || lblEmailError.isVisible()
        || lblPasswordError.isVisible()) {
      return;
    }

    setLoginLoading(true);

    new LoginCommand(username, email, password).executeAsync(
        response -> Platform.runLater(() -> onLoginResponse(response)),
        () -> Platform.runLater(() -> {
          setLoginLoading(false);
          showAlert(Alert.AlertType.ERROR, "Lỗi kết nối", "Không thể kết nối đến Server!");
        })
    );
  }

  @FXML
  private void handleRegister(ActionEvent event) {
    if (!validateRegisterForm()) {
      return;
    }

    setRegisterLoading(true);

    new RegisterCommand(
        regUsername.getText().trim(),
        regEmail.getText().trim(),
        regPassword.getText()
    ).executeAsync(
        response -> Platform.runLater(() -> onRegisterResponse(response)),
        () -> Platform.runLater(() -> {
          setRegisterLoading(false);
          showAlert(Alert.AlertType.ERROR, "Lỗi kết nối", "Không thể kết nối đến Server!");
        })
    );
  }

  private void onLoginResponse(JsonObject response) {
    setLoginLoading(false);

    if (ServerCommand.isSuccess(response)) {
      try {
        Gson gson = new Gson();
        UserDTO user = gson.fromJson(response.getAsJsonObject("user"), UserDTO.class);

        SessionManager.login(user);
        stopBackgroundVideo();
        if ("ADMIN".equalsIgnoreCase(user.getRole())) {
          ViewManager.navigateTo(ViewManager.Views.ADMIN_DASHBOARD);
        } else {
          ViewManager.navigateTo(ViewManager.Views.HOME);
        }
      } catch (Exception e) {
        showAlert(Alert.AlertType.ERROR, "Lỗi dữ liệu",
            "Không thể đọc dữ liệu từ máy chủ!");
        System.err.println("[LoginController] Parse JSON: " + e.getMessage());
      }
    } else {
      String message = response.has("message") ? response.get("message").getAsString()
          : "Sai tài khoản hoặc mật khẩu!";
      showAlert(Alert.AlertType.ERROR, "Đăng nhập thất bại", message);
    }
  }

  private void onRegisterResponse(JsonObject response) {
    setRegisterLoading(false);
    if (ServerCommand.isSuccess(response)) {
      showAlert(Alert.AlertType.INFORMATION,
          "Thành công", "Đăng ký thành công! Vui lòng đăng nhập.");
      clearRegisterForm();
      showLoginForm();
    } else {
      isUsernameAvailable = false;
      lblRegUsernameError.setText("Tên đăng nhập đã tồn tại!");
      lblRegUsernameError.setVisible(true);
    }
  }

  private void checkUsernameAsync() {
    String username = regUsername.getText().trim();
    if (username.isEmpty()) {
      return;
    }

    new CheckUsernameCommand(username).executeAsync(
        response -> Platform.runLater(() -> {
          if (!regUsername.getText().trim().equals(username)) {
            return;
          }

          if (CheckUsernameCommand.isAvailable(response)) {
            isUsernameAvailable = true;
            lblRegUsernameError.setVisible(false);
          } else {
            isUsernameAvailable = false;
            lblRegUsernameError.setText("Tên đăng nhập đã tồn tại!");
            lblRegUsernameError.setVisible(true);
          }
        }),
        () -> {
          // Username availability is helpful, but connection failure should not block typing.
        }
    );
  }

  private boolean validateRegisterForm() {
    boolean ok = true;

    if (regUsername.getText().trim().isEmpty()) {
      lblRegUsernameError.setText("Vui lòng nhập tên đăng nhập!");
      lblRegUsernameError.setVisible(true);
      ok = false;
    }
    if (regEmail.getText().trim().isEmpty()) {
      lblRegEmailError.setText("Vui lòng nhập email!");
      lblRegEmailError.setVisible(true);
      ok = false;
    }
    if (regPassword.getText().isEmpty()) {
      lblRegPasswordError.setText("Vui lòng nhập mật khẩu!");
      lblRegPasswordError.setVisible(true);
      ok = false;
    }
    if (regConfirmPassword.getText().isEmpty()) {
      lblRegConfirmError.setText("Vui lòng xác nhận mật khẩu!");
      lblRegConfirmError.setVisible(true);
      ok = false;
    }

    if (!ok) {
      return false;
    }
    if (lblRegEmailError.isVisible() || lblRegPasswordError.isVisible()) {
      return false;
    }
    if (!isUsernameAvailable) {
      return false;
    }
    if (!regPassword.getText().equals(regConfirmPassword.getText())) {
      lblRegConfirmError.setText("Mật khẩu không khớp!");
      lblRegConfirmError.setVisible(true);
      return false;
    }

    return true;
  }

  private void clearRegisterForm() {
    regUsername.clear();
    regEmail.clear();
    regPassword.clear();
    regConfirmPassword.clear();
    lblRegUsernameError.setVisible(false);
    lblRegEmailError.setVisible(false);
    lblRegPasswordError.setVisible(false);
    lblRegConfirmError.setVisible(false);
    isUsernameAvailable = true;
  }

  private void setLoginLoading(boolean loading) {
    btnLogin.setDisable(loading);
    btnLogin.setText(loading ? "Đang đăng nhập..." : "ĐĂNG NHẬP");
  }

  private void setRegisterLoading(boolean loading) {
    btnRegister.setDisable(loading);
    btnRegister.setText(loading ? "Đang xử lý..." : "XÁC NHẬN ĐĂNG KÝ");
  }

  private void stopBackgroundVideo() {
    disposePlayer(heroPlayer);
    disposePlayer(closingPlayer);
    heroPlayer = null;
    closingPlayer = null;
  }

  private void disposePlayer(MediaPlayer player) {
    if (player != null) {
      player.stop();
      player.dispose();
    }
  }

  private void showAlert(Alert.AlertType type, String title, String msg) {
    Alert a = new Alert(type);
    a.setTitle(title);
    a.setHeaderText(null);
    a.setContentText(msg);
    a.showAndWait();
  }

  private void bindManagedToVisible(Node... nodes) {
    for (Node node : nodes) {
      node.managedProperty().bind(node.visibleProperty());
    }
  }
}
