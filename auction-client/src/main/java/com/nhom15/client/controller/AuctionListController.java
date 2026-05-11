package com.nhom15.client.controller;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.nhom15.client.command.GetActiveAuctionsCommand;
import com.nhom15.client.command.ServerCommand;
import com.nhom15.client.util.CardFactory;
import com.nhom15.client.util.ViewManager;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.ArrayList;
import java.util.List;

public class AuctionListController {

    @FXML private FlowPane         flowAuctions;
    @FXML private TextField        txtSearch;
    @FXML private ComboBox<String> cmbCategory;
    @FXML private Label            lblCount;
    @FXML private Button           btnFilterAll;
    @FXML private Button           btnFilterActive;
    @FXML private Button           btnFilterEnded;

    private final List<Timeline> countdownTimers = new ArrayList<>();
    private JsonArray            cachedAll        = new JsonArray();
    private String               activeFilter     = "ALL";

    @FXML
    public void initialize() {
        cmbCategory.getItems().setAll(
                "Tất cả danh mục", "Điện tử", "Thời trang",
                "Nhà cửa & Sân vườn", "Đồ sưu tầm", "Thể thao");
        cmbCategory.setValue("Tất cả danh mục");
        loadAuctions();
    }

    // ── Load ─────────────────────────────────────────────────────────────────

    public void loadAuctions() {
        showLoading();
        new GetActiveAuctionsCommand().executeAsync(
                res -> {
                    if (ServerCommand.isSuccess(res) && res.has("auctions")) {
                        cachedAll = res.getAsJsonArray("auctions");
                        applyFilter();
                    } else {
                        showEmpty("Không thể tải danh sách đấu giá");
                    }
                },
                () -> showEmpty("Lỗi kết nối!")
        );
    }

    // ── Filter & Search ───────────────────────────────────────────────────────

    private void applyFilter() {
        JsonArray filtered = new JsonArray();
        for (int i = 0; i < cachedAll.size(); i++) {
            JsonObject a = cachedAll.get(i).getAsJsonObject();
            String status = a.has("status") ? a.get("status").getAsString() : "";
            if ("ALL".equals(activeFilter) || activeFilter.equals(status))
                filtered.add(a);
        }
        populateGrid(filtered);
        updateFilterButtons();
    }

    @FXML private void handleFilter(ActionEvent e) {
        Button src = (Button) e.getSource();
        if      (src == btnFilterAll)    activeFilter = "ALL";
        else if (src == btnFilterActive) activeFilter = "ACTIVE";
        else if (src == btnFilterEnded)  activeFilter = "ENDED";
        applyFilter();
    }

    @FXML private void handleSearch(ActionEvent e) {
        String keyword = txtSearch.getText().trim().toLowerCase();
        String category = cmbCategory.getValue();
        boolean allCat  = "Tất cả danh mục".equals(category);

        JsonArray filtered = new JsonArray();
        for (int i = 0; i < cachedAll.size(); i++) {
            JsonObject a = cachedAll.get(i).getAsJsonObject();
            boolean matchName = keyword.isEmpty()
                    || a.has("name") && a.get("name").getAsString().toLowerCase().contains(keyword);
            boolean matchCat  = allCat
                    || a.has("category") && a.get("category").getAsString().equalsIgnoreCase(category);
            if (matchName && matchCat) filtered.add(a);
        }
        populateGrid(filtered);
    }

    @FXML private void handleRefresh(ActionEvent e) { loadAuctions(); }

    @FXML private void handleBack(ActionEvent e) {
        countdownTimers.forEach(Timeline::stop);
        ViewManager.navigateTo(ViewManager.Views.HOME);
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    private void handleGoToBidding(int auctionId) {
        countdownTimers.forEach(Timeline::stop);
        ViewManager.navigateTo(ViewManager.Views.BIDDING_ROOM,
                "Phòng đấu giá #" + auctionId,
                c -> { if (c instanceof BiddingRoomController b) {
                    b.setAuctionId(auctionId);
                    b.setOnBack(this::loadAuctions);
                }});
    }

    // ── Populate ─────────────────────────────────────────────────────────────

    private void populateGrid(JsonArray data) {
        countdownTimers.forEach(Timeline::stop);
        countdownTimers.clear();
        flowAuctions.getChildren().clear();

        if (data.size() == 0) {
            flowAuctions.getChildren().add(CardFactory.buildEmptyLabel("Không có phiên đấu giá nào"));
            lblCount.setText("0 phiên");
            return;
        }
        lblCount.setText(data.size() + " phiên");
        for (int i = 0; i < data.size(); i++)
            flowAuctions.getChildren().add(CardFactory.buildAuctionCard(
                    data.get(i).getAsJsonObject(), countdownTimers, this::handleGoToBidding));
    }

    private void updateFilterButtons() {
        String on  = "-fx-background-color:#4285F4;-fx-text-fill:white;-fx-background-radius:20;-fx-padding:5 16 5 16;-fx-cursor:hand;";
        String off = "-fx-background-color:#EEF2FF;-fx-text-fill:#4285F4;-fx-background-radius:20;-fx-padding:5 16 5 16;-fx-cursor:hand;";
        btnFilterAll.setStyle   ("ALL"   .equals(activeFilter) ? on : off);
        btnFilterActive.setStyle("ACTIVE".equals(activeFilter) ? on : off);
        btnFilterEnded.setStyle ("ENDED" .equals(activeFilter) ? on : off);
    }

    private void showLoading() { flowAuctions.getChildren().setAll(CardFactory.buildEmptyLabel("Đang tải...")); }
    private void showEmpty(String msg) { flowAuctions.getChildren().setAll(CardFactory.buildEmptyLabel(msg)); lblCount.setText("0 phiên"); }
}