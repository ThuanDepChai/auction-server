package com.nhom15.model.user;

import com.nhom15.model.Entity;
import com.nhom15.model.auction.Observer;

public abstract class User extends Entity implements Observer {

  // Thuộc tính chung
  protected String username;
  protected String passwordHash;
  protected String email;
  protected UserRole role;
  protected String fullName = "";
  protected String phone = "";
  protected double balance = 0.0;
  protected String avatarPath = "";

  public User(int id, String username, String passwordHash, String email, UserRole role) {
    this.id = id;
    this.username = username;
    this.passwordHash = passwordHash;
    this.email = email;
    this.role = role;
  }

  public User() {
  }

  public String getFullName() {
    return fullName;
  }

  public void setFullName(String fullName) {
    this.fullName = fullName;
  }

  public String getPhone() {
    return phone;
  }

  public void setPhone(String phone) {
    this.phone = phone;
  }

  public double getBalance() {
    return balance;
  }

  public void setBalance(double balance) {
    this.balance = balance;
  }

  public String getAvatarPath() {
    return avatarPath;
  }

  public void setAvatarPath(String avatarPath) {
    this.avatarPath = avatarPath;
  }

  // Getter & Setter
  public int getId() {
    return id;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getEmail() {
    return email;
  }

  public String getPasswordHash() {
    return passwordHash;
  }

  public void setPasswordHash(String passwordHash) {
    this.passwordHash = passwordHash;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public UserRole getRole() {
    return role;
  }

  public void setRole(UserRole role) {
    this.role = role;
  }

  // Phương thức trừu tượng
  public abstract void printInfo();

  @Override
  public void update(String message) {
    System.out.println("[Thông báo cho " + username + "]: " + message);
  }
}

