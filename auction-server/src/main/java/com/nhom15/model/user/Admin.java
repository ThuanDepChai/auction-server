package com.nhom15.model.user;

public class Admin extends User {

  public Admin(int id, String username, String passwordHash, String email) {
    super(id, username, passwordHash, email, UserRole.ADMIN);
  }

  @Override
  public void printInfo() {
    System.out.println("Admin: " + username + " | Email: " + email);
  }

}
