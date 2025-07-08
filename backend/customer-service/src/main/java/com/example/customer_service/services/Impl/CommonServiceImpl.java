package com.example.customer_service.services.Impl;

import com.example.common_service.dto.CustomerDTO;
import com.example.common_service.services.CommonService;
import com.example.customer_service.exceptions.AppException;
import com.example.customer_service.exceptions.ErrorCode;
import com.example.customer_service.models.Customer;
import com.example.customer_service.models.KycProfile;
import com.example.customer_service.repositories.CustomerRepository;
import com.example.customer_service.repositories.KycProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

@DubboService(interfaceClass = CommonService.class)
@RequiredArgsConstructor
@Slf4j
public class CommonServiceImpl implements CommonService {

    private final CustomerRepository customerRepository;
    private final KycProfileRepository kycProfileRepository;

    @Override
    public Boolean checkCustomer(String cifCode) {
        return customerRepository.existsByCifCode(cifCode);
    }

    @Override
    public CustomerDTO getCurrentCustomer(String userId) {
        try {
            Customer customer = customerRepository.findCustomerByUserId(userId);
            if (customer == null) {
                throw new AppException(ErrorCode.CUSTOMER_NOTEXISTED);
            }
            
            // Get KYC status
            boolean isKycVerified = getKycStatus(customer);
            
            return CustomerDTO.builder()
                    .customerId(customer.getCustomerId())
                    .userId(customer.getUserId())
                    .cifCode(customer.getCifCode())
                    .username(customer.getUsername())
                    .email(customer.getEmail())
                    .fullName(customer.getFullName())
                    .status(customer.getStatus())
                    .dateOfBirth(customer.getDateOfBirth())
                    .kycVerified(isKycVerified)
                    .build();
        } catch (Exception e) {
            log.error("Error while getting current customer for userId {}: {}", userId, e.getMessage());
            throw new RuntimeException("Unable to get customer info", e);
        }
    }

    @Override
    public CustomerDTO getCustomerByCifCode(String cifCode) {
        Customer customer = customerRepository.findByCifCode(cifCode)
                .orElseThrow(() -> new AppException(ErrorCode.CUSTOMER_NOTEXISTED));
        
        // Get KYC status
        boolean isKycVerified = getKycStatus(customer);
        
        return CustomerDTO.builder()
                .customerId(customer.getCustomerId())
                .userId(customer.getUserId())
                .cifCode(customer.getCifCode())
                .username(customer.getUsername())
                .email(customer.getEmail())
                .fullName(customer.getFullName())
                .status(customer.getStatus())
                .dateOfBirth(customer.getDateOfBirth())
                .identityNumber(customer.getIdentityNumber())
                .kycVerified(isKycVerified)
                .phoneNumber(customer.getPhoneNumber())
                .build();
    }

    private boolean getKycStatus(Customer customer) {
        try {
            Optional<KycProfile> kycProfileOpt = kycProfileRepository.findByCustomer(customer);
            if (kycProfileOpt.isPresent()) {
                return kycProfileOpt.get().getStatus() == com.example.customer_service.models.KycStatus.VERIFIED;
            }
            return false;
        } catch (Exception e) {
            log.warn("Error getting KYC status for customer {}: {}", customer.getCifCode(), e.getMessage());
            return false;
        }
    }
}


