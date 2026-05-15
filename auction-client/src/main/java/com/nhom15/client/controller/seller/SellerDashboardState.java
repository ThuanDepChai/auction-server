package com.nhom15.client.controller.seller;

import com.google.gson.JsonArray;
import java.util.HashMap;
import java.util.Map;

public class SellerDashboardState {

  private JsonArray items = new JsonArray();
  private JsonArray auctions = new JsonArray();
  private final Map<String, Integer> itemNameToId = new HashMap<>();
  private final Map<String, Long> itemNameToPrice = new HashMap<>();
  private String auctionFilter = "ALL";
  private String selectedImageBase64 = "";
  private String selectedImageExt = "jpg";

  public JsonArray items() {
    return items;
  }

  public void setItems(JsonArray items) {
    this.items = items != null ? items : new JsonArray();
  }

  public JsonArray auctions() {
    return auctions;
  }

  public void setAuctions(JsonArray auctions) {
    this.auctions = auctions != null ? auctions : new JsonArray();
  }

  public Map<String, Integer> itemNameToId() {
    return itemNameToId;
  }

  public Map<String, Long> itemNameToPrice() {
    return itemNameToPrice;
  }

  public void clearAuctionItemIndex() {
    itemNameToId.clear();
    itemNameToPrice.clear();
  }

  public String auctionFilter() {
    return auctionFilter;
  }

  public void setAuctionFilter(String auctionFilter) {
    this.auctionFilter = auctionFilter;
  }

  public String selectedImageBase64() {
    return selectedImageBase64;
  }

  public void setSelectedImageBase64(String selectedImageBase64) {
    this.selectedImageBase64 = selectedImageBase64 != null ? selectedImageBase64 : "";
  }

  public String selectedImageExt() {
    return selectedImageExt;
  }

  public void setSelectedImageExt(String selectedImageExt) {
    this.selectedImageExt = selectedImageExt != null ? selectedImageExt : "jpg";
  }
}
