# 📚 Hướng dẫn chi tiết cách chạy kiểm thử (dành cho người mới)

## 🎯 Mục tiêu: Giúp bạn hiểu và thực thi các file test một cách dễ dàng

## 1. Hiểu file .sh là gì?

File `.sh` là **script shell** - giống như một "danh sách công việc" tự động cho máy tính thực hiện.

**Ví dụ đơn giản:**
```bash
# Giống như bạn viết:
# 1. Mở trình duyệt
# 2. Vào website localhost:9827
# 3. Click nút "Test"
# 4. Ghi lại kết quả
```

## 2. Cách thực thi file .sh (3 cách đơn giản)

### Cách 1: Click đúp chuột (giống mở file .exe)
1. Mở terminal (Command Prompt hoặc Terminal)
2. Gõ lệnh:
```bash
cd /home/phuc-khang/java-banking-phase2/backend/loan-service
bash scripts/test-temporal.sh
```

### Cách 2: Kéo thả vào terminal
1. Mở terminal
2. Kéo file `test-temporal.sh` vào terminal
3. Nhấn Enter

### Cách 3: Click chuột phải → "Run in Terminal"

## 3. Nhận biết kết quả như thế nào?

### ✅ Khi thành công:
```
✅ Loan Service đang chạy
✅ Temporal Server đang chạy
✅ Workflow hoàn thành thành công
🎉 Tất cả tests đã PASS
```

### ❌ Khi thất bại:
```
❌ Loan Service không khả dụng
❌ Workflow thất bại
❌ Có lỗi xảy ra
```

### 📊 Kết quả chi tiết:
```
📊 Kết quả kiểm thử:
- Số test thành công: 5/5
- Thời gian test: 30 giây
- Lỗi: Không có
```

## 4. Hướng dẫn từng bước thực thi

### Bước 1: Chuẩn bị (giống chuẩn bị bài kiểm tra)
```bash
# 1. Mở terminal
# 2. Di chuyển đến thư mục
cd /home/phuc-khang/java-banking-phase2/backend/loan-service

# 3. Kiểm tra file có tồn tại không
ls -la scripts/
```

### Bước 2: Chạy test đơn giản
```bash
# Test Temporal (kiểm tra workflow)
./scripts/test-temporal.sh

# Kết quả sẽ hiện ra từng dòng:
# 🚀 Bắt đầu kiểm thử Temporal Workflow
# ✅ Loan Service đang chạy
# ✅ Temporal Server đang chạy
# 🎯 Test 1: Happy Path Workflow
# ✅ Workflow hoàn thành thành công
```

### Bước 3: Chạy test Resilience4j
```bash
# Test Resilience4j (kiểm tra khả năng chịu lỗi)
./scripts/test-resilience4j.sh

# Kết quả:
# ⚡ Bắt đầu kiểm thử Resilience4j
# ✅ Circuit breaker hoạt động
# ✅ Rate limiter hoạt động
```

## 5. Giải thích từng phần trong kết quả

### Ví dụ kết quả chi tiết:
```
🚀 Bắt đầu kiểm thử Temporal Workflow
🔍 Kiểm tra kết nối service...
✅ Loan Service đang chạy     ← Service đã khởi động
✅ Temporal Server đang chạy  ← Temporal đã sẵn sàng
🎯 Test 1: Happy Path Workflow
Tạo loan test với ID: 12345   ← Đang tạo dữ liệu test
✅ Workflow hoàn thành thành công  ← Test thành công
📊 Tổng kết: 5/5 tests PASS   ← Tóm tắt kết quả
```

## 6. Cách xử lý khi gặp lỗi

### Lỗi 1: "Permission denied"
```bash
# Giải pháp: Cấp quyền thực thi
chmod +x scripts/test-temporal.sh
chmod +x scripts/test-resilience4j.sh
```

### Lỗi 2: "Service không khả dụng"
```bash
# Giải pháp: Khởi động lại service
mvn spring-boot:run
```

### Lỗi 3: "Connection refused"
```bash
# Giải pháp: Kiểm tra port
netstat -tulpn | grep 9827
```

## 7. Cách kiểm tra bằng tay (không cần script)

### Test Temporal bằng tay:
```bash
# 1. Tạo loan test
curl -X POST http://localhost:9827/api/loans/test-workflow \
  -H "Content-Type: application/json" \
  -d '{"loanId": 99999, "amount": 1000000}'

# 2. Kiểm tra kết quả
curl http://localhost:9827/api/loans/99999/status
```

### Test Resilience4j bằng tay:
```bash
# 1. Test circuit breaker
curl http://localhost:9827/api/test/circuit-breaker/normal

# 2. Kiểm tra metrics
curl http://localhost:9827/actuator/health/circuitbreakers
```

## 8. Visual Studio Code - Cách đơn giản nhất

### Trong VS Code:
1. Mở terminal (Ctrl + `)
2. Gõ: `cd backend/loan-service`
3. Gõ: `bash scripts/test-temporal.sh`
4. Xem kết quả hiện ra trong terminal

## 9. Kiểm tra bằng Postman (dễ nhìn)

### Import collection:
1. Mở Postman
2. Import file: `loan-service-test.postman_collection.json`
3. Click "Run" để thấy kết quả trực quan

## 10. Tóm tắt nhanh

| Cách thực hiện | Lệnh | Kết quả mong đợi |
|----------------|------|------------------|
| Terminal | `bash scripts/test-temporal.sh` | ✅ PASS hoặc ❌ FAIL |
| Manual curl | `curl localhost:9827/api/test` | JSON response |
| Postman | Import collection | GUI hiển thị |
| Browser | `localhost:9827/actuator/health` | Web interface |

## 🔍 Khi nào thì OK?
- ✅ Khi thấy "PASS" hoặc "SUCCESS"
- ✅ Khi không có lỗi ERROR
- ✅ Khi metrics hiển thị đúng giá trị

## ❗ Khi nào cần fix?
- ❌ Khi thấy "FAIL" hoặc "ERROR"
- ❌ Khi service không khởi động được
- ❌ Khi kết nối bị từ chối

**Lưu ý: Bạn không cần hiểu code trong file .sh, chỉ cần biết cách chạy và đọc kết quả!**
