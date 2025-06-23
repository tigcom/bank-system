# Card Registration Error Handling Guide

## Overview
Khi đăng ký thẻ tín dụng với tổ chức thẻ (VISA/MasterCard), có nhiều loại lỗi khác nhau có thể xảy ra. Việc xử lý retry phải phân biệt rõ các loại lỗi để tránh retry vô ích.

## 1. Business Logic Failures (❌ KHÔNG RETRY)

### Customer Information Issues
- **INVALID_CUSTOMER**: Thông tin khách hàng không hợp lệ
- **DUPLICATE_REGISTRATION**: Khách hàng đã có thẻ tại tổ chức này
- **BLACKLISTED_CUSTOMER**: Khách hàng trong danh sách đen
- **AGE_RESTRICTION**: Không đủ tuổi (< 18 tuổi)
- **CITIZENSHIP_RESTRICTION**: Không phải công dân hợp lệ

### Credit Assessment Failures
- **INSUFFICIENT_CREDIT_SCORE**: Điểm tín dụng không đạt yêu cầu
- **INCOME_VERIFICATION_FAILED**: Thu nhập không đủ hoặc không xác minh được
- **KYC_FAILED**: Thất bại xác minh danh tính (Know Your Customer)

### Compliance & Regulatory
- **SANCTIONS_LIST_MATCH**: Trùng khớp với danh sách trừng phạt
- **COMPLIANCE_VIOLATION**: Vi phạm quy định tuân thủ
- **AML_FLAG**: Cảnh báo chống rửa tiền

**⚠️ Các lỗi này KHÔNG NÊN retry vì là business rule violations**

## 2. Technical Failures (✅ CÓ THỂ RETRY)

### Network Issues
- **NETWORK_TIMEOUT**: Timeout kết nối mạng
- **CONNECTION_REFUSED**: Từ chối kết nối
- **DNS_RESOLUTION_FAILED**: Không resolve được domain
- **SSL_HANDSHAKE_FAILED**: Lỗi SSL certificate

### Server Errors  
- **HTTP 5xx**: Internal server errors (500, 502, 503, 504)
- **DATABASE_ERROR**: Lỗi database của tổ chức thẻ
- **SYSTEM_UNAVAILABLE**: Hệ thống tạm thời không khả dụng
- **SERVICE_MAINTENANCE**: Đang bảo trì hệ thống

**✅ Các lỗi này NÊN retry với exponential backoff**

## 3. Rate Limiting (🔄 RETRY VỚI DELAY DÀI)

### API Throttling
- **HTTP 429**: Too Many Requests
- **RATE_LIMITED**: Vượt quá giới hạn API calls
- **QUOTA_EXCEEDED**: Hết quota cho ngày/tháng

**🔄 Retry với delay dài hơn (1-5 phút)**

## 4. Temporary Issues (⏳ RETRY VỚI BACKOFF)

### Resource Constraints
- **PROCESSING_QUEUE_FULL**: Hàng đợi xử lý đầy
- **TEMPORARY_SERVICE_DEGRADATION**: Dịch vụ tạm thời chậm
- **DATABASE_LOCKS**: Database đang bị lock
- **EXTERNAL_SERVICE_TIMEOUT**: Timeout service bên thứ 3

**⏳ Retry với exponential backoff**

## Retry Strategy Implementation

```java
// Retry Configuration
maxRetries = 3
baseDelay = 2 seconds
multiplier = 2
maxDelay = 60 seconds

// Business Logic Failures -> NO RETRY
if (isBusinessLogicFailure(response)) {
    updateStatus(REGISTRATION_FAILED);
    return; // Stop immediately
}

// Rate Limiting -> RETRY with longer delay
if (response.getStatus().equals("RATE_LIMITED")) {
    delay = 60000; // 1 minute
    retry();
}

// Technical Failures -> RETRY with exponential backoff
if (isTechnicalFailure(response)) {
    delay = baseDelay * Math.pow(2, attempt);
    retry();
}
```

## Real-world Examples

### Example 1: Invalid Customer Data
```json
{
  "status": "FAILED",
  "errorCode": "INVALID_CUSTOMER", 
  "message": "Customer age below minimum requirement"
}
```
**Action**: ❌ NO RETRY - Update status to REGISTRATION_FAILED

### Example 2: Network Timeout
```json
{
  "status": "ERROR",
  "errorCode": "NETWORK_TIMEOUT",
  "message": "Connection timeout after 30 seconds"
}
```
**Action**: ✅ RETRY - Wait 2s, 4s, 8s then retry

### Example 3: Rate Limited
```json
{
  "status": "RATE_LIMITED", 
  "errorCode": "TOO_MANY_REQUESTS",
  "message": "API rate limit exceeded. Try again after 60 seconds"
}
```
**Action**: 🔄 RETRY - Wait 60s then retry

## Monitoring & Alerting

### Success Rate Metrics
- Track success rate by error type
- Alert if business logic failures > 10%
- Alert if technical failures > 5%

### Retry Metrics
- Average retry attempts per registration
- Time to success after retries
- Most common failure reasons

## Best Practices

1. **Always log error codes and messages** for debugging
2. **Different retry delays** for different error types
3. **Circuit breaker** for persistent failures
4. **Dead letter queue** for failed registrations
5. **Manual review queue** for business logic failures 