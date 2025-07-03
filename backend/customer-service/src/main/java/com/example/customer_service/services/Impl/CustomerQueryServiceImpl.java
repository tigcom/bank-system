package com.example.customer_service.services.Impl;

import com.example.common_service.dto.CustomerDTO;
import com.example.common_service.dto.CustomerResponseDTO;
import com.example.common_service.services.customer.CustomerQueryService;
import com.example.customer_service.models.Customer;
import com.example.customer_service.repositories.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

@DubboService
@RequiredArgsConstructor
public class CustomerQueryServiceImpl implements CustomerQueryService {

    private static final Logger log = LoggerFactory.getLogger(CustomerQueryServiceImpl.class);

    private final CustomerRepository customerRepository;

    @Override
    public CustomerDTO getCustomerByCifCode(String cifCode) {
        String requestId = UUID.randomUUID().toString();
        log.info("GET_CUSTOMER_BY_CIF - RequestId: {}, CifCode: {}", requestId, cifCode);
        
        Customer customer = customerRepository.findByCifCode(cifCode)
                .orElse(null);
        
        if (customer != null) {
            log.info("GET_CUSTOMER_BY_CIF_SUCCESS - RequestId: {}, CifCode: {}, Status: {}", requestId, cifCode, customer.getStatus());
            CustomerDTO customerDTO = CustomerDTO.builder()
                    .customerId(customer.getCustomerId())
                    .userId(customer.getUserId())
                    .cifCode(customer.getCifCode())
                    .username(customer.getUsername())
                    .fullName(customer.getFullName())
                    .email(customer.getEmail())
                    .status(customer.getStatus())
                    .build();
            return customerDTO;
        } else {
            log.warn("GET_CUSTOMER_BY_CIF_NOT_FOUND - RequestId: {}, CifCode: {}", requestId, cifCode);
            return null;
        }
    }

    @Override
    public CustomerResponseDTO getCustomerById(Long id) {
        String requestId = UUID.randomUUID().toString();
        log.info("GET_CUSTOMER_BY_ID - RequestId: {}, CustomerId: {}", requestId, id);
        
        Customer customer = customerRepository.findById(id)
                .orElse(null);
        
        if (customer != null) {
            log.info("GET_CUSTOMER_BY_ID_SUCCESS - RequestId: {}, CustomerId: {}, CifCode: {}", requestId, id, customer.getCifCode());
            CustomerResponseDTO customerDTO = CustomerResponseDTO.builder()
                    .id(customer.getCustomerId())
                    .cifCode(customer.getCifCode())
                    .fullName(customer.getFullName())
                    .email(customer.getEmail())
                    .status(customer.getStatus())
                    .dateOfBirth(customer.getDateOfBirth())
                    .build();
            return customerDTO;
        } else {
            log.warn("GET_CUSTOMER_BY_ID_NOT_FOUND - RequestId: {}, CustomerId: {}", requestId, id);
            return null;
        }
    }
}
