package com.nhom15.network;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nhom15.network.handler.RequestHandler;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * AuctionServer — chỉ chịu trách nhiệm: 1. Khởi động ServerSocket 2. Accept kết nối 3. Đọc JSON từ
 * client 4. Chuyển cho RequestHandler xử lý 5. Ghi JSON response về client
 * <p>
 * Không có bất kỳ business logic nào ở đây.
 */
public class AuctionServer {

  private static final String SERVER_IP = "0.0.0.0";
  private static final int PORT = 8888;

  // Giới hạn tối đa 100 client đồng thời — tránh crash khi bị flood kết nối.
  // Nếu đầy, request mới được xếp hàng chờ thay vì tạo thread vô hạn.
  private static final int MAX_THREADS = 100;
  private static final ExecutorService threadPool = Executors.newFixedThreadPool(MAX_THREADS);

  /** Kết nối SUBSCRIBE giữ thread lâu — tách khỏi pool request cố định. */
  private static final ExecutorService subscriberPool = Executors.newCachedThreadPool();

  // RequestHandler dùng chung — thread-safe vì các handler không có state mutable
  private static final RequestHandler requestHandler = new RequestHandler();

  public static void main(String[] args) {
    // Tạo thư mục lưu avatar nếu chưa có
    File avatarDir = new File("avatars");
    if (!avatarDir.exists()) {
      avatarDir.mkdirs();
    }

    try {
      InetAddress serverIP = InetAddress.getByName(SERVER_IP);
      ServerSocket serverSocket = new ServerSocket(PORT, 50, serverIP);

      System.out.println(
              "✅ Server đang chạy trên IP " + serverIP.getHostAddress() + " ở cổng " + PORT + "...");
      System.out.println("📁 Thư mục avatars: " + avatarDir.getAbsolutePath());
      System.out.println("🧵 Thread pool: tối đa " + MAX_THREADS + " client đồng thời.");

      while (true) {
        Socket clientSocket = serverSocket.accept();
        System.out.println("🔌 Client mới kết nối: " + clientSocket.getInetAddress());
        // Submit vào pool — không tạo thread mới vô hạn, tránh server crash
        threadPool.submit(() -> handleClientConnection(clientSocket));
      }
    } catch (IOException e) {
      System.err.println("❌ Lỗi khi khởi động Server: " + e.getMessage());
      e.printStackTrace();
    }
  }

  /**
   * Đọc dòng đầu; nếu là SUBSCRIBE_AUCTION thì chuyển sang pool riêng (kết nối dài),
   * không đóng socket ở đây. Ngược lại: xử lý request ngắn rồi đóng trong {@code finally}.
   */
  private static void handleClientConnection(Socket clientSocket) {
    boolean handOffToSubscriber = false;
    BufferedReader in = null;
    PrintWriter out = null;
    try {
      in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
      out = new PrintWriter(clientSocket.getOutputStream(), true);

      String jsonFromClient = in.readLine();
      if (jsonFromClient == null) {
        return;
      }

      JsonObject request = JsonParser.parseString(jsonFromClient).getAsJsonObject();
      String action = request.has("action") ? request.get("action").getAsString() : "";

      if ("SUBSCRIBE_AUCTION".equals(action)) {
        handOffToSubscriber = true;
        BufferedReader inRef = in;
        PrintWriter outRef = out;
        Socket socketRef = clientSocket;
        JsonObject requestRef = request;
        subscriberPool.submit(
                () -> handleSubscribeAuction(socketRef, inRef, outRef, requestRef));
        return;
      }

      JsonObject response = requestHandler.handle(request);
      out.println(response.toString());

    } catch (Exception e) {
      System.err.println("❌ Lỗi khi xử lý request từ " + clientSocket.getInetAddress()
              + ": " + e.getMessage());
      e.printStackTrace();
    } finally {
      if (!handOffToSubscriber) {
        try {
          clientSocket.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
  }

  /**
   * Giữ kết nối mở: gửi ACK rồi đăng ký broadcaster; đọc thêm dòng cho đến khi client gửi
   * UNSUBSCRIBE_AUCTION hoặc đóng socket.
   */
  private static void handleSubscribeAuction(
          Socket socket, BufferedReader in, PrintWriter out, JsonObject request) {
    JsonObject data = request.has("data") ? request.getAsJsonObject("data") : new JsonObject();
    if (!data.has("auctionId")) {
      JsonObject err = new JsonObject();
      err.addProperty("status", "FAIL");
      err.addProperty("message", "Thiếu auctionId");
      out.println(err);
      try {
        socket.close();
      } catch (IOException ignored) {
        // ignore
      }
      return;
    }
    int auctionId = data.get("auctionId").getAsInt();

    JsonObject ack = new JsonObject();
    ack.addProperty("status", "SUCCESS");
    ack.addProperty("message", "SUBSCRIBED");
    ack.addProperty("auctionId", auctionId);
    out.println(ack);

    AuctionRoomBroadcaster.INSTANCE.register(auctionId, out);
    try {
      String line;
      while ((line = in.readLine()) != null) {
        JsonObject msg = JsonParser.parseString(line).getAsJsonObject();
        String a = msg.has("action") ? msg.get("action").getAsString() : "";
        if ("UNSUBSCRIBE_AUCTION".equals(a)) {
          break;
        }
      }
    } catch (Exception e) {
      System.err.println("⚠️ Subscribe auction #" + auctionId + " kết thúc: " + e.getMessage());
    } finally {
      AuctionRoomBroadcaster.INSTANCE.unregister(auctionId, out);
      try {
        socket.close();
      } catch (IOException ignored) {
        // ignore
      }
    }
  }
}