package com.example.loan_service.service;

import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
public class TestResilienceService {
	@CircuitBreaker(name = "coreBanking", fallbackMethod = "unstableCallFallback")
	@Retry(name = "coreBanking", fallbackMethod = "unstableCallFallback")
	@Bulkhead(name = "externalApiCalls", fallbackMethod = "unstableCallFallback")
	public String unstableCall(boolean fail, long delayMs) {
		log.info("TEST_UNSTABLE_CALL - fail={}, delayMs={}", fail, delayMs);
		try {
			if (delayMs > 0) {
				Thread.sleep(delayMs);
			}
			if (fail) {
				throw new RuntimeException("Forced failure for testing");
			}
			return "OK";
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException("Interrupted", e);
		}
	}

	public String unstableCallFallback(boolean fail, long delayMs, Throwable t) {
		log.warn("TEST_UNSTABLE_CALL_FALLBACK - fail={}, delayMs={}, error={}", fail, delayMs, t.getMessage());
		return "FALLBACK: " + t.getMessage();
	}

	@Retry(name = "dubboServices", fallbackMethod = "rateLimitedFallback")
	@io.github.resilience4j.ratelimiter.annotation.RateLimiter(name = "loanCreation", fallbackMethod = "rateLimitedFallback")
	public String rateLimitedCall(String note) {
		log.info("TEST_RATELIMITER - note={}", note);
		return "RATE_OK: " + note;
	}

	public String rateLimitedFallback(String note, Throwable t) {
		log.warn("TEST_RATELIMITER_FALLBACK - note={}, error={}", note, t.getMessage());
		return "RATE_FALLBACK: " + t.getMessage();
	}

	@Bulkhead(name = "loanProcessing", fallbackMethod = "bulkheadFallback")
	public String slowCall(long delayMs) {
		log.info("TEST_BULKHEAD_SLOW_CALL - delayMs={}", delayMs);
		try {
			Thread.sleep(delayMs);
			return "SLOW_DONE:" + delayMs;
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException("Interrupted", e);
		}
	}

	public String bulkheadFallback(long delayMs, Throwable t) {
		log.warn("TEST_BULKHEAD_FALLBACK - delayMs={}, error={}", delayMs, t.getMessage());
		return "BULKHEAD_FALLBACK: " + t.getMessage();
	}

	@TimeLimiter(name = "loanProcessing", fallbackMethod = "timeLimiterFallback")
	public CompletableFuture<String> timeLimitedCall(long delayMs) {
		log.info("TEST_TIMELIMITER_CALL - delayMs={}", delayMs);
		return CompletableFuture.supplyAsync(() -> {
			try {
				Thread.sleep(delayMs);
				return "TIME_OK:" + delayMs;
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new RuntimeException("Interrupted", e);
			}
		});
	}

	public CompletableFuture<String> timeLimiterFallback(long delayMs, Throwable t) {
		log.warn("TEST_TIMELIMITER_FALLBACK - delayMs={}, error={}", delayMs, t.getMessage());
		return CompletableFuture.completedFuture("TIME_FALLBACK: " + t.getMessage());
	}
} 