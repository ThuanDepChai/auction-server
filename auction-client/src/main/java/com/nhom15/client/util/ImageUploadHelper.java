package com.nhom15.client.util;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.file.Files;
import java.util.Base64;
import java.util.Locale;
import java.util.function.Consumer;
import javafx.application.Platform;
import javafx.scene.image.Image;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

public final class ImageUploadHelper {

  private static final long MAX_UPLOAD_BYTES = 2L * 1024L * 1024L;
  private static final int MAX_DIMENSION = 1600;
  private static final float[] JPEG_QUALITIES = {0.85f, 0.75f, 0.65f};

  private ImageUploadHelper() {
  }

  public static void pickProductImage(Window owner, Consumer<ImageSelection> onSuccess,
      Consumer<String> onError) {
    if (!Platform.isFxApplicationThread()) {
      Platform.runLater(() -> pickProductImage(owner, onSuccess, onError));
      return;
    }

    FileChooser chooser = new FileChooser();
    chooser.setTitle("Chon anh san pham");
    chooser.getExtensionFilters().add(
        new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg"));
    File file = chooser.showOpenDialog(owner);
    if (file == null) {
      return;
    }

    Thread worker = new Thread(() -> process(file, onSuccess, onError), "seller-image-picker");
    worker.setDaemon(true);
    worker.start();
  }

  private static void process(File file, Consumer<ImageSelection> onSuccess,
      Consumer<String> onError) {
    String originalName = file.getName();
    String ext = extensionOf(originalName);
    long originalSize = file.length();
    log("Picked file=%s size=%d ext=%s thread=%s", originalName, originalSize, ext,
        Thread.currentThread().getName());

    try {
      if (!isSupportedExtension(ext)) {
        fail(onError, "Dinh dang anh khong ho tro. Vui long chon PNG, JPG hoac JPEG.");
        return;
      }
      if (!file.exists() || !file.isFile() || !file.canRead()) {
        fail(onError, "Khong the doc file anh. Hay kiem tra quyen truy cap file.");
        return;
      }

      byte[] originalBytes = Files.readAllBytes(file.toPath());
      BufferedImage source = ImageIO.read(file);
      if (source == null) {
        fail(onError, "File da chon khong phai anh hop le hoac Java khong doc duoc dinh dang nay.");
        return;
      }

      ProcessedImage processed = prepareForUpload(source, originalBytes, ext);
      Platform.runLater(() -> buildPreview(processed, originalName, originalSize, onSuccess, onError));
    } catch (OutOfMemoryError e) {
      logError(file, e);
      fail(onError, "Anh qua lon de xu ly. Vui long chon anh nho hon hoac nen anh truoc.");
    } catch (Exception e) {
      logError(file, e);
      fail(onError, "Khong the doc file anh: " + safeMessage(e));
    }
  }

  private static ProcessedImage prepareForUpload(BufferedImage source, byte[] originalBytes,
      String originalExt) throws Exception {
    String normalizedExt = "png".equals(originalExt) ? "png" : "jpg";
    if (originalBytes.length <= MAX_UPLOAD_BYTES) {
      return new ProcessedImage(originalBytes, normalizedExt);
    }

    BufferedImage scaled = scaleDown(source, MAX_DIMENSION);
    for (float quality : JPEG_QUALITIES) {
      byte[] encoded = encodeJpeg(scaled, quality);
      if (encoded.length <= MAX_UPLOAD_BYTES) {
        return new ProcessedImage(encoded, "jpg");
      }
    }

    BufferedImage smaller = scaleDown(source, 1200);
    byte[] fallback = encodeJpeg(smaller, 0.6f);
    if (fallback.length > MAX_UPLOAD_BYTES) {
      throw new IllegalArgumentException(
          "Anh van vuot qua 2MB sau khi nen. Vui long chon anh nho hon.");
    }
    return new ProcessedImage(fallback, "jpg");
  }

  private static void buildPreview(ProcessedImage processed, String originalName, long originalSize,
      Consumer<ImageSelection> onSuccess, Consumer<String> onError) {
    try {
      Image preview = new Image(new ByteArrayInputStream(processed.bytes()));
      if (preview.isError()) {
        Throwable error = preview.getException();
        fail(onError, "JavaFX khong hien thi duoc anh nay: " + safeMessage(error));
        return;
      }
      String base64 = Base64.getEncoder().encodeToString(processed.bytes());
      log("Prepared image=%s original=%d final=%d ext=%s fxThread=%s", originalName,
          originalSize, processed.bytes().length, processed.extension(), Platform.isFxApplicationThread());
      if (onSuccess != null) {
        onSuccess.accept(new ImageSelection(base64, processed.extension(), preview,
            originalSize, processed.bytes().length));
      }
    } catch (Exception e) {
      logError(new File(originalName), e);
      fail(onError, "Khong the tao preview anh: " + safeMessage(e));
    }
  }

  private static BufferedImage scaleDown(BufferedImage source, int maxDimension) {
    int width = source.getWidth();
    int height = source.getHeight();
    int maxSide = Math.max(width, height);
    if (maxSide <= maxDimension) {
      return toRgb(source);
    }

    double scale = (double) maxDimension / maxSide;
    int targetWidth = Math.max(1, (int) Math.round(width * scale));
    int targetHeight = Math.max(1, (int) Math.round(height * scale));
    BufferedImage target = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
    Graphics2D g = target.createGraphics();
    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
    g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    g.drawImage(source, 0, 0, targetWidth, targetHeight, null);
    g.dispose();
    return target;
  }

  private static BufferedImage toRgb(BufferedImage source) {
    if (source.getType() == BufferedImage.TYPE_INT_RGB) {
      return source;
    }
    BufferedImage rgb = new BufferedImage(source.getWidth(), source.getHeight(),
        BufferedImage.TYPE_INT_RGB);
    Graphics2D g = rgb.createGraphics();
    g.drawImage(source, 0, 0, null);
    g.dispose();
    return rgb;
  }

  private static byte[] encodeJpeg(BufferedImage image, float quality) throws Exception {
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
    ImageWriteParam params = writer.getDefaultWriteParam();
    params.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
    params.setCompressionQuality(quality);
    try (ImageOutputStream imageOutput = ImageIO.createImageOutputStream(output)) {
      writer.setOutput(imageOutput);
      writer.write(null, new IIOImage(toRgb(image), null, null), params);
    } finally {
      writer.dispose();
    }
    return output.toByteArray();
  }

  private static String extensionOf(String fileName) {
    int dot = fileName.lastIndexOf('.');
    return dot >= 0 ? fileName.substring(dot + 1).toLowerCase(Locale.ROOT) : "";
  }

  private static boolean isSupportedExtension(String ext) {
    return "png".equals(ext) || "jpg".equals(ext) || "jpeg".equals(ext);
  }

  private static void fail(Consumer<String> onError, String message) {
    Platform.runLater(() -> {
      if (onError != null) {
        onError.accept(message);
      }
    });
  }

  private static void log(String format, Object... args) {
    System.out.println("[ImageUploadHelper] " + String.format(format, args));
  }

  private static void logError(File file, Throwable e) {
    System.err.println("[ImageUploadHelper] Loi xu ly anh file=" + file.getAbsolutePath()
        + " thread=" + Thread.currentThread().getName() + " message=" + safeMessage(e));
    e.printStackTrace();
  }

  private static String safeMessage(Throwable e) {
    if (e == null) {
      return "khong ro nguyen nhan";
    }
    return e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
  }

  private record ProcessedImage(byte[] bytes, String extension) {
  }

  public record ImageSelection(
      String base64,
      String extension,
      Image previewImage,
      long originalSize,
      long finalSize) {
  }
}
