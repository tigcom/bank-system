# Kiến trúc Keycloak cho Banking Microservices

## 1. Khái niệm cơ bản

### 🔑 **Users vs Clients**
- **Users**: Customers được lưu ở **REALM level** 
- **Clients**: Applications/Services được cấu hình để authenticate

```
Realm: myrealm
├── 👥 Users (Customers)
│   ├── customer1@email.com  
│   ├── customer2@email.com
│   └── ...
└── 🔧 Clients (Apps/Services)
    ├── frontend-app
    ├── customer-service  
    ├── account-service
    ├── visa-service
    └── master-service
```

## 2. Cấu trúc Client Architecture

### **Option 1: Shared Client (❌ Không khuyến nghị)**
```yaml
# Tất cả services dùng chung 1 client
keycloak:
  client-id: customer-service        # ← Dùng chung
  client-secret: shared-secret-123   # ← Rủi ro bảo mật
```

**Vấn đề:**
- Nếu 1 service bị hack → Tất cả services bị ảnh hưởng
- Khó quản lý permissions
- Không thể revoke access cho từng service

### **Option 2: Separate Clients (✅ Khuyến nghị)**
```yaml
# Mỗi service có client riêng
account-service:
  keycloak:
    client-id: account-service
    client-secret: account-secret-456

visa-service:  
  keycloak:
    client-id: visa-service
    client-secret: visa-secret-789
```

## 3. Client Types & Use Cases

### A. **Frontend Client** (Public)
```javascript
// Purpose: User login
const config = {
  clientId: 'frontend-app',
  // No client secret (public client)
  grantType: 'authorization_code'
};
```

### B. **Customer Service** (Confidential)  
```yaml
# Purpose: User management + Accept user tokens
keycloak:
  client-id: customer-service
  client-secret: customer-service-secret
  # Can create/manage users in Keycloak
```

### C. **Account Service** (Confidential)
```yaml  
# Purpose: Account operations + Call other services
keycloak:
  client-id: account-service  
  client-secret: account-service-secret
  # Uses client_credentials to call VISA/Master
```

### D. **VISA/Master Services** (Confidential)
```yaml
# Purpose: Accept tokens from account-service
keycloak:
  client-id: visa-service
  client-secret: visa-service-secret
  # Only accepts tokens from account-service
```

## 4. Authentication Flows

### **Flow 1: User Login**
```
User → Frontend → Keycloak → Customer Service
      (frontend-app)      (user token)
```

### **Flow 2: Kafka Consumer → External Service**
```
Account Service → Keycloak → VISA Service
(client_credentials)    (service token)
```

## 5. Practical Example

### Trong thực tế, bạn sẽ có:

**1. Keycloak Realm Setup:**
```
myrealm/
├── Users: customer1, customer2, ...
├── Clients:
│   ├── frontend-app (public)
│   ├── customer-service (confidential)  
│   ├── account-service (confidential)
│   ├── visa-service (confidential)
│   └── master-service (confidential)
```

**2. Account Service Config:**
```yaml
# backend/account-service/application.yaml
keycloak:
  client-id: account-service
  client-secret: acc-svc-secret-123

visa:
  service:
    url: http://localhost:8087
    
master:
  service:  
    url: http://localhost:8086
```

**3. VISA Service Config:**
```yaml
# backend/visa-service/application.yaml  
keycloak:
  client-id: visa-service
  client-secret: visa-svc-secret-456

spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          audiences: account-service  # Only accept tokens from account-service
```

## 6. Code Implementation

### Account Service (Caller)
```java
// CardRegistrationConsumer.java
@Autowired
@Qualifier("interServiceRestTemplate")  // ← Uses client_credentials
private RestTemplate interServiceRestTemplate;

public void handleVisaRegistration() {
    // Token automatically added by interceptor
    ResponseEntity<VisaResponse> response = interServiceRestTemplate.exchange(
        visaServiceUrl + "/api/visa-service/registration",
        HttpMethod.POST,
        new HttpEntity<>(request),
        VisaResponse.class
    );
}
```

### VISA Service (Receiver)
```java
// VisaController.java
@RestController
@RequestMapping("/api/visa-service")
public class VisaController {
    
    @PostMapping("/registration")
    @PreAuthorize("hasRole('SERVICE_ACCOUNT')")  // Only service tokens
    public VisaResponse registerCard(@RequestBody CardRequest request) {
        // Process card registration
        return new VisaResponse();
    }
}
```

## 7. Token Examples

### Service Token (từ account-service)
```json
{
  "sub": "service-account-account-service",
  "azp": "account-service",           // ← Client ID
  "aud": ["visa-service"],           // ← Can call VISA service
  "realm_access": {
    "roles": ["SERVICE_ACCOUNT"]
  },
  "clientId": "account-service"
}
```

### User Token (từ frontend)
```json
{
  "sub": "user-uuid-123",
  "preferred_username": "customer@email.com",
  "azp": "frontend-app",              // ← User logged in via frontend
  "aud": ["customer-service", "account-service"],
  "realm_access": {
    "roles": ["CUSTOMER"]
  }
}
```

## 8. Tóm tắt

### ✅ **Best Practice:**
- **1 realm** cho tất cả users
- **1 client per service** cho security isolation  
- **client_credentials** cho service-to-service calls
- **authorization_code** cho user login

### 📋 **Setup Steps:**
1. Tạo realm `myrealm`
2. Tạo users (customers) trong realm
3. Tạo client riêng cho mỗi service
4. Configure client_credentials cho inter-service communication
5. Configure proper audiences và roles

### 🚀 **Result:**
- Users login qua frontend → có thể access customer-service, account-service
- Account service có thể gọi VISA/Master services via client_credentials
- Mỗi service isolated về security
- Dễ manage và scale 