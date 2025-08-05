package com.example.loan_service.service.impl;

import com.example.common_service.dto.customer.CoreCustomerDTO;
import com.example.common_service.dto.customer.CoreResponse;
import com.example.loan_service.dto.request.LoanRequestDTO;
import com.example.loan_service.dto.response.LoanResponseDTO;
import com.example.loan_service.service.CoreBankingClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class CoreBankingClientImpl implements CoreBankingClient {
    @Qualifier("restTemplate")
    private final RestTemplate restTemplate;
    @Override
    public CoreResponse updateAccount(com.example.common_service.dto.CoreAccountRequest request) {
        log.info("UPDATE_ACCOUNT_START - request: {}", request);
        try {
            ResponseEntity<CoreResponse> response = restTemplate.postForEntity(
                    "http://localhost:8083/corebanking/api/core/update-account",
                    request,
                    CoreResponse.class
            );
            log.info("UPDATE_ACCOUNT_SUCCESS - request: {}", request);
            return response.getBody();
        } catch (Exception e) {
            log.error("UPDATE_ACCOUNT_ERROR - request: {}, error: {}", request, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public void deleteLoan(long id) {
        log.info("DELETE_LOAN_START - id: {}", id);
        try {
            restTemplate.delete(
                    "http://localhost:8083/corebanking/api/core/loans/sync",
                    id
            );
            log.info("DELETE_LOAN_SUCCESS - id: {}", id);
        } catch (Exception e) {
            log.error("DELETE_LOAN_ERROR - id: {}, error: {}", id, e.getMessage(), e);
            throw e;
        }
    }

}
