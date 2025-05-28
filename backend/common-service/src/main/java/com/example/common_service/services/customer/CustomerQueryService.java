package com.example.common_service.services.customer;

import com.example.common_service.dto.CustomerDTO;

public interface CustomerQueryService {
    CustomerDTO getCustomerByCifCode(String cifCode);
}
