# 📋 Kịch bản kiểm thử Temporal và Resilience4j cho Loan Service

## 🎯 Mục tiêu
Xác minh Temporal và Resilience4j hoạt động ổn định trong môi trường thực tế

## 🧪 Các kịch bản kiểm thử

### 1. 🔗 Temporal Workflow Testing

#### 1.1 Kiểm tra workflow hoàn chỉnh
```bash
# Gọi API khởi tạo workflow
curl -X POST http://localhost:9827/api/loans/test-workflow \
  -H "Content-Type: application/json" \
  -d '{"loanId": 12345, "amount": 50000000}'
```

#### 1.2 Kiểm tra các bước workflow
| Bước | API Test | Kết quả mong đợi |
|------|----------|------------------|
| Validate Loan | GET /api/loans/12345/validate | Trả về loan data |
| Create Account | GET /api/loans/12345/account | Account number được tạo |
| Disbursement | GET /api/loans/12345/disbursement | Transaction reference |
| Final Approval | GET /api/loans/12345/status | Status = APPROVED |

#### 1.3 Kiểm tra retry và timeout
```bash
# Test timeout scenario
curl -X POST http://localhost:9827/api/loans/test-timeout \
  -d '{"loanId": 99999, "simulateTimeout": true}'
```

### 2. ⚡ Resilience4j Testing

#### 2.1 Circuit Breaker Testing
```bash
# Test 1: Normal flow
curl -X GET http://localhost:9827/api/test/circuit-breaker/normal

# Test 2: Force failure (50% failure rate)
for i in {1..20}; do
  curl -X GET http://localhost:9827/api/test/circuit-breaker/fail
done

# Test 3: Check circuit breaker state
curl -X GET http://localhost:9827/actuator/health/circuitbreakers
```

#### 2.2 Rate Limiter Testing
```bash
# Test rate limiting (10 requests/minute)
for i in {1..15}; do
  curl -X POST http://localhost:9827/api/loans/create-test \
    -d "{\"amount\":1000000,\"testId\":$i}"
done
```

#### 2.3 Retry Testing
```bash
# Test retry mechanism
curl -X GET http://localhost:9827/api/test/retry/fail-then-success
```

### 3. 🔍 Monitoring & Metrics

#### 3.1 Health Check Endpoints
```bash
# Circuit breaker health
curl http://localhost:9827/actuator/health/circuitbreakers

# Rate limiter metrics
curl http://localhost:9827/actuator/metrics/resilience4j.ratelimiter

# Temporal metrics
curl http://localhost:9827/actuator/metrics/temporal.workflow
```

#### 3.2 Prometheus Metrics
```bash
# Check all metrics
curl http://localhost:9827/actuator/prometheus | grep -E "(resilience4j|temporal)"
```

## 🛠️ Công cụ kiểm thử

### 1. Postman Collection
Tải file: `loan-service-test.postman_collection.json`

### 2. JMeter Script
Tải file: `loan-service-load-test.jmx`

### 3. Monitoring Dashboard
- Grafana: http://localhost:3000 (dashboard: "Loan Service Health")
- Temporal Web UI: http://localhost:8088

## 📊 Kịch bản chi tiết

### Scenario 1: Happy Path
1. Tạo loan request
2. Workflow chạy hoàn chỉnh
3. Kiểm tra kết quả cuối cùng

### Scenario 2: Failure Handling
1. Simulate Core Banking service down
2. Verify circuit breaker opens
3. Check fallback behavior
4. Verify retry mechanism

### Scenario 3: Rate Limiting
1. Send burst requests
2. Verify rate limiting kicks in
3. Check 429 responses

### Scenario 4: Timeout Handling
1. Simulate slow external service
2. Verify timeout handling
3. Check workflow compensation

## 🎯 Checklist kiểm tra

### ✅ Temporal Workflow
- [ ] Workflow khởi động thành công
- [ ] Các activities được gọi đúng thứ tự
- [ ] Retry hoạt động khi có lỗi
- [ ] Timeout được xử lý đúng cách
- [ ] Rollback hoạt động khi cần thiết

### ✅ Resilience4j
- [ ] Circuit breaker mở/đóng đúng cách
- [ ] Rate limiter giới hạn đúng tốc độ
- [ ] Retry thử lại đúng số lần
- [ ] Bulkhead tách biệt tài nguyên
- [ ] Fallback methods được gọi khi cần

### ✅ Monitoring
- [ ] Metrics được ghi nhận đầy đủ
- [ ] Health checks trả về đúng trạng thái
- [ ] Logs ghi nhận đầy đủ thông tin
- [ ] Dashboard hiển thị đúng dữ liệu

## 🚀 Cách chạy kiểm thử

### Bước 1: Khởi động môi trường
```bash
# Khởi động Temporal server
docker run -d -p 7233:7233 --name temporal-server temporalio/server:latest

# Khởi động Loan Service
cd backend/loan-service
mvn spring-boot:run
```

### Bước 2: Chạy test script
```bash
# Chạy tất cả kịch bản test
./scripts/run-all-tests.sh

# Hoặc chạy từng kịch bản
./scripts/test-temporal.sh
./scripts/test-resilience4j.sh
```

### Bước 3: Kiểm tra kết quả
```bash
# Xem logs
tail -f logs/app.log

# Kiểm tra metrics
curl http://localhost:9827/actuator/prometheus
```

## 📈 Kết quả mong đợi

| Metric | Giá trị mong đợi |
|--------|------------------|
| Workflow success rate | ≥ 99% |
| Circuit breaker open/close | Đúng threshold |
| Rate limiter 429 responses | Đúng giới hạn |
| Retry attempts | ≤ 3 lần |
| Response time | < 5s cho 95% requests |

## 🔄 Maintenance
- Chạy test suite hàng tuần
- Monitor metrics hàng ngày
- Update configuration khi cần thiết
