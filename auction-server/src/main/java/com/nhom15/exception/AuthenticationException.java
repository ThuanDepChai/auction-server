package com.nhom15.exception;

/**
 * Ném khi xác thực người dùng thất bại:
 * - Sai tài khoản / mật khẩu
 * - Tài khoản không đủ quyền thực hiện thao tác
 */
public class AuthenticationException extends Exception {

    public AuthenticationException(String message) {
        super(message);
    }
}