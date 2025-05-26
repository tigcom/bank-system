package com.example.customer_service.exceptions;

import lombok.Getter;

@Getter
public enum ErrorCode {
    USER_NOT_FOUND("Không tìm thấy người dùng"),
    USERNAME_EXISTS("Tên đăng nhập đã tồn tại"),
    EMAIL_EXISTS("Email đã tồn tại"),
    KEYCLOAK_ERROR("Lỗi khi gọi Keycloak API"),
    INVALID_CREDENTIALS("Sai tên đăng nhập hoặc mật khẩu");

    private final String message;

    ErrorCode(String message) {
        this.message = message;
    }

}
