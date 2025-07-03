package com.example.customer_service.services;

import com.example.customer_service.dtos.*;
import com.example.customer_service.models.KycStatus;
import com.example.customer_service.responses.*;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.time.LocalDateTime;

public interface CustomerService {
    ApiResponseWrapper<?> initiateRegister(RegisterCustomerDTO request);
    ApiResponseWrapper<?> processKycAndSendOtp(String email, KycRequest kycRequest);
    ApiResponseWrapper<?> reSendOtp(String email);
    ApiResponseWrapper<?> confirmRegister(String email, String otp);
    ApiResponseWrapper<?> updateCustomer(UpdateCustomerDTO request);
    ApiResponseWrapper<?> updateCustomerPassword(ChangePasswordDTO request);
    ApiResponseWrapper<?> updateCustomerStatus(UpdateStatusRequest request);
    void sentEmailForgotPassword(String email);
    ApiResponseWrapper<?> resetPassword(ResetPasswordDTO request);
    CustomerListResponse getCustomerList(int page, int size, String keyword);
    CustomerResponse getCustomerDetail(String userId);
    CustomerResponse getCustomerDetailByCifCode(String cifCode);
    KycResponse verifyKyc(String userId, KycRequest request);
    KycResponse approveKyc(String cifCode, KycStatus status, String reason);
    KycListResponse getPendingKycRequests(int page, int size, String keyword);
    KycStatisticsResponse getKycStatistics(LocalDate startDate, LocalDate endDate);
    CustomerGrowthResponse getCustomerGrowth(LocalDate startDate, LocalDate endDate);
}