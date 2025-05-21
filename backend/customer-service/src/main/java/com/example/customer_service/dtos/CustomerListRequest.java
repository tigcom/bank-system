package com.example.customer_service.dtos;

import lombok.Data;

@Data
public class CustomerListRequest {
    private String name;
    private String address;
}
