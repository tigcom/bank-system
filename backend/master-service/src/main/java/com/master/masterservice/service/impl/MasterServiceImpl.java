package com.master.masterservice.service.impl;

import com.master.masterservice.dto.request.CardRegistrationRequest;
import com.master.masterservice.dto.response.MasterCardResponse;
import com.master.masterservice.service.MasterService;
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
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
@Slf4j
public class MasterServiceImpl implements MasterService {
    private final RestTemplate restTemplate;

    @Value("${master.api.url}")
    private String masterApiUrl;

    @Value("${master.api.key}")
    private String masterApiKey;
    @Value("${master.api.key-value}")
    private String masterApiKeyValue;

    @Override
    @Retry(name = "mastercard-registration", fallbackMethod = "fallbackMasterRegistration")
    public MasterCardResponse registerCard(CardRegistrationRequest request) {
        log.info("=== RETRY ATTEMPT - Calling Master API for card registration: {} ===", request);
        log.debug("Method called with @Retry annotation - attempt will be tracked by Resilience4j");
        
        // Log current thread info to verify proxy is working
        log.debug("Current thread: {}, Class: {}", Thread.currentThread().getName(), this.getClass().getName());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(masterApiKey, "ApiKey " + masterApiKeyValue);

        HttpEntity<CardRegistrationRequest> entity = new HttpEntity<>(request, headers);

        try {
            ResponseEntity<MasterCardResponse> response = restTemplate.postForEntity(
                    masterApiUrl + "/registration", entity, MasterCardResponse.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                MasterCardResponse responseBody = response.getBody();
                if (responseBody != null) {
                    log.info("Master registration response for account {}: status={}, message={}",
                            request.getAccountNumber(), responseBody.getStatus(), responseBody.getMessage());

                    // Check if it's a business logic failure that shouldn't be retried
                    if ("FAILED".equals(responseBody.getStatus()) && isBusinessLogicFailure(responseBody)) {
                        log.warn("Business logic failure for account {}, will not retry: {}",
                                request.getAccountNumber(), responseBody.getMessage());
                        return responseBody; // Return as-is, don't throw exception to avoid retry
                    }

                    return responseBody;
                } else {
                    log.error("Master API returned null body for account: {}", request.getAccountNumber());
                    throw new RuntimeException("Master API returned null response body");
                }
            } else {
                log.error("Master API returned non-success status: {} for account: {}",
                        response.getStatusCode(), request.getAccountNumber());
                throw new RuntimeException("Master API returned status: " + response.getStatusCode());
            }
        } catch (HttpClientErrorException e) {
            // Handle 4xx errors differently
            if (e.getStatusCode().is4xxClientError()) {
                log.error("Client error calling Master API for account: {}. Status: {}, Error: {}",
                        request.getAccountNumber(), e.getStatusCode(), e.getMessage());

                // 4xx errors usually shouldn't be retried (except 429)
                if (e.getStatusCode().value() == 429) {
                    log.warn("Rate limit exceeded (429), will retry for account: {}", request.getAccountNumber());
                    throw e; // Let retry handle rate limiting
                } else {
                    log.warn("Client error (4xx), will NOT retry for account: {}", request.getAccountNumber());
                    return MasterCardResponse.builder()
                            .status("FAILED")
                            .errorCode("CLIENT_ERROR")
                            .message("Client error: " + e.getMessage())
                            .build();
                }
            } else {
                throw e; // Let retry handle other errors
            }
        } catch (HttpServerErrorException e) {
            // Handle 5xx errors - these should be retried
            log.error("Server error calling Master API for account: {}. Status: {}, Error: {} - WILL RETRY",
                    request.getAccountNumber(), e.getStatusCode(), e.getMessage());
            throw e; // Let retry handle server errors
        } catch (RestClientException e) {
            // Handle connection errors - these should be retried
            log.error("Connection error calling Master API for account: {}. Error: {} - WILL RETRY",
                    request.getAccountNumber(), e.getMessage());
            throw e; // Let retry handle connection errors
        }
    }

    public MasterCardResponse fallbackMasterRegistration(CardRegistrationRequest request, Exception e) {
        log.error("=== FALLBACK TRIGGERED === Master registration failed for account: {} after all retry attempts. Error type: {}, Message: {}", 
                request.getAccountNumber(), e.getClass().getSimpleName(), e.getMessage());
        
        // Log the full stack trace for debugging
        log.error("Full exception trace:", e);
        
        return MasterCardResponse.builder()
                .status("ERROR")
                .errorCode("MASTER_SERVICE_ERROR")
                .message("Master service is currently unavailable after " + 
                        " retry attempts. Error: " + e.getMessage())
                .build();
    }

    private boolean isBusinessLogicFailure(MasterCardResponse response) {
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