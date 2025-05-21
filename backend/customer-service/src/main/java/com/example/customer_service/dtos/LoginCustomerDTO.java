package com.example.customer_service.dtos;

import lombok.Data;

@Data
public class LoginCustomerDTO {
    private String email;
    private String password;
}
