package com.example.customer_service.responses;

import lombok.Data;

@Data
public class UpdateCustomerResponse {
    private boolean success;
    private String message;

    public UpdateCustomerResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }
}
