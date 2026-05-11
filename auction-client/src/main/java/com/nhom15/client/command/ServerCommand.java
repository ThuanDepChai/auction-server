package com.nhom15.client.command;

import com.google.gson.JsonObject;
import com.nhom15.client.network.SocketClient;
import java.util.function.Consumer;
import javafx.application.Platform;

/**
 * ServerCommand — base class cho Command Pattern phía client.
 * <p>
 * Mỗi subclass chỉ cần override buildRequest() để cung cấp JsonObject tương ứng với action của
 * mình.
 * <p>
 * executeAsync() xử lý toàn bộ: - Gửi request trên background thread - Callback onSuccess / onError
 * trên JavaFX thread (Platform.runLater) - Phân biệt lỗi kết nối (response null) với lỗi nghiệp vụ
 * (status FAIL)
 */
public abstract class ServerCommand {

  // ── Subclass implement ───────────────────────────────────────────────────

  /**
   * Build JsonObject gửi lên server. Ví dụ: JsonObject req = new JsonObject();
   * req.addProperty("action", "LOGIN"); req.add("data", data); return req;
   */
  protected abstract JsonObject buildRequest();

  // ── Execute ──────────────────────────────────────────────────────────────

  /**
   * Gửi request bất đồng bộ.
   *
   * @param onSuccess callback nhận JsonObject response, chạy trên FX thread. Được gọi kể cả khi
   *                  status = "FAIL" — caller tự kiểm tra.
   * @param onError   callback khi mất kết nối (response null), chạy trên FX thread.
   */
  public void executeAsync(Consumer<JsonObject> onSuccess, Runnable onError) {
    JsonObject request = buildRequest();
    new Thread(() -> {
      JsonObject response = SocketClient.sendRequest(request);
      Platform.runLater(() -> {
        if (response == null) {
          if (onError != null) {
            onError.run();
          }
        } else {
          if (onSuccess != null) {
            onSuccess.accept(response);
          }
        }
      });
    }, "cmd-" + getClass().getSimpleName()).start();
  }

  /**
   * Overload không cần onError (fire-and-forget hoặc caller tự xử lý null).
   */
  public void executeAsync(Consumer<JsonObject> onSuccess) {
    executeAsync(onSuccess, null);
  }

  // ── Static helpers ───────────────────────────────────────────────────────

  /**
   * Kiểm tra response có status SUCCESS không.
   */
  public static boolean isSuccess(JsonObject response) {
    return response != null
        && response.has("status")
        && "SUCCESS".equals(response.get("status").getAsString());
  }

  /**
   * Lấy message từ response, trả về fallback nếu không có. Dùng để hiển thị lỗi từ server mà không
   * cần try-catch.
   */
  public static String getMessage(JsonObject response, String fallback) {
    if (response != null && response.has("message")) {
      return response.get("message").getAsString();
    }
    return fallback;
  }
}