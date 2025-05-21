package com.example.customer_service.dtos;

import lombok.Data;

@Data
public class RegisterCustomerDTO {
    private String fullName;
    private String address;
    private String identityNumber;
    private String email;
    private String phoneNumber;
    private String password;
}
