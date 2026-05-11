package com.nhom15.dao;

public class DAOTest {

  static void main(String[] args) {
    System.out.println("--- ĐANG BẮT ĐẦU TEST DAO ---");

    // 1. Test UserDAO
    UserDAO userDAO = new UserDAO();
    boolean userOk = userDAO.registerUser("DucLeader", "123456", "abc123@gmail.com");
    System.out.println("Test UserDAO: " + (userOk ? "Thành công" : "Thất bại"));

    // 2. Test BidDAO
    BidDAO bidDAO = new BidDAO();
    boolean bidOk = bidDAO.placeBid(1, 1, 999.0); // ID 1 giả định là đã có trong DB
    System.out.println("Test BidDAO: " + (bidOk ? "Thành công" : "Thất bại"));

    // 3. Test AuctionDAO
    AuctionDAO auctionDAO = new AuctionDAO();
    System.out.println("Danh sách Auction: " + auctionDAO.getActiveAuctions(10));

    System.out.println("--- KẾT THÚC TEST ---");
  }
}