package com.nhom15.model.user;

import com.nhom15.model.Entity;
import com.nhom15.model.auction.Observer;
import org.mindrot.jbcrypt.BCrypt;

public abstract class User extends Entity implements Observer {
    // Thuộc tính chung
    protected String username;
    protected String passwordHash;
    protected String email;
    protected UserRole role;

    // Constructor
    public User(int id, String username, String passwordHash, String email, UserRole role) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.email = email;
        this.role = role;
    }
    public User(){}

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

    public String getPasswordHash(){return passwordHash;}

    public void setPasswordHash(String passwordHash){
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

