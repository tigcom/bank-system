package com.example.customer_service.responses;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class KycResponse {
    private boolean verified;
    private String message;
    private String details;
}
