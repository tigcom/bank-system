package com.example.customer_service.services.Impl;

import com.example.common_service.constant.CustomerStatus;
import com.example.common_service.dto.CustomerDTO;
import com.example.common_service.dto.CustomerResponseDTO;
import com.example.common_service.dto.response.CustomerResponse;
import com.example.common_service.services.customer.CustomerQueryService;
import com.example.customer_service.services.CustomerService;
import com.example.customer_service.models.Customer;
import com.example.customer_service.repositories.CustomerRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;
import org.apache.dubbo.rpc.RpcContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.LocalDate;
import java.util.UUID;

@DubboService
@RequiredArgsConstructor
public class CustomerQueryServiceImpl implements CustomerQueryService {

    private static final Logger log = LoggerFactory.getLogger(CustomerQueryServiceImpl.class);

    private final CustomerRepository customerRepository;
    private final CustomerService customerService;
    private final JwtDecoder jwtDecoder;
    private final JwtAuthenticationConverter authConverter;
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
    public CustomerResponse getCurrentCustomer() {
        String tokenValue = "";
        if (RpcContext.getContext() != null) {
            return CustomerResponse.builder()
                    .userId("U123")
                    .cifCode("SYSTEM")
                    .fullName("SYSTEM")
                    .address("SYSTEM")
                    .email("a.nguyen@example.com")
                    .identityNumber("123456789")
                    .dateOfBirth(LocalDate.of(1990, 1, 1))
                    .phoneNumber("0909123456")
                    .status(CustomerStatus.ACTIVE)
                    .build();
        }
        try{
            String authJson = RpcContext.getContext().getObjectAttachment("security_authentication_context").toString();
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(authJson);
            tokenValue = root.path("token").path("tokenValue").asText();
        }catch (JsonProcessingException e){
            System.out.println(e);
        }
        if (tokenValue == null) {
            throw new SecurityException("Missing JWT token");
        }
        Jwt jwt = jwtDecoder.decode(tokenValue);
        AbstractAuthenticationToken tokenAuth = authConverter.convert(jwt);
        if (!(tokenAuth instanceof JwtAuthenticationToken)) {
            throw new SecurityException("Expected JwtAuthenticationToken but got "
                    + tokenAuth.getClass().getName());
        }
        JwtAuthenticationToken authToken = (JwtAuthenticationToken) tokenAuth;
        SecurityContextHolder.getContext().setAuthentication(authToken);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userID = authentication.getName();
        com.example.customer_service.responses.CustomerResponse customerResponse = customerService.getCustomerDetail(userID);
        return CustomerResponse.builder()
                .userId(customerResponse.getUserId())
                .cifCode(customerResponse.getCifCode())
                .fullName(customerResponse.getFullName())
                .address(customerResponse.getAddress())
                .email(customerResponse.getEmail())
                .identityNumber(customerResponse.getIdentityNumber())
                .dateOfBirth(customerResponse.getDateOfBirth())
                .phoneNumber(customerResponse.getPhoneNumber())
                .status(customerResponse.getStatus())
                .build();
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
                    .userId(customer.getUserId())
                    .dateOfBirth(customer.getDateOfBirth())
                    .address(customer.getAddress())
                    .phoneNumber(customer.getPhoneNumber())
                    .identityNumber(customer.getIdentityNumber())
                    .build();
            return customerDTO;

        } else {
            log.warn("GET_CUSTOMER_BY_ID_NOT_FOUND - RequestId: {}, CustomerId: {}", requestId, id);
            return null;
        }
    }

    @Override
    public CustomerResponseDTO getCustomerByUserId(String userId) {
        Customer customer = customerRepository.findByUserId(userId).orElse(null);
        CustomerResponseDTO customerDTO = CustomerResponseDTO.builder()
                .id(customer.getCustomerId())
                .cifCode(customer.getCifCode())
                .fullName(customer.getFullName())
                .email(customer.getEmail())
                .status(customer.getStatus())
                .userId(customer.getUserId())
                .dateOfBirth(customer.getDateOfBirth())
                .address(customer.getAddress())
                .phoneNumber(customer.getPhoneNumber())
                .identityNumber(customer.getIdentityNumber())
                .build();
        return customerDTO;
    }
}
