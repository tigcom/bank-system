package com.example.customer_service.services;

import com.example.customer_service.dtos.*;
import com.example.customer_service.responses.*;

public interface CustomerService {
    Response register(RegisterCustomerDTO request) throws Exception;
//    LoginResponse login(LoginCustomerDTO request) throws Exception;
    Response forgotPassword(String email);
    CustomerListResponse getCustomerList();
    CustomerResponse getCustomerDetail(String cifCode);
    Response updateCustomerPassword(ChangePasswordDTO request);
    Response updateCustomer(UpdateCustomerDTO request);
    Response updateCustomerStatus(UpdateStatusRequest request);
    KycResponse verifyKyc(KycRequest request);
}