1. CircuitBreaker (Mạch ngắt)

application.properties
# CircuitBreaker - backendA
resilience4j.circuitbreaker.instances.backendA.registerHealthIndicator=true
resilience4j.circuitbreaker.instances.backendA.slidingWindowSize=100
resilience4j.circuitbreaker.instances.backendA.failureRateThreshold=50
# Giá trị tính bằng milliseconds (ví dụ 10000 = 10 giây)
resilience4j.circuitbreaker.instances.backendA.waitDurationInOpenState=10000
resilience4j.circuitbreaker.instances.backendA.permittedNumberOfCallsInHalfOpenState=10


Ví dụ programmatic :
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.time.Duration;
import java.util.function.Supplier;
import io.vavr.control.Try; // nếu dùng vavr Try

CircuitBreakerConfig config = CircuitBreakerConfig.custom()
    .failureRateThreshold(50)
    .waitDurationInOpenState(Duration.ofMillis(10000))
    .build();

CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(config);
CircuitBreaker circuitBreaker = registry.circuitBreaker("backendA");

Supplier<String> supplier = CircuitBreaker.decorateSupplier(circuitBreaker, this::someMethod);
Try<String> result = Try.ofSupplier(supplier);
Ví dụ annotation trong Spring:
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.stereotype.Service;

@Service
public class BackendService {

    @CircuitBreaker(name = "backendA", fallbackMethod = "fallbackMethod")
    public String someMethod() {
        // Logic xử lý ở đây
        return "Success";
    }

    public String fallbackMethod(Throwable t) {
        return "Fallback response due to: " + t.getMessage();
    }
}
2. Retry (Thử lại)

application.properties
# Retry - backendA
resilience4j.retry.instances.backendA.maxAttempts=3
# waitDuration tính bằng milliseconds (5000 = 5 giây)
resilience4j.retry.instances.backendA.waitDuration=5000
Ví dụ programmatic:
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import java.time.Duration;
import java.util.function.Supplier;
import io.vavr.control.Try;

RetryConfig config = RetryConfig.custom()
    .maxAttempts(3)
    .waitDuration(Duration.ofMillis(5000))
    .build();

RetryRegistry registry = RetryRegistry.of(config);
Retry retry = registry.retry("backendA");

Supplier<String> supplier = Retry.decorateSupplier(retry, this::someMethod);
Try<String> result = Try.ofSupplier(supplier);
Ví dụ annotation:
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.stereotype.Service;

@Service
public class BackendService {

    @Retry(name = "backendA", fallbackMethod = "fallbackMethod")
    public String someMethod() {
        // Logic xử lý ở đây
        return "Success";
    }

    public String fallbackMethod(Throwable t) {
        return "Fallback response due to: " + t.getMessage();
    }
}
3. RateLimiter (Giới hạn tốc độ)

application.properties
# RateLimiter - backendA
resilience4j.ratelimiter.instances.backendA.limitForPeriod=10
# limitRefreshPeriod tính bằng milliseconds (5000 = 5 giây)
resilience4j.ratelimiter.instances.backendA.limitRefreshPeriod=5000
Ví dụ programmatic:
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import java.time.Duration;
import java.util.function.Supplier;
import io.vavr.control.Try;

RateLimiterConfig config = RateLimiterConfig.custom()
    .limitForPeriod(10)
    .limitRefreshPeriod(Duration.ofMillis(5000))
    .build();

RateLimiterRegistry registry = RateLimiterRegistry.of(config);
RateLimiter rateLimiter = registry.rateLimiter("backendA");

Supplier<String> supplier = RateLimiter.decorateSupplier(rateLimiter, this::someMethod);
Try<String> result = Try.ofSupplier(supplier);
Ví dụ annotation:
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import org.springframework.stereotype.Service;

@Service
public class BackendService {

    @RateLimiter(name = "backendA", fallbackMethod = "fallbackMethod")
    public String someMethod() {
        // Logic xử lý ở đây
        return "Success";
    }

    public String fallbackMethod(Throwable t) {
        return "Fallback response due to: " + t.getMessage();
    }
}

4. Bulkhead (Chia sẻ tài nguyên)

application.properties
# Bulkhead - backendA
resilience4j.bulkhead.instances.backendA.maxConcurrentCalls=5
# maxWaitDuration tính bằng milliseconds (5000 = 5 giây)
resilience4j.bulkhead.instances.backendA.maxWaitDuration=5000
Ví dụ programmatic:
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import java.time.Duration;
import java.util.function.Supplier;
import io.vavr.control.Try;

BulkheadConfig config = BulkheadConfig.custom()
    .maxConcurrentCalls(5)
    .maxWaitDuration(Duration.ofMillis(5000))
    .build();

BulkheadRegistry registry = BulkheadRegistry.of(config);
Bulkhead bulkhead = registry.bulkhead("backendA");

Supplier<String> supplier = Bulkhead.decorateSupplier(bulkhead, this::someMethod);
Try<String> result = Try.ofSupplier(supplier);


Ví dụ annotation:
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import org.springframework.stereotype.Service;

@Service
public class BackendService {

    @Bulkhead(name = "backendA", fallbackMethod = "fallbackMethod")
    public String someMethod() {
        // Logic xử lý ở đây
        return "Success";
    }

    public String fallbackMethod(Throwable t) {
        return "Fallback response due to: " + t.getMessage();
    }
}
5. TimeLimiter (Giới hạn thời gian)

application.properties
# TimeLimiter - backendA
# timeoutDuration tính bằng milliseconds (2000 = 2 giây)
resilience4j.timelimiter.instances.backendA.timeoutDuration=2000


Ví dụ programmatic:
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;
import io.vavr.control.Try;

TimeLimiterConfig config = TimeLimiterConfig.custom()
    .timeoutDuration(Duration.ofMillis(2000))
    .build();

TimeLimiterRegistry registry = TimeLimiterRegistry.of(config);
TimeLimiter timeLimiter = registry.timeLimiter("backendA");

Supplier<CompletionStage<String>> supplier = () -> CompletableFuture.supplyAsync(this::someMethod);
Supplier<CompletionStage<String>> decoratedSupplier = TimeLimiter.decorateCompletionStage(timeLimiter, supplier);

Try<CompletionStage<String>> result = Try.ofSupplier(decoratedSupplier);
Ví dụ annotation (Spring):
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import org.springframework.stereotype.Service;
import java.util.concurrent.CompletableFuture;

@Service
public class BackendService {

    @TimeLimiter(name = "backendA", fallbackMethod = "fallbackMethod")
    public CompletableFuture<String> someMethod() {
        return CompletableFuture.supplyAsync(() -> {
            // Logic xử lý ở đây
            return "Success";
        });
    }

    public CompletableFuture<String> fallbackMethod(Throwable t) {
        return CompletableFuture.completedFuture("Fallback response due to: " + t.getMessage());
    }
}
Annotation + Spring Boot — Lưu ý cấu hình

Để dùng annotation của Resilience4j trong Spring Boot, cần thêm starter resilience4j-spring-boot3 và spring-boot-starter-aop vào pom.xml (đã trình bày ở trên).

[Unverified] Một số dự án/Spring Boot version có thể yêu cầu cấu hình hoặc annotation bổ sung; nếu bạn thấy annotation không hoạt động, hãy kiểm tra:

rằng AOP đang bật (thường spring-boot-starter-aop đủ),

rằng dependency resilience4j tương thích với phiên bản Spring Boot của bạn,

đọc tài liệu chính thức của Resilience4j cho phiên bản bạn đang dùng.

Ví dụ main application (không bắt buộc annotation thêm):
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
