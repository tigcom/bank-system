package com.example.loan_service.service.impl;

import com.example.common_service.dto.customer.CoreCustomerDTO;
import com.example.common_service.dto.customer.CoreResponse;
import com.example.loan_service.dto.request.LoanRequestDTO;
import com.example.loan_service.service.CoreBankingClient;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
public class CoreBankingClientImpl implements CoreBankingClient {
    private final RestTemplate restTemplate;
    @Override
    public CoreResponse syncLoan(LoanRequestDTO dto) {
        ResponseEntity<CoreResponse> response = restTemplate.postForEntity(
                "http://localhost:8083/corebanking/api/core/loans/sync",
                dto,
                CoreResponse.class
        );
        return response.getBody();
    }

    @Override
    public void deleteLoan(long id) {
        restTemplate.delete(
                "http://localhost:8083/corebanking/api/core/loans/sync",
                id
        );
    }
}
