package com.nhom15.client.controller.bidding;

import com.google.gson.JsonObject;
import com.nhom15.client.command.GetItemImageCommand;
import com.nhom15.client.command.ServerCommand;
import com.nhom15.client.util.ImageCache;
import java.io.ByteArrayInputStream;
import java.util.Base64;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Circle;
import javafx.util.Duration;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.geometry.Insets;
import com.google.gson.JsonParser;
import com.google.gson.JsonElement;

/**
 * ProductPanelController — panel SẢN PHẨM bên trái.
 *
 * FIX:
 *  - applyImage(): thêm Platform.runLater() khi set ảnh lên ImageView
 *    (callback từ executeAsync chạy trên FX thread nhưng setImage vẫn cần guard)
 *  - loadImage(): log rõ hơn để debug khi imageId null hoặc rỗng
 *  - populate(): guard imageId null/blank trước khi gọi loadImage
 */
public class ProductPanelController {

    private ImageView   imgProduct;
    private Label       lblImgPlaceholder;
    private Label       lblProductName;
    private Label       lblCategory;
    private Label       lblCondition;
    private Label       lblDescription;
    private Label       lblSeller;
    private Label       lblAvatarInitial;
    private Circle      avatarCircle;
    private Label       lblStartPrice;
    private ProgressBar progressReserve;
    private Label       lblReserveHint;
    private GridPane    gridSpecs;

    // ── Inject ────────────────────────────────────────────────────────────

    public void setGridSpecs(GridPane gridSpecs) {
        this.gridSpecs = gridSpecs;
    }

    public void setNodes(
            ImageView imgProduct, Label lblImgPlaceholder,
            Label lblProductName, Label lblCategory, Label lblCondition,
            Label lblDescription, Label lblSeller, Label lblAvatarInitial,
            Circle avatarCircle, Label lblStartPrice,
            ProgressBar progressReserve, Label lblReserveHint) {

        this.imgProduct        = imgProduct;
        this.lblImgPlaceholder = lblImgPlaceholder;
        this.lblProductName    = lblProductName;
        this.lblCategory       = lblCategory;
        this.lblCondition      = lblCondition;
        this.lblDescription    = lblDescription;
        this.lblSeller         = lblSeller;
        this.lblAvatarInitial  = lblAvatarInitial;
        this.avatarCircle      = avatarCircle;
        this.lblStartPrice     = lblStartPrice;
        this.progressReserve   = progressReserve;
        this.lblReserveHint    = lblReserveHint;
    }

    // ── Populate ─────────────────────────────────────────────────────────

    public void populate(JsonObject auction) {
        setText(lblProductName,  str(auction, "name",        "N/A"));
        setText(lblCategory,     str(auction, "category",    "Chung"));
        setText(lblDescription,  str(auction, "description", "Không có mô tả."));

        double startPrice = dbl(auction, "startPrice", 0);
        setText(lblStartPrice, String.format("%,.0fđ", startPrice));

        String seller = str(auction, "sellerUsername", str(auction, "seller", "N/A"));
        setText(lblSeller, seller);
        if (lblAvatarInitial != null && !seller.isEmpty() && !seller.equals("N/A"))
            lblAvatarInitial.setText(String.valueOf(Character.toUpperCase(seller.charAt(0))));

        String condition = str(auction, "condition", "Mới 100%");
        setText(lblCondition, condition);

        // FIX: kiểm tra cả null lẫn blank
        String imageId = str(auction, "imageId", null);
        if (imageId != null && !imageId.isBlank()) {
            loadImage(imageId);
        } else {
            // Thử field tên khác mà server có thể trả
            String imagePath = str(auction, "imagePath", str(auction, "image", null));
            if (imagePath != null && !imagePath.isBlank()) {
                loadImage(imagePath);
            } else {
                System.out.println("ℹ️ [ProductPanel] Không có imageId, giữ placeholder.");
            }
        }

        if (gridSpecs != null) {
            gridSpecs.getChildren().clear();
            String extraInfoStr = str(auction, "extraInfo", "");
            if (!extraInfoStr.isEmpty()) {
                try {
                    JsonObject extraInfo = JsonParser.parseString(extraInfoStr).getAsJsonObject();
                    int col = 0, row = 0;
                    for (String key : extraInfo.keySet()) {
                        String value = extraInfo.get(key).getAsString();
                        if (value == null || value.isBlank()) continue;
                        
                        String labelTxt = translateKey(key);
                        VBox vbox = new VBox(3);
                        vbox.setStyle("-fx-background-color:#F4F7FC;-fx-background-radius:10;");
                        vbox.setPadding(new Insets(11, 13, 11, 13));
                        
                        Label lblKey = new Label(labelTxt);
                        lblKey.setStyle("-fx-text-fill:#888888;-fx-font-size:11;");
                        Label lblVal = new Label(value);
                        lblVal.setStyle("-fx-text-fill:#202124;-fx-font-weight:bold;-fx-font-size:14;");
                        
                        vbox.getChildren().addAll(lblKey, lblVal);
                        gridSpecs.add(vbox, col, row);
                        
                        col++;
                        if (col > 1) {
                            col = 0;
                            row++;
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Lỗi parse extraInfo: " + e.getMessage());
                }
            }
        }
    }

    private String translateKey(String key) {
        switch (key) {
            case "brand": return "Thương hiệu";
            case "size": return "Kích cỡ (Size)";
            case "color": return "Màu sắc";
            case "year": return "Năm sản xuất";
            case "mileage": return "Số km đã đi";
            case "artist": return "Tác giả / Nghệ sĩ";
            case "sportType": return "Môn thể thao";
            case "condition": return "Tình trạng";
            default: return key.substring(0, 1).toUpperCase() + key.substring(1);
        }
    }

    // ── Image loading ─────────────────────────────────────────────────────

    private void loadImage(String imagePath) {
        // FIX: Kiểm tra ImageCache trước — nếu ảnh đã tải thì apply ngay, không cần TCP
        String cached = ImageCache.get(imagePath);
        if (cached != null) {
            applyImage(buildFakeSuccess(cached), imagePath, false);
            return;
        }
        System.out.println("[ProductPanel] Gửi yêu cầu load ảnh: " + imagePath);
        new GetItemImageCommand(imagePath).executeAsync(
                res -> {
                    if (ServerCommand.isSuccess(res) && res.has("imageBase64")) {
                        ImageCache.put(imagePath, res.get("imageBase64").getAsString());
                    }
                    applyImage(res, imagePath, false);
                },
                ()  -> System.err.println("[ProductPanel] Loi ket noi khi load anh: " + imagePath)
        );
    }

    /** Tạo JsonObject giả SUCCESS để tái dùng applyImage() khi lấy dữ liệu từ cache. */
    private static JsonObject buildFakeSuccess(String base64) {
        JsonObject o = new JsonObject();
        o.addProperty("status", "SUCCESS");
        o.addProperty("imageBase64", base64);
        return o;
    }

    /**
     * FIX: Bọc toàn bộ logic set ảnh trong Platform.runLater() để đảm bảo
     * chạy đúng trên JavaFX Application Thread, tránh IllegalStateException.
     */
    private void applyImage(JsonObject res, String imagePath, boolean isRetry) {
        Platform.runLater(() -> {
            if (ServerCommand.isSuccess(res) && res.has("imageBase64")) {
                try {
                    String b64 = res.get("imageBase64").getAsString();
                    if (b64 == null || b64.isBlank()) {
                        System.err.println("❌ [ProductPanel] imageBase64 rỗng.");
                        scheduleRetry(imagePath, isRetry);
                        return;
                    }
                    byte[] bytes = Base64.getDecoder().decode(b64.trim());
                    Image img = new Image(new ByteArrayInputStream(bytes));
                    if (!img.isError()) {
                        if (imgProduct        != null) {
                            imgProduct.setImage(img);
                            imgProduct.setVisible(true);
                        }
                        if (lblImgPlaceholder != null) lblImgPlaceholder.setVisible(false);
                        System.out.println("✅ [ProductPanel] Load ảnh thành công.");
                        return;
                    } else {
                        System.err.println("❌ [ProductPanel] Image lỗi sau decode: " + img.getException());
                    }
                } catch (Exception e) {
                    System.err.println("❌ [ProductPanel] Lỗi decode ảnh: " + e.getMessage());
                }
            } else {
                System.err.println("❌ [ProductPanel] Server không trả imageBase64. Response: " + res);
            }
            scheduleRetry(imagePath, isRetry);
        });
    }

    private void scheduleRetry(String imagePath, boolean isRetry) {
        if (!isRetry) {
            System.out.println("🔄 [ProductPanel] Retry sau 2 giây...");
            new Timeline(new KeyFrame(Duration.seconds(2), e ->
                    new GetItemImageCommand(imagePath).executeAsync(
                            retryRes -> applyImage(retryRes, imagePath, true)
                    )
            )).play();
        } else {
            System.err.println("❌ [ProductPanel] Retry thất bại, giữ placeholder.");
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private void setText(Label lbl, String val) {
        if (lbl != null) lbl.setText(val);
    }

    private String str(JsonObject o, String key, String def) {
        return (o != null && o.has(key) && !o.get(key).isJsonNull())
                ? o.get(key).getAsString() : def;
    }

    private double dbl(JsonObject o, String key, double def) {
        return (o != null && o.has(key)) ? o.get(key).getAsDouble() : def;
    }
}