package com.nhom15.client.network;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Client TCP — mọi request REST-style một kết nối ngắn.
 *
 * <p>Host/cổng có thể ghi đè khi chạy local (trùng với server đấu giá):
 * {@code -Dauction.server.host=127.0.0.1 -Dauction.server.port=8888}
 *
 * <p><b>Lưu ý realtime:</b> SUBSCRIBE_AUCTION dùng kết nối dài tới cùng host/port. Nếu phía trước
 * server có load balancer nhiều JVM, push có thể không tới mọi client — cần 1 replica hoặc sticky
 * session, hoặc dùng poll (RealtimePollingController).
 */
public class SocketClient {

  private static final String DEFAULT_SERVER_IP = "viaduct.proxy.rlwy.net";
  private static final int DEFAULT_SERVER_PORT = 30802;

  // Timeout kết nối: 5 giây — tránh treo vô thời hạn khi server không trả lời TCP handshake
  private static final int CONNECT_TIMEOUT_MS = 5000;

  public static String getServerHost() {
    String h = System.getProperty("auction.server.host");
    return (h != null && !h.isBlank()) ? h.trim() : DEFAULT_SERVER_IP;
  }

  public static int getServerPort() {
    String p = System.getProperty("auction.server.port");
    if (p != null && !p.isBlank()) {
      try {
        return Integer.parseInt(p.trim());
      } catch (NumberFormatException ignored) {
        // fall through
      }
    }
    return DEFAULT_SERVER_PORT;
  }

  public static int getConnectTimeoutMs() {
    return CONNECT_TIMEOUT_MS;
  }

  // Timeout đọc: 15 giây — đủ cho các request nặng (tạo item, upload), ngắt sớm nếu server hang
  private static final int READ_TIMEOUT_MS = 15000;

  /**
   * Hàm này nhận vào một JsonObject (yêu cầu từ giao diện), gửi lên Server, và trả về JsonObject
   * (phản hồi từ Server).
   *
   * FIX PERF: Đặt TCP_NODELAY=true — tắt Nagle's algorithm.
   * Nagle buffer các gói nhỏ và đợi ACK trước khi gửi, có thể gây thêm 40ms delay
   * cho mỗi JSON request nhỏ (PLACE_BID, GET_AUCTION_DETAIL...). TCP_NODELAY
   * đảm bảo dữ liệu được gửi ngay lập tức không chờ buffer.
   */
  public static JsonObject sendRequest(JsonObject request) {
    // Tạo socket thủ công để set timeout trước khi connect — try-with-resources đảm bảo đóng đúng
    try (Socket socket = new Socket()) {
      // Kết nối với timeout — nếu server không respond trong 5s, ném SocketTimeoutException
      socket.connect(new InetSocketAddress(getServerHost(), getServerPort()), CONNECT_TIMEOUT_MS);
      // FIX PERF: tắt Nagle — gửi JSON nhỏ ngay lập tức, không đợi buffer
      socket.setTcpNoDelay(true);
      // Timeout đọc — nếu server nhận nhưng không trả lời trong 15s, ném SocketTimeoutException
      socket.setSoTimeout(READ_TIMEOUT_MS);

      try (PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
           BufferedReader in = new BufferedReader(
                   new InputStreamReader(socket.getInputStream()))) {

        // 1. Gửi dữ liệu JSON lên Server
        out.println(request.toString());

        // 2. Chờ và đọc kết quả Server trả về
        String responseStr = in.readLine();

        // 3. Chuyển kết quả dạng chuỗi (String) thành JsonObject và trả về cho Controller
        if (responseStr != null) {
          return JsonParser.parseString(responseStr).getAsJsonObject();
        }
      }
    } catch (java.net.SocketTimeoutException e) {
      System.err.println("❌ Timeout kết nối đến Server: " + e.getMessage());
    } catch (Exception e) {
      System.err.println("❌ Lỗi kết nối đến Server: " + e.getMessage());
    }

    // Trả về null nếu Server sập hoặc không kết nối được
    return null;
  }
}
