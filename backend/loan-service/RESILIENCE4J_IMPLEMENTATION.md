# Resilience4j Implementation Guide for Loan Service

## Tổng quan

Loan Service đã được tích hợp Resilience4j để tăng cường khả năng chịu lỗi và ổn định của hệ thống. Tài liệu này mô tả cách Resilience4j được áp dụng và cấu hình trong dự án.

## Các Pattern đã áp dụng

### 1. CircuitBreaker (Mạch ngắt)

**Mục đích**: Bảo vệ hệ thống khỏi cascade failure khi các service bên ngoài bị lỗi.

**Các service được áp dụng**:
- **Core Banking Service**: `coreBanking`
- **CIC Service**: `cicService` 
- **Dubbo Services**: `dubboServices`

**Cấu hình**:
```properties
# Core Banking Service
resilience4j.circuitbreaker.instances.coreBanking.slidingWindowSize=100
resilience4j.circuitbreaker.instances.coreBanking.failureRateThreshold=50
resilience4j.circuitbreaker.instances.coreBanking.waitDurationInOpenState=10000
resilience4j.circuitbreaker.instances.coreBanking.permittedNumberOfCallsInHalfOpenState=10

# CIC Service
resilience4j.circuitbreaker.instances.cicService.slidingWindowSize=50
resilience4j.circuitbreaker.instances.cicService.failureRateThreshold=40
resilience4j.circuitbreaker.instances.cicService.waitDurationInOpenState=15000
resilience4j.circuitbreaker.instances.cicService.permittedNumberOfCallsInHalfOpenState=5

# Dubbo Services
resilience4j.circuitbreaker.instances.dubboServices.slidingWindowSize=200
resilience4j.circuitbreaker.instances.dubboServices.failureRateThreshold=30
resilience4j.circuitbreaker.instances.dubboServices.waitDurationInOpenState=20000
resilience4j.circuitbreaker.instances.dubboServices.permittedNumberOfCallsInHalfOpenState=15
```

**Ví dụ sử dụng**:
```java
@CircuitBreaker(name = "coreBanking", fallbackMethod = "updateAccountFallback")
public CoreResponse updateAccount(CoreAccountRequest request) {
    // Logic gọi Core Banking API
}

public CoreResponse updateAccountFallback(CoreAccountRequest request, Throwable t) {
    log.warn("UPDATE_ACCOUNT_FALLBACK - request: {}, error: {}", request, t.getMessage());
    throw new RuntimeException("Core banking service is temporarily unavailable: " + t.getMessage(), t);
}
```

### 2. Retry (Thử lại)

**Mục đích**: Tự động thử lại khi có lỗi tạm thời.

**Cấu hình**:
```properties
# Core Banking Service
resilience4j.retry.instances.coreBanking.maxAttempts=3
resilience4j.retry.instances.coreBanking.waitDuration=5000

# CIC Service
resilience4j.retry.instances.cicService.maxAttempts=2
resilience4j.retry.instances.cicService.waitDuration=3000

# Dubbo Services
resilience4j.retry.instances.dubboServices.maxAttempts=3
resilience4j.retry.instances.dubboServices.waitDuration=2000
```

**Ví dụ sử dụng**:
```java
@Retry(name = "coreBanking", fallbackMethod = "getBalanceFallback")
public BigDecimal getBalance(String accountNumber) {
    // Logic gọi Core Banking API
}
```

### 3. RateLimiter (Giới hạn tốc độ)

**Mục đích**: Bảo vệ hệ thống khỏi quá tải và đảm bảo fair usage.

**Cấu hình**:
```properties
# Loan Creation
resilience4j.ratelimiter.instances.loanCreation.limitForPeriod=10
resilience4j.ratelimiter.instances.loanCreation.limitRefreshPeriod=60000

# Loan Approval
resilience4j.ratelimiter.instances.loanApproval.limitForPeriod=5
resilience4j.ratelimiter.instances.loanApproval.limitRefreshPeriod=60000

# Repayment Processing
resilience4j.ratelimiter.instances.repaymentProcessing.limitForPeriod=20
resilience4j.ratelimiter.instances.repaymentProcessing.limitRefreshPeriod=60000
```

**Ví dụ sử dụng**:
```java
@RateLimiter(name = "loanCreation", fallbackMethod = "createLoanFallback")
public Loan createLoan(Loan loan) {
    // Logic tạo khoản vay
}
```

### 4. Bulkhead (Chia sẻ tài nguyên)

**Mục đích**: Tách biệt các operation để tránh một operation làm ảnh hưởng toàn bộ system.

**Cấu hình**:
```properties
# External API Calls
resilience4j.bulkhead.instances.externalApiCalls.maxConcurrentCalls=10
resilience4j.bulkhead.instances.externalApiCalls.maxWaitDuration=5000

# Loan Processing
resilience4j.bulkhead.instances.loanProcessing.maxConcurrentCalls=5
resilience4j.bulkhead.instances.loanProcessing.maxWaitDuration=3000

# Database Operations
resilience4j.bulkhead.instances.databaseOperations.maxConcurrentCalls=15
resilience4j.bulkhead.instances.databaseOperations.maxWaitDuration=2000
```

**Ví dụ sử dụng**:
```java
@Bulkhead(name = "externalApiCalls", fallbackMethod = "checkCICFallback")
public CicResponse checkCIC(CICRequest cicRequest) {
    // Logic gọi CIC API
}
```

### 5. TimeLimiter (Giới hạn thời gian)

**Mục đích**: Tránh blocking indefinitely và đảm bảo SLA.

**Cấu hình**:
```properties
# Core Banking Service
resilience4j.timelimiter.instances.coreBanking.timeoutDuration=10000

# CIC Service
resilience4j.timelimiter.instances.cicService.timeoutDuration=8000

# Dubbo Services
resilience4j.timelimiter.instances.dubboServices.timeoutDuration=15000

# Loan Processing
resilience4j.timelimiter.instances.loanProcessing.timeoutDuration=30000
```

**Ví dụ sử dụng**:
```java
@TimeLimiter(name = "coreBanking", fallbackMethod = "getBalanceFallback")
public BigDecimal getBalance(String accountNumber) {
    // Logic gọi Core Banking API
}
```

## Các Service được áp dụng

### 1. CoreBankingClientImpl
- **CircuitBreaker**: Bảo vệ khi Core Banking service bị lỗi
- **Retry**: Thử lại khi có lỗi tạm thời
- **TimeLimiter**: Timeout cho các API call
- **Bulkhead**: Tách biệt external API calls

### 2. CICClientImpl
- **CircuitBreaker**: Bảo vệ khi CIC service bị lỗi
- **Retry**: Thử lại khi có lỗi tạm thời
- **TimeLimiter**: Timeout cho CIC API calls
- **Bulkhead**: Tách biệt external API calls

### 3. LoanServiceImpl
- **RateLimiter**: Giới hạn số lượng loan creation/approval
- **Bulkhead**: Tách biệt loan processing
- **TimeLimiter**: Timeout cho loan processing

### 4. RepaymentServiceImpl
- **RateLimiter**: Giới hạn số lượng repayment processing
- **Bulkhead**: Tách biệt database operations
- **Retry**: Thử lại khi có lỗi Dubbo services

### 5. LoanHandler
- **CircuitBreaker**: Bảo vệ khi Dubbo services bị lỗi
- **Retry**: Thử lại khi có lỗi tạm thời
- **TimeLimiter**: Timeout cho Dubbo service calls
- **Bulkhead**: Tách biệt loan processing

## Fallback Methods

Mỗi annotation Resilience4j đều có fallback method tương ứng:

```java
// Ví dụ fallback method
public CoreResponse updateAccountFallback(CoreAccountRequest request, Throwable t) {
    log.warn("UPDATE_ACCOUNT_FALLBACK - request: {}, error: {}", request, t.getMessage());
    throw new RuntimeException("Core banking service is temporarily unavailable: " + t.getMessage(), t);
}
```

## Monitoring và Metrics

Resilience4j tự động expose metrics qua Actuator endpoints:

- **Health Check**: `/actuator/health`
- **Circuit Breaker State**: `/actuator/health/circuitbreakers`
- **Metrics**: `/actuator/metrics`

### Các metrics quan trọng:
- `resilience4j.circuitbreaker.calls`: Số lần gọi circuit breaker
- `resilience4j.retry.calls`: Số lần retry
- `resilience4j.ratelimiter.available.permissions`: Số permission còn lại
- `resilience4j.bulkhead.available.concurrent.calls`: Số concurrent calls còn lại

## Best Practices

### 1. Fallback Strategy
- Luôn có fallback method cho mỗi annotation
- Fallback method nên log warning và throw meaningful exception
- Có thể return default value hoặc cached data

### 2. Configuration
- Điều chỉnh threshold dựa trên business requirements
- Monitor metrics để tối ưu configuration
- Test với different failure scenarios

### 3. Error Handling
- Log đầy đủ thông tin lỗi trong fallback methods
- Throw specific exceptions để client có thể handle appropriately
- Consider graceful degradation

### 4. Testing
- Test với simulated failures
- Verify fallback behavior
- Monitor performance impact

## Troubleshooting

### 1. Circuit Breaker không hoạt động
- Kiểm tra configuration trong application.properties
- Verify annotation names match configuration
- Check AOP is enabled

### 2. Fallback method không được gọi
- Verify method signature matches (parameters + Throwable)
- Check method visibility (public)
- Ensure no compilation errors

### 3. Performance issues
- Monitor metrics để identify bottlenecks
- Adjust timeout và retry configuration
- Consider bulkhead configuration

## Dependencies

```xml
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-spring-boot3</artifactId>
    <version>2.2.0</version>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-aop</artifactId>
</dependency>
<dependency>
    <groupId>io.vavr</groupId>
    <artifactId>vavr</artifactId>
    <version>0.10.4</version>
</dependency>
```

## Kết luận

Resilience4j đã được tích hợp thành công vào Loan Service, cung cấp:
- **Fault tolerance**: Bảo vệ khỏi cascade failures
- **Performance**: Rate limiting và bulkhead isolation
- **Reliability**: Retry mechanism cho transient failures
- **Monitoring**: Comprehensive metrics và health checks

Việc áp dụng Resilience4j giúp Loan Service trở nên robust hơn và có khả năng handle failures gracefully. 