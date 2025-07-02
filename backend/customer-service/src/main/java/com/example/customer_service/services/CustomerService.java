package com.example.customer_service.services;

import com.example.customer_service.dtos.*;
import com.example.customer_service.responses.*;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;

public interface CustomerService {

    ApiResponseWrapper<?> initiateRegister(RegisterCustomerDTO request);

    ApiResponseWrapper<?> processKycAndSendOtp(String email, KycRequest kycRequest);

    ApiResponseWrapper<?> reSendOtp(String email);

    ApiResponseWrapper<?> confirmRegister(String email, String otp);

    CustomerListResponse getCustomerList(int page, int size, String keyword);

    CustomerResponse getCustomerDetail(String userId);


    CustomerResponse getCustomerDetailByCifCode(String cifCode);

    ApiResponseWrapper<?> updateCustomerPassword(ChangePasswordDTO request);

    ApiResponseWrapper<?> updateCustomer(UpdateCustomerDTO request);

    ApiResponseWrapper<?> updateCustomerStatus(UpdateStatusRequest request);

    KycResponse verifyKyc(String userId, KycRequest request);

    void sentEmailForgotPassword(String email);

    ApiResponseWrapper<?> resetPassword(ResetPasswordDTO request);
}