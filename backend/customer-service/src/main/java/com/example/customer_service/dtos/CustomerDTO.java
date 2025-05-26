package com.example.customer_service.dtos;

import com.example.customer_service.models.CustomerStatus;
import lombok.Data;

@Data
public class CustomerDTO {
    private Long id;
    private String cifCode;
    private String fullName;
    private String address;
    private String email;
    private String phoneNumber;
    private CustomerStatus status;
    private String kycStatus;
}
