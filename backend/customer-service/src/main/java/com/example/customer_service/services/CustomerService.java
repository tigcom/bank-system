package com.example.customer_service.services;

import com.example.customer_service.dtos.*;
import com.example.customer_service.models.Customer;
import com.example.customer_service.responses.*;

import java.util.List;

public interface CustomerService {
    RegisterResponse register(RegisterRequest request);
    LoginResponse login(LoginRequest request);
    ForgotPasswordResponse forgotPassword(String email);
    List<CustomerDTO> getCustomerList();
    CustomerDTO getCustomerDetail(String cifCode);
    UpdateCustomerResponse updateCustomer(UpdateCustomerRequest request);
    UpdateStatusResponse updateCustomerStatus(UpdateStatusRequest request);
}
