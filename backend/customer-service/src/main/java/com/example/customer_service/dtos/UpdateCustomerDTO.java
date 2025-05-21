package com.example.customer_service.dtos;

import lombok.Data;

@Data
public class UpdateCustomerDTO {
    private Long id;
    private String fullName;
    private String address;
    private String phoneNumber;
}
