# BÁO CÁO DỰ ÁN HỆ THỐNG NGÂN HÀNG
## Java Banking Phase 2 - Microservices Architecture

---

## TỔNG QUAN DỰ ÁN

Dự án Java Banking Phase 2 là một hệ thống ngân hàng hiện đại được xây dựng theo kiến trúc microservices, bao gồm nhiều dịch vụ chuyên biệt phục vụ các chức năng khác nhau của ngân hàng. Dự án được phát triển với công nghệ Java Spring Boot cho backend và Angular cho frontend.

---

## PHẦN 1: BACKEND

### 1.1. CÔNG CỤ VÀ CÔNG NGHỆ

#### **Framework và Platform:**
- **Spring Boot 3.3.11**: Framework chính cho việc phát triển ứng dụng Java
- **Java 17**: Ngôn ngữ lập trình chính với LTS version
- **Maven**: Công cụ quản lý dependencies và build project

#### **Microservices & Communication:**
- **Spring Cloud Gateway**: API Gateway để routing và load balancing
- **Apache Dubbo 3.3.2**: Framework RPC cho giao tiếp giữa các microservices
- **Apache Zookeeper 3.7.1**: Service registry và discovery
- **Apache Kafka 3.7.0**: Message broker cho event-driven architecture
- **Spring Cloud Stream 4.2.1**: Stream processing với Kafka

#### **Database & Persistence:**
- **MySQL**: Database chính cho lưu trữ dữ liệu
- **Spring Data JPA**: ORM framework
- **Hibernate**: JPA implementation
- **HikariCP**: Connection pooling

#### **Security & Authentication:**
- **Keycloak 26.0.0**: Identity and Access Management (IAM)
- **Spring Security**: Security framework
- **OAuth2 Resource Server**: JWT token authentication
- **BCrypt**: Password hashing

#### **Monitoring & Observability:**
- **Zipkin**: Distributed tracing
- **Spring Boot Actuator**: Health checks và metrics
- **OpenTelemetry**: Telemetry và monitoring

#### **Infrastructure:**
- **Docker & Docker Compose**: Containerization
- **Hashicorp Vault**: Secret management
- **Redis**: Caching và session storage

#### **Development Tools:**
- **Lombok**: Code generation và boilerplate reduction
- **MapStruct 1.6.3**: Object mapping
- **Spring Boot DevTools**: Development utilities

### 1.2. KIẾN TRÚC VÀ MÔ HÌNH ÁP DỤNG

#### **Kiến trúc Microservices:**
Dự án áp dụng kiến trúc microservices với các service chính:

1. **API Gateway** (Port 8888): Entry point cho tất cả requests
2. **Account Service**: Quản lý tài khoản khách hàng
3. **Customer Service**: Quản lý thông tin khách hàng
4. **Loan Service** (Port 9827): Quản lý khoản vay - **Service chính bạn đảm nhiệm**
5. **Transaction Service**: Xử lý giao dịch
6. **Core Banking Service**: Dịch vụ core banking
7. **CIC Service**: Credit Information Center
8. **Opening Banking Service**: Open Banking APIs
9. **Notification Service**: Gửi thông báo
10. **Mock Provider Server**: Mock external services

#### **Loan Service Architecture (Service chính):**

**Cấu trúc thư mục và tổ chức code:**

Loan Service được tổ chức theo kiến trúc layered architecture với cấu trúc thư mục rõ ràng và phân tách trách nhiệm. Tổng cộng có hơn 2,000 dòng code được phân bổ hợp lý:

**Config Layer (Cấu hình hệ thống):**
- SecurityConfig.java: Cấu hình bảo mật JWT và Keycloak integration
- RestTemplateConfig.java: Cấu hình HTTP client cho external API calls
- InterServiceRestTemplateConfig.java: Cấu hình inter-service communication

**Controller Layer (REST API Controllers):**
- LoanController.java (414 dòng): Xử lý tất cả API endpoints liên quan đến loan management
- RepaymentController.java (255 dòng): Xử lý API endpoints cho repayment operations
- InfoIncomeController.java (90 dòng): Xử lý API endpoints cho income verification

**Handler Layer (Business Logic Orchestration):**
- LoanHandler.java (489 dòng): Layer trung gian điều phối business logic, tích hợp với các service khác

**Service Layer (Business Logic Implementation):**
- LoanService.java: Interface định nghĩa business logic cho loan management
- RepaymentService.java: Interface định nghĩa business logic cho repayment processing
- InfoIncomeService.java: Interface định nghĩa business logic cho income verification
- CoreBankingClient.java: Interface tích hợp với core banking system
- CICClient.java: Interface tích hợp với Credit Information Center
- OpeningBankingClient.java: Interface tích hợp với Open Banking APIs

**Service Implementation Layer:**
- LoanServiceImpl.java (221 dòng): Implementation của loan business logic
- RepaymentServiceImpl.java (358 dòng): Implementation của repayment business logic
- InfoIncomeServiceImpl.java (78 dòng): Implementation của income verification logic
- CoreBankingClientImpl.java (54 dòng): Implementation tích hợp core banking
- CICClientImpl.java (47 dòng): Implementation tích hợp CIC
- OpeningBankingClientImpl.java (51 dòng): Implementation tích hợp Open Banking

**Repository Layer (Data Access):**
- LoanRepository.java: Data access cho Loan entity với custom queries
- RepaymentRepository.java: Data access cho Repayment entity
- InfoIncomeRepository.java: Data access cho InfoIncome entity
- LoanRejectionReasonRepository.java: Data access cho rejection reasons

**Entity Layer (Domain Models):**
- Loan.java (64 dòng): Entity chính đại diện cho khoản vay
- Repayment.java (43 dòng): Entity đại diện cho lịch trả nợ
- InfoIncome.java (36 dòng): Entity đại diện cho thông tin thu nhập
- LoanRejectionReason.java (33 dòng): Entity đại diện cho lý do từ chối

**DTO Layer (Data Transfer Objects):**
- Request DTOs: LoanRequestDTO, InfoIncomeRequestDto, LoanRejectionReasonRequestDTO
- Response DTOs: TransactionDto, CicResponse

**Mapper Layer (Object Mapping):**
- LoanMapper.java: Chuyển đổi giữa Loan entity và DTO
- RepaymentMapper.java: Chuyển đổi giữa Repayment entity và DTO
- InfoIncomeMapper.java: Chuyển đổi giữa InfoIncome entity và DTO

**Models Layer (Domain Enums):**
- LoanStatus.java: Enum định nghĩa các trạng thái khoản vay (PENDING, APPROVED, REJECTED, CLOSED, CANCELLED)
- RepaymentStatus.java: Enum định nghĩa các trạng thái trả nợ (UNPAID, PAID, OVERDUE)

**Kiến trúc Layered Architecture:**

Loan Service áp dụng kiến trúc 5-layer architecture với luồng dữ liệu từ trên xuống dưới:

**Presentation Layer (Controller):** Đây là layer đầu tiên tiếp nhận HTTP requests từ client. Các controller xử lý request validation, authentication, và chuyển đổi dữ liệu. Mỗi controller có trách nhiệm cụ thể: LoanController xử lý loan operations, RepaymentController xử lý repayment operations, và InfoIncomeController xử lý income verification.

**Business Logic Layer (Handler):** Layer này đóng vai trò điều phối và orchestration. LoanHandler là component chính với 489 dòng code, tích hợp với nhiều service khác nhau để thực hiện business logic phức tạp như loan approval workflow, CIC checking, và core banking integration.

**Service Layer:** Chứa core business logic được chia thành các service riêng biệt. LoanService xử lý loan lifecycle management, RepaymentService xử lý repayment calculation và processing, InfoIncomeService xử lý income verification logic. Các service này implement các business rules và validation logic.

**Data Access Layer (Repository):** Cung cấp abstraction cho database operations. Các repository interface extend JpaRepository và cung cấp custom queries như findAllByStatusIs() và findAllByStatusIsAndCustomerId() để tối ưu performance.

**Database Layer:** MySQL database với các bảng chính: loan, repayment, info_income, và loan_rejection_reason. Database được thiết kế với proper indexing và relationships.

**Design Patterns áp dụng:**

**1. Layered Architecture Pattern:** Kiến trúc được chia thành 5 layer rõ ràng với trách nhiệm phân tách. Controller layer xử lý HTTP requests và responses, Handler layer điều phối business logic, Service layer chứa core business rules, Repository layer quản lý data access, và Entity layer định nghĩa domain models.

**2. DTO Pattern (Data Transfer Object):** Sử dụng để tách biệt API contracts khỏi domain models. LoanRequestDTO định nghĩa structure cho loan creation/update requests, InfoIncomeRequestDto cho income verification, và LoanRejectionReasonRequestDTO cho rejection requests. Pattern này đảm bảo loose coupling giữa API và domain logic.

**3. Repository Pattern:** Cung cấp abstraction cho data access operations. LoanRepository, RepaymentRepository, và InfoIncomeRepository implement CRUD operations và custom queries. Pattern này giúp dễ dàng thay đổi database implementation và testing.

**4. Service Layer Pattern:** Encapsulate business logic trong các service riêng biệt. LoanService quản lý loan lifecycle, RepaymentService xử lý repayment logic, và InfoIncomeService xử lý income verification. Mỗi service có trách nhiệm cụ thể và có thể được test độc lập.

**5. Mapper Pattern:** Sử dụng MapStruct để tự động chuyển đổi giữa entities và DTOs. LoanMapper, RepaymentMapper, và InfoIncomeMapper giúp giảm boilerplate code và đảm bảo consistency trong data transformation.

**6. Strategy Pattern:** Áp dụng cho different loan processing strategies dựa trên loan type, different repayment calculation strategies, và different approval workflows. Pattern này cho phép dễ dàng thêm new strategies mà không ảnh hưởng đến existing code.

**7. Observer Pattern:** Implement thông qua event-driven architecture với Kafka. Loan status change events và repayment due notifications được publish để các service khác có thể subscribe và react accordingly.

**Communication Patterns:**

**Synchronous Communication:** Sử dụng REST APIs cho direct service-to-service communication, Dubbo RPC cho high-performance inter-service calls, và HTTP Client cho external service integration. REST APIs được sử dụng cho loan operations, Dubbo RPC cho internal service calls, và HTTP Client cho integration với external systems.

**Asynchronous Communication:** Implement event-driven architecture với Kafka messaging, Spring Cloud Stream cho stream processing, và Event Sourcing cho loan status change events. Kafka được sử dụng để publish loan approval events, repayment events, và notification events.

**Integration Points:** Loan Service tích hợp với Core Banking Service cho account validation và transaction processing, CIC Service cho credit history verification, Opening Banking Service cho transaction history retrieval, và Notification Service cho email/SMS notifications.

**Security Implementation:**

**JWT-based Authentication:** Tích hợp với Keycloak cho identity management, JWT token validation và parsing, và role-based access control (RBAC). Mỗi request được validate JWT token và extract user information.

**Method-level Security:** Sử dụng @PreAuthorize annotations cho admin operations ("hasRole('ADMIN')"), employee operations ("hasRole('EMPLOYEE')"), và customer-specific data access control. Security được implement ở method level để đảm bảo fine-grained access control.

**API Security:** CORS configuration cho cross-origin requests, CSRF protection disabled cho API endpoints, và input validation với Bean Validation annotations. Tất cả input được validate trước khi xử lý.

**Data Security:** Sensitive data encryption, audit logging cho all operations, và secure communication với external services. Tất cả database operations được log để audit trail.

**Loan Lifecycle Management:**

Loan Service quản lý lifecycle của khoản vay thông qua 5 trạng thái chính:

**PENDING:** Trạng thái ban đầu khi khoản vay được tạo. Loan được submit và chờ approval từ admin/employee. Trong trạng thái này, loan có thể được updated hoặc deleted.

**APPROVED:** Trạng thái sau khi loan được approve. Core banking system sẽ disbursed amount vào customer account, repayment schedule được generate, và notification được gửi cho customer.

**REJECTED:** Trạng thái khi loan bị từ chối. Admin/employee phải provide rejection reason. Customer sẽ nhận notification về rejection.

**CLOSED:** Trạng thái khi loan được hoàn thành (paid off). Tất cả repayments đã được completed và loan được close.

**CANCELLED:** Trạng thái khi loan bị cancel trước khi approve. Customer có thể cancel loan trong PENDING status.

**Business Logic Flow:**

**Loan Creation Flow:** Customer submit loan request → System validate customer information → Check CIC (Credit Information Center) → Validate account information → Create loan record → Set status to PENDING → Send confirmation notification.

**Loan Approval Flow:** Admin/Employee review loan application → Apply business rules → Check customer eligibility → Process disbursement through Core Banking → Generate repayment schedule → Update loan status to APPROVED → Send approval notification.

**Repayment Processing Flow:** Customer initiate payment → System validate payment amount → Send OTP for verification → Process transaction through Core Banking → Update repayment status → Update loan outstanding amount → Send payment confirmation.

**Performance Optimization:**

**Database Optimization:** Implement indexed queries cho customer_id và status fields, lazy loading cho related entities để giảm memory usage, và connection pooling với HikariCP để optimize database connections.

**Caching Strategy:** Redis caching cho frequently accessed data như customer information và loan statistics, application-level caching cho business rules và configuration, và query result caching để giảm database load.

**Async Processing:** Kafka messaging cho non-critical operations như notifications và audit logging, background job processing cho batch operations, và event-driven architecture để handle high-volume events.

---

## PHẦN 2: FRONTEND

### 2.1. CÔNG CỤ VÀ CÔNG NGHỆ

#### **Framework và Platform:**
- **Angular 18.2.13**: Framework chính cho frontend
- **TypeScript 5.5.2**: Ngôn ngữ lập trình
- **Node.js**: Runtime environment
- **Angular CLI**: Command line interface

#### **UI/UX Framework:**
- **Angular Material 18.2.14**: Material Design components
- **PrimeNG 18.0.2**: Rich UI component library
- **PrimeFlex 4.0.0**: CSS utility framework
- **PrimeIcons 7.0.0**: Icon library
- **Angular CDK 18.2.14**: Component development kit
- **Angular Flex Layout 15.0.0**: Flexbox layout utilities

#### **State Management:**
- **NgRx 18.1.1**: State management library
  - @ngrx/store: Core state management
  - @ngrx/effects: Side effects handling
  - @ngrx/entity: Entity state management
  - @ngrx/store-devtools: Development tools

#### **HTTP & Communication:**
- **Angular HttpClient**: HTTP client
- **RxJS 7.8.0**: Reactive programming
- **@auth0/angular-jwt 5.2.0**: JWT handling

#### **UI Enhancement:**
- **Chart.js 4.4.9**: Data visualization
- **SweetAlert2 11.6.13**: Beautiful alerts
- **ngx-toastr 19.0.0**: Toast notifications
- **Angular Animations**: Smooth transitions

#### **Development Tools:**
- **Jasmine & Karma**: Testing framework
- **Angular DevTools**: Development utilities
- **SCSS**: CSS preprocessor

### 2.2. KIẾN TRÚC VÀ MÔ HÌNH ÁP DỤNG

#### **Angular Architecture:**
```
src/app/
├── loan/                    # Loan module - Bạn đảm nhiệm
│   ├── apply-new-loan/
│   ├── dashboard-loan/
│   ├── loan-history/
│   ├── pay-repayment/
│   └── ...
├── loan-employee/           # Employee loan management
├── admin/                   # Admin dashboard
├── customer/                # Customer management
├── auth/                    # Authentication
├── core/                    # Core services & guards
├── shared/                  # Shared components
└── services/                # API services
```

#### **Loan Module Architecture (Module chính bạn đảm nhiệm):**

**Components Structure:**
- **apply-new-loan**: Đăng ký khoản vay mới
- **dashboard-loan**: Dashboard tổng quan khoản vay
- **loan-history**: Lịch sử khoản vay
- **pay-repayment**: Thanh toán khoản vay
- **current-repayment-schedule**: Lịch trả nợ hiện tại
- **detail-loan**: Chi tiết khoản vay
- **detail-loan-reject**: Chi tiết khoản vay bị từ chối
- **warning-apply-loan**: Cảnh báo khi đăng ký vay

**Employee Loan Management:**
- **pending-loans-list**: Danh sách khoản vay chờ duyệt
- **loan-detail-view**: Xem chi tiết khoản vay (employee view)

#### **Design Patterns áp dụng:**
- **Component-Based Architecture**: Modular, reusable components
- **Service Layer Pattern**: Business logic trong services
- **Observer Pattern**: RxJS observables
- **Factory Pattern**: Component creation
- **Strategy Pattern**: Different loan processing strategies
- **Repository Pattern**: Data access abstraction

#### **State Management với NgRx:**
- **Store**: Centralized state
- **Actions**: State changes
- **Reducers**: State transformations
- **Effects**: Side effects handling
- **Selectors**: State queries

#### **Security Implementation:**
- JWT token storage trong localStorage
- HTTP interceptors cho authentication headers
- Route guards cho protected routes
- Role-based component rendering

#### **Responsive Design:**
- Mobile-first approach
- Flexbox layouts
- Material Design responsive grid
- Progressive Web App (PWA) ready

---

## PHẦN 3: LOAN SERVICE - CHI TIẾT KỸ THUẬT

### 3.1. Backend Loan Service

#### **API Endpoints:**
```java
// Loan Management
POST   /api/loans                    // Tạo khoản vay mới
PUT    /api/loans                    // Cập nhật khoản vay
GET    /api/loans/{id}              // Lấy khoản vay theo ID
DELETE /api/loans/{id}              // Xóa khoản vay
GET    /api/loans/customer          // Lấy khoản vay theo customer
GET    /api/loans/getAllloans       // Lấy tất cả khoản vay

// Loan Processing
POST   /api/loans/{id}/approve      // Phê duyệt khoản vay
POST   /api/loans/{id}/reject       // Từ chối khoản vay
POST   /api/loans/{id}/close        // Đóng khoản vay

// Statistics
GET    /api/loans/total-borrowed    // Tổng tiền đã vay
GET    /api/loans/total-outstanding // Tổng tiền chưa trả
GET    /api/loans/admin/total-disbursed  // Thống kê admin
GET    /api/loans/admin/total-collected  // Thống kê admin
GET    /api/loans/admin/total-profit     // Thống kê admin
```

#### **Database Schema:**
- **Loan**: Thông tin khoản vay
- **Repayment**: Lịch trả nợ
- **InfoIncome**: Thông tin thu nhập
- **LoanRejectionReason**: Lý do từ chối

#### **Integration Points:**
- **Core Banking Service**: Kiểm tra tài khoản và thực hiện giao dịch
- **CIC Service**: Kiểm tra lịch sử tín dụng
- **Opening Banking Service**: Lấy thông tin giao dịch
- **Notification Service**: Gửi thông báo qua Kafka

### 3.2. Frontend Loan Module

#### **Key Features:**
1. **Loan Application**: Form đăng ký khoản vay với validation
2. **Loan Dashboard**: Tổng quan khoản vay của khách hàng
3. **Repayment Management**: Quản lý thanh toán khoản vay
4. **Loan History**: Lịch sử khoản vay
5. **Admin Dashboard**: Thống kê và quản lý khoản vay (admin)
6. **Employee Interface**: Duyệt và quản lý khoản vay (employee)

#### **User Roles:**
- **Customer**: Đăng ký vay, xem khoản vay, thanh toán
- **Employee**: Duyệt khoản vay, quản lý khoản vay
- **Admin**: Thống kê, quản lý hệ thống

---

## PHẦN 4: DEPLOYMENT VÀ INFRASTRUCTURE

### 4.1. Containerization
- **Docker Compose**: Orchestration cho development
- **Multi-stage builds**: Optimized production images
- **Environment-specific configurations**

### 4.2. Service Discovery
- **Zookeeper**: Service registry
- **Dubbo**: RPC service discovery
- **Load balancing**: Client-side load balancing

### 4.3. Monitoring & Logging
- **Zipkin**: Distributed tracing
- **Spring Boot Actuator**: Health checks
- **Structured logging**: Logback configuration

---

## KẾT LUẬN

Dự án Java Banking Phase 2 là một hệ thống ngân hàng hiện đại với kiến trúc microservices mạnh mẽ. Loan Service mà bạn đảm nhiệm là một thành phần quan trọng, được xây dựng với các công nghệ tiên tiến và tuân thủ các best practices trong phát triển phần mềm.

**Điểm mạnh của dự án:**
- Kiến trúc microservices scalable
- Security mạnh mẽ với Keycloak
- Monitoring và observability tốt
- UI/UX hiện đại với Angular Material và PrimeNG
- Code quality cao với TypeScript và Java 17

**Công nghệ chính được sử dụng:**
- **Backend**: Spring Boot, Dubbo, Kafka, MySQL, Keycloak
- **Frontend**: Angular, NgRx, PrimeNG, TypeScript
- **Infrastructure**: Docker, Zookeeper, Redis, Vault

Dự án thể hiện sự hiểu biết sâu sắc về kiến trúc phần mềm hiện đại và khả năng áp dụng các công nghệ tiên tiến trong thực tế. 