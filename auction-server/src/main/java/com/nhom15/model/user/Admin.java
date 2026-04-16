package com.nhom15.model.user;
import com.nhom15.model.auction.Auction;
import com.nhom15.model.item.Item;
import java.util.List;

public class Admin extends User {
    public Admin (int id, String username,String password,String email) {
        super(id, username, password, email, UserRole.ADMIN);
    }
    @Override
    public void printInfo(){
        System.out.println("Admin: " + username + " | Email: " + email);
    }

}
