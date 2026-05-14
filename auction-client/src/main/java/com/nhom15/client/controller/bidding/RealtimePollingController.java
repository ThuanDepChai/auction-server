package com.nhom15.client.controller.bidding;

import com.google.gson.JsonObject;
import com.nhom15.client.command.GetAuctionDetailCommand;
import com.nhom15.client.command.ServerCommand;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;

/**
 * RealtimePollingController — chịu trách nhiệm duy nhất:
 * Định kỳ gọi GetAuctionDetailCommand mỗi 3 giây và thông báo
 * kết quả về BiddingRoomController qua callback.
 *
 * <p>Ngoài ra hỗ trợ {@link #notifyBidResult} để BiddingRoomController
 * báo kết quả bid tức thì (kèm newEndTime nếu anti-sniping đã gia hạn).
 *
 * <p>Không giữ tham chiếu đến bất kỳ UI node nào — hoàn toàn tách rời UI.
 */
public class RealtimePollingController {

    private static final int POLL_INTERVAL_SEC = 3;

    private AuctionState state;
    private Timeline     poller;

    /** Callback: gọi khi giá thay đổi, truyền (newPrice, oldPrice). */
    private PriceChangeListener onPriceChanged;

    /** Callback: gọi khi auction ENDED hoặc CANCELLED. */
    private Runnable onAuctionEnded;

    /** Callback: gọi khi nhận được dữ liệu mới (leader, totalBids). */
    private UpdateListener onUpdate;

    /**
     * Callback: gọi khi end_time thay đổi (anti-sniping gia hạn).
     * Truyền chuỗi endTime mới để UI cập nhật bộ đếm ngược.
     */
    private EndTimeChangedListener onEndTimeChanged;

    // ── Interfaces ────────────────────────────────────────────────────────

    @FunctionalInterface
    public interface PriceChangeListener {
        void onChanged(double newPrice, double oldPrice);
    }

    @FunctionalInterface
    public interface UpdateListener {
        void onUpdate(String leader, int totalBids);
    }

    @FunctionalInterface
    public interface EndTimeChangedListener {
        void onChanged(String newEndTime);
    }

    // ── Setup ─────────────────────────────────────────────────────────────

    public void setup(AuctionState state,
                      PriceChangeListener onPriceChanged,
                      UpdateListener onUpdate,
                      Runnable onAuctionEnded) {
        this.state          = state;
        this.onPriceChanged = onPriceChanged;
        this.onUpdate       = onUpdate;
        this.onAuctionEnded = onAuctionEnded;
    }

    public void setOnEndTimeChanged(EndTimeChangedListener listener) {
        this.onEndTimeChanged = listener;
    }

    // ── Start / Stop ──────────────────────────────────────────────────────

    public void start() {
        stop();
        poller = new Timeline(new KeyFrame(Duration.seconds(POLL_INTERVAL_SEC),
                e -> poll()));
        poller.setCycleCount(Timeline.INDEFINITE);
        poller.play();
    }

    public void stop() {
        if (poller != null) { poller.stop(); poller = null; }
    }

    /**
     * Gọi ngay sau khi PLACE_BID thành công — xử lý newEndTime (anti-sniping)
     * và newPrice (có thể đã bị auto-bid đẩy lên) từ response của server.
     *
     * <p>Gọi từ ManualBidController / AutoBidController thông qua BiddingRoomController.
     */
    public void notifyBidResult(JsonObject serverResponse) {
        if (serverResponse == null) return;

        // Cập nhật giá mới nhất (server trả về sau khi auto-bid đã chạy xong)
        if (serverResponse.has("currentPrice")) {
            double serverPrice = serverResponse.get("currentPrice").getAsDouble();
            if (serverPrice != state.getCurrentPrice()) {
                double old = state.getCurrentPrice();
                state.setCurrentPrice(serverPrice);
                if (onPriceChanged != null) onPriceChanged.onChanged(serverPrice, old);
            }
        }

        // Anti-sniping: server gia hạn → cập nhật countdown ngay, không chờ poll tiếp theo
        if (serverResponse.has("newEndTime") && onEndTimeChanged != null) {
            String newEndTime = serverResponse.get("newEndTime").getAsString();
            if (!newEndTime.equals(state.getEndTime())) {
                state.setEndTime(newEndTime);
                onEndTimeChanged.onChanged(newEndTime);
            }
        }
    }

    // ── Private: Poll ─────────────────────────────────────────────────────

    private void poll() {
        new GetAuctionDetailCommand(state.getAuctionId()).executeAsync(res -> {
            if (!ServerCommand.isSuccess(res) || !res.has("auction")) return;
            JsonObject a = res.getAsJsonObject("auction");

            double newPrice = a.has("currentPrice")
                    ? a.get("currentPrice").getAsDouble()
                    : state.getCurrentPrice();
            String status = str(a, "status", "ACTIVE");

            if (newPrice != state.getCurrentPrice()) {
                double old = state.getCurrentPrice();
                state.setCurrentPrice(newPrice);
                if (onPriceChanged != null) onPriceChanged.onChanged(newPrice, old);
            }

            // Cập nhật end_time nếu server đã gia hạn (anti-sniping)
            if (a.has("endTime") && onEndTimeChanged != null) {
                String serverEndTime = a.get("endTime").getAsString();
                if (!serverEndTime.equals(state.getEndTime())) {
                    state.setEndTime(serverEndTime);
                    onEndTimeChanged.onChanged(serverEndTime);
                }
            }

            if ("ENDED".equals(status) || "CANCELLED".equals(status)) {
                state.setAuctionEnded(true);
                stop();
                if (onAuctionEnded != null) onAuctionEnded.run();
            }

            String leader    = str(a, "leadingBidder", "");
            int    totalBids = a.has("totalBids") ? a.get("totalBids").getAsInt() : 0;
            if (onUpdate != null) onUpdate.onUpdate(leader, totalBids);
        });
    }

    private String str(JsonObject o, String key, String def) {
        return (o != null && o.has(key) && !o.get(key).isJsonNull())
                ? o.get(key).getAsString() : def;
    }
}