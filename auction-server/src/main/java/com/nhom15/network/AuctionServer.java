package com.nhom15.network;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nhom15.dao.UserDAO;
import com.nhom15.service.UserService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class AuctionServer {

    public static void main(String[] args) {
        try {java.net.InetAddress serverIP = java.net.InetAddress.getByName("26.159.224.110");
        ServerSocket serverSocket = new ServerSocket(8888, 50, serverIP);

        System.out.println("✅ Server đang chạy trên IP " + serverIP.getHostAddress() + " ở cổng 8888...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Có Client mới kết nối: " + clientSocket.getInetAddress());

                // Mở luồng đa nhiệm (Thread) để Server phục vụ nhiều Client cùng lúc
                new Thread(() -> handleClient(clientSocket)).start();
            }
        } catch (IOException e) {
            System.err.println("❌ Lỗi khi khởi động Server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void handleClient(Socket clientSocket) {
        try (
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)
        ) {
            String jsonFromClient = in.readLine();
            if (jsonFromClient != null) {
                JsonObject request = JsonParser.parseString(jsonFromClient).getAsJsonObject();
                String action = request.get("action").getAsString();
                JsonObject response = new JsonObject();

                // KHỞI TẠO UserService
                UserService userService = new UserService();

                if ("REGISTER".equals(action)) {
                    JsonObject data = request.getAsJsonObject("data");
                    String username = data.get("username").getAsString();
                    String email = data.get("email").getAsString();
                    String password = data.get("password").getAsString();

                    // GỌI UserDAO ĐỂ LƯU ĐĂNG KÝ
                    // Lưu ý: Thứ tự tham số truyền vào khớp với hàm registerUser(username, password, email) của bạn
                    if (userService.register(username, password, email)) {
                        response.addProperty("status", "SUCCESS");
                        response.addProperty("message", "Tạo tài khoản thành công!");
                    } else {
                        response.addProperty("status", "FAIL");
                        response.addProperty("message", "Tên đăng nhập đã tồn tại hoặc lỗi DB!");
                    }
                }
                // XỬ LÝ ĐĂNG NHẬP
                else if ("LOGIN".equals(action)) {
                    JsonObject data = request.getAsJsonObject("data");
                    String username = data.get("username").getAsString();
                    String password = data.get("password").getAsString();

                    // GỌI UserDAO ĐỂ KIỂM TRA ĐĂNG NHẬP
                    if (userService.login(username, password)) {
                        response.addProperty("status", "SUCCESS");
                        response.addProperty("message", "Đăng nhập thành công!");
                    } else {
                        response.addProperty("status", "FAIL");
                        response.addProperty("message", "Sai tài khoản hoặc mật khẩu!");
                    }
                }

                out.println(response.toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try { clientSocket.close(); } catch (IOException e) { e.printStackTrace(); }
        }
    }
}