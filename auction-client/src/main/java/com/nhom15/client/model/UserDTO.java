package com.nhom15.client.model;

public class UserDTO {
    private String username;
    private String email;
    private String password;

    public UserDTO(String username, String email, String password) {
        this.username = username;
        this.email = email;
        this.password = password;
    }

    // Tạo các Getters và Setters ở đây
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getPassword() { return password; }
}
