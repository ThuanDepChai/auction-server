package com.nhom15.client.network;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;

public class SocketClient {

  // Đổi thành IP thực tế của Server. Dùng "localhost" nếu chạy cùng máy.
  private static final String SERVER_IP = "26.159.224.110";

  // Cổng này phải khớp với cổng đang chạy ở AuctionServer
  private static final int SERVER_PORT = 8888;

  // Timeout kết nối: 5 giây — tránh treo vô thời hạn khi server không trả lời TCP handshake
  private static final int CONNECT_TIMEOUT_MS = 5000;

  public static String getServerHost() {
    return SERVER_IP;
  }

  public static int getServerPort() {
    return SERVER_PORT;
  }

  public static int getConnectTimeoutMs() {
    return CONNECT_TIMEOUT_MS;
  }

  // Timeout đọc: 15 giây — đủ cho các request nặng (tạo item, upload), ngắt sớm nếu server hang
  private static final int READ_TIMEOUT_MS = 15000;

  /**
   * Hàm này nhận vào một JsonObject (yêu cầu từ giao diện), gửi lên Server, và trả về JsonObject
   * (phản hồi từ Server).
   */
  public static JsonObject sendRequest(JsonObject request) {
    // Tạo socket thủ công để set timeout trước khi connect — try-with-resources đảm bảo đóng đúng
    try (Socket socket = new Socket()) {
      // Kết nối với timeout — nếu server không respond trong 5s, ném SocketTimeoutException
      socket.connect(new InetSocketAddress(SERVER_IP, SERVER_PORT), CONNECT_TIMEOUT_MS);
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
