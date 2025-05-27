package com.example.common_service.services;

import com.example.common_service.dto.CustomerDTO;

public interface CommonService {
    Boolean checkCustomer(String cifCode);

    CustomerDTO getCurrentCustomer(String userID);

    CustomerDTO getCustomerByCifCode(String cifCode);
}
