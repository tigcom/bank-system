package com.example.customer_service.responses;

import com.example.customer_service.models.CustomerStatus;
import lombok.Data;

@Data
public class CustomerResponse {
    private Long id;
    private String cifCode;
    private String fullName;
    private String address;
    private String email;
    private String phoneNumber;
    private CustomerStatus status;
    private String kycStatus;
}
