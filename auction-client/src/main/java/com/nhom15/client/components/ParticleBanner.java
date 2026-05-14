package com.nhom15.client.components;

import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;

public class ParticleBanner extends StackPane {

  // Tăng mật độ hạt (giảm khoảng cách)
  private static final int DOT_SPACING = 8;
  private double mouseX = -999, mouseY = -999;
  private long startTime = -1;
  private final Canvas canvas;

  public ParticleBanner() {
    canvas = new Canvas();
    this.getChildren().add(canvas);

    // 1. Logic tự căn chỉnh (Responsiveness): Ràng buộc Canvas với StackPane
    canvas.widthProperty().bind(this.widthProperty());
    canvas.heightProperty().bind(this.heightProperty());

    // Bắt sự kiện chuột (Mouse tracking) tinh tế hơn
    this.setOnMouseMoved(e -> { mouseX = e.getX(); mouseY = e.getY(); });
    this.setOnMouseExited(e -> { mouseX = -999; mouseY = -999; });

    // Khởi chạy logic animation sau khi Canvas đã được gán kích thước
    Platform.runLater(() -> {
      if (this.getWidth() > 0 && this.getHeight() > 0) {
        startAnimation();
      } else {
        // Nếu chưa có kích thước, đợi cho đến khi có
        this.widthProperty().addListener((obs, oldV, newV) -> {
          if (newV.doubleValue() > 0 && this.getHeight() > 0) {
            startAnimation();
          }
        });
        this.heightProperty().addListener((obs, oldV, newV) -> {
          if (newV.doubleValue() > 0 && this.getWidth() > 0) {
            startAnimation();
          }
        });
      }
    });
  }

  private void startAnimation() {
    GraphicsContext gc = canvas.getGraphicsContext2D();

    // 2. Tự căn chỉnh: Số lượng hạt sẽ được tính toán lại trong handle() dựa trên kích thước thực tế
    new AnimationTimer() {
      @Override
      public void handle(long now) {
        if (startTime < 0) startTime = now;
        double t = (now - startTime) / 1_000_000_000.0 * 1.8; // speed

        // Tự căn chỉnh: Lấy kích thước hiện tại
        double bannerW = canvas.getWidth();
        double bannerH = canvas.getHeight();

        if (bannerW <= 0 || bannerH <= 0) return;

        // 2. Giống hình mẫu: Phức tạp hóa bảng màu
        LinearGradient bg = new LinearGradient(
            0, 0, bannerW, bannerH, false, CycleMethod.NO_CYCLE,
            new Stop(0.0, Color.rgb(153, 51, 153)), // Tím hồng đậm (trên bên trái)
            new Stop(0.3, Color.rgb(25, 25, 112)),  // Xanh lam đêm (trung tâm)
            new Stop(0.7, Color.rgb(0, 191, 255)),  // Xanh lam da trời tươi (trên bên phải)
            new Stop(1.0, Color.rgb(0, 0, 139))   // Xanh lam đậm (dưới)
        );
        gc.setFill(bg);
        gc.fillRect(0, 0, bannerW, bannerH);

        // Tính toán lại lưới hạt cho kích thước mới
        int cols = (int) bannerW / DOT_SPACING + 2;
        int rows = (int) bannerH / DOT_SPACING + 2;
        int totalDots = cols * rows;

        // 2. Giống hình mẫu: Màu hạt xanh lam nhạt tinh tế
        Color particleColor = Color.rgb(173, 216, 230, 0.5); // Xanh lam da trời rất nhạt
        Color mouseGlowColor = Color.rgb(173, 216, 230, 0.12);

        // Vòng lặp vẽ hạt với logic sóng phức tạp hơn
        for (int r = 0; r < rows; r++) {
          for (int c = 0; c < cols; c++) {
            double bx = c * DOT_SPACING;
            double by = r * DOT_SPACING;

            // 2. Giống hình mẫu: Logic sóng 3D có rãnh phức tạp
            // Sóng có rãnh (ribbed wave): Biên độ sóng phụ thuộc vào cột để tạo các dải
            double waveFactor1 = Math.cos(c * 0.1) * Math.sin(r * 0.05);
            double wave = (Math.sin(c * 0.4 + t * 1.1 + waveFactor1 * 5) +
                Math.cos(r * 0.35 + t * 0.8)) * 0.5;
            double norm = (wave + 1.0) / 2.0;

            // Tương tác chuột tinh tế
            double mdx = bx - mouseX;
            double mdy = by - mouseY;
            double mdist = Math.sqrt(mdx * mdx + mdy * mdy);
            double mouseEffect = Math.max(0, 1 - mdist / 60.0);
            double mx = mdist > 0 ? mdx / mdist * mouseEffect * 15 : 0;
            double my = mdist > 0 ? mdy / mdist * mouseEffect * 15 : 0;

            double x = bx + mx;
            double y = by + my;

            // 2. Giống hình mẫu: Hạt rất nhỏ và độ sáng phức tạp
            // Độ sáng bị ảnh hưởng mạnh bởi sóng để tạo kết cấu "có rãnh"
            double bright = norm * 0.6 + mouseEffect * 0.6;
            double alpha = Math.min(1.0, 0.1 + bright * 0.7);
            double radius = 0.3 + bright * 0.7 + mouseEffect * 1.5; // Kích thước hạt nhỏ hơn

            // Áp dụng màu hạt với alpha
            gc.setFill(particleColor.deriveColor(0, 1.0, 1.0, alpha));
            gc.fillOval(x - radius, y - radius, radius * 2, radius * 2);

            // Hiệu ứng chuột tinh tế
            if (mouseEffect > 0.3) {
              gc.setFill(mouseGlowColor.deriveColor(0, 1.0, 1.0, mouseEffect * alpha));
              double hr = radius * 2.5;
              gc.fillOval(x - hr, y - hr, hr * 2, hr * 2);
            }
          }
        }

        // Loại bỏ vệt sáng di động cũ để giống hình mẫu
      }
    }.start();
  }
}