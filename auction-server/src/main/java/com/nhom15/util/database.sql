-- File tạo cấu trúc DB cho cả nhóm
CREATE DATABASE IF NOT EXISTS auction_db;
USE auction_db;

-- User
CREATE TABLE IF NOT EXISTS user (
                                    user_id INT AUTO_INCREMENT PRIMARY KEY,
                                    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    role VARCHAR(20) DEFAULT 'BIDDER'
    );

-- Item
CREATE TABLE IF NOT EXISTS item (
                                    item_id INT AUTO_INCREMENT PRIMARY KEY,
                                    item_name VARCHAR(100) NOT NULL,
    owner_id INT,
    FOREIGN KEY (owner_id) REFERENCES user(user_id)
    );

-- Auction
CREATE TABLE IF NOT EXISTS auction (
                                       auction_id INT AUTO_INCREMENT PRIMARY KEY,
                                       item_id INT,
                                       start_price DOUBLE,
                                       current_price DOUBLE,
                                       end_time BIGINT,
                                       FOREIGN KEY (item_id) REFERENCES item(item_id)
    );

-- Bidtransaction
CREATE TABLE IF NOT EXISTS bidtransaction (
                                              bid_id INT AUTO_INCREMENT PRIMARY KEY,
                                              auction_id INT,
                                              user_id INT,
                                              bid_amount DOUBLE,
                                              FOREIGN KEY (auction_id) REFERENCES auction(auction_id),
    FOREIGN KEY (user_id) REFERENCES user(user_id)
    );