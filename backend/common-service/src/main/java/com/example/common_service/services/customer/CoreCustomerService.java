package com.example.common_service.services.customer;

import com.example.common_service.dto.customer.CoreCustomerDTO;
import com.example.common_service.dto.customer.CoreResponse;

public interface CoreCustomerService {
    CoreResponse syncCoreCustomer(CoreCustomerDTO coreCustomerDTO);
}


