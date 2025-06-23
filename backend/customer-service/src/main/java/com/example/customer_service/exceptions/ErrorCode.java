package com.example.customer_service.exceptions;

import lombok.Getter;

@Getter
public enum ErrorCode {

    CUSTOMER_NOTEXISTED("error.customer_not_existed");

    private final String messageKey;

    ErrorCode(String messageKey) {
        this.messageKey = messageKey;
    }
}