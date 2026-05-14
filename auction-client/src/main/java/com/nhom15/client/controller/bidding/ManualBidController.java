package com.nhom15.client.controller.bidding;

import com.google.gson.JsonObject;
import com.nhom15.client.command.PlaceBidCommand;
import com.nhom15.client.command.ServerCommand;
import com.nhom15.client.util.SessionManager;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.util.Duration;

/**
 * ManualBidController — quản lý TAB ĐẶT GIÁ THỦ CÔNG.
 *
 * FIX: Không dùng @FXML nữa. Nodes được inject qua setNodes().
 * handlePlaceBid() đổi thành public để BiddingRoomController delegate được.
 * initialize() được gọi thủ công từ BiddingRoomController sau setNodes().
 */
public class ManualBidController {

    private TextField txtBidAmount;
    private Label     lblMinBid;
    private Label     lblBidError;
    private Label     lblBidStatus;
    private Button    btnPlaceBid;
    private Button    btnQuick1;
    private Button    btnQuick2;
    private Button    btnQuick3;
    private Button    btnQuick4;
    private Slider    bidSlider;
    private Label     lblSliderVal;
    private CheckBox  chkConfirmBid;

    private AuctionState state;
    private java.util.function.DoubleConsumer onBidPlaced;

    // ── Inject thủ công từ BiddingRoomController ──────────────────────────

    public void setNodes(
            TextField txtBidAmount, Label lblMinBid,
            Label lblBidError, Label lblBidStatus,
            Button btnPlaceBid,
            Button btnQuick1, Button btnQuick2,
            Button btnQuick3, Button btnQuick4,
            Slider bidSlider, Label lblSliderVal,
            CheckBox chkConfirmBid) {

        this.txtBidAmount  = txtBidAmount;
        this.lblMinBid     = lblMinBid;
        this.lblBidError   = lblBidError;
        this.lblBidStatus  = lblBidStatus;
        this.btnPlaceBid   = btnPlaceBid;
        this.btnQuick1     = btnQuick1;
        this.btnQuick2     = btnQuick2;
        this.btnQuick3     = btnQuick3;
        this.btnQuick4     = btnQuick4;
        this.bidSlider     = bidSlider;
        this.lblSliderVal  = lblSliderVal;
        this.chkConfirmBid = chkConfirmBid;
    }

    // ── initialize() — gọi thủ công sau setNodes() ────────────────────────

    public void initialize() {
        bindLabelVisible(lblBidError);
        bindLabelVisible(lblBidStatus);
    }

    public void setup(AuctionState state, java.util.function.DoubleConsumer onBidPlaced) {
        this.state       = state;
        this.onBidPlaced = onBidPlaced;
        setupSlider();
        setupQuickBidButtons();
        refreshLabels();
    }

    public void refreshLabels() {
        if (state == null || btnQuick1 == null) return;
        double p = state.getCurrentPrice(), s = state.getMinStep();
        btnQuick1.setText("+1 bước  " + formatShort(p + s));
        btnQuick2.setText("+3 bước  " + formatShort(p + s * 3));
        btnQuick3.setText("+5 bước  " + formatShort(p + s * 5));
        if (btnQuick4 != null) btnQuick4.setText("+10 bước  " + formatShort(p + s * 10));
        if (lblMinBid != null)
            lblMinBid.setText("Tối thiểu: " + String.format("%,.0fđ", p + s) +
                    "  (bước: " + String.format("%,.0f", s) + "đ)");
        if (bidSlider != null) {
            bidSlider.setMin(p + s);
            bidSlider.setMax(p + s * 20);
        }
    }

    public void disableAll() {
        if (btnPlaceBid  != null) btnPlaceBid.setDisable(true);
        if (txtBidAmount != null) txtBidAmount.setDisable(true);
        if (btnQuick1    != null) { btnQuick1.setDisable(true); btnQuick2.setDisable(true);
            btnQuick3.setDisable(true); }
        if (btnQuick4    != null) btnQuick4.setDisable(true);
        if (bidSlider    != null) bidSlider.setDisable(true);
    }

    // ── Handler — public để BiddingRoomController delegate ───────────────

    public void handlePlaceBid() {
        if (!SessionManager.isBidder() && !SessionManager.isSeller()) {
            showError("⛔ Vui lòng đăng nhập để đặt giá!"); return;
        }
        hideFeedback();

        String raw = txtBidAmount.getText().trim().replaceAll("[^0-9]", "");
        if (raw.isEmpty()) { showError("⚠️ Vui lòng nhập số tiền!"); return; }

        double amount = Double.parseDouble(raw);
        if (amount < state.nextMinBid()) {
            showError(String.format("⚠️ Giá phải ít nhất %,.0fđ!", state.nextMinBid())); return;
        }

        setLoading(true);
        new PlaceBidCommand(state.getAuctionId(), SessionManager.getUserId(), amount)
                .executeAsync(
                        res -> {
                            setLoading(false);
                            if (ServerCommand.isSuccess(res)) {
                                if (txtBidAmount != null) txtBidAmount.clear();
                                showSuccess("✓ Đặt giá thành công!");
                                if (onBidPlaced != null) onBidPlaced.accept(amount);
                            } else {
                                showError(ServerCommand.getMessage(res, "❌ Đặt giá thất bại!"));
                            }
                        },
                        () -> {
                            setLoading(false);
                            showError("🔌 Lỗi kết nối đến server!");
                        }
                );
    }

    // ── Private helpers ───────────────────────────────────────────────────

    private void setupSlider() {
        if (bidSlider == null) return;
        bidSlider.valueProperty().addListener((obs, o, n) -> {
            double val = n.doubleValue();
            if (lblSliderVal  != null) lblSliderVal.setText(String.format("%,.0fđ", val));
            if (txtBidAmount  != null) txtBidAmount.setText(String.format("%.0f", val));
        });
    }

    private void setupQuickBidButtons() {
        if (btnQuick1 == null) return;
        btnQuick1.setOnAction(e -> fillQuick(1));
        btnQuick2.setOnAction(e -> fillQuick(3));
        btnQuick3.setOnAction(e -> fillQuick(5));
        if (btnQuick4 != null) btnQuick4.setOnAction(e -> fillQuick(10));
    }

    private void fillQuick(int mult) {
        if (state == null || txtBidAmount == null) return;
        txtBidAmount.setText(String.format("%.0f",
                state.getCurrentPrice() + state.getMinStep() * mult));
    }

    private void setLoading(boolean loading) {
        if (btnPlaceBid == null) return;
        btnPlaceBid.setDisable(loading);
        btnPlaceBid.setText(loading ? "⏳ Đang xử lý..." : "🔨  ĐẶT GIÁ NGAY");
    }

    private void showError(String msg) {
        if (lblBidError == null) return;
        lblBidError.setText(msg); lblBidError.setVisible(true);
        if (lblBidStatus != null) lblBidStatus.setVisible(false);
        autoHide(lblBidError, 4);
    }

    private void showSuccess(String msg) {
        if (lblBidStatus == null) return;
        lblBidStatus.setText(msg); lblBidStatus.setVisible(true);
        if (lblBidError != null) lblBidError.setVisible(false);
        autoHide(lblBidStatus, 3);
    }

    private void hideFeedback() {
        if (lblBidError  != null) lblBidError.setVisible(false);
        if (lblBidStatus != null) lblBidStatus.setVisible(false);
    }

    private void autoHide(Label lbl, int secs) {
        new Timeline(new KeyFrame(Duration.seconds(secs), e -> lbl.setVisible(false))).play();
    }

    private void bindLabelVisible(Label lbl) {
        if (lbl != null) {
            lbl.managedProperty().bind(lbl.visibleProperty());
            lbl.setVisible(false);
        }
    }

    private String formatShort(double v) {
        if (v >= 1_000_000_000) return String.format("%.1fT", v / 1_000_000_000);
        if (v >= 1_000_000)     return String.format("%.0fM", v / 1_000_000);
        if (v >= 1_000)         return String.format("%.0fK", v / 1_000);
        return String.format("%.0f", v);
    }
}