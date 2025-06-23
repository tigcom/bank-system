# Payment Account Creation Workflow

## Tổng quan
Quy trình tạo tài khoản thanh toán được cập nhật để tối ưu trải nghiệm người dùng:
- **Lần đầu tạo tài khoản**: Tạo luôn không cần OTP
- **Tạo thêm tài khoản**: Yêu cầu xác thực OTP

## Quy trình mới

### **Bước 1: Gửi yêu cầu tạo tài khoản thanh toán**
**Endpoint:** `POST /create-payment-request`

**Request Body:**
```json
{
  "cifCode": "CIF00000001"
}
```

**Response Case 1 - Lần đầu tạo (không cần OTP):**
```json
{
  "status": 201,
  "message": "Tài khoản thanh toán đã được tạo thành công",
  "data": {
    "id": "actual-account-id",
    "cifCode": "CIF00000001",
    "accountType": "PAYMENT",
    "status": "APPROVED"
  }
}
```

**Response Case 2 - Đã có tài khoản (cần OTP):**
```json
{
  "status": 201,
  "message": "OTP đã được gửi đến email của bạn. Vui lòng xác thực để hoàn tất tạo tài khoản.",
  "data": {
    "id": "TEMP_PAYMENT_REQUEST:CIF00000001:1704067200000",
    "cifCode": "CIF00000001",
    "accountType": "PAYMENT",
    "status": "PENDING"
  }
}
```

### **Bước 2: Xác thực OTP (chỉ khi cần)**
**Endpoint:** `POST /confirm-otp-payment`

**Request Body:**
```json
{
  "paymentRequestId": "TEMP_PAYMENT_REQUEST:CIF00000001:1704067200000",
  "otpCode": "123456"
}
```

**Response:**
```json
{
  "status": 201,
  "message": "Xác thực OTP thành công! Tài khoản thanh toán đã được tạo.",
  "data": {
    "accountNumber": "CIF00000001012345",
    "cifCode": "CIF00000001",
    "id": "actual-account-id",
    "accountType": "PAYMENT",
    "status": "ACTIVE"
  }
}
```

### **Bước 3: Gửi lại OTP (nếu cần)**
**Endpoint:** `POST /resend-payment-otp/{tempRequestKey}`

**Response:**
```json
{
  "status": 200,
  "message": "OTP đã được gửi lại thành công.",
  "data": "OTP resent to user email."
}
```

## Logic kiểm tra

1. **Validate cifCode**: Kiểm tra customer tồn tại và có trạng thái ACTIVE
2. **Kiểm tra tài khoản payment hiện có**:
   - Gọi API: `GET /corebanking/get-all-paymentaccount-by-cifcode/{cifCode}`
   - Nếu danh sách trống → Tạo luôn
   - Nếu có tài khoản → Yêu cầu OTP

## OTP Security

- **Thời gian hiệu lực**: 10 phút
- **Số lần thử sai tối đa**: 3 lần
- **Key format**: `OTP:PAYMENT:{tempRequestKey}`
- **Fail count key**: `OTP_FAIL_COUNT:PAYMENT:{tempRequestKey}`

## Temporary Data

- **Format**: `TEMP_PAYMENT_REQUEST:{cifCode}:{timestamp}`
- **Thời gian lưu**: 60 phút
- **Cleanup**: Tự động xóa sau khi xác thực thành công hoặc quá 3 lần sai

## Error Handling

- **Customer không tồn tại**: `CUSTOMER_NOT_FOUND`
- **Customer không active**: `CUSTOMER_NOTACTIVE`
- **OTP hết hạn/sai**: Sử dụng error codes có sẵn
- **Quá 3 lần sai OTP**: Xóa temp data và yêu cầu tạo request mới 