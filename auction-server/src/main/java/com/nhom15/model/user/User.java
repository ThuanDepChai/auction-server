package com.nhom15.model.user;
public abstract class User {
    // Thuộc tính chung
    protected int id;
    protected String username;
    protected String password;
    protected String email;
    protected UserRole role;

    // Constructor
    public User(int id, String username, String password, String email, UserRole role) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.email = email;
        this.role = role;
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

    public void setEmail(String email) {
        this.email = email;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    // Phương thức chung
    public boolean login(String username, String password) {
        // Kiểm tra username và password
        if (this.username.equals(username) && this.password.equals(password)) {
            System.out.println("Đăng nhập thành công!");
            return true;
        } else {
            System.out.println("Sai thông tin đăng nhập!");
            return false;
        }
    }

    public void logout() {
        System.out.println(username + " đã đăng xuất.");
    }

    // Phương thức trừu tượng
    public abstract void printInfo();
}

