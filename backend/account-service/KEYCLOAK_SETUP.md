# Hướng dẫn cấu hình Keycloak cho Account Service

## Tạo Client cho Account Service với Client Credentials

### 1. Truy cập Keycloak Admin Console
- URL: http://localhost:8180/admin
- Đăng nhập với admin credentials

### 2. Tạo Client mới
1. Vào realm `myrealm`
2. Chọn `Clients` > `Create client`
3. Điền thông tin:
   - **Client ID**: `account-service`
   - **Name**: `Account Service`
   - **Description**: `Client for account service inter-service communication`
   - **Client type**: `OpenID Connect`
   - **Client authentication**: `On` (bật authentication)
   - **Authorization**: `Off`
   - **Service accounts roles**: `On` (bật service account)

### 3. Cấu hình Client Settings
1. Vào tab `Settings`:
   - **Access Type**: `confidential`
   - **Service Accounts Enabled**: `ON`
   - **Authorization Enabled**: `OFF`
   - **Valid Redirect URIs**: `*` (hoặc để trống)
   - **Web Origins**: `*`

2. Vào tab `Credentials`:
   - **Client Authenticator**: `Client Id and Secret`
   - Copy `Secret` và cập nhật vào `application.yaml`:
     ```yaml
     keycloak:
       client-secret: YOUR_COPIED_SECRET_HERE
     ```

### 4. Cấu hình Service Account Roles
1. Vào tab `Service account roles`
2. Assign các roles cần thiết cho account service:
   - `realm-management` roles để có thể gọi các API khác
   - Hoặc tạo custom roles tùy theo yêu cầu

### 5. Test Client Credentials Flow
```bash
curl -X POST \
  http://localhost:8180/realms/myrealm/protocol/openid-connect/token \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -d 'grant_type=client_credentials&client_id=account-service&client_secret=YOUR_SECRET'
```

### 6. Cấu hình các Service khác (VISA, Master)
Tương tự, tạo clients cho:
- `visa-service`
- `master-service`

Và cấu hình để chúng có thể accept token từ `account-service`.

## Lưu ý bảo mật
- **Client Secret** phải được bảo mật tuyệt đối
- Nên sử dụng environment variables trong production:
  ```yaml
  keycloak:
    client-secret: ${KEYCLOAK_CLIENT_SECRET}
  ```
- Token sẽ được cache và tự động refresh
- Token có thời gian expire, hệ thống sẽ tự động lấy token mới

## Troubleshooting
1. **401 Unauthorized**: Kiểm tra client-secret và client-id
2. **Token expired**: Hệ thống sẽ tự động refresh, kiểm tra logs
3. **Service không nhận token**: Đảm bảo service đích có cấu hình OAuth2 Resource Server 