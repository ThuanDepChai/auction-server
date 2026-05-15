package com.nhom15.client.controller.seller;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class SellerDashboardStats {

  private static final DateTimeFormatter SERVER_DATE_TIME =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  private SellerDashboardStats() {
  }

  public static int soldItemCount(JsonArray items) {
    int sold = 0;
    for (int i = 0; i < size(items); i++) {
      if ("SOLD".equals(getString(items.get(i).getAsJsonObject(), "status", ""))) {
        sold++;
      }
    }
    return sold;
  }

  public static int activeAuctionCount(JsonArray auctions) {
    int active = 0;
    for (int i = 0; i < size(auctions); i++) {
      if ("ACTIVE".equals(getString(auctions.get(i).getAsJsonObject(), "status", ""))) {
        active++;
      }
    }
    return active;
  }

  public static int endingSoonCount(JsonArray auctions) {
    int count = 0;
    for (int i = 0; i < size(auctions); i++) {
      if (isEndingSoon(auctions.get(i).getAsJsonObject())) {
        count++;
      }
    }
    return count;
  }

  public static double revenue(JsonArray auctions) {
    double revenue = 0;
    for (int i = 0; i < size(auctions); i++) {
      JsonObject auction = auctions.get(i).getAsJsonObject();
      if ("ENDED".equals(getString(auction, "status", ""))) {
        revenue += getDouble(auction, "currentPrice", 0);
      }
    }
    return revenue;
  }

  public static boolean matchesAuctionFilter(JsonObject auction, String filter) {
    if ("ALL".equals(filter)) {
      return true;
    }
    String status = getString(auction, "status", "");
    if ("UPCOMING".equals(filter)) {
      return "SCHEDULED".equals(status) || "UPCOMING".equals(status);
    }
    return filter.equals(status);
  }

  public static boolean isEndingSoon(JsonObject auction) {
    try {
      String endTime = getString(auction, "endTime", "");
      LocalDateTime end = LocalDateTime.parse(endTime, SERVER_DATE_TIME);
      LocalDateTime now = LocalDateTime.now();
      return end.isAfter(now) && end.isBefore(now.plusHours(24));
    } catch (Exception e) {
      return false;
    }
  }

  public static String getString(JsonObject object, String key, String defaultValue) {
    return object != null && object.has(key) && !object.get(key).isJsonNull()
        ? object.get(key).getAsString()
        : defaultValue;
  }

  public static double getDouble(JsonObject object, String key, double defaultValue) {
    try {
      return object != null && object.has(key) && !object.get(key).isJsonNull()
          ? object.get(key).getAsDouble()
          : defaultValue;
    } catch (Exception e) {
      return defaultValue;
    }
  }

  public static String formatMoney(double value) {
    return String.format("%,.0f d", value);
  }

  private static int size(JsonArray array) {
    return array == null ? 0 : array.size();
  }
}
