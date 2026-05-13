-- File tạo cấu trúc DB cho toàn bộ hệ thống
-- Chỉ chạy file này MỘT LẦN khi setup lần đầu
CREATE DATABASE IF NOT EXISTS auction_db;
USE auction_db;

-- ============================================================
-- 1. Bảng USER
-- ============================================================
CREATE TABLE IF NOT EXISTS user
(
    user_id    INT AUTO_INCREMENT PRIMARY KEY,
    username   VARCHAR(50)  NOT NULL UNIQUE,
    password   VARCHAR(255) NOT NULL,
    email      VARCHAR(100) NOT NULL UNIQUE,
    role       VARCHAR(20)  DEFAULT 'BIDDER',
    full_name  VARCHAR(100) DEFAULT NULL,
    phone      VARCHAR(20)  DEFAULT NULL,
    balance    DOUBLE       DEFAULT 0.0,
    avatar_path VARCHAR(255) DEFAULT NULL,
    created_at TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
    );

-- ============================================================
-- 2. Bảng ITEM (Sản phẩm)
-- ============================================================
CREATE TABLE IF NOT EXISTS item
(
    item_id     INT AUTO_INCREMENT PRIMARY KEY,
    seller_id   INT          NOT NULL,
    name        VARCHAR(255) NOT NULL,
    description TEXT,
    category    VARCHAR(100),
    start_price DOUBLE  DEFAULT 0.0,
    image_path  VARCHAR(255),
    status      VARCHAR(50) DEFAULT 'AVAILABLE',
    created_at  TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (seller_id) REFERENCES user (user_id)
    );

-- ============================================================
-- 3. Bảng AUCTION (Phiên đấu giá)
-- ============================================================
CREATE TABLE IF NOT EXISTS auction
(
    auction_id    INT AUTO_INCREMENT PRIMARY KEY,
    item_id       INT    NOT NULL,
    seller_id     INT    NOT NULL,
    start_price   DOUBLE DEFAULT 0.0,
    current_price DOUBLE DEFAULT 0.0,
    min_step      DOUBLE DEFAULT 0.0,
    winner_id     INT,
    start_time    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    end_time      DATETIME,
    status        VARCHAR(50) DEFAULT 'ACTIVE',
    FOREIGN KEY (item_id)    REFERENCES item (item_id),
    FOREIGN KEY (seller_id)  REFERENCES user (user_id),
    FOREIGN KEY (winner_id)  REFERENCES user (user_id)
    );

-- ============================================================
-- 4. Bảng BID (Lịch sử trả giá)
-- ============================================================
CREATE TABLE IF NOT EXISTS bid
(
    bid_id     INT AUTO_INCREMENT PRIMARY KEY,
    auction_id INT    NOT NULL,
    bidder_id  INT    NOT NULL,
    amount     DOUBLE NOT NULL,
    bid_time   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (auction_id) REFERENCES auction (auction_id),
    FOREIGN KEY (bidder_id)  REFERENCES user (user_id)
    );

-- ============================================================
-- 5. Bảng AUTO_BID (Đấu giá tự động)
-- ============================================================
CREATE TABLE IF NOT EXISTS auto_bid
(
    auto_bid_id    INT AUTO_INCREMENT PRIMARY KEY,
    auction_id     INT    NOT NULL,
    bidder_id      INT    NOT NULL,
    max_bid        DOUBLE NOT NULL,
    increment_step DOUBLE NOT NULL,
    active         BOOLEAN DEFAULT TRUE,
    created_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_auto_bid (auction_id, bidder_id),
    FOREIGN KEY (auction_id) REFERENCES auction (auction_id),
    FOREIGN KEY (bidder_id)  REFERENCES user (user_id)
    );