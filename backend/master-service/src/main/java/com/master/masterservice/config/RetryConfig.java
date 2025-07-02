package com.master.masterservice.config;

import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class RetryConfig {

    @Bean
    public RetryRegistry retryRegistry() {
        RetryRegistry retryRegistry = RetryRegistry.ofDefaults();
        
        // Get the retry instance and add event listeners
        Retry retry = retryRegistry.retry("mastercard-registration");
        
        retry.getEventPublisher()
                .onRetry(event -> log.info("=== RETRY EVENT - Attempt #{} for: {} ===", 
                        event.getNumberOfRetryAttempts(), event.getName()))
                .onSuccess(event -> log.info("=== RETRY SUCCESS - After {} attempts for: {} ===", 
                        event.getNumberOfRetryAttempts(), event.getName()))
                .onError(event -> log.error("=== RETRY FAILED - After {} attempts for: {}, Last exception: {} ===", 
                        event.getNumberOfRetryAttempts(), event.getName(), event.getLastThrowable().getMessage()));
        
        return retryRegistry;
    }
} 