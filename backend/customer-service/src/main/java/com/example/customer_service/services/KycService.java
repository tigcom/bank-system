package com.example.customer_service.services;

import com.example.customer_service.responses.KycResponse;

public interface KycService {
    KycResponse verifyIdentity(String identityNumber, String fullName);
}
