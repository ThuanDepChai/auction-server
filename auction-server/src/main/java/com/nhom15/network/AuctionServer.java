package com.nhom15.network;

import com.nhom15.network.handler.RequestHandler;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * AuctionServer — chỉ chịu trách nhiệm:
 *  1. Khởi động ServerSocket
 *  2. Accept kết nối
 *  3. Đọc JSON từ client
 *  4. Chuyển cho RequestHandler xử lý
 *  5. Ghi JSON response về client
 *
 * Không có bất kỳ business logic nào ở đây.
 */
public class AuctionServer {

    private static final String SERVER_IP = "26.159.224.110";
    private static final int    PORT      = 8888;

    // RequestHandler dùng chung — thread-safe vì các handler không có state mutable
    private static final RequestHandler requestHandler = new RequestHandler();

    public static void main(String[] args) {
        // Tạo thư mục lưu avatar nếu chưa có
        File avatarDir = new File("avatars");
        if (!avatarDir.exists()) avatarDir.mkdirs();

        try {
            InetAddress serverIP    = InetAddress.getByName(SERVER_IP);
            ServerSocket serverSocket = new ServerSocket(PORT, 50, serverIP);

            System.out.println("✅ Server đang chạy trên IP " + serverIP.getHostAddress() + " ở cổng " + PORT + "...");
            System.out.println("📁 Thư mục avatars: " + avatarDir.getAbsolutePath());

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("🔌 Client mới kết nối: " + clientSocket.getInetAddress());
                // Mỗi client chạy trên thread riêng
                new Thread(() -> handleClient(clientSocket)).start();
            }
        } catch (IOException e) {
            System.err.println("❌ Lỗi khi khởi động Server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void handleClient(Socket clientSocket) {
        try (
          BufferedReader in  = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
          PrintWriter    out = new PrintWriter(clientSocket.getOutputStream(), true)
        ) {
            String jsonFromClient = in.readLine();
            if (jsonFromClient == null) return;

            JsonObject request  = JsonParser.parseString(jsonFromClient).getAsJsonObject();
            JsonObject response = requestHandler.handle(request);

            out.println(response.toString());

        } catch (Exception e) {
            System.err.println("❌ Lỗi khi xử lý request từ " + clientSocket.getInetAddress()
              + ": " + e.getMessage());
            e.printStackTrace();
        } finally {
            try { clientSocket.close(); } catch (IOException e) { e.printStackTrace(); }
        }
    }
}