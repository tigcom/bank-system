package com.example.customer_service.dtos;

import com.example.customer_service.models.CustomerStatus;
import lombok.Data;

@Data
public class UpdateStatusRequest {
    private Long id;
    private CustomerStatus status;
}
