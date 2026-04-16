package com.nhom15.model.user;
import com.nhom15.model.auction.Auction;
import com.nhom15.model.item.*;
import java.util.List;
public class Seller extends User {
    private List<Item> items;

    public Seller(int id, String username, String password, String email) {
        super(id, username, password, email, UserRole.SELLER);
    }

    @Override
    public void printInfo() {
        System.out.println("Seller: " + username + " | Email: " + email);
    }
}
