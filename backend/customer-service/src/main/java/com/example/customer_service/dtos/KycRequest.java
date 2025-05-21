package com.example.customer_service.dtos;

import lombok.Data;

@Data
public class KycRequest {
    private Long customerId;
    private String identityNumber;
    private String fullName;
}
