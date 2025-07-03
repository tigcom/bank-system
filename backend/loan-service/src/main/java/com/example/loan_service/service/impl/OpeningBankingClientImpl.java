package com.example.loan_service.service.impl;

import com.example.common_service.dto.request.CICRequest;
import com.example.loan_service.dto.request.InfoIncomeRequestDto;
import com.example.loan_service.dto.response.CicResponse;
import com.example.loan_service.dto.response.TransactionDto;
import com.example.loan_service.response.ApiResponseWrapper;
import com.example.loan_service.service.CICClient;
import com.example.loan_service.service.OpeningBankingClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Slf4j
@Service
public class OpeningBankingClientImpl implements OpeningBankingClient {
    private final RestTemplate restTemplate;

    public OpeningBankingClientImpl(
            @Qualifier("interServiceRestTemplate")
            RestTemplate restTemplate
    ) {
        this.restTemplate = restTemplate;
    }
    @Override
    public List<TransactionDto> checkIncome(InfoIncomeRequestDto infoIncome) {
        log.info("CHECK_INCOME - Request: {}", infoIncome);
        try {
            ResponseEntity<ApiResponseWrapper<List<TransactionDto>>> resp = restTemplate.exchange(
                    "http://localhost:9085/api/openingbanking-servive/check-income",
                    HttpMethod.POST,
                    new HttpEntity<>(infoIncome),
                    new ParameterizedTypeReference<ApiResponseWrapper<List<TransactionDto>>>() {}
            );
            List<TransactionDto> body = resp.getBody().getData();
            log.info("CHECK_INCOME - CICResponse: {}", body);
            return body;
        } catch (Exception e) {
            log.error("CHECKI_NCOME_ERROR - infoIncome: {}, error: {}", infoIncome, e.getMessage(), e);
            throw e;
        }
    }
}
