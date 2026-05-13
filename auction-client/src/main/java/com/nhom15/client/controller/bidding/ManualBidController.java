package com.nhom15.client.controller.bidding;

import com.google.gson.JsonObject;
import com.nhom15.client.command.PlaceBidCommand;
import com.nhom15.client.command.ServerCommand;
import com.nhom15.client.util.SessionManager;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.util.Duration;

/**
 * ManualBidController — quản lý TAB ĐẶT GIÁ THỦ CÔNG:
 * - Nhập giá, validate, gọi PlaceBidCommand
 * - Nút quick-bid (+1/+3/+5/+10 bước)
 * - Slider kéo chọn mức giá
 * - Hiển thị lỗi / thành công
 *
 * Callback onBidPlaced báo lên BiddingRoomController để cập nhật chart và history.
 */
public class ManualBidController {

    // ── FXML refs ─────────────────────────────────────────────────────────
    @FXML private TextField txtBidAmount;
    @FXML private Label     lblMinBid;
    @FXML private Label     lblBidError;
    @FXML private Label     lblBidStatus;
    @FXML private Button    btnPlaceBid;
    @FXML private Button    btnQuick1;
    @FXML private Button    btnQuick2;
    @FXML private Button    btnQuick3;
    @FXML private Button    btnQuick4;
    @FXML private Slider    bidSlider;
    @FXML private Label     lblSliderVal;
    @FXML private CheckBox  chkConfirmBid;

    // ── Dependencies ───────────────────────────────────────────────────────
    private AuctionState state;

    /** Callback: được gọi sau khi đặt giá thành công, truyền số tiền đã đặt. */
    private java.util.function.DoubleConsumer onBidPlaced;

    // ── Init ──────────────────────────────────────────────────────────────

    @FXML
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

    /** Gọi sau mỗi lần currentPrice / minStep thay đổi để cập nhật nhãn nút. */
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

        // Cập nhật slider range
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

    // ── FXML handlers ─────────────────────────────────────────────────────

    @FXML
    private void handlePlaceBid() {
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

        // Confirm check
        if (chkConfirmBid != null && chkConfirmBid.isSelected()) {
            // Đã có checkbox xác nhận, tiếp tục
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

    @FXML private void handleQuick1() { fillQuick(1); }
    @FXML private void handleQuick2() { fillQuick(3); }
    @FXML private void handleQuick3() { fillQuick(5); }
    @FXML private void handleQuick4() { fillQuick(10); }

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
