package com.nhom15.client.controller.bidding;

import com.google.gson.JsonObject;
import com.nhom15.client.command.GetItemImageCommand;
import com.nhom15.client.command.ServerCommand;
import java.io.ByteArrayInputStream;
import java.util.Base64;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

/**
 * ProductPanelController — quản lý panel SẢN PHẨM bên trái.
 *
 * FIX: Không dùng @FXML nữa vì controller được khởi tạo bằng `new`
 * (không qua FXMLLoader). Tất cả node được nhận qua setNodes().
 */
public class ProductPanelController {

    private ImageView  imgProduct;
    private Label      lblImgPlaceholder;
    private Label      lblProductName;
    private Label      lblCategory;
    private Label      lblCondition;
    private Label      lblDescription;
    private Label      lblSeller;
    private Label      lblAvatarInitial;
    private Circle     avatarCircle;
    private Label      lblStartPrice;
    private ProgressBar progressReserve;
    private Label      lblReserveHint;

    // ── Inject thủ công từ BiddingRoomController ──────────────────────────

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
        setText(lblProductName, str(auction, "name",        "N/A"));
        setText(lblCategory,    str(auction, "category",    "Chung"));
        setText(lblDescription, str(auction, "description", "Không có mô tả."));

        double startPrice = dbl(auction, "startPrice", 0);
        setText(lblStartPrice, String.format("%,.0fđ", startPrice));

        String seller = str(auction, "sellerUsername", str(auction, "seller", "N/A"));
        setText(lblSeller, seller);
        if (lblAvatarInitial != null && !seller.isEmpty())
            lblAvatarInitial.setText(String.valueOf(Character.toUpperCase(seller.charAt(0))));

        String condition = str(auction, "condition", "Mới 100%");
        setText(lblCondition, condition);

        String imageId = str(auction, "imageId", null);
        if (imageId != null) loadImage(imageId);
    }

    // ── Image loading ─────────────────────────────────────────────────────

    private void loadImage(String imagePath) {
        System.out.println("🖼️ [ProductPanel] Load ảnh: " + imagePath);
        new GetItemImageCommand(imagePath).executeAsync(
                res -> applyImage(res, imagePath, false),
                ()  -> System.err.println("🔌 [ProductPanel] Lỗi kết nối khi load ảnh!")
        );
    }

    private void applyImage(JsonObject res, String imagePath, boolean isRetry) {
        if (ServerCommand.isSuccess(res) && res.has("imageBase64")) {
            try {
                byte[] bytes = Base64.getDecoder().decode(res.get("imageBase64").getAsString());
                Image img = new Image(new ByteArrayInputStream(bytes));
                if (!img.isError()) {
                    if (imgProduct        != null) imgProduct.setImage(img);
                    if (lblImgPlaceholder != null) lblImgPlaceholder.setVisible(false);
                    return;
                }
            } catch (Exception e) {
                System.err.println("❌ [ProductPanel] Lỗi decode ảnh: " + e.getMessage());
            }
        }
        if (!isRetry) {
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