package com.nhom15.model.user;

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

