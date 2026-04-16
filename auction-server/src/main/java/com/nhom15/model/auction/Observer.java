package com.nhom15.model.auction;

public interface Observer {
    // Hàm này sẽ được gọi mỗi khi giá của phiên đấu giá thay đổi
    void update(String message);
}