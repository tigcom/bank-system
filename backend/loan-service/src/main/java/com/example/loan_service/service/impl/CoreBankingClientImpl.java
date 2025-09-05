package com.example.loan_service.service.impl;

import com.example.common_service.dto.CoreAccountRequest;
import com.example.common_service.dto.customer.CoreCustomerDTO;
import com.example.common_service.dto.customer.CoreResponse;
import com.example.common_service.dto.response.ApiResponse;
import com.example.loan_service.dto.request.LoanRequestDTO;
import com.example.loan_service.dto.response.LoanResponseDTO;
import com.example.loan_service.service.CoreBankingClient;
import com.example.loan_service.service.LoanMetricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import io.micrometer.core.instrument.Timer;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import org.springframework.beans.factory.annotation.Value;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class CoreBankingClientImpl implements CoreBankingClient {
    @Qualifier("restTemplate")
    private final RestTemplate restTemplate;
    private final LoanMetricsService metricsService;

    @Value("${app.simulate.coreBanking.fail:false}")
    private boolean simulateFail;
    @Value("${app.simulate.coreBanking.delayMs:0}")
    private long simulateDelayMs;
    @Override
    @CircuitBreaker(name = "coreBanking", fallbackMethod = "updateAccountFallback")
    @Retry(name = "coreBanking", fallbackMethod = "updateAccountFallback")
    @Bulkhead(name = "externalApiCalls", fallbackMethod = "updateAccountFallback")
    public CoreResponse updateAccount(CoreAccountRequest request) {
        log.info("UPDATE_ACCOUNT_START - request: {}", request);
        Timer.Sample timer = metricsService.startCoreBankingCall();
        
        try {
            // Simulation toggles for testing Resilience4j
            if (simulateDelayMs > 0) {
                Thread.sleep(simulateDelayMs);
            }
            if (simulateFail) {
                throw new RuntimeException("Simulated coreBanking updateAccount failure");
            }
            // Increment core banking calls counter
            metricsService.incrementCoreBankingCalls();
            
            ResponseEntity<CoreResponse> response = restTemplate.postForEntity(
                    "http://localhost:8083/corebanking/update-account",
                    request,
                    CoreResponse.class
            );
            log.info("UPDATE_ACCOUNT_SUCCESS - request: {}", request);
            return response.getBody();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted", e);
        } catch (Exception e) {
            log.error("UPDATE_ACCOUNT_ERROR - request: {}, error: {}", request, e.getMessage(), e);
            throw new RuntimeException("Failed to update account in core banking: " + e.getMessage(), e);
        } finally {
            metricsService.stopCoreBankingCall(timer);
        }
    }

    public CoreResponse updateAccountFallback(com.example.common_service.dto.CoreAccountRequest request, Throwable t) {
        log.warn("UPDATE_ACCOUNT_FALLBACK - request: {}, error: {}", request, t.getMessage());
        // Return a default response or throw a specific exception
        throw new RuntimeException("Core banking service is temporarily unavailable: " + t.getMessage(), t);
    }

    @Override
    @CircuitBreaker(name = "coreBanking", fallbackMethod = "deleteLoanFallback")
    @Retry(name = "coreBanking", fallbackMethod = "deleteLoanFallback")
    @Bulkhead(name = "externalApiCalls", fallbackMethod = "deleteLoanFallback")
    public void deleteLoan(long id) {
        log.info("DELETE_LOAN_START - id: {}", id);
        try {
            if (simulateDelayMs > 0) {
                Thread.sleep(simulateDelayMs);
            }
            if (simulateFail) {
                throw new RuntimeException("Simulated coreBanking deleteLoan failure");
            }
            restTemplate.delete(
                    "http://localhost:8083/corebanking/api/core/loans/sync",
                    id
            );
            log.info("DELETE_LOAN_SUCCESS - id: {}", id);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted", e);
        } catch (Exception e) {
            log.error("DELETE_LOAN_ERROR - id: {}, error: {}", id, e.getMessage(), e);
            throw new RuntimeException("Failed to delete loan in core banking: " + e.getMessage(), e);
        }
    }

    public void deleteLoanFallback(long id, Throwable t) {
        log.warn("DELETE_LOAN_FALLBACK - id: {}, error: {}", id, t.getMessage());
        throw new RuntimeException("Core banking service is temporarily unavailable: " + t.getMessage(), t);
    }

    @Override
    @CircuitBreaker(name = "coreBanking", fallbackMethod = "getBalanceFallback")
    @Retry(name = "coreBanking", fallbackMethod = "getBalanceFallback")
    @Bulkhead(name = "externalApiCalls", fallbackMethod = "getBalanceFallback")
    public BigDecimal getBalance(String accountNumber) {
        log.info("GET_BALANCE_START - accountNumber: {}", accountNumber);
        Timer.Sample timer = metricsService.startCoreBankingCall();
        
        try {
            if (simulateDelayMs > 0) {
                Thread.sleep(simulateDelayMs);
            }
            if (simulateFail) {
                throw new RuntimeException("Simulated coreBanking getBalance failure");
            }
            // Increment core banking calls counter
            metricsService.incrementCoreBankingCalls();
            
            ResponseEntity<ApiResponse<BigDecimal>> response = restTemplate.exchange(
                    "http://localhost:8083/corebanking/api/core-bank/get-balance/{accountNumber}",
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<ApiResponse<BigDecimal>>() {},
                    accountNumber
            );
            
            if (response.getBody() != null && response.getBody().getResult() != null) {
                BigDecimal balance = response.getBody().getResult();
                log.info("GET_BALANCE_SUCCESS - accountNumber: {}, balance: {}", accountNumber, balance);
                return balance;
            }
            
            log.warn("GET_BALANCE_NULL_RESPONSE - accountNumber: {}, using zero balance", accountNumber);
            return BigDecimal.ZERO;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted", e);
        } catch (Exception e) {
            log.error("GET_BALANCE_ERROR - accountNumber: {}, error: {}", accountNumber, e.getMessage(), e);
            throw new RuntimeException("Failed to get balance from core banking: " + e.getMessage(), e);
        } finally {
            metricsService.stopCoreBankingCall(timer);
        }
    }

    public BigDecimal getBalanceFallback(String accountNumber, Throwable t) {
        log.warn("GET_BALANCE_FALLBACK - accountNumber: {}, error: {}", accountNumber, t.getMessage());
        // Return zero balance as fallback
        return BigDecimal.ZERO;
    }
}
