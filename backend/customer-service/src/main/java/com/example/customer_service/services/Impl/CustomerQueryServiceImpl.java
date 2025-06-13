package com.example.customer_service.services.Impl;


import com.example.common_service.dto.CustomerDTO;
import com.example.common_service.dto.CustomerResponseDTO;
import com.example.common_service.services.customer.CustomerQueryService;
import com.example.customer_service.models.Customer;
import com.example.customer_service.repositories.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;

@DubboService
@RequiredArgsConstructor
public class CustomerQueryServiceImpl implements CustomerQueryService {

    private final CustomerRepository customerRepository;

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
