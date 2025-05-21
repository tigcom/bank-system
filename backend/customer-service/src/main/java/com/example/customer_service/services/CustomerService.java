package com.example.customer_service.services;

import com.example.customer_service.dtos.*;
import com.example.customer_service.responses.*;

public interface CustomerService {
    RegisterResponse register(RegisterCustomerDTO request);
    LoginResponse login(LoginCustomerDTO request);
    RegisterResponse forgotPassword(String email);
    CustomerListResponse getCustomerList();
    CustomerResponse getCustomerDetail(String cifCode);
    RegisterResponse updateCustomer(UpdateCustomerDTO request);
    RegisterResponse updateCustomerStatus(UpdateStatusRequest request);
    KycResponse verifyKyc(KycRequest request);
}