package com.example.customer_service.services;

import com.example.customer_service.responses.KycResponse;

import java.time.LocalDate;

public interface KycService {

    KycResponse verifyIdentity(String identityNumber, String fullName);

    void saveKycInfo(Long customerId, KycResponse kycResponse, String identityNumber, String fullName,
                     LocalDate dateOfBirth, String gender);
}