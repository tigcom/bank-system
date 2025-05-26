package com.example.customer_service.exceptions;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class AppException extends RuntimeException {
    private final ErrorCode errorCode;

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}

