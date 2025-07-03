package com.example.customer_service.services.Impl;

import com.example.common_service.dto.CustomerDTO;
import com.example.common_service.services.CommonService;
import com.example.customer_service.exceptions.AppException;
import com.example.customer_service.exceptions.ErrorCode;
import com.example.customer_service.models.Customer;
import com.example.customer_service.repositories.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

@DubboService(interfaceClass = CommonService.class)
@RequiredArgsConstructor
public class CommonServiceImpl implements CommonService {

    private static final Logger log = LoggerFactory.getLogger(CommonServiceImpl.class);

    private final CustomerRepository customerRepository;

    @Override
    public CustomerDTO getCurrentCustomer(String userId) {
        String requestId = UUID.randomUUID().toString();
        log.info("GET_CURRENT_CUSTOMER - RequestId: {}, UserId: {}", requestId, userId);
        
        try {
            Customer customer = customerRepository.findCustomerByUserId(userId);
            if (customer == null) {
                log.error("GET_CURRENT_CUSTOMER_NOT_FOUND - RequestId: {}, UserId: {}", requestId, userId);
                throw new AppException(ErrorCode.CUSTOMER_NOTEXISTED);
            }
            
            CustomerDTO customerDTO = CustomerDTO.builder()
                    .customerId(customer.getCustomerId())
                    .userId(customer.getUserId())
                    .cifCode(customer.getCifCode())
                    .username(customer.getUsername())
                    .email(customer.getEmail())
                    .fullName(customer.getFullName())
                    .status(customer.getStatus())
                    .dateOfBirth(customer.getDateOfBirth())
                    .build();
            
            log.info("GET_CURRENT_CUSTOMER_SUCCESS - RequestId: {}, UserId: {}, CifCode: {}", requestId, userId, customer.getCifCode());
            return customerDTO;
        } catch (Exception e) {
            log.error("GET_CURRENT_CUSTOMER_FAILED - RequestId: {}, UserId: {}, Error: {}", requestId, userId, e.getMessage(), e);
            throw new RuntimeException("Unable to get customer info", e);
        }
    }

    @Override
    public CustomerDTO getCustomerByCifCode(String cifCode) {
        String requestId = UUID.randomUUID().toString();
        log.info("GET_CUSTOMER_BY_CIF_CODE - RequestId: {}, CifCode: {}", requestId, cifCode);
        
        try {
            Customer customer = customerRepository.findByCifCode(cifCode)
                    .orElseThrow(() -> {
                        log.error("GET_CUSTOMER_BY_CIF_CODE_NOT_FOUND - RequestId: {}, CifCode: {}", requestId, cifCode);
                        return new AppException(ErrorCode.CUSTOMER_NOTEXISTED);
                    });
            
            CustomerDTO customerDTO = CustomerDTO.builder()
                    .customerId(customer.getCustomerId())
                    .userId(customer.getUserId())
                    .cifCode(customer.getCifCode())
                    .username(customer.getUsername())
                    .email(customer.getEmail())
                    .fullName(customer.getFullName())
                    .status(customer.getStatus())
                    .dateOfBirth(customer.getDateOfBirth())
                    .identityNumber(customer.getIdentityNumber())
                    .build();
            
            log.info("GET_CUSTOMER_BY_CIF_CODE_SUCCESS - RequestId: {}, CifCode: {}, Status: {}", requestId, cifCode, customer.getStatus());
            return customerDTO;
        } catch (Exception e) {
            log.error("GET_CUSTOMER_BY_CIF_CODE_FAILED - RequestId: {}, CifCode: {}, Error: {}", requestId, cifCode, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public Boolean checkCustomer(String cifCode) {
        String requestId = UUID.randomUUID().toString();
        log.info("CHECK_CUSTOMER - RequestId: {}, CifCode: {}", requestId, cifCode);
        
        try {
            Customer customer = customerRepository.findByCifCode(cifCode)
                    .orElseThrow(() -> {
                        log.error("CHECK_CUSTOMER_NOT_FOUND - RequestId: {}, CifCode: {}", requestId, cifCode);
                        return new RuntimeException("Customer not found with CIF code: " + cifCode);
                    });
            
            boolean isActive = customer.getStatus().equals("ACTIVE");
            log.info("CHECK_CUSTOMER_RESULT - RequestId: {}, CifCode: {}, Status: {}, IsActive: {}", 
                    requestId, cifCode, customer.getStatus(), isActive);
            return isActive;
        } catch (Exception e) {
            log.error("CHECK_CUSTOMER_FAILED - RequestId: {}, CifCode: {}, Error: {}", requestId, cifCode, e.getMessage(), e);
            throw e;
        }
    }
}


