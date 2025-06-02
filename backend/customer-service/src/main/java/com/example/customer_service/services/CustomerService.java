package com.example.customer_service.services;

import com.example.customer_service.dtos.*;
import com.example.customer_service.responses.*;
import org.springframework.http.ResponseEntity;

public interface CustomerService {
//    Response register(RegisterCustomerDTO request) throws Exception;

//    ApiResponseWrapper<?> login(LoginCustomerDTO request) throws Exception;

    Response forgotPassword(String email);

    CustomerListResponse getCustomerList();

    CustomerResponse getCustomerDetail();

    CustomerResponse getCustomerDetailByCifCode(String cifCode);

    ApiResponseWrapper<?> updateCustomerPassword(ChangePasswordDTO request);

    ApiResponseWrapper<?> updateCustomer(UpdateCustomerDTO request);

    ApiResponseWrapper<?> updateCustomerStatus(UpdateStatusRequest request);

    KycResponse verifyKyc(KycRequest request);

    void sentOtpRegister(RegisterCustomerDTO request);

    ApiResponseWrapper<?> confirmRegister(String email, String otp);

    void sentEmailForgotPassword(String email);

    ApiResponseWrapper<?> resetPassword(String token, ResetPasswordDTO request);
}