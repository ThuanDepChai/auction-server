package com.nhom15.network;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class AuctionServer {
    public static void main(String[] args) {
        int port = 8080; // Số cổng để anh em kết nối vào

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("--- SERVER ĐẤU GIÁ ĐANG TRỰC Ở CỔNG " + port + " ---");

            while (true) {
                // Máy ông sẽ "dừng" ở đây để đợi có người kết nối
                Socket clientSocket = serverSocket.accept();

                System.out.println("Có một ông vừa kết nối vào: " + clientSocket.getInetAddress());

                // Sau này mình sẽ viết thêm logic xử lý tin nhắn của Thuận/Hiệp ở đây
            }
        } catch (IOException e) {
            System.out.println("Lỗi Server rồi " + e.getMessage());
        }
    }
}