package com.example.account_service.service;

import com.example.account_service.dto.kafkaMessage.CardRegistrationMessage;
import com.example.account_service.dto.response.MasterCardResponse;
import com.example.account_service.dto.response.VisaCardResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
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
@Slf4j
public class CardRegistrationService {
    @Autowired
    @Qualifier("MockServerRestTemplate")
    private  RestTemplate mockServerTemplate;

    @Retry(name = "visa-registration", fallbackMethod = "fallbackVisaRegistration")
    @CircuitBreaker(name = "visa-registration", fallbackMethod = "fallbackVisaRegistration")
    public VisaCardResponse registerVisaCard(CardRegistrationMessage message) {
        log.info("Attempting VISA card registration for account: {}", message.getAccountNumber());
        
        String url = "http://localhost:8089/api/visa/registration";
        
        Map<String, String> request = new HashMap<>();
        request.put("cardType", message.getCardType());
        request.put("accountNumber", message.getAccountNumber());
        request.put("cifCode", message.getCifCode());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(request, headers);

        try {
            ResponseEntity<VisaCardResponse> response = mockServerTemplate.postForEntity(url, entity, VisaCardResponse.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                VisaCardResponse responseBody = response.getBody();
                if (responseBody != null) {
                    log.info("VISA registration response for account {}: status={}, message={}", 
                            message.getAccountNumber(), responseBody.getStatus(), responseBody.getMessage());
                    
                    // Check if it's a business logic failure that shouldn't be retried
                    if ("FAILED".equals(responseBody.getStatus()) && isBusinessLogicFailure(responseBody)) {
                        log.warn("Business logic failure for account {}, will not retry: {}", 
                                message.getAccountNumber(), responseBody.getMessage());
                        return responseBody; // Return as-is, don't throw exception to avoid retry
                    }
                    
                    return responseBody;
                } else {
                    log.error("VISA API returned null body for account: {}", message.getAccountNumber());
                    throw new RuntimeException("VISA API returned null response body");
                }
            } else {
                log.error("VISA API returned non-success status: {} for account: {}", 
                         response.getStatusCode(), message.getAccountNumber());
                throw new RuntimeException("VISA API returned status: " + response.getStatusCode());
            }
        } catch (HttpClientErrorException e) {
            // Handle 4xx errors differently
            if (e.getStatusCode().is4xxClientError()) {
                log.error("Client error calling VISA API for account: {}. Status: {}, Error: {}", 
                         message.getAccountNumber(), e.getStatusCode(), e.getMessage());
                
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

    @Retry(name = "mastercard-registration", fallbackMethod = "fallbackMasterCardRegistration")
    @CircuitBreaker(name = "mastercard-registration", fallbackMethod = "fallbackMasterCardRegistration")
    public MasterCardResponse registerMasterCard(CardRegistrationMessage message) {
        log.info("Attempting MasterCard registration for account: {}", message.getAccountNumber());
        
        String url = "http://localhost:8089/api/master/registration";
        
        Map<String, String> request = new HashMap<>();
        request.put("cardType", message.getCardType());
        request.put("accountNumber", message.getAccountNumber());
        request.put("cifCode", message.getCifCode());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(request, headers);

        ResponseEntity<MasterCardResponse> response = mockServerTemplate.postForEntity(url, entity, MasterCardResponse.class);
        
        if (response.getStatusCode().is2xxSuccessful()) {
            MasterCardResponse responseBody = response.getBody();
            if (responseBody != null) {
                log.info("MasterCard registration response for account {}: status={}, message={}", 
                        message.getAccountNumber(), responseBody.getStatus(), responseBody.getMessage());
                
                if ("FAILED".equals(responseBody.getStatus()) && isBusinessLogicFailure(responseBody)) {
                    log.warn("Business logic failure for account {}, will not retry: {}", 
                            message.getAccountNumber(), responseBody.getMessage());
                    return responseBody;
                }
                
                return responseBody;
            } else {
                throw new RuntimeException("MasterCard API returned null response body");
            }
        } else {
            throw new RuntimeException("MasterCard API returned status: " + response.getStatusCode());
        }
    }

    // Fallback method for VISA registration
    public VisaCardResponse fallbackVisaRegistration(CardRegistrationMessage message, Exception ex) {
        log.error("VISA registration fallback for account: {}. Error: {}", 
                 message.getAccountNumber(), ex.getMessage());
        
        return VisaCardResponse.builder()
                .status("ERROR")
                .errorCode("SERVICE_UNAVAILABLE")
                .message("VISA registration service is currently unavailable.")
                .build();
    }

    // Fallback method for MasterCard registration
    public MasterCardResponse fallbackMasterCardRegistration(CardRegistrationMessage message, Exception ex) {
        log.error("MasterCard registration fallback triggered for account: {}. Error: {}", 
                 message.getAccountNumber(), ex.getMessage(), ex);
        
        return MasterCardResponse.builder()
                .status("ERROR")
                .errorCode("SERVICE_UNAVAILABLE")
                .message("MasterCard registration service is currently unavailable. Please try again later.")
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