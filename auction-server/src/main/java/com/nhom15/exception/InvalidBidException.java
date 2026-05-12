package com.nhom15.exception;

/**
 * Ném khi giá đặt không hợp lệ:
 * - Thấp hơn giá hiện tại
 * - Không đủ bước giá tối thiểu (minStep)
 */
public class InvalidBidException extends Exception {

    private final double currentPrice;
    private final double minRequired;

    public InvalidBidException(double currentPrice, double minRequired) {
        super(String.format(
                "Giá đặt không hợp lệ! Giá tối thiểu phải là %.0f (hiện tại: %.0f + bước: %.0f)",
                minRequired, currentPrice, minRequired - currentPrice));
        this.currentPrice = currentPrice;
        this.minRequired = minRequired;
    }

    public double getCurrentPrice() {
        return currentPrice;
    }

    public double getMinRequired() {
        return minRequired;
    }
}