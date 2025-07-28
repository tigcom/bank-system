package com.example.loan_service.service.impl;

import com.example.common_service.dto.customer.CoreResponse;
import com.example.common_service.dto.request.CICRequest;
import com.example.loan_service.dto.response.CicResponse;
import com.example.loan_service.response.ApiResponseWrapper;
import com.example.loan_service.service.CICClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
@Slf4j
@Service
public class CICClientImpl implements CICClient {
    private final RestTemplate restTemplate;

    public CICClientImpl(
            @Qualifier("interServiceRestTemplate")
            RestTemplate restTemplate
    ) {
        this.restTemplate = restTemplate;
    }
    @Override
    public CicResponse checkCIC(CICRequest cicRequest) {
        log.info("CHECK_CIC - CICRequest: {}", cicRequest);
        try {
            ResponseEntity<ApiResponseWrapper<CicResponse>> resp = restTemplate.exchange(
                    "http://localhost:8085/api/cic-servive/check-cic",
                    HttpMethod.POST,
                    new HttpEntity<>(cicRequest),
                    new ParameterizedTypeReference<ApiResponseWrapper<CicResponse>>() {}
            );
            CicResponse body = resp.getBody().getData();
            log.info("CHECK_CIC - CICResponse: {}", body);
            return body;
        } catch (Exception e) {
            log.error("CHECK_CIC_ERROR - cicRequest: {}, error: {}", cicRequest, e.getMessage(), e);
            throw e;
        }
    }
}
