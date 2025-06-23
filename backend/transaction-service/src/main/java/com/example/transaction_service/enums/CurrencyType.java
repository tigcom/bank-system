package com.example.transaction_service.enums;

import lombok.Getter;

@Getter
public enum CurrencyType {
    VND("VND"),
    USD("USD"),
    EUR("EUR");

    private final String displayName;

    CurrencyType(String displayName) {
        this.displayName = displayName;
    }
}
