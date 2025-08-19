package com.example.loan_service.service.impl;

import com.example.common_service.dto.customer.CoreResponse;
import com.example.common_service.dto.request.CICRequest;
import com.example.loan_service.dto.response.CicResponse;
import com.example.loan_service.response.ApiResponseWrapper;
import com.example.loan_service.service.CICClient;
import com.example.loan_service.service.LoanMetricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import io.micrometer.core.instrument.Timer;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
@Slf4j
@Service
public class CICClientImpl implements CICClient {
    private final RestTemplate restTemplate;
    private final LoanMetricsService metricsService;

    public CICClientImpl(
            @Qualifier("interServiceRestTemplate")
            RestTemplate restTemplate,
            LoanMetricsService metricsService
    ) {
        this.restTemplate = restTemplate;
        this.metricsService = metricsService;
    }
    @Override
    @CircuitBreaker(name = "cicService", fallbackMethod = "checkCICFallback")
    @Retry(name = "cicService", fallbackMethod = "checkCICFallback")
    @Bulkhead(name = "externalApiCalls", fallbackMethod = "checkCICFallback")
    public CicResponse checkCIC(CICRequest cicRequest) {
        log.info("CHECK_CIC - CICRequest: {}", cicRequest);
        Timer.Sample timer = metricsService.startCicCheck();
        
        try {
            // Increment CIC checks counter
            metricsService.incrementCicChecks();
            
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
            throw new RuntimeException("Failed to check CIC: " + e.getMessage(), e);
        } finally {
            metricsService.stopCicCheck(timer);
        }
    }

    public CicResponse checkCICFallback(CICRequest cicRequest, Throwable t) {
        log.warn("CHECK_CIC_FALLBACK - cicRequest: {}, error: {}", cicRequest, t.getMessage());
        // Return a default CIC response indicating service unavailable
        CicResponse fallbackResponse = new CicResponse();
        fallbackResponse.setStatus("error");
        fallbackResponse.setMessage("CIC service is temporarily unavailable: " + t.getMessage());
        fallbackResponse.setCreditScore(0);
        fallbackResponse.setOverdue(false);
        fallbackResponse.setDebtGroup(0);
        return fallbackResponse;
    }
}
