# Credit Request OTP Workflow - Updated

## Tổng quan
Đã cập nhật quy trình tạo yêu cầu thẻ tín dụng với bước xác thực OTP TRƯỚC KHI TẠO REQUEST, sau đó admin sẽ review và gửi email thông báo kết quả.

## Quy trình mới (Updated)

### **Bước 1: Khởi tạo yêu cầu và gửi OTP**
**Endpoint:** `POST /initiate-credit-request`

**Request Body:**
```json
{
  "occupation": "Software Engineer",
  "monthlyIncome": 15000000.00,
  "cartTypeId": "VISA"
}
```

**Response:**
```json
{
  "status": 201,
  "message": "OTP đã được gửi đến email của bạn. Vui lòng xác thực để hoàn tất yêu cầu.",
  "data": {
    "id": "TEMP_CREDIT_REQUEST:CIF123456:1704067200000",
    "cifCode": "CIF123456",
    "occupation": "Software Engineer",
    "monthlyIncome": 15000000.00,
    "cartTypeId": "VISA",
    "status": "PENDING"
  }
}
```

**Lưu ý:** 
- Thông tin được lưu tạm trong Redis với key: `TEMP_CREDIT_REQUEST:{cifCode}:{timestamp}`
- OTP được gửi qua email HTML template đẹp
- Có hiệu lực 10 phút

### **Bước 2: Xác thực OTP và tạo Credit Request**
**Endpoint:** `POST /confirm-otp-credit`

**Request Body:**
```json
{
  "creditRequestId": "TEMP_CREDIT_REQUEST:CIF123456:1704067200000",
  "otpCode": "123456"
}
```

**Response:**
```json
{
  "status": 201,
  "message": "Xác thực OTP thành công! Yêu cầu thẻ tín dụng đã được tạo và đang chờ duyệt.",
  "data": {
    "id": "actual-credit-request-id",
    "cifCode": "CIF123456",
    "accountType": "CREDIT",
    "status": "ACTIVE"
  }
}
```

### **Bước 3: Admin Review và Phê duyệt**
**Endpoint:** `POST /admin/approve-credit-request/{id}`

**Response:**
```json
{
  "status": 200,
  "message": "Tạo tài khoản tín dụng thành công",
  "data": {
    "id": "account-id",
    "accountNumber": "1234567890",
    "cifCode": "CIF123456",
    "accountType": "CREDIT",
    "status": "ACTIVE"
  }
}
```

**Auto Actions:**
- ✅ Kiểm tra business rules (tuổi, thu nhập)
- ✅ Tạo tài khoản tín dụng trong local DB
- ✅ Tạo tài khoản trong core banking
- ✅ Gửi email thông báo phê duyệt (HTML template đẹp)

### **Bước 4: Admin Từ chối (nếu cần)**
**Endpoint:** `POST /admin/reject-credit-request/{id}`

**Auto Actions:**
- ✅ Update status thành REJECTED
- ✅ Gửi email thông báo từ chối với lý do chi tiết
- ✅ Gợi ý cải thiện hồ sơ

## Các tính năng bảo mật nâng cao

### 1. **OTP Management**
- **Thời gian hiệu lực:** 10 phút (giảm từ 60 phút)
- **Lưu trữ:** Redis với key pattern `OTP:CREDIT:{tempKey}`
- **Rate limiting:** Tối đa 3 lần nhập sai → Xóa request
- **Auto cleanup:** Xóa data sau khi verify thành công

### 2. **Temporary Data Storage**
- **Key pattern:** `TEMP_CREDIT_REQUEST:{cifCode}:{timestamp}`
- **TTL:** 1 giờ
- **Auto cleanup:** Sau khi tạo request thành công

### 3. **Email Templates HTML**
#### OTP Template (`dto-template.html`)
- Design đẹp với gradient background
- Hiển thị OTP nổi bật
- Warning về bảo mật
- Brand identity ngân hàng

#### Approval Template (`credit-approval-template.html`)
- 🎉 Celebration design
- Thông tin tài khoản và loại thẻ
- Hướng dẫn sử dụng
- Thông tin liên hệ

#### Rejection Template (`credit-rejection-template.html`)
- ❌ Professional rejection design  
- Lý do từ chối chi tiết
- Gợi ý cải thiện hồ sơ
- Thông tin về việc đăng ký lại

## API Endpoints Summary

### **Customer APIs**
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/initiate-credit-request` | Bước 1: Khởi tạo và gửi OTP |
| POST | `/resend-otp-credit/{tempKey}` | Gửi lại OTP |
| POST | `/confirm-otp-credit` | Bước 2: Xác thực OTP và tạo request |

### **Admin APIs**
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/admin/get-all-credit-request` | Lấy danh sách chờ duyệt |
| POST | `/admin/approve-credit-request/{id}` | Phê duyệt và tạo tài khoản |
| POST | `/admin/reject-credit-request/{id}` | Từ chối với email thông báo |

## Configuration Updates

### **Notification Service**
```properties
# New topic for credit notifications
spring.cloud.stream.bindings.creditNotificationConsumer-in-0.destination=send-credit-notification
spring.cloud.stream.bindings.creditNotificationConsumer-in-0.group=credit-notification-group
```

### **Account Service**
```properties
# OTP settings
otp.credit.expiry.minutes=10
otp.credit.max-attempts=3

# Temp request settings  
temp-request.expiry.minutes=60
```

## Error Handling

### **OTP Related**
- `OTP_EXPIRED`: OTP hết hạn
- `OTP_WRONG_MANY`: Nhập sai quá 3 lần → Xóa request
- `INVALID_OTP`: OTP không đúng

### **Business Rules**
- `AGE_INVALID`: Dưới 21 tuổi → Auto reject
- `INCOME_INVALID`: Thu nhập không đủ → Auto reject
- `CREDIT_REQUEST_NOTEXISTED`: Request không tồn tại

## Monitoring & Analytics

### **Key Metrics**
- OTP success rate
- Request completion rate  
- Admin approval rate
- Time from request to decision
- Email delivery rate

### **Redis Keys to Monitor**
- `TEMP_CREDIT_REQUEST:*` - Temporary requests
- `OTP:CREDIT:*` - Active OTPs
- `OTP_FAIL_COUNT:CREDIT:*` - Failure counts

## Benefits của Workflow Mới

### **Cho Customer**
✅ Xác thực OTP ngay từ đầu → Bảo mật cao  
✅ Email thông báo đẹp và chi tiết  
✅ Biết rõ trạng thái và next steps  
✅ Có gợi ý nếu bị từ chối  

### **Cho Admin**  
✅ Chỉ review các request đã verify OTP  
✅ Auto business rules validation  
✅ Email notification tự động  
✅ Centralized management console  

### **Cho System**
✅ Giảm spam requests  
✅ Better security với rate limiting  
✅ Clean data flow  
✅ Comprehensive audit trail  

## Future Enhancements
1. **SMS OTP** option
2. **Push notification** cho mobile app
3. **Advanced fraud detection** 
4. **AI-powered risk assessment**
5. **Real-time decision engine** 