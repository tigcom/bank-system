package com.example.common_service.services.customer;

import com.example.common_service.dto.CustomerDTO;
import com.example.common_service.dto.CustomerResponseDTO;
import com.example.common_service.dto.response.CustomerResponse;

public interface CustomerQueryService {
    CustomerDTO getCustomerByCifCode(String cifCode);
    CustomerResponse getCurrentCustomer();
    CustomerResponseDTO getCustomerById(Long id);

}
