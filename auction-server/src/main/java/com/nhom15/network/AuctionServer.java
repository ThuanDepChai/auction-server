package com.nhom15.network;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nhom15.model.user.User;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet; // Đã thêm thư viện này cho phần Login
import java.sql.SQLException;

public class AuctionServer {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/auction_db";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "12345678";

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(8888)) {
            System.out.println("✅ Server đang chạy và lắng nghe tại cổng 3306...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Có Client mới kết nối: " + clientSocket.getInetAddress());

                // Mở luồng đa nhiệm (Thread) để Server phục vụ nhiều Client cùng lúc
                new Thread(() -> handleClient(clientSocket)).start();
            }
        } catch (IOException e) {
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

                if ("REGISTER".equals(action)) {
                    JsonObject data = request.getAsJsonObject("data");
                    String username = data.get("username").getAsString();
                    String email = data.get("email").getAsString();
                    String password = data.get("password").getAsString();

                    // GỌI DATABASE ĐỂ LƯU ĐĂNG KÝ
                    if (registerUserToDB(username, email, password)) {
                        response.addProperty("status", "SUCCESS");
                        response.addProperty("message", "Tạo tài khoản thành công!");
                    } else {
                        response.addProperty("status", "FAIL");
                        response.addProperty("message", "Tên đăng nhập đã tồn tại hoặc lỗi DB!");
                    }
                }
                // ĐÃ THÊM: XỬ LÝ ĐĂNG NHẬP
                else if ("LOGIN".equals(action)) {
                    JsonObject data = request.getAsJsonObject("data");
                    String username = data.get("username").getAsString();
                    String password = data.get("password").getAsString();

                    // GỌI DATABASE ĐỂ KIỂM TRA ĐĂNG NHẬP
                    if (loginUserFromDB(username, password)) {
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

    // Hàm thực thi câu lệnh INSERT vào MySQL (ĐĂNG KÝ)
    private static boolean registerUserToDB(String username, String email, String password) {
        String sql = "INSERT INTO users (username, email, password) VALUES (?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            pstmt.setString(2, email);
            pstmt.setString(3, password);

            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            System.err.println("Lỗi Database: " + e.getMessage());
            return false;
        }
    }

    // Hàm kiểm tra ĐĂNG NHẬP từ MySQL
    private static boolean loginUserFromDB(String username, String password) {
        String sql = "SELECT * FROM users WHERE username = ? AND password = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            pstmt.setString(2, password);

            ResultSet rs = pstmt.executeQuery();
            // Nếu rs.next() trả về true, nghĩa là tìm thấy ít nhất 1 dòng khớp (đúng tài khoản/mật khẩu)
            return rs.next();

        } catch (SQLException e) {
            System.err.println("Lỗi Database: " + e.getMessage());
            return false;
        }
    }
}