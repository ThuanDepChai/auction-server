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
import java.net.SocketTimeoutException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * AuctionServer — TCP server điều phối toàn bộ kết nối client.
 *
 * <h3>Các vấn đề hiệu năng đã fix:</h3>
 *
 * <b>FIX 1 — SO_TIMEOUT bị thiếu trên accepted socket:</b><br>
 * Code cũ không set {@code socket.setSoTimeout()} cho socket nhận về từ {@code accept()}.
 * Nếu client TCP connect nhưng không gửi dữ liệu, thread trong pool bị block vô thời hạn
 * trên {@code in.readLine()} → pool cạn dần → server không nhận thêm client.<br>
 * Fix: set {@code SO_TIMEOUT = 10s} ngay sau {@code accept()}.
 *
 * <b>FIX 2 — subscriberPool là CachedThreadPool không giới hạn:</b><br>
 * Code cũ dùng {@code Executors.newCachedThreadPool()} cho subscriber — không có ceiling.
 * Nếu 500 client SUBSCRIBE cùng lúc, JVM tạo 500 thread → OOM hoặc context-switch overhead.<br>
 * Fix: {@code newFixedThreadPool(200)} — đủ dùng cho subscriber dài hạn.
 *
 * <b>FIX 3 — ServerSocket backlog = 50 quá nhỏ:</b><br>
 * Khi pool bận, OS phải xếp hàng TCP handshake ở backlog. Backlog = 50 nghĩa là nếu có
 * 51 client kết nối đồng thời trong 1ms, client thứ 51 nhận RST (refused). Fix: 200.
 *
 * <b>FIX 4 — Không có shutdown hook → port bị giữ khi restart:</b><br>
 * Code cũ không đóng ServerSocket khi process kết thúc (Ctrl+C). OS giữ port 8888 trong
 * TIME_WAIT → server restart bị lỗi "Address already in use". Fix: {@code setReuseAddress(true)}
 * + shutdown hook graceful.
 */
public class AuctionServer {

  private static final String SERVER_IP  = "0.0.0.0";
  private static final int    PORT       = 8888;

  // FIX 3: backlog 200 thay vì 50 — tránh RST khi nhiều client đến cùng lúc
  private static final int BACKLOG       = 200;

  // FIX 1: timeout cho socket thường (request ngắn)
  private static final int CLIENT_SO_TIMEOUT_MS     = 10_000;  // 10 giây
  // FIX 1: timeout cho socket subscriber (kết nối dài, cần lớn hơn)
  private static final int SUBSCRIBER_SO_TIMEOUT_MS = 60_000;  // 60 giây
  private static final boolean DEBUG_CONNECTIONS =
      Boolean.getBoolean("auction.server.debugConnections");

  // Pool xử lý request ngắn — cố định 100 thread
  private static final int MAX_REQUEST_THREADS    = 100;

  // FIX 2: subscriber pool giới hạn 200 (thay vì CachedThreadPool không giới hạn)
  private static final int MAX_SUBSCRIBER_THREADS = 200;

  private static final ExecutorService threadPool =
      Executors.newFixedThreadPool(MAX_REQUEST_THREADS);

  // FIX 2: bounded pool thay vì CachedThreadPool
  private static final ExecutorService subscriberPool =
      Executors.newFixedThreadPool(MAX_SUBSCRIBER_THREADS);

  private static final RequestHandler requestHandler = new RequestHandler();

  // FIX 4: giữ tham chiếu để shutdown hook có thể đóng
  private static volatile ServerSocket serverSocket;

  public static void main(String[] args) {
    File avatarDir = new File("avatars");
    if (!avatarDir.exists()) avatarDir.mkdirs();

    // FIX 4: Shutdown hook — graceful shutdown, tránh "Address already in use"
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      System.out.println("\n🛑 Server đang shutdown...");
      shutdownPool(threadPool,    "requestPool");
      shutdownPool(subscriberPool, "subscriberPool");
      try {
        if (serverSocket != null && !serverSocket.isClosed()) {
          serverSocket.close();
        }
      } catch (IOException ignored) {}
      System.out.println("✅ Shutdown hoàn tất.");
    }, "shutdown-hook"));

    try {
      serverSocket = new ServerSocket();
      // FIX 4: SO_REUSEADDR — cho phép bind lại port ngay sau khi restart
      serverSocket.setReuseAddress(true);
      serverSocket.bind(new java.net.InetSocketAddress(
          InetAddress.getByName(SERVER_IP), PORT), BACKLOG);

      System.out.println("✅ Server đang chạy trên IP " + SERVER_IP + " ở cổng " + PORT);
      System.out.printf("🧵 requestPool=%d  subscriberPool=%d  backlog=%d%n",
          MAX_REQUEST_THREADS, MAX_SUBSCRIBER_THREADS, BACKLOG);

      while (!serverSocket.isClosed()) {
        Socket clientSocket = serverSocket.accept();

        // FIX 1: đặt SO_TIMEOUT ngay sau accept — không để thread block vô thời hạn
        clientSocket.setSoTimeout(CLIENT_SO_TIMEOUT_MS);
        clientSocket.setTcpNoDelay(true);

        if (DEBUG_CONNECTIONS) {
          System.out.println("Client connected: " + clientSocket.getInetAddress());
        }
        threadPool.submit(() -> handleClientConnection(clientSocket));
      }

    } catch (IOException e) {
      if (serverSocket != null && serverSocket.isClosed()) {
        // Bình thường — shutdown hook đã đóng ServerSocket
        System.out.println("ℹ️ ServerSocket đã đóng.");
      } else {
        System.err.println("❌ Lỗi khi khởi động Server: " + e.getMessage());
        e.printStackTrace();
      }
    }
  }

  // ── Connection handler ────────────────────────────────────────────────────

  /**
   * Đọc dòng đầu tiên; nếu là SUBSCRIBE_AUCTION thì:
   * 1. Điều chỉnh SO_TIMEOUT sang mức dài hơn (subscriber cần giữ kết nối lâu).
   * 2. Chuyển sang subscriberPool — giải phóng requestPool cho request tiếp theo.
   */
  private static void handleClientConnection(Socket clientSocket) {
    boolean handOffToSubscriber = false;
    BufferedReader in  = null;
    PrintWriter    out = null;
    try {
      in  = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
      out = new PrintWriter(clientSocket.getOutputStream(), true);

      String jsonFromClient;
      try {
        jsonFromClient = in.readLine();
      } catch (SocketTimeoutException e) {
        // FIX 1: client kết nối nhưng không gửi gì trong CLIENT_SO_TIMEOUT_MS → đóng
        System.err.println("⏱️ Client " + clientSocket.getInetAddress()
            + " không gửi dữ liệu trong " + CLIENT_SO_TIMEOUT_MS + "ms → đóng.");
        return;
      }

      if (jsonFromClient == null) return;

      JsonObject request = JsonParser.parseString(jsonFromClient).getAsJsonObject();
      String     action  = request.has("action") ? request.get("action").getAsString() : "";

      if ("SUBSCRIBE_AUCTION".equals(action)) {
        handOffToSubscriber = true;

        // FIX 1: subscriber cần giữ kết nối lâu → timeout lớn hơn
        try {
          clientSocket.setSoTimeout(SUBSCRIBER_SO_TIMEOUT_MS);
        } catch (IOException e) {
          System.err.println("⚠️ Không set subscriber SO_TIMEOUT: " + e.getMessage());
        }

        BufferedReader inRef      = in;
        PrintWriter    outRef     = out;
        Socket         socketRef  = clientSocket;
        JsonObject     requestRef = request;
        subscriberPool.submit(
            () -> handleSubscribeAuction(socketRef, inRef, outRef, requestRef));
        return;
      }

      JsonObject response = requestHandler.handle(request);
      out.println(response.toString());
      out.flush();

    } catch (SocketTimeoutException e) {
      System.err.println("⏱️ Read timeout " + clientSocket.getInetAddress());
    } catch (Exception e) {
      System.err.println("❌ Lỗi request từ " + clientSocket.getInetAddress()
          + ": " + e.getMessage());
    } finally {
      if (!handOffToSubscriber) {
        try { clientSocket.close(); } catch (IOException ignored) {}
      }
    }
  }

  /**
   * Subscriber loop: giữ kết nối mở, đăng ký AuctionRoomBroadcaster,
   * xử lý SUBSCRIBE_PING và UNSUBSCRIBE_AUCTION.
   */
  private static void handleSubscribeAuction(
      Socket socket, BufferedReader in, PrintWriter out, JsonObject request) {

    JsonObject data = request.has("data") ? request.getAsJsonObject("data") : new JsonObject();
    if (!data.has("auctionId")) {
      JsonObject err = new JsonObject();
      err.addProperty("status", "FAIL");
      err.addProperty("message", "Thiếu auctionId");
      out.println(err);
      out.flush();
      try { socket.close(); } catch (IOException ignored) {}
      return;
    }

    int auctionId = data.get("auctionId").getAsInt();

    // Gửi ACK
    JsonObject ack = new JsonObject();
    ack.addProperty("status",    "SUCCESS");
    ack.addProperty("message",   "SUBSCRIBED");
    ack.addProperty("auctionId", auctionId);
    out.println(ack);
    out.flush();

    AuctionRoomBroadcaster.INSTANCE.register(auctionId, out);
    try {
      String line;
      while ((line = in.readLine()) != null) {
        String trimmed = line.trim();
        if (trimmed.isEmpty()) continue;
        try {
          JsonObject msg = JsonParser.parseString(trimmed).getAsJsonObject();
          String a = msg.has("action") ? msg.get("action").getAsString() : "";
          if ("UNSUBSCRIBE_AUCTION".equals(a)) break;
          // SUBSCRIBE_PING hoặc dòng khác → giữ kết nối, không làm gì
        } catch (com.google.gson.JsonSyntaxException ex) {
          System.err.println("⚠️ Subscribe #" + auctionId + " bỏ qua JSON lỗi: "
              + trimmed.substring(0, Math.min(60, trimmed.length())));
        }
      }
    } catch (SocketTimeoutException e) {
      // Subscriber timeout (60s không nhận gì) → bình thường nếu không có bid
      System.out.println("⏱️ Subscribe #" + auctionId + " idle timeout → đóng.");
    } catch (Exception e) {
      System.err.println("⚠️ Subscribe #" + auctionId + " kết thúc: " + e.getMessage());
    } finally {
      AuctionRoomBroadcaster.INSTANCE.unregister(auctionId, out);
      try { socket.close(); } catch (IOException ignored) {}
    }
  }

  // ── Shutdown helper ───────────────────────────────────────────────────────

  private static void shutdownPool(ExecutorService pool, String name) {
    pool.shutdown();
    try {
      if (!pool.awaitTermination(5, TimeUnit.SECONDS)) {
        pool.shutdownNow();
        System.out.println("⚠️ " + name + " forced shutdown.");
      }
    } catch (InterruptedException e) {
      pool.shutdownNow();
      Thread.currentThread().interrupt();
    }
  }
}
