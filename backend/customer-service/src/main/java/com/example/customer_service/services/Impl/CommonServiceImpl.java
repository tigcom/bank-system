package com.example.customer_service.services.Impl;

import com.example.common_service.services.CommonService;
import com.example.customer_service.models.Customer;
import com.example.customer_service.repositories.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;

@DubboService(interfaceClass = CommonService.class)
@RequiredArgsConstructor
public class CommonServiceImpl implements CommonService {
    private final CustomerRepository customerRepository;

    @Override
    public Boolean checkCustomer(String cifCode) {
        Customer customer = customerRepository.findByCifCode(cifCode)
                .orElseThrow(() -> new RuntimeException("Customer not found with CIF code: " + cifCode));
        if (customer.getStatus().equals("ACTIVE")) {
            return true;
        } else {
            return false;
        }
    }
}


