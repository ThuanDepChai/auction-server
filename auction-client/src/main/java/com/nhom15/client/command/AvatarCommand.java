package com.nhom15.client.command;

import com.nhom15.client.controller.CropAvatarController;
import com.nhom15.client.util.SessionManager;
import com.nhom15.client.util.ViewManager;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Base64;
import java.util.function.Consumer;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * AvatarCommand — stateless, không giữ bất kỳ UI reference nào.
 * <p>
 * Trả data về controller qua callback: - onPreview(base64)  → controller tự set ImageView -
 * onStatus(message)  → controller tự hiển thị status label
 */
public class AvatarCommand {

  private static final long MAX_FILE_SIZE = 2 * 1024 * 1024; // 2MB

  // ── Public API ────────────────────────────────────────────────────────────

  /**
   * Flow đầy đủ: pick file → crop → preview callback → upload → status callback.
   *
   * @param userId    để upload lên server
   * @param onPreview nhận base64 ngay sau crop để controller preview
   * @param onStatus  nhận message kết quả ("✓ ..." hoặc "Lỗi ...")
   */
  public static void pickAndUpload(int userId,
      Consumer<String> onPreview,
      Consumer<String> onStatus) {
    File file = pickFile();
    if (file == null) {
      return;
    }

    if (file.length() > MAX_FILE_SIZE) {
      onStatus.accept("Ảnh không được vượt quá 2MB!");
      return;
    }

    openCropWindow(file, base64 -> {
      onPreview.accept(base64);                          // controller preview ngay
      onStatus.accept("Đang tải ảnh lên...");
      upload(userId, base64, file.getName(), onStatus); // upload lên server
    });
  }

  /**
   * Decode base64 thành Image — controller gọi để hiển thị avatar từ session/server. Trả null nếu
   * base64 rỗng hoặc lỗi decode.
   */
  public static Image decodeImage(String base64) {
    if (base64 == null || base64.isEmpty()) {
      return null;
    }
    try {
      byte[] bytes = Base64.getDecoder().decode(base64);
      Image img = new Image(new ByteArrayInputStream(bytes));
      return img.isError() ? null : img;
    } catch (Exception e) {
      System.err.println("[AvatarCommand] decodeImage: " + e.getMessage());
      return null;
    }
  }

  /**
   * Load Image từ đường dẫn local — controller dùng để restore avatar từ SessionManager. Trả null
   * nếu file không tồn tại.
   */
  public static Image loadFromPath(String path) {
    if (path == null || path.isEmpty()) {
      return null;
    }
    try {
      File f = new File(path);
      return f.exists() ? new Image(f.toURI().toString()) : null;
    } catch (Exception e) {
      System.err.println("[AvatarCommand] loadFromPath: " + e.getMessage());
      return null;
    }
  }

  // ── Private helpers ───────────────────────────────────────────────────────

  private static File pickFile() {
    FileChooser fc = new FileChooser();
    fc.setTitle("Chọn ảnh đại diện");
    fc.getExtensionFilters().add(
        new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg"));
    return fc.showOpenDialog(ViewManager.getStage());
  }

  private static void openCropWindow(File file, Consumer<String> onCropped) {
    try {
      FXMLLoader loader = new FXMLLoader(
          AvatarCommand.class.getResource("/view/CropAvatarView.fxml"));
      Parent root = loader.load();
      CropAvatarController crop = loader.getController();
      crop.initImage(file, onCropped);

      Stage dialog = new Stage();
      dialog.initModality(Modality.APPLICATION_MODAL);
      dialog.initStyle(StageStyle.UNDECORATED);
      dialog.setScene(new Scene(root));
      dialog.showAndWait();
    } catch (IOException e) {
      System.err.println("[AvatarCommand] openCropWindow: " + e.getMessage());
    }
  }

  private static void upload(int userId, String base64,
      String fileName, Consumer<String> onStatus) {
    String ext = fileName.toLowerCase().endsWith(".png") ? "png" : "jpg";
    ProfileCommand.updateAvatar(userId, base64, ext, response -> {
      if (response != null && "SUCCESS".equals(response.get("status").getAsString())) {
        SessionManager.updateAvatar(response.get("avatarPath").getAsString());
        onStatus.accept("✓ Cập nhật ảnh thành công!");
      } else {
        String msg = (response != null && response.has("message"))
            ? response.get("message").getAsString() : "Tải ảnh thất bại!";
        onStatus.accept("Lỗi: " + msg);
      }
    });
  }
}