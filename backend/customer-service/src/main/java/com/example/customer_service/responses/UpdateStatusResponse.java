package com.example.customer_service.responses;

import lombok.Data;

@Data
public class UpdateStatusResponse {
    private boolean success;
    private String message;

    public UpdateStatusResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }
}
