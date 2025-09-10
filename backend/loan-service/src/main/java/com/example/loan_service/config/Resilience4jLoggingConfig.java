package com.example.loan_service.config;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class Resilience4jLoggingConfig {

    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final RetryRegistry retryRegistry;
    private final RateLimiterRegistry rateLimiterRegistry;
    private final BulkheadRegistry bulkheadRegistry;
    private final TimeLimiterRegistry timeLimiterRegistry;

    @PostConstruct
    public void registerEventConsumers() {
        // Register for already existing instances
        circuitBreakerRegistry.getAllCircuitBreakers().forEach(this::attachCircuitBreakerLogger);
        retryRegistry.getAllRetries().forEach(this::attachRetryLogger);
        rateLimiterRegistry.getAllRateLimiters().forEach(this::attachRateLimiterLogger);
        bulkheadRegistry.getAllBulkheads().forEach(this::attachBulkheadLogger);
        timeLimiterRegistry.getAllTimeLimiters().forEach(this::attachTimeLimiterLogger);

        // Also register for future instances
        circuitBreakerRegistry.getEventPublisher()
                .onEntryAdded(event -> attachCircuitBreakerLogger(event.getAddedEntry()))
                .onEntryReplaced(event -> attachCircuitBreakerLogger(event.getNewEntry()));

        retryRegistry.getEventPublisher()
                .onEntryAdded(event -> attachRetryLogger(event.getAddedEntry()))
                .onEntryReplaced(event -> attachRetryLogger(event.getNewEntry()));

        rateLimiterRegistry.getEventPublisher()
                .onEntryAdded(event -> attachRateLimiterLogger(event.getAddedEntry()))
                .onEntryReplaced(event -> attachRateLimiterLogger(event.getNewEntry()));

        bulkheadRegistry.getEventPublisher()
                .onEntryAdded(event -> attachBulkheadLogger(event.getAddedEntry()))
                .onEntryReplaced(event -> attachBulkheadLogger(event.getNewEntry()));

        timeLimiterRegistry.getEventPublisher()
                .onEntryAdded(event -> attachTimeLimiterLogger(event.getAddedEntry()))
                .onEntryReplaced(event -> attachTimeLimiterLogger(event.getNewEntry()));
    }

    private void attachCircuitBreakerLogger(CircuitBreaker cb) {
        String name = cb.getName();
        cb.getEventPublisher()
                .onStateTransition(e -> log.warn("CB_STATE_TRANSITION - name: {}, from: {}, to: {}", name, e.getStateTransition().getFromState(), e.getStateTransition().getToState()))
                .onError(e -> log.error("CB_ERROR - name: {}, durationMs: {}, throwable: {}", name, e.getElapsedDuration().toMillis(), safeMessage(e.getThrowable())))
                .onCallNotPermitted(e -> log.warn("CB_CALL_NOT_PERMITTED - name: {}", name))
                .onFailureRateExceeded(e -> log.warn("CB_FAILURE_RATE_EXCEEDED - name: {}, failureRate: {}", name, e.getFailureRate()))
                .onSlowCallRateExceeded(e -> log.warn("CB_SLOW_CALL_RATE_EXCEEDED - name: {}, slowCallRate: {}", name, e.getSlowCallRate()))
                .onIgnoredError(e -> log.info("CB_IGNORED_ERROR - name: {}, throwable: {}", name, safeMessage(e.getThrowable())))
                .onSuccess(e -> log.debug("CB_SUCCESS - name: {}, durationMs: {}", name, e.getElapsedDuration().toMillis()));
    }

    private void attachRetryLogger(Retry retry) {
        String name = retry.getName();
        retry.getEventPublisher()
                .onRetry(e -> log.warn("RETRY_ATTEMPT - name: {}, attempt: {}, lastException: {}", name, e.getNumberOfRetryAttempts(), safeMessage(e.getLastThrowable())))
                .onError(e -> log.error("RETRY_EXHAUSTED - name: {}, attempts: {}, lastException: {}", name, e.getNumberOfRetryAttempts(), safeMessage(e.getLastThrowable())))
                .onSuccess(e -> log.info("RETRY_SUCCESS - name: {}, afterAttempts: {}", name, e.getNumberOfRetryAttempts()));
    }

    private void attachRateLimiterLogger(RateLimiter rl) {
        String name = rl.getName();
        rl.getEventPublisher()
                .onSuccess(e -> log.debug("RL_SUCCESSFUL_ACQUIRE - name: {}", name))
                .onFailure(e -> log.warn("RL_FAILED_ACQUIRE - name: {}", name));
    }

    private void attachBulkheadLogger(Bulkhead bh) {
        String name = bh.getName();
        bh.getEventPublisher()
                .onCallPermitted(e -> log.debug("BH_CALL_PERMITTED - name: {}", name))
                .onCallRejected(e -> log.warn("BH_CALL_REJECTED - name: {}", name))
                .onCallFinished(e -> log.debug("BH_CALL_FINISHED - name: {}", name));
    }

    private void attachTimeLimiterLogger(TimeLimiter tl) {
        String name = tl.getName();
        tl.getEventPublisher()
                .onTimeout(e -> log.warn("TL_TIMEOUT - name: {}", name))
                .onError(e -> log.error("TL_ERROR - name: {}, throwable: {}", name, safeMessage(e.getThrowable())))
                .onSuccess(e -> log.debug("TL_SUCCESS - name: {}", name));
    }

    private String safeMessage(Throwable t) {
        return t == null ? "null" : (t.getClass().getSimpleName() + ": " + t.getMessage());
    }
}


