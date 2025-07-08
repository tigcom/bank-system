package com.example.account_service.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class Resilience4jConfig {

    @Bean
    public RetryRegistry retryRegistry() {
        RetryRegistry retryRegistry = RetryRegistry.ofDefaults();
        
        // Register event listeners for VISA retry
        Retry visaRetry = retryRegistry.retry("visa-registration");
        visaRetry.getEventPublisher()
                .onRetry(event -> log.warn("VISA registration retry attempt #{} for account. Reason: {}", 
                        event.getNumberOfRetryAttempts(), event.getLastThrowable().getMessage()))
                .onSuccess(event -> log.info("VISA registration succeeded after {} attempts", 
                        event.getNumberOfRetryAttempts()))
                .onError(event -> log.error("VISA registration failed after {} attempts. Final error: {}", 
                        event.getNumberOfRetryAttempts(), event.getLastThrowable().getMessage()));
        
        // Register event listeners for MasterCard retry
        Retry mastercardRetry = retryRegistry.retry("mastercard-registration");
        mastercardRetry.getEventPublisher()
                .onRetry(event -> log.warn("MasterCard registration retry attempt #{} for account. Reason: {}", 
                        event.getNumberOfRetryAttempts(), event.getLastThrowable().getMessage()))
                .onSuccess(event -> log.info("MasterCard registration succeeded after {} attempts", 
                        event.getNumberOfRetryAttempts()))
                .onError(event -> log.error("MasterCard registration failed after {} attempts. Final error: {}", 
                        event.getNumberOfRetryAttempts(), event.getLastThrowable().getMessage()));
        
        return retryRegistry;
    }

    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();
        
        // Register event listeners for VISA circuit breaker
        CircuitBreaker visaCircuitBreaker = circuitBreakerRegistry.circuitBreaker("visa-registration");
        visaCircuitBreaker.getEventPublisher()
                .onStateTransition(event -> log.warn("VISA registration circuit breaker state transition: {} -> {}", 
                        event.getStateTransition().getFromState(), event.getStateTransition().getToState()))
                .onSlowCallRateExceeded(event -> log.warn("VISA registration slow call rate exceeded: {}%", 
                        event.getSlowCallRate()))
                .onFailureRateExceeded(event -> log.error("VISA registration failure rate exceeded: {}%", 
                        event.getFailureRate()));
        
        // Register event listeners for MasterCard circuit breaker
        CircuitBreaker mastercardCircuitBreaker = circuitBreakerRegistry.circuitBreaker("mastercard-registration");
        mastercardCircuitBreaker.getEventPublisher()
                .onStateTransition(event -> log.warn("MasterCard registration circuit breaker state transition: {} -> {}", 
                        event.getStateTransition().getFromState(), event.getStateTransition().getToState()))
                .onSlowCallRateExceeded(event -> log.warn("MasterCard registration slow call rate exceeded: {}%", 
                        event.getSlowCallRate()))
                .onFailureRateExceeded(event -> log.error("MasterCard registration failure rate exceeded: {}%", 
                        event.getFailureRate()));
        
        return circuitBreakerRegistry;
    }
} 