package com.nhom15.client.controller;

import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Base64;
import java.util.function.Consumer;

public class CropAvatarController {

    @FXML private Pane paneImageContainer;
    @FXML private ImageView imgSource;
    @FXML private Slider sliderZoom;

    private Image originalImage;
    private double currentScale = 1.0;
    private double deltaX, deltaY;

    // Khung cắt hình tròn có đường kính 250px (radius 125px) nằm giữa màn hình 600x350
    // Vị trí tâm khung cắt (so với paneImageContainer): x=125, y=50
    private static final double CROP_SIZE = 250.0;
    private static final double CROP_TOP_MARGIN = 50.0;
    private static final double CROP_LEFT_MARGIN = 175.0; // (600 - 250) / 2

    // Callback để trả kết quả về ProfileController
    private Consumer<String> onImageCropped;

    @FXML
    public void initialize() {
        // 1. Cấu hình thanh trượt phóng to/thu nhỏ
        sliderZoom.valueProperty().addListener((observable, oldValue, newValue) -> {
            currentScale = newValue.doubleValue() / 100.0 * 2.0; // Thang đo từ 0.02 đến 2.0
            if (currentScale < 0.1) currentScale = 0.1; // Tối thiểu 10%
            imgSource.setScaleX(currentScale);
            imgSource.setScaleY(currentScale);
        });

        // 2. Thêm sự kiện để người dùng có thể kéo thả ảnh
        imgSource.setOnMousePressed(event -> {
            deltaX = imgSource.getTranslateX() - event.getSceneX();
            deltaY = imgSource.getTranslateY() - event.getSceneY();
            imgSource.setCursor(javafx.scene.Cursor.MOVE);
        });

        imgSource.setOnMouseDragged(event -> {
            imgSource.setTranslateX(event.getSceneX() + deltaX);
            imgSource.setTranslateY(event.getSceneY() + deltaY);
        });

        imgSource.setOnMouseReleased(event -> {
            imgSource.setCursor(javafx.scene.Cursor.DEFAULT);
        });
    }

    /**
     * Hàm được gọi từ ProfileController để khởi tạo ảnh gốc và callback
     */
    public void initImage(File file, Consumer<String> callback) {
        this.onImageCropped = callback;
        try {
            originalImage = new Image(file.toURI().toString());
            imgSource.setImage(originalImage);

            // Căn chỉnh ảnh gốc nằm giữa khung cắt
            double initialScale = CROP_SIZE / Math.min(originalImage.getWidth(), originalImage.getHeight());
            if (initialScale > 1.0) initialScale = 1.0; // Không phóng to nếu ảnh nhỏ hơn khung
            sliderZoom.setValue(initialScale * 100.0 / 2.0); // Cập nhật slider

            imgSource.setTranslateX((350.0 - originalImage.getWidth()) / 2.0); // paneImageContainer rộng 350
            imgSource.setTranslateY((350.0 - originalImage.getHeight()) / 2.0);

        } catch (Exception e) {
            System.err.println("❌ Lỗi khi khởi tạo ảnh cắt: " + e.getMessage());
        }
    }

    @FXML
    void handleCancel(ActionEvent event) {
        closeWindow();
    }

    /**
     * Xử lý thực hiện câu lệnh cắt ảnh
     */
    @FXML
    void handleSave(ActionEvent event) {
        if (originalImage == null || onImageCropped == null) {
            closeWindow();
            return;
        }

        try {
            // 1. Tính toán vùng ảnh cần cắt dựa trên vị trí và độ phóng của ImageView so với khung cắt
            PixelReader reader = originalImage.getPixelReader();
            if (reader == null) {
                System.err.println("❌ Không thể đọc pixel của ảnh!");
                return;
            }

            // Tính toán vùng viewport trong ảnh gốc
            double viewportX = (CROP_LEFT_MARGIN - imgSource.getTranslateX()) / currentScale;
            double viewportY = (CROP_TOP_MARGIN - imgSource.getTranslateY()) / currentScale;
            double viewportSize = CROP_SIZE / currentScale;

            // Đảm bảo không cắt ra ngoài biên ảnh gốc
            viewportX = Math.max(0, Math.min(viewportX, originalImage.getWidth() - viewportSize));
            viewportY = Math.max(0, Math.min(viewportY, originalImage.getHeight() - viewportSize));

            // 2. Tạo WritableImage (ảnh mới) bằng cách cắt vùng pixel đã tính
            WritableImage croppedWritableImage = new WritableImage(reader, (int)viewportX, (int)viewportY, (int)viewportSize, (int)viewportSize);

            // 3. Chuyển WritableImage thành PNG mảng byte
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            java.awt.image.BufferedImage bufferedImage = SwingFXUtils.fromFXImage(croppedWritableImage, null);
            ImageIO.write(bufferedImage, "png", outputStream);
            byte[] imageBytes = outputStream.toByteArray();

            // 4. Mã hóa mảng byte thành Base64
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);

            // 5. Trả chuỗi Base64 về cho ProfileController
            onImageCropped.accept(base64Image);

            System.out.println("✅ Cắt ảnh thành công!");
            closeWindow();

        } catch (IOException e) {
            System.err.println("❌ Lỗi xử lý ảnh: " + e.getMessage());
        }
    }

    private void closeWindow() {
        Stage stage = (Stage) paneImageContainer.getScene().getWindow();
        stage.close();
    }
}