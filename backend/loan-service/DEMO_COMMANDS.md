# 🎯 Demo Commands - Chỉ cần copy-paste để chạy

## 📋 Cách sử dụng: Copy từng lệnh và paste vào terminal

### 1. Chuẩn bị môi trường (chỉ cần làm 1 lần)

```bash
# Di chuyển đến thư mục loan-service
cd /home/phuc-khang/java-banking-phase2/backend/loan-service

# Cấp quyền thực thi cho scripts
chmod +x scripts/*.sh
```

### 2. Khởi động các dịch vụ (copy từng lệnh)

```bash
# Terminal 1: Khởi động Temporal
docker run -d -p 7233:7233 --name temporal-server temporalio/server:latest

# Terminal 2: Khởi động MySQL
docker run -d -p 3306:3306 --name mysql -e MYSQL_ROOT_PASSWORD=root mysql:8.0

# Terminal 3: Khởi động Loan Service
mvn spring-boot:run
```

### 3. Kiểm tra service đã chạy (copy từng lệnh)

```bash
# Kiểm tra Loan Service
curl http://localhost:9827/actuator/health

# Kiểm tra Temporal
curl http://localhost:7233/health

# Kiểm tra MySQL
docker exec mysql mysql -u root -proot -e "SELECT 1"
```

### 4. Chạy test đơn giản (copy từng lệnh)

#### Test 1: Kiểm tra Temporal workflow
```bash
# Chạy test Temporal
./scripts/test-temporal.sh
```

#### Test 2: Kiểm tra Resilience4j
```bash
# Chạy test Resilience4j
./scripts/test-resilience4j.sh
```

#### Test 3: Chạy tất cả tests
```bash
# Chạy tất cả
./scripts/run-all-tests.sh
```

### 5. Test bằng tay (copy từng lệnh)

#### Test Temporal bằng tay:
```bash
# Tạo loan test
curl -X POST http://localhost:9827/api/loans/test-workflow \
  -H "Content-Type: application/json" \
  -d '{"loanId": 12345, "amount": 50000000}'

# Kiểm tra kết quả sau 10 giây
sleep 10 && curl http://localhost:9827/api/loans/12345/status
```

#### Test Resilience4j bằng tay:
```bash
# Test circuit breaker
curl http://localhost:9827/actuator/health/circuitbreakers

# Test rate limiter (chạy nhanh 15 lần)
for i in {1..15}; do
  curl -s http://localhost:9827/api/test/rate-limit &
done
```

### 6. Kiểm tra kết quả (copy từng lệnh)

#### Xem logs:
```bash
# Xem logs real-time
tail -f logs/app.log

# Tìm lỗi
grep "ERROR" logs/app.log | tail -10
```

#### Kiểm tra metrics:
```bash
# Xem tất cả metrics
curl http://localhost:9827/actuator/metrics | jq .

# Xem circuit breaker
curl http://localhost:9827/actuator/health/circuitbreakers | jq .

# Xem Temporal metrics
curl http://localhost:9827/actuator/metrics | grep temporal
```

### 7. Lệnh dọn dẹp (khi cần restart)

```bash
# Dừng containers
docker stop temporal-server mysql

# Xóa containers
docker rm temporal-server mysql

# Xóa logs cũ
rm -f logs/app.log
```

## 🎯 Kết quả mong đợi khi chạy

### Khi thành công:
```
✅ Loan Service đang chạy
✅ Temporal Server đang chạy
✅ Workflow hoàn thành thành công
✅ Circuit breaker hoạt động
✅ Tất cả tests đã PASS
```

### Khi thất bại:
```
❌ Loan Service không khả dụng
❌ Workflow thất bại
❌ Có lỗi xảy ra
```

## 🚨 Lỗi thường gặp và cách fix

### Lỗi 1: "Permission denied"
```bash
# Fix: Chạy lệnh này
chmod +x scripts/*.sh
```

### Lỗi 2: "Connection refused"
```bash
# Fix: Kiểm tra service
docker ps
# Nếu thiếu service nào, chạy lại lệnh khởi động
```

### Lỗi 3: "No such file"
```bash
# Fix: Kiểm tra đường dẫn
pwd
ls -la scripts/
```

## 📊 Quick Test Commands (30 giây)

```bash
# Test nhanh trong 30 giây
cd /home/phuc-khang/java-banking-phase2/backend/loan-service
./scripts/test-temporal.sh | grep -E "✅|❌"
```

## 🎮 Interactive Test

```bash
# Test tương tác
echo "Nhập loan ID:"
read loan_id
curl -X POST http://localhost:9827/api/loans/test-workflow \
  -d "{\"loanId\": $loan_id, \"amount\": 1000000}"
echo "Đang kiểm tra..."
sleep 5
curl http://localhost:9827/api/loans/$loan_id/status
```

## 📱 Test bằng browser

Mở browser và vào:
- http://localhost:9827/actuator/health (kiểm tra service)
- http://localhost:9827/actuator/metrics (xem metrics)
- http://localhost:7233 (Temporal Web UI)

## 🎯 Tóm tắt 3 bước nhanh

1. **Chuẩn bị**: Copy lệnh khởi động service
2. **Test**: Copy `./scripts/test-temporal.sh`
3. **Kiểm tra**: Copy `curl http://localhost:9827/actuator/health`

**Lưu ý: Chỉ cần copy-paste từng lệnh, không cần hiểu code!**
