# Fix: "Client not enabled to retrieve service account"

## Lỗi
```json
{
    "error": "unauthorized_client",
    "error_description": "Client not enabled to retrieve service account"
}
```

## Nguyên nhân
Client `account-service` trong Keycloak chưa được cấu hình để sử dụng Service Accounts (client_credentials grant type).

## Cách khắc phục

### Bước 1: Truy cập Keycloak Admin Console
- URL: http://localhost:8180/admin
- Login với admin credentials

### Bước 2: Vào Client Settings
1. Chọn realm `myrealm`
2. Vào `Clients` → Tìm `account-service`
3. Click vào client `account-service`

### Bước 3: Cấu hình Client Settings Tab
```
Settings Tab:
├── Client ID: account-service
├── Client Protocol: openid-connect
├── Access Type: confidential                    ← QUAN TRỌNG
├── Standard Flow Enabled: OFF                   ← Tắt authorization code
├── Implicit Flow Enabled: OFF                   ← Tắt implicit
├── Direct Access Grants Enabled: OFF            ← Tắt password grant
├── Service Accounts Enabled: ON                 ← BẬT SERVICE ACCOUNTS
├── Authorization Enabled: OFF                   ← Tắt authorization
└── Valid Redirect URIs: (để trống hoặc *)
```

### Bước 4: Kiểm tra Credentials Tab
```
Credentials Tab:
├── Client Authenticator: Client Id and Secret
└── Secret: [COPY SECRET NÀY]
```

### Bước 5: Kiểm tra Service Account Roles Tab
- Tab `Service Account Roles` sẽ xuất hiện sau khi enable Service Accounts
- Client sẽ có service account user: `service-account-account-service`

### Bước 6: Cập nhật application.yaml
```yaml
keycloak:
  auth-server-url: http://localhost:8180
  realm: myrealm
  client-id: account-service
  client-secret: [PASTE_SECRET_FROM_STEP_4]
```

### Bước 7: Test Client Credentials
```bash
curl -X POST \
  'http://localhost:8180/realms/myrealm/protocol/openid-connect/token' \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -d 'grant_type=client_credentials&client_id=account-service&client_secret=YOUR_SECRET'
```

## Kết quả mong đợi
```json
{
  "access_token": "eyJhbGciOiJSUzI1NiIs...",
  "expires_in": 300,
  "refresh_expires_in": 0,
  "token_type": "Bearer",
  "not-before-policy": 0,
  "scope": "profile email"
}
```

## Troubleshooting

### Lỗi 1: Client secret không đúng
```json
{"error": "invalid_client"}
```
**Fix**: Copy lại secret từ Credentials tab

### Lỗi 2: Client chưa có Service Accounts
```json
{"error": "unauthorized_client", "error_description": "Client not enabled to retrieve service account"}
```
**Fix**: Enable Service Accounts Enabled = ON

### Lỗi 3: Grant type không được support
```json
{"error": "unsupported_grant_type"}
```
**Fix**: Kiểm tra `Service Accounts Enabled: ON`

## Verification Steps

### 1. Kiểm tra Client Configuration
- Access Type: `confidential`
- Service Accounts Enabled: `ON`
- Standard Flow: `OFF` (không cần cho service-to-service)

### 2. Kiểm tra Service Account User
- Vào `Users` → Tìm `service-account-account-service`
- User này được tạo tự động khi enable Service Accounts

### 3. Test với curl
```bash
# Replace YOUR_SECRET với secret thực tế
curl -X POST \
  'http://localhost:8180/realms/myrealm/protocol/openid-connect/token' \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -d 'grant_type=client_credentials&client_id=account-service&client_secret=YOUR_SECRET' \
  | jq
```

### 4. Kiểm tra logs application
```
2024-01-01 10:00:00 INFO  - Successfully obtained access token for inter-service communication
``` 