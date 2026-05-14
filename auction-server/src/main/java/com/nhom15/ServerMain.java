package com.nhom15;

import com.nhom15.network.AuctionServer;

/**
 * ServerMain — entry point duy nhất để khởi động server.
 *
 * <p>Chỉ có 1 nhiệm vụ: gọi AuctionServer.main() để bắt đầu lắng nghe kết nối.
 * Mọi business logic nằm trong các Handler / Service / DAO tương ứng.
 */
public class ServerMain {

  public static void main(String[] args) {
    AuctionServer.main(args);
  }
}