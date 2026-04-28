package com.nhom15.network;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nhom15.dao.UserDAO;
import com.nhom15.model.user.User;
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

                UserService userService = new UserService();

                switch (action) {
                    // 1. KIỂM TRA NHANH USERNAME (Dành cho việc check khi đang gõ)
                    case "CHECK_USERNAME":
                        JsonObject checkData = request.getAsJsonObject("data");
                        String userToCheck = checkData.get("username").getAsString();

                        if (userService.isExists(userToCheck)) {
                            response.addProperty("status", "EXISTS");
                            response.addProperty("message", "Tên đăng nhập đã tồn tại!");
                        } else {
                            response.addProperty("status", "AVAILABLE");
                            response.addProperty("message", "Tên này có thể sử dụng.");
                        }
                        break;

                    // 2. ĐĂNG KÝ MỚI
                    case "REGISTER":
                        JsonObject regData = request.getAsJsonObject("data");
                        String regUser = regData.get("username").getAsString();
                        String regEmail = regData.get("email").getAsString();
                        String regPass = regData.get("password").getAsString();

                        if (userService.register(regUser, regPass, regEmail)) {
                            response.addProperty("status", "SUCCESS");
                            response.addProperty("message", "Tạo tài khoản thành công!");
                        } else {
                            response.addProperty("status", "FAIL");
                            response.addProperty("message", "Đăng ký thất bại (Tên đã tồn tại hoặc lỗi hệ thống)!");
                        }
                        break;

                    // 3. ĐĂNG NHẬP
                    case "LOGIN":
                        JsonObject loginData = request.getAsJsonObject("data");
                        String loginUser = loginData.get("username").getAsString();
                        String loginPass = loginData.get("password").getAsString();

                        User loggedInUser = userService.getUserForLogin(loginUser, loginPass);

                        if (loggedInUser != null) {
                            response.addProperty("status",  "SUCCESS");
                            response.addProperty("message", "Đăng nhập thành công!");
                            response.addProperty("userId",      loggedInUser.getId());
                            response.addProperty("username",    loggedInUser.getUsername());
                            response.addProperty("email",       loggedInUser.getEmail());
                            response.addProperty("role", loggedInUser.getRole().name());
                            response.addProperty("balance",     0.0);   // thêm cột balance vào DB sau
                            response.addProperty("avatarPath",  "");    // thêm cột avatar vào DB sau
                        } else {
                            response.addProperty("status",  "FAIL");
                            response.addProperty("message", "Sai tài khoản hoặc mật khẩu!");
                        }
                        break;
                }

                // Gửi phản hồi duy nhất về Client
                out.println(response.toString());
            }
        } catch (Exception e) {
            System.err.println("❌ Lỗi khi xử lý request: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try { clientSocket.close(); } catch (IOException e) { e.printStackTrace(); }
        }
    }
}