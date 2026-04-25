package com.nhom15.model.user;

import com.nhom15.model.auction.Auction;
import com.nhom15.model.auction.Bid;
import com.nhom15.model.auction.Observer;

public class Bidder extends User {
    // Constructor
    public Bidder(int id, String username, String passwordHash, String email) {
        super(id, username, passwordHash, email, UserRole.BIDDER);
    }

    @Override
    public void printInfo() {
        System.out.println("Bidder: " + username + " | Email: " + email);
    }
}

