package com.example.customer_service.dtos;

import lombok.Data;

@Data
public class UpdateStatusRequest {
    private Long id;
    private String status;
}
