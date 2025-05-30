package com.example.common_service.services.customer;
import com.example.common_service.dto.CustomerResponseDTO;

public interface CustomerService {
    CustomerResponseDTO getCustomer(Long customerId);
}
