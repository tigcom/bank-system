package com.visa.visaservice.service.impl;

import com.visa.visaservice.dto.request.CardRegistrationRequest;
import com.visa.visaservice.dto.response.VisaCardResponse;
import com.visa.visaservice.service.VisaService;
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

@Service
@RequiredArgsConstructor
@Slf4j
public class VisaServiceImpl implements VisaService {
    private final RestTemplate restTemplate;

    @Value("${visa.api.url}")
    private String visaApiUrl;
    @Value("${visa.api.key.value}")
    private String visaApiKeyValue;

    @Value("${visa.api.key}")
    private String visaApiKey;

    @Override
    @CircuitBreaker(name = "visaRegistration", fallbackMethod = "fallbackVisaRegistration")
    @Retry(name = "visaRegistration")
    public VisaCardResponse registerCard(CardRegistrationRequest request) {
        log.info("Calling VISA API for card registration: {}", request);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(visaApiKey, "ApiKey "+visaApiKeyValue);

        HttpEntity<CardRegistrationRequest> entity = new HttpEntity<>(request, headers);

        try {

            ResponseEntity<VisaCardResponse> response = restTemplate.postForEntity(
                    visaApiUrl + "/registration", entity, VisaCardResponse.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                VisaCardResponse responseBody = response.getBody();
                if (responseBody != null) {
                    log.info("VISA registration response for account {}: status={}, message={}",
                            request.getAccountNumber(), responseBody.getStatus(), responseBody.getMessage());

                    // Check if it's a business logic failure that shouldn't be retried
                    if ("FAILED".equals(responseBody.getStatus()) && isBusinessLogicFailure(responseBody)) {
                        log.warn("Business logic failure for account {}, will not retry: {}",
                                request.getAccountNumber(), responseBody.getMessage());
                        return responseBody; // Return as-is, don't throw exception to avoid retry
                    }

                    return responseBody;
                } else {
                    log.error("VISA API returned null body for account: {}", request.getAccountNumber());
                    throw new RuntimeException("VISA API returned null response body");
                }
            } else {
                log.error("VISA API returned non-success status: {} for account: {}",
                        response.getStatusCode(), request.getAccountNumber());
                throw new RuntimeException("VISA API returned status: " + response.getStatusCode());
            }
        } catch (HttpClientErrorException e) {
            // Handle 4xx errors differently
            if (e.getStatusCode().is4xxClientError()) {
                log.error("Client error calling VISA API for account: {}. Status: {}, Error: {}",
                        request.getAccountNumber(), e.getStatusCode(), e.getMessage());

                // 4xx errors usually shouldn't be retried (except 429)
                if (e.getStatusCode().value() == 429) {
                    throw e; // Let retry handle rate limiting
                } else {
                    return VisaCardResponse.builder()
                            .status("FAILED")
                            .errorCode("CLIENT_ERROR")
                            .message("Client error: " + e.getMessage())
                            .build();
                }
            } else {
                throw e; // Let retry handle other errors
            }
        }


    }

    private VisaCardResponse fallbackVisaRegistration(CardRegistrationRequest request, Exception e) {
        log.error("Fallback: VISA registration failed for account: {}. Error: {}",
                request.getAccountNumber(), e.getMessage());

        return VisaCardResponse.builder()
                .status("ERROR")
                .errorCode("VISA_SERVICE_ERROR")
                .message("VISA service is currently unavailable. Please try again later.")
                .build();
    }
    private boolean isBusinessLogicFailure(VisaCardResponse response) {
        if (response.getErrorCode() == null) {
            return isBusinessLogicFailure(response.getMessage());
        }

        String errorCode = response.getErrorCode();
        return errorCode.equals("INVALID_CUSTOMER") ||
                errorCode.equals("DUPLICATE_REGISTRATION") ||
                errorCode.equals("BLACKLISTED_CUSTOMER") ||
                errorCode.equals("KYC_FAILED") ||
                errorCode.equals("INSUFFICIENT_CREDIT_SCORE") ||
                errorCode.equals("AGE_RESTRICTION") ||
                errorCode.equals("CITIZENSHIP_RESTRICTION") ||
                errorCode.equals("SANCTIONS_LIST_MATCH") ||
                errorCode.equals("COMPLIANCE_VIOLATION") ||
                errorCode.equals("INCOME_VERIFICATION_FAILED");
    }
    private boolean isBusinessLogicFailure(String errorMessage) {
        if (errorMessage == null) return false;

        String message = errorMessage.toLowerCase();
        return message.contains("invalid customer") ||
                message.contains("duplicate registration") ||
                message.contains("blacklisted") ||
                message.contains("kyc failed") ||
                message.contains("insufficient credit score") ||
                message.contains("age restriction") ||
                message.contains("citizenship") ||
                message.contains("sanctions") ||
                message.contains("compliance violation") ||
                message.contains("regulatory");
    }
}