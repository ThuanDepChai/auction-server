package com.nhom15.client.controller.seller;

import com.google.gson.JsonObject;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

public final class SellerDashboardForms {

  private static final DateTimeFormatter SERVER_DATE_TIME =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  private SellerDashboardForms() {
  }

  public static void bindErrorLabels(Label... labels) {
    for (Label label : labels) {
      label.managedProperty().bind(label.visibleProperty());
    }
  }

  public static void configureCategoryExtras(ComboBox<String> category,
      VBox fashion, VBox electronics, VBox vehicle, VBox art, VBox sports) {
    category.valueProperty().addListener((obs, oldVal, newVal) -> {
      hide(fashion, electronics, vehicle, art, sports);
      if (newVal == null) {
        return;
      }
      switch (newVal) {
        case "Thoi trang", "Thá»i trang", "Thời trang" -> show(fashion);
        case "Dien tu", "Äiá»‡n tá»­", "Điện tử" -> show(electronics);
        case "Xe co", "Xe cá»™", "Xe cộ" -> show(vehicle);
        case "Nghe thuat", "Nghá»‡ thuáº­t", "Nghệ thuật" -> show(art);
        case "The thao", "Thá»ƒ thao", "Thể thao" -> show(sports);
        default -> {
        }
      }
    });
  }

  public static JsonObject buildItemPayload(ItemForm form, int sellerId, String imageBase64,
      String imageExt) {
    clear(form.nameError, form.categoryError, form.priceError);
    form.status.setText("");

    boolean hasError = false;
    String name = text(form.name);
    String description = form.description.getText().trim();
    String category = form.category.getValue();

    if (name.isEmpty()) {
      showError(form.nameError, "Vui long nhap ten!");
      hasError = true;
    }
    if (category == null) {
      showError(form.categoryError, "Vui long chon danh muc!");
      hasError = true;
    }

    double price = parseMoney(form.startPrice);
    if (price <= 0) {
      showError(form.priceError, "Gia khong hop le!");
      hasError = true;
    }
    if (hasError) {
      return null;
    }

    JsonObject payload = new JsonObject();
    payload.addProperty("sellerId", sellerId);
    payload.addProperty("name", name);
    payload.addProperty("description", description);
    payload.addProperty("category", category);
    payload.addProperty("startPrice", price);
    payload.addProperty("imageBase64", imageBase64);
    payload.addProperty("extension", imageExt);
    payload.addProperty("extraInfo", buildExtraInfo(form, category).toString());
    return payload;
  }

  public static JsonObject buildAuctionPayload(AuctionForm form, int sellerId,
      Map<String, Integer> itemNameToId) {
    clear(form.itemError, form.priceError, form.stepError, form.timeError);
    form.status.setText("");

    boolean hasError = false;
    String itemName = form.item.getValue();
    if (itemName == null || !itemNameToId.containsKey(itemName)) {
      showError(form.itemError, "Chon san pham!");
      hasError = true;
    }

    double startPrice = parseMoney(form.startPrice);
    if (startPrice <= 0) {
      showError(form.priceError, "Gia khong hop le!");
      hasError = true;
    }

    double minStep = parseMoney(form.minStep);
    if (minStep <= 0) {
      showError(form.stepError, "Buoc gia khong hop le!");
      hasError = true;
    }

    String endTime = buildEndTime(form.endDate, form.endTime);
    if (endTime == null) {
      showError(form.timeError, "Ngay gio khong hop le!");
      hasError = true;
    }
    if (hasError) {
      return null;
    }

    JsonObject payload = new JsonObject();
    payload.addProperty("itemId", itemNameToId.get(itemName));
    payload.addProperty("sellerId", sellerId);
    payload.addProperty("startPrice", startPrice);
    payload.addProperty("minStep", minStep);
    payload.addProperty("endTime", endTime);
    return payload;
  }

  public static void clearItemForm(ItemForm form) {
    form.name.clear();
    form.description.clear();
    form.category.setValue(null);
    form.startPrice.clear();
    form.fashionBrand.clear();
    form.fashionSize.clear();
    form.fashionColor.clear();
    form.electronicsBrand.clear();
    form.electronicsSpecs.clear();
    form.vehicleBrand.clear();
    form.vehicleYear.clear();
    form.vehicleMileage.clear();
    form.artArtist.clear();
    form.artYear.clear();
    form.sportType.clear();
    form.sportCondition.clear();
  }

  public static void clearAuctionForm(AuctionForm form) {
    form.item.setValue(null);
    form.startPrice.clear();
    form.minStep.clear();
    form.endDate.setValue(null);
    form.endTime.clear();
  }

  private static JsonObject buildExtraInfo(ItemForm form, String category) {
    JsonObject extra = new JsonObject();
    if (isCategory(category, "Thời trang", "Thá»i trang")) {
      extra.addProperty("brand", text(form.fashionBrand));
      extra.addProperty("size", text(form.fashionSize));
      extra.addProperty("color", text(form.fashionColor));
    } else if (isCategory(category, "Điện tử", "Äiá»‡n tá»­")) {
      extra.addProperty("brand", text(form.electronicsBrand));
      extra.addProperty("specs", text(form.electronicsSpecs));
    } else if (isCategory(category, "Xe cộ", "Xe cá»™")) {
      extra.addProperty("brand", text(form.vehicleBrand));
      addInt(extra, "year", form.vehicleYear);
      addDouble(extra, "mileage", form.vehicleMileage);
    } else if (isCategory(category, "Nghệ thuật", "Nghá»‡ thuáº­t")) {
      extra.addProperty("artist", text(form.artArtist));
      addInt(extra, "year", form.artYear);
    } else if (isCategory(category, "Thể thao", "Thá»ƒ thao")) {
      extra.addProperty("sportType", text(form.sportType));
      extra.addProperty("condition", text(form.sportCondition));
    }
    return extra;
  }

  private static String buildEndTime(DatePicker date, TextField time) {
    try {
      if (date.getValue() == null || text(time).isEmpty()) {
        return null;
      }
      String value = date.getValue().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
          + " " + text(time) + ":00";
      LocalDateTime end = LocalDateTime.parse(value, SERVER_DATE_TIME);
      return end.isAfter(LocalDateTime.now()) ? value : null;
    } catch (Exception e) {
      return null;
    }
  }

  private static void hide(VBox... boxes) {
    for (VBox box : boxes) {
      box.setVisible(false);
      box.setManaged(false);
    }
  }

  private static void show(VBox box) {
    box.setVisible(true);
    box.setManaged(true);
  }

  private static void clear(Label... labels) {
    for (Label label : labels) {
      label.setVisible(false);
      label.setText("");
    }
  }

  private static void showError(Label label, String message) {
    label.setText(message);
    label.setVisible(true);
  }

  private static String text(TextField field) {
    return field.getText() == null ? "" : field.getText().trim();
  }

  private static double parseMoney(TextField field) {
    try {
      return Double.parseDouble(text(field).replaceAll("[^0-9]", ""));
    } catch (Exception e) {
      return 0;
    }
  }

  private static boolean isCategory(String actual, String... candidates) {
    for (String candidate : candidates) {
      if (candidate.equals(actual)) {
        return true;
      }
    }
    return false;
  }

  private static void addInt(JsonObject object, String key, TextField field) {
    try {
      object.addProperty(key, Integer.parseInt(text(field)));
    } catch (Exception ignored) {
    }
  }

  private static void addDouble(JsonObject object, String key, TextField field) {
    try {
      object.addProperty(key, Double.parseDouble(text(field)));
    } catch (Exception ignored) {
    }
  }

  public record ItemForm(
      TextField name,
      TextArea description,
      ComboBox<String> category,
      TextField startPrice,
      Label nameError,
      Label categoryError,
      Label priceError,
      Label status,
      TextField fashionBrand,
      TextField fashionSize,
      TextField fashionColor,
      TextField electronicsBrand,
      TextField electronicsSpecs,
      TextField vehicleBrand,
      TextField vehicleYear,
      TextField vehicleMileage,
      TextField artArtist,
      TextField artYear,
      TextField sportType,
      TextField sportCondition) {
  }

  public record AuctionForm(
      ComboBox<String> item,
      TextField startPrice,
      TextField minStep,
      DatePicker endDate,
      TextField endTime,
      Label itemError,
      Label priceError,
      Label stepError,
      Label timeError,
      Label status) {
  }
}
