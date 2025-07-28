package com.cic.cicservice.service.impl;

import com.example.common_service.dto.request.CICRequest;
import com.cic.cicservice.dto.response.CicResponse;
import com.cic.cicservice.service.CICService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CICServiceImpl implements CICService {
    private final RestTemplate restTemplate;

    @Value("${cic.api.url}")
    private String cicApiUrl;

    @Value("${cic.api.key}")
    private String cicApiKey;

    @Override
    @CircuitBreaker(name = "cicCheck", fallbackMethod = "fallbackCicCheck")
    @Retry(name = "cicCheck")
    public CicResponse checkCIC(CICRequest cicRequest) {
        log.info("Calling CIC API for credit check: {}", cicRequest);

        Map<String, String> request = new HashMap<>();
        request.put("idNumber", cicRequest.getIdNumber());
        request.put("name", cicRequest.getName() != null ? cicRequest.getName() : "Nguyen Van A");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(cicApiKey, "ApiKey abc123xyz");

        HttpEntity<Map<String, String>> entity = new HttpEntity<>(request, headers);

        try {
            ResponseEntity<CicResponse> response = restTemplate.postForEntity(
                cicApiUrl + "/check", entity, CicResponse.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                CicResponse responseBody = response.getBody();
                if (responseBody != null) {
                    log.info("CIC check response for ID {}: status={}, creditScore={}", 
                            cicRequest.getIdNumber(), responseBody.getStatus(), responseBody.getCreditScore());

                    // Check if it's a business logic failure that shouldn't be retried
                    if ("failed".equals(responseBody.getStatus()) && isBusinessLogicFailure(responseBody)) {
                        log.warn("Business logic failure for ID {}, will not retry: {}",
                                cicRequest.getIdNumber(), responseBody.getStatus());
                        return responseBody; // Return as-is, don't throw exception to avoid retry
                    }
                    return responseBody;
                } else {
                    log.error("CIC API returned null body for ID: {}", cicRequest.getIdNumber());
                    throw new RuntimeException("CIC API returned null response body");
                }
            } else {
                log.error("CIC API returned non-success status: {} for ID: {}",
                        response.getStatusCode(), cicRequest.getIdNumber());
                throw new RuntimeException("CIC API returned status: " + response.getStatusCode());
            }
        } catch (HttpClientErrorException e) {
            // Handle 4xx errors differently
            if (e.getStatusCode().is4xxClientError()) {
                log.error("Client error calling CIC API for ID: {}. Status: {}, Error: {}",
                        cicRequest.getIdNumber(), e.getStatusCode(), e.getMessage());

                // 4xx errors usually shouldn't be retried (except 429)
                if (e.getStatusCode().value() == 429) {
                    throw e; // Let retry handle rate limiting
                } else {
                    return CicResponse.builder()
                            .status("failed")
                            .creditScore(0)
                            .overdue(true)
                            .debtGroup(5)
                            .errorCode("CLIENT_ERROR")
                            .message("Client error: " + e.getMessage())
                            .build();
                }
            } else {
                throw e; // Let retry handle other errors
            }
        }
    }

    private CicResponse fallbackCicCheck(CICRequest cicRequest, Exception e) {
        log.error("Fallback: CIC check failed for ID: {}. Error: {}", 
                cicRequest.getIdNumber(), e.getMessage());
        
        return CicResponse.builder()
                .status("error")
                .creditScore(0)
                .overdue(true)
                .debtGroup(5)
                .errorCode("CIC_SERVICE_ERROR")
                .message("CIC service is currently unavailable. Please try again later.")
                .build();
    }

    private boolean isBusinessLogicFailure(CicResponse response) {
        if (response.getErrorCode() == null) {
            return false;
        }

        String errorCode = response.getErrorCode();
        return errorCode.equals("INVALID_ID_NUMBER") ||
                errorCode.equals("ID_NOT_FOUND") ||
                errorCode.equals("BLACKLISTED_ID") ||
                errorCode.equals("DECEASED_PERSON") ||
                errorCode.equals("INVALID_PERSONAL_INFO") ||
                errorCode.equals("PRIVACY_RESTRICTION") ||
                errorCode.equals("LEGAL_RESTRICTION") ||
                errorCode.equals("COMPLIANCE_VIOLATION");
    }
}
