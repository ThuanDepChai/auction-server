package com.nhom15.client.controller.bidding;

import com.google.gson.JsonObject;
import com.nhom15.client.command.AutoBidCommand;
import com.nhom15.client.command.GetAutoBidStatusCommand;
import com.nhom15.client.command.ServerCommand;
import com.nhom15.client.util.SessionManager;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.util.Duration;

/**
 * AutoBidController — quản lý TAB AUTO-BID.
 *
 * FIX: Không dùng @FXML nữa. Nodes được inject qua setNodes().
 * handleSetAutoBid() và handleCancelAutoBid() đổi thành public
 * để BiddingRoomController delegate được.
 * initialize() được gọi thủ công từ BiddingRoomController sau setNodes().
 */
public class AutoBidController {

    private TextField        txtMaxBid;
    private TextField        txtIncrement;
    private Button           btnSetAutoBid;
    private Button           btnCancelAutoBid;
    private Label            lblAutoBidStatus;
    private Label            lblAutoBidInfo;
    private ComboBox<String> cmbStrategy;
    private Slider           autoDelaySlider;
    private Label            lblAutoDelay;

    private AuctionState state;

    // ── Inject thủ công từ BiddingRoomController ──────────────────────────

    public void setNodes(
            TextField txtMaxBid, TextField txtIncrement,
            Button btnSetAutoBid, Button btnCancelAutoBid,
            Label lblAutoBidStatus, Label lblAutoBidInfo,
            ComboBox<String> cmbStrategy,
            Slider autoDelaySlider, Label lblAutoDelay) {

        this.txtMaxBid        = txtMaxBid;
        this.txtIncrement     = txtIncrement;
        this.btnSetAutoBid    = btnSetAutoBid;
        this.btnCancelAutoBid = btnCancelAutoBid;
        this.lblAutoBidStatus = lblAutoBidStatus;
        this.lblAutoBidInfo   = lblAutoBidInfo;
        this.cmbStrategy      = cmbStrategy;
        this.autoDelaySlider  = autoDelaySlider;
        this.lblAutoDelay     = lblAutoDelay;
    }

    // ── initialize() — gọi thủ công sau setNodes() ────────────────────────

    public void initialize() {
        bindLabelVisible(lblAutoBidStatus);
        setupDelaySlider();
    }

    public void setup(AuctionState state) {
        this.state = state;
    }

    public void loadStatus() {
        if (!SessionManager.isBidder() && !SessionManager.isSeller()) return;
        new GetAutoBidStatusCommand(state.getAuctionId(), SessionManager.getUserId())
                .executeAsync(res -> {
                    if (ServerCommand.isSuccess(res) && res.has("autoBid")) {
                        JsonObject ab = res.getAsJsonObject("autoBid");
                        boolean active = ab.has("active") && ab.get("active").getAsBoolean();
                        state.setAutoBidActive(active);
                        updateInfoDisplay(active, ab);
                    }
                });
    }

    public void disableAll() {
        if (btnSetAutoBid    != null) btnSetAutoBid.setDisable(true);
        if (btnCancelAutoBid != null) btnCancelAutoBid.setDisable(true);
        if (txtMaxBid        != null) txtMaxBid.setDisable(true);
        if (txtIncrement     != null) txtIncrement.setDisable(true);
    }

    // ── Handlers — public để BiddingRoomController delegate ──────────────

    public void handleSetAutoBid() {
        if (!SessionManager.isBidder() && !SessionManager.isSeller()) {
            showStatus("⛔ Vui lòng đăng nhập!", false); return;
        }
        String rawMax = txtMaxBid.getText().trim().replaceAll("[^0-9]", "");
        String rawInc = txtIncrement.getText().trim().replaceAll("[^0-9]", "");
        if (rawMax.isEmpty() || rawInc.isEmpty()) {
            showStatus("⚠️ Nhập đầy đủ Giá tối đa và Bước giá!", false); return;
        }
        double maxBid    = Double.parseDouble(rawMax);
        double increment = Double.parseDouble(rawInc);

        if (maxBid <= state.getCurrentPrice()) {
            showStatus("⚠️ Giá tối đa phải > " +
                    String.format("%,.0fđ", state.getCurrentPrice()) + "!", false); return;
        }
        if (increment < state.getMinStep()) {
            showStatus("⚠️ Bước tối thiểu " +
                    String.format("%,.0fđ", state.getMinStep()) + "!", false); return;
        }

        if (btnSetAutoBid != null) btnSetAutoBid.setDisable(true);
        new AutoBidCommand(state.getAuctionId(), SessionManager.getUserId(), maxBid, increment)
                .executeAsync(
                        res -> {
                            if (btnSetAutoBid != null) btnSetAutoBid.setDisable(false);
                            if (ServerCommand.isSuccess(res)) {
                                state.setAutoBidActive(true);
                                showStatus("✅ Auto-Bid đã kích hoạt!", true);
                                JsonObject ab = new JsonObject();
                                ab.addProperty("active",    true);
                                ab.addProperty("maxBid",    maxBid);
                                ab.addProperty("increment", increment);
                                updateInfoDisplay(true, ab);
                                if (txtMaxBid    != null) txtMaxBid.clear();
                                if (txtIncrement != null) txtIncrement.clear();
                            } else {
                                showStatus(ServerCommand.getMessage(res,
                                        "❌ Không thể kích hoạt Auto-Bid!"), false);
                            }
                        },
                        () -> {
                            if (btnSetAutoBid != null) btnSetAutoBid.setDisable(false);
                            showStatus("🔌 Lỗi kết nối!", false);
                        }
                );
    }

    public void handleCancelAutoBid() {
        if (!state.isAutoBidActive()) return;
        if (btnCancelAutoBid != null) btnCancelAutoBid.setDisable(true);
        new AutoBidCommand(state.getAuctionId(), SessionManager.getUserId())
                .executeAsync(
                        res -> {
                            if (ServerCommand.isSuccess(res)) {
                                state.setAutoBidActive(false);
                                showStatus("Auto-Bid đã bị huỷ.", false);
                                JsonObject ab = new JsonObject();
                                ab.addProperty("active", false);
                                updateInfoDisplay(false, ab);
                            } else {
                                if (btnCancelAutoBid != null) btnCancelAutoBid.setDisable(false);
                                showStatus(ServerCommand.getMessage(res, "❌ Không thể huỷ!"), false);
                            }
                        },
                        () -> {
                            if (btnCancelAutoBid != null) btnCancelAutoBid.setDisable(false);
                            showStatus("🔌 Lỗi kết nối!", false);
                        }
                );
    }

    // ── Private helpers ───────────────────────────────────────────────────

    private void updateInfoDisplay(boolean active, JsonObject ab) {
        if (lblAutoBidInfo != null) {
            if (active) {
                double maxBid = ab.has("maxBid")    ? ab.get("maxBid").getAsDouble()    : 0;
                double inc    = ab.has("increment") ? ab.get("increment").getAsDouble() : 0;
                lblAutoBidInfo.setText("✅ Auto-Bid đang BẬT\n" +
                        "Giá tối đa: " + String.format("%,.0fđ", maxBid) + "\n" +
                        "Bước tăng: " + String.format("%,.0fđ", inc));
                lblAutoBidInfo.setStyle("-fx-text-fill:#27AE60;-fx-font-size:12px;");
            } else {
                lblAutoBidInfo.setText("❌ Auto-Bid đang TẮT\nHệ thống sẽ không tự đặt giá.");
                lblAutoBidInfo.setStyle("-fx-text-fill:#888;-fx-font-size:12px;");
            }
        }
        if (btnCancelAutoBid != null) btnCancelAutoBid.setDisable(!active);
    }

    private void showStatus(String msg, boolean success) {
        if (lblAutoBidStatus == null) return;
        lblAutoBidStatus.setText(msg);
        lblAutoBidStatus.setStyle("-fx-font-size:12px;-fx-text-fill:" +
                (success ? "#27AE60" : "#D96570") + ";");
        lblAutoBidStatus.setVisible(true);
        new Timeline(new KeyFrame(Duration.seconds(4),
                e -> lblAutoBidStatus.setVisible(false))).play();
    }

    private void setupDelaySlider() {
        if (autoDelaySlider == null) return;
        autoDelaySlider.valueProperty().addListener((obs, o, n) -> {
            if (lblAutoDelay != null)
                lblAutoDelay.setText((int) n.doubleValue() + " giây");
        });
    }

    private void bindLabelVisible(Label lbl) {
        if (lbl != null) {
            lbl.managedProperty().bind(lbl.visibleProperty());
            lbl.setVisible(false);
        }
    }
}