# 🧪 Hướng dẫn kiểm thử Temporal và Resilience4j cho Loan Service

## 📋 Tổng quan
Đây là bộ công cụ kiểm thử toàn diện để xác minh Temporal và Resilience4j hoạt động ổn định trong Loan Service.

## 🚀 Cách sử dụng

### 1. Chuẩn bị môi trường

```bash
# 1. Khởi động các dịch vụ cần thiết
docker run -d -p 7233:7233 --name temporal-server temporalio/server:latest
docker run -d -p 3306:3306 --name mysql -e MYSQL_ROOT_PASSWORD=root mysql:8.0

# 2. Khởi động Loan Service
cd backend/loan-service
mvn spring-boot:run
```

### 2. Chạy kiểm thử

#### Cách 1: Chạy từng bộ test riêng lẻ
```bash
# Test Temporal Workflow
bash scripts/test-temporal.sh

# Test Resilience4j
bash scripts/test-resilience4j.sh
```

#### Cách 2: Chạy tất cả tests
```bash
# Chạy toàn bộ test suite
bash scripts/run-all-tests.sh
```

### 3. Các kịch bản kiểm thử chi tiết

#### 🔗 Temporal Workflow Tests
| Test Case | Mục tiêu | Lệnh |
|-----------|----------|------|
| Happy Path | Kiểm tra workflow hoàn chỉnh | `curl -X POST localhost:9827/api/loans/test-workflow` |
| Retry Test | Kiểm tra retry mechanism | `curl -X POST localhost:9827/api/loans/test-retry` |
| Timeout Test | Kiểm tra timeout handling | `curl -X POST localhost:9827/api/loans/test-timeout` |
| Rollback Test | Kiểm tra rollback khi lỗi | `curl -X POST localhost:9827/api/loans/test-failure` |

#### ⚡ Resilience4j Tests
| Test Case | Mục tiêu | Lệnh |
|-----------|----------|------|
| Circuit Breaker | Kiểm tra mở/đóng circuit | `curl -X GET localhost:9827/api/test/circuit-breaker` |
| Rate Limiter | Kiểm tra giới hạn tốc độ | `curl -X POST localhost:9827/api/test/rate-limit` |
| Retry | Kiểm tra thử lại khi lỗi | `curl -X GET localhost:9827/api/test/retry` |
| Bulkhead | Kiểm tra isolation | `curl -X GET localhost:9827/api/test/bulkhead` |
| Timeout | Kiểm tra time limiter | `curl -X GET localhost:9827/api/test/timeout` |

### 4. Monitoring và kiểm tra kết quả

#### Health Checks
```bash
# Kiểm tra health của service
curl http://localhost:9827/actuator/health

# Kiểm tra circuit breakers
curl http://localhost:9827/actuator/health/circuitbreakers

# Kiểm tra metrics
curl http://localhost:9827/actuator/metrics
```

#### Dashboard Monitoring
- **Temporal Web UI**: http://localhost:8088
- **Loan Service Metrics**: http://localhost:9827/actuator/prometheus
- **Health Dashboard**: http://localhost:9827/actuator/health

### 5. Các endpoints test API

#### Temporal Test Endpoints
```bash
# Tạo loan test
POST /api/loans/test-workflow
Body: {"loanId": 12345, "amount": 50000000}

# Kiểm tra status workflow
GET /api/loans/{loanId}/status

# Test retry mechanism
POST /api/loans/test-retry
Body: {"loanId": 12346, "simulateFailure": true}
```

#### Resilience4j Test Endpoints
```bash
# Test circuit breaker
GET /api/test/circuit-breaker/normal
GET /api/test/circuit-breaker/fail

# Test rate limiter
POST /api/test/rate-limit

# Test bulkhead
GET /api/test/bulkhead/external-api
```

### 6. Cách đọc kết quả

#### ✅ PASS khi:
- Workflow hoàn thành trong < 30s
- Circuit breaker mở đúng threshold (50% failure rate)
- Rate limiter trả về 429 khi vượt quá giới hạn
- Retry thử lại đúng 3 lần
- Fallback được kích hoạt khi service down

#### ❌ FAIL khi:
- Workflow timeout hoặc lỗi không được xử lý
- Circuit breaker không mở khi cần thiết
- Rate limiter không hoạt động
- Retry không thử lại hoặc thử lại sai số lần

### 7. Troubleshooting

#### Lỗi thường gặp
1. **Temporal Server không kết nối được**
   - Kiểm tra: `docker ps | grep temporal`
   - Fix: `docker restart temporal-server`

2. **Circuit breaker không mở**
   - Kiểm tra logs: `tail -f logs/app.log`
   - Kiểm tra config: `curl localhost:9827/actuator/configprops`

3. **Rate limiter không hoạt động**
   - Kiểm tra metrics: `curl localhost:9827/actuator/metrics/resilience4j.ratelimiter`

### 8. Công cụ hỗ trợ

#### Postman Collection
Tải file: `loan-service-test.postman_collection.json`

#### JMeter Load Test
Tải file: `loan-service-load-test.jmx`

#### Log Analysis
```bash
# Xem logs real-time
tail -f backend/loan-service/logs/app.log

# Tìm lỗi
grep "ERROR" backend/loan-service/logs/app.log
```

## 📊 Kết quả mong đợi

| Metric | Target | Cách kiểm tra |
|--------|--------|---------------|
| Workflow Success Rate | ≥ 99% | `curl localhost:9827/actuator/metrics/temporal.workflow.success` |
| Circuit Breaker Response Time | < 5s | `curl localhost:9827/actuator/metrics/resilience4j.circuitbreaker.calls` |
| Rate Limiter 429 Rate | Đúng config | `curl localhost:9827/actuator/metrics/resilience4j.ratelimiter` |
| Retry Attempts | ≤ 3 lần | `curl localhost:9827/actuator/metrics/resilience4j.retry.calls` |

## 🎯 Next Steps
Sau khi tests pass thành công:
1. Deploy lên staging environment
2. Chạy load test với JMeter
3. Monitor trong 24h với production traffic
4. Document kết quả và cấu hình tối ưu
