package com.nhom15.service;

import com.nhom15.model.auction.Auction;
import com.nhom15.model.item.Item;
import com.nhom15.model.user.User;

import java.util.List;

public class AdminService {
    // Quản lý người dùng
    public void addUser(User user, List<User> users){
        users.add(user);
        System.out.println("Thêm người dùng: " + user.getUsername());
    }
    public void removeUser(User user, List<User> users) {
        if (users.remove(user)) {
            System.out.println("Xóa người dùng: " + user.getUsername());
        } else {
            System.out.println("Không tìm thấy người dùng để xóa: " + user.getUsername());
        }
    }
    // Quản lý vật phẩm
    public void removeItem(Item item, List<Item> items) {
        if (items.remove(item)) {
            System.out.println("Admin đã xóa sản phẩm: " + item.getName());
        } else {
            System.out.println("Không tìm thấy sản phẩm để xóa: " + item.getName());
        }
    }
    // Quản lý phiên đấu giá
    public void cancelAuction(Auction auction, List<Auction> auctions) {
        if (auctions.remove(auction)) {
            System.out.println("Admin đã hủy phiên đấu giá cho sản phẩm: " + auction.getItem().getName());
        } else {
            System.out.println("Không tìm thấy phiên đấu giá để hủy.");
        }
    }
    // Xem báo cáo hệ thống (ví dụ đơn giản)
    public void viewSystemReport(List<User> users, List<Item> items, List<Auction> auctions) {
        System.out.println("===== Báo cáo hệ thống =====");
        System.out.println("Tổng số người dùng: " + users.size());
        System.out.println("Tổng số sản phẩm: " + items.size());
        System.out.println("Tổng số phiên đấu giá: " + auctions.size());
        System.out.println("============================");
    }
}
