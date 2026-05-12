package com.nhom15.client.network;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class SocketClient {

  // Đổi thành IP thực tế của Server. Dùng "localhost" nếu chạy cùng máy.
  private static final String SERVER_IP = "26.159.224.110";

  // Cổng này phải khớp với cổng đang chạy ở AuctionServer
  private static final int SERVER_PORT = 8888;

  /**
   * Hàm này nhận vào một JsonObject (yêu cầu từ giao diện), gửi lên Server, và trả về JsonObject
   * (phản hồi từ Server).
   */
  public static JsonObject sendRequest(JsonObject request) {
    // Sử dụng try-with-resources để tự động đóng kết nối khi xong việc
    try (Socket socket = new Socket(SERVER_IP, SERVER_PORT);
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

      // 1. Gửi dữ liệu JSON lên Server
      out.println(request.toString());

      // 2. Chờ và đọc kết quả Server trả về
      String responseStr = in.readLine();

      // 3. Chuyển kết quả dạng chuỗi (String) thành JsonObject và trả về cho Controller
      if (responseStr != null) {
        return JsonParser.parseString(responseStr).getAsJsonObject();
      }

    } catch (Exception e) {
      System.err.println("❌ Lỗi kết nối đến Server: " + e.getMessage());
      // In ra chi tiết lỗi nếu cần debug: e.printStackTrace();
    }

    // Trả về null nếu Server sập hoặc không kết nối được
    return null;
  }
}
