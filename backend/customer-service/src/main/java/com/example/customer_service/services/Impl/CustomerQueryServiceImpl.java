package com.example.customer_service.services.Impl;


import com.example.common_service.dto.CustomerDTO;
import com.example.common_service.dto.CustomerResponseDTO;
import com.example.common_service.dto.response.CustomerResponse;
import com.example.common_service.services.customer.CustomerQueryService;
import com.example.customer_service.models.Customer;
import com.example.customer_service.repositories.CustomerRepository;
import com.example.customer_service.services.CustomerService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;
import org.apache.dubbo.rpc.RpcContext;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

@DubboService
@RequiredArgsConstructor
public class CustomerQueryServiceImpl implements CustomerQueryService {

    private final CustomerService customerService;
    private final CustomerRepository customerRepository;
    private final JwtDecoder jwtDecoder;
    private final JwtAuthenticationConverter authConverter;

    @Override
    public CustomerDTO getCustomerByCifCode(String cifCode) {
        Customer customer = customerRepository.findByCifCode(cifCode)
                .orElse(null);
        System.out.println(customer.getStatus());
        if(customer!=null){
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
        }
        else return null;
    }

    @Override
    public CustomerResponse getCurrentCustomer() {
        String tokenValue = "";
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
        Customer customer = customerRepository.findById(id)
                .orElse(null);
        if(customer!=null){
            CustomerResponseDTO customerDTO = CustomerResponseDTO.builder()
                    .id(customer.getCustomerId())
                    .cifCode(customer.getCifCode())
                    .fullName(customer.getFullName())
                    .email(customer.getEmail())
                    .status(customer.getStatus())
                    .dateOfBirth(customer.getDateOfBirth())
                    .build();
            return customerDTO;
        }
        else return null;
    }

    
}
