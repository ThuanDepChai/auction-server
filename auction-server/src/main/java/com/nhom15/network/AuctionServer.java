package com.nhom15.network;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nhom15.dao.UserDAO;
import com.nhom15.model.user.User;
import com.nhom15.service.AuctionService;
import com.nhom15.service.ItemService;
import com.nhom15.service.UserService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class AuctionServer {

    public static void main(String[] args) {
        try {
            java.net.InetAddress serverIP = java.net.InetAddress.getByName("26.159.224.110");
            ServerSocket serverSocket = new ServerSocket(8888, 50, serverIP);

            // Tạo thư mục lưu avatar nếu chưa có
            java.io.File avatarDir = new java.io.File("avatars");
            if (!avatarDir.exists()) avatarDir.mkdirs();

            System.out.println("✅ Server đang chạy trên IP " + serverIP.getHostAddress() + " ở cổng 8888...");
            System.out.println("📁 Thư mục avatars: " + avatarDir.getAbsolutePath());

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
                    case "CHECK_USERNAME": {
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
                    }

                    // 2. ĐĂNG KÝ MỚI
                    case "REGISTER": {
                        JsonObject regData = request.getAsJsonObject("data");
                        String regUser  = regData.get("username").getAsString();
                        String regEmail = regData.get("email").getAsString();
                        String regPass  = regData.get("password").getAsString();
                        if (userService.register(regUser, regPass, regEmail)) {
                            response.addProperty("status", "SUCCESS");
                            response.addProperty("message", "Tạo tài khoản thành công!");
                        } else {
                            response.addProperty("status", "FAIL");
                            response.addProperty("message", "Đăng ký thất bại (Tên đã tồn tại hoặc lỗi hệ thống)!");
                        }
                        break;
                    }

                    // 3. ĐĂNG NHẬP
                    case "LOGIN": {
                        JsonObject loginData = request.getAsJsonObject("data");
                        String loginUser = loginData.get("username").getAsString();
                        String loginPass = loginData.get("password").getAsString();
                        User loggedInUser = userService.getUserForLogin(loginUser, loginPass);
                        if (loggedInUser != null) {
                            response.addProperty("status",     "SUCCESS");
                            response.addProperty("message",    "Đăng nhập thành công!");
                            response.addProperty("userId",     loggedInUser.getId());
                            response.addProperty("username",   loggedInUser.getUsername());
                            response.addProperty("email",      loggedInUser.getEmail());
                            response.addProperty("role",       loggedInUser.getRole().name());
                            response.addProperty("balance",    0.0);  // thêm cột balance vào DB sau
                            response.addProperty("avatarPath", "");   // thêm cột avatar vào DB sau
                        } else {
                            response.addProperty("status",  "FAIL");
                            response.addProperty("message", "Sai tài khoản hoặc mật khẩu!");
                        }
                        break;
                    }

                    // 4. LẤY THÔNG TIN PROFILE ĐẦY ĐỦ
                    case "GET_PROFILE": {
                        JsonObject d = request.getAsJsonObject("data");
                        int uid = d.get("userId").getAsInt();
                        JsonObject profile = userService.getProfile(uid);
                        if (profile != null) {
                            response.addProperty("status", "SUCCESS");
                            response.add("profile", profile);
                        } else {
                            response.addProperty("status",  "FAIL");
                            response.addProperty("message", "Không tìm thấy người dùng!");
                        }
                        break;
                    }

                    // 5. CẬP NHẬT THÔNG TIN CÁ NHÂN
                    case "UPDATE_PROFILE": {
                        JsonObject d    = request.getAsJsonObject("data");
                        int    uid      = d.get("userId").getAsInt();
                        String fullName = d.get("fullName").getAsString();
                        String email    = d.get("email").getAsString();
                        String phone    = d.get("phone").getAsString();
                        boolean ok = userService.updateProfile(uid, fullName, email, phone);
                        response.addProperty("status",  ok ? "SUCCESS" : "FAIL");
                        response.addProperty("message", ok ? "Cập nhật thành công!" : "Cập nhật thất bại!");
                        break;
                    }

                    // 6. ĐỔI MẬT KHẨU
                    case "CHANGE_PASSWORD": {
                        JsonObject d   = request.getAsJsonObject("data");
                        int    uid     = d.get("userId").getAsInt();
                        String oldPass = d.get("oldPassword").getAsString();
                        String newPass = d.get("newPassword").getAsString();
                        boolean ok = userService.changePassword(uid, oldPass, newPass);
                        response.addProperty("status",  ok ? "SUCCESS" : "FAIL");
                        response.addProperty("message", ok ? "Đổi mật khẩu thành công!" : "Mật khẩu hiện tại không đúng!");
                        break;
                    }

                    // 7. NÂNG CẤP LÊN SELLER
                    case "UPGRADE_TO_SELLER": {
                        JsonObject d = request.getAsJsonObject("data");
                        int uid = d.get("userId").getAsInt();
                        boolean ok = userService.upgradeToSeller(uid);
                        response.addProperty("status",  ok ? "SUCCESS" : "FAIL");
                        response.addProperty("message", ok ? "Đăng ký bán hàng thành công!" : "Thất bại, vui lòng thử lại!");
                        break;
                    }

                    // 8. CẬP NHẬT ẢNH ĐẠI DIỆN
                    case "UPDATE_AVATAR": {
                        JsonObject d      = request.getAsJsonObject("data");
                        int    uid        = d.get("userId").getAsInt();
                        String base64Data = d.get("imageBase64").getAsString();
                        String extension  = d.get("extension").getAsString(); // "png" hoặc "jpg"
                        try {
                            byte[] imageBytes = java.util.Base64.getDecoder().decode(base64Data);
                            String fileName   = "avatar_" + uid + "." + extension;
                            String filePath   = "avatars/" + fileName;
                            java.nio.file.Files.write(
                                    java.nio.file.Paths.get(filePath), imageBytes
                            );
                            boolean ok = userService.updateAvatar(uid, filePath);
                            response.addProperty("status",     ok ? "SUCCESS" : "FAIL");
                            response.addProperty("message",    ok ? "Cập nhật ảnh thành công!" : "Lưu DB thất bại!");
                            response.addProperty("avatarPath", filePath);
                        } catch (Exception e) {
                            response.addProperty("status",  "FAIL");
                            response.addProperty("message", "Lỗi xử lý ảnh: " + e.getMessage());
                        }
                        break;
                    }

                    // 9. LẤY ẢNH ĐẠI DIỆN (trả về Base64 để client hiển thị)
                    case "GET_AVATAR": {
                        JsonObject d = request.getAsJsonObject("data");
                        int uid = d.get("userId").getAsInt();
                        String avatarPath = userService.getAvatarPath(uid);
                        try {
                            java.io.File imgFile = new java.io.File(avatarPath);
                            if (avatarPath != null && !avatarPath.isEmpty() && imgFile.exists()) {
                                byte[] imageBytes = java.nio.file.Files.readAllBytes(imgFile.toPath());
                                String base64 = java.util.Base64.getEncoder().encodeToString(imageBytes);
                                response.addProperty("status",      "SUCCESS");
                                response.addProperty("imageBase64", base64);
                                response.addProperty("avatarPath",  avatarPath);
                            } else {
                                response.addProperty("status",  "NO_AVATAR");
                                response.addProperty("message", "Chưa có ảnh đại diện!");
                            }
                        } catch (Exception e) {
                            response.addProperty("status",  "FAIL");
                            response.addProperty("message", "Lỗi đọc ảnh: " + e.getMessage());
                        }
                        break;
                    }
                    // ── ITEM ─────────────────────────────────────────────────────────────────

                    // 10. ĐĂNG SẢN PHẨM MỚI
                    case "CREATE_ITEM": {
                        JsonObject d       = request.getAsJsonObject("data");
                        int    sellerId    = d.get("sellerId").getAsInt();
                        String name        = d.get("name").getAsString();
                        String description = d.get("description").getAsString();
                        String category    = d.get("category").getAsString();
                        double startPrice  = d.get("startPrice").getAsDouble();
                        String imgBase64   = d.has("imageBase64") ? d.get("imageBase64").getAsString() : "";
                        String extension   = d.has("extension")   ? d.get("extension").getAsString()   : "jpg";

                        ItemService itemService = new ItemService();
                        response = itemService.createItem(sellerId, name, description, category, startPrice, imgBase64, extension);
                        break;
                    }

                    // 11. LẤY SẢN PHẨM NỔI BẬT
                    case "GET_FEATURED_PRODUCTS": {
                        ItemService itemService = new ItemService();
                        JsonArray items = itemService.getFeaturedItems();
                        response.addProperty("status", "SUCCESS");
                        response.add("items", items);
                        break;
                    }

                    // 12. TÌM KIẾM SẢN PHẨM
                    case "SEARCH_PRODUCTS": {
                        JsonObject d    = request.getAsJsonObject("data");
                        String keyword  = d.has("keyword")  ? d.get("keyword").getAsString()  : "";
                        String category = d.has("category") ? d.get("category").getAsString() : "";

                        ItemService itemService = new ItemService();
                        JsonArray items = itemService.searchItems(keyword, category);
                        response.addProperty("status", "SUCCESS");
                        response.add("items", items);
                        break;
                    }

                    // 13. LẤY SẢN PHẨM CỦA SELLER
                    case "GET_MY_ITEMS": {
                        JsonObject d = request.getAsJsonObject("data");
                        int sellerId = d.get("sellerId").getAsInt();

                        ItemService itemService = new ItemService();
                        JsonArray items = itemService.getItemsBySeller(sellerId);
                        response.addProperty("status", "SUCCESS");
                        response.add("items", items);
                        break;
                    }

                    // 14. XÓA SẢN PHẨM
                    case "DELETE_ITEM": {
                        JsonObject d = request.getAsJsonObject("data");
                        int itemId   = d.get("itemId").getAsInt();

                        ItemService itemService = new ItemService();
                        boolean ok = itemService.deleteItem(itemId);
                        response.addProperty("status",  ok ? "SUCCESS" : "FAIL");
                        response.addProperty("message", ok ? "Xóa thành công!" : "Xóa thất bại!");
                        break;
                    }

                    // ── AUCTION ───────────────────────────────────────────────────────────────

                    // 15. TẠO PHIÊN ĐẤU GIÁ
                    case "CREATE_AUCTION": {
                        JsonObject d      = request.getAsJsonObject("data");
                        int    itemId     = d.get("itemId").getAsInt();
                        int    sellerId   = d.get("sellerId").getAsInt();
                        double startPrice = d.get("startPrice").getAsDouble();
                        double minStep    = d.get("minStep").getAsDouble();
                        String endTime    = d.get("endTime").getAsString();

                        AuctionService auctionService = new AuctionService();
                        response = auctionService.createAuction(itemId, sellerId, startPrice, minStep, endTime);
                        break;
                    }

                    // 16. LẤY PHIÊN ĐẤU GIÁ ĐANG ACTIVE
                    case "GET_ACTIVE_AUCTIONS": {
                        AuctionService auctionService = new AuctionService();
                        JsonArray auctions = auctionService.getActiveAuctions();
                        response.addProperty("status", "SUCCESS");
                        response.add("auctions", auctions);
                        break;
                    }

                    // 17. CHI TIẾT PHIÊN ĐẤU GIÁ
                    case "GET_AUCTION_DETAIL": {
                        JsonObject d  = request.getAsJsonObject("data");
                        int auctionId = d.get("auctionId").getAsInt();

                        AuctionService auctionService = new AuctionService();
                        JsonObject auction = auctionService.getAuctionDetail(auctionId);
                        if (auction != null) {
                            response.addProperty("status", "SUCCESS");
                            response.add("auction", auction);
                        } else {
                            response.addProperty("status",  "FAIL");
                            response.addProperty("message", "Không tìm thấy phiên đấu giá!");
                        }
                        break;
                    }

                    // 18. ĐẶT GIÁ
                    case "PLACE_BID": {
                        JsonObject d  = request.getAsJsonObject("data");
                        int auctionId = d.get("auctionId").getAsInt();
                        int bidderId  = d.get("bidderId").getAsInt();
                        double amount = d.get("amount").getAsDouble();

                        AuctionService auctionService = new AuctionService();
                        response = auctionService.placeBid(auctionId, bidderId, amount);
                        break;
                    }

                    // 19. LỊCH SỬ ĐẶT GIÁ
                    case "GET_BID_HISTORY": {
                        JsonObject d  = request.getAsJsonObject("data");
                        int auctionId = d.get("auctionId").getAsInt();

                        AuctionService auctionService = new AuctionService();
                        JsonArray history = auctionService.getBidHistory(auctionId);
                        response.addProperty("status", "SUCCESS");
                        response.add("history", history);
                        break;
                    }

                    // 20. LẤY AUCTION CỦA SELLER
                    case "GET_MY_AUCTIONS": {
                        JsonObject d = request.getAsJsonObject("data");
                        int sellerId = d.get("sellerId").getAsInt();

                        AuctionService auctionService = new AuctionService();
                        JsonArray auctions = auctionService.getAuctionsBySeller(sellerId);
                        response.addProperty("status", "SUCCESS");
                        response.add("auctions", auctions);
                        break;
                    }

                    // 21. KẾT THÚC PHIÊN ĐẤU GIÁ
                    case "END_AUCTION": {
                        JsonObject d  = request.getAsJsonObject("data");
                        int auctionId = d.get("auctionId").getAsInt();

                        AuctionService auctionService = new AuctionService();
                        boolean ok = auctionService.endAuction(auctionId);
                        response.addProperty("status",  ok ? "SUCCESS" : "FAIL");
                        response.addProperty("message", ok ? "Kết thúc phiên thành công!" : "Thất bại!");
                        break;
                    }

                    default: {
                        response.addProperty("status",  "ERROR");
                        response.addProperty("message", "Hành động không xác định: " + action);
                        break;
                    }
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