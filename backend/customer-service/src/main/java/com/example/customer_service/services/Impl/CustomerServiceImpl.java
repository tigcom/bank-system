package com.example.customer_service.services.Impl;

import com.example.common_service.dto.customer.CoreCustomerDTO;
import com.example.common_service.dto.customer.CoreResponse;
import com.example.common_service.services.customer.CoreCustomerService;
import com.example.customer_service.dtos.*;
import com.example.customer_service.exceptions.AppException;
import com.example.customer_service.exceptions.ErrorCode;
import com.example.customer_service.models.*;
import com.example.customer_service.repositories.CustomerRepository;
import com.example.customer_service.repositories.KycProfileRepository;
import com.example.customer_service.responses.*;
import com.example.customer_service.services.CoreBankingClient;
import com.example.customer_service.services.CustomerService;
import com.example.customer_service.services.KycService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.CreatedResponseUtil;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;
    private final KycService kycService;
    private final KycProfileRepository kycProfileRepository;
    private final RestTemplate restTemplate;

    private final CoreBankingClient coreBankingClient;

    @Value("${idp.url}")
    private String keycloakUrl;

    @Value("${idp.realm}")
    private String realm;

    @Value("${idp.client-id}")
    private String clientId;

    @Value("${idp.client-secret}")
    private String clientSecret;

    @Transactional
    @Override
    public Response register(RegisterCustomerDTO request) {
        // Kiểm tra trùng lặp
        if (customerRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new IllegalArgumentException("Tên đăng nhập đã tồn tại");
        }
        if (customerRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email đã tồn tại");
        }
        if (customerRepository.findByIdentityNumber(request.getIdentityNumber()).isPresent()) {
            throw new IllegalArgumentException("Số CMND/CCCD đã tồn tại");
        }
        if (customerRepository.findByPhoneNumber(request.getPhoneNumber()).isPresent()) {
            throw new IllegalArgumentException("Số điện thoại đã tồn tại");
        }

        // Tạo người dùng trong Keycloak
        String userId;
        try {
            userId = createKeycloakUser(request);
        } catch (Exception e) {
            log.error("Tạo người dùng Keycloak thất bại: {}", e.getMessage(), e);
            throw e;
        }

        // Tạo khách hàng
        Customer customer = Customer.builder()
                .userId(userId)
                .username(request.getUsername())
                .fullName(request.getFullName())
                .address(request.getAddress())
                .identityNumber(request.getIdentityNumber())
                .email(request.getEmail())
                .phoneNumber(request.getPhoneNumber())
                .status(CustomerStatus.SUSPENDED)
                .dateOfBirth(request.getDateOfBirth())
                .gender(request.getGender())
                .cifCode("TEMP_" + UUID.randomUUID().toString().substring(0, 10))
                .build();

        try {
            // Lưu khách hàng
            Customer savedCustomer = customerRepository.save(customer);

            String cifCode = generateCifCode(
                    savedCustomer.getCustomerId(),
                    savedCustomer.getDateOfBirth(),
                    savedCustomer.getGender(),
                    savedCustomer.getPhoneNumber()
            );
            savedCustomer.setCifCode(cifCode);

            customerRepository.save(savedCustomer);

            // Đồng bộ với core banking
            CoreCustomerDTO coreCustomerDTO = CoreCustomerDTO.builder()
                    .cifCode(savedCustomer.getCifCode())
                    .status(savedCustomer.getStatus().toString())
                    .build();

            log.info("Đang gọi đồng bộ core banking với CIF: {}", savedCustomer.getCifCode());
            // Gọi qua RestTemplate
            CoreResponse coreResponse = coreBankingClient.syncCustomer(coreCustomerDTO);
            if (!coreResponse.isSuccess()) {
                log.error("Đồng bộ core banking thất bại: {}", coreResponse.getMessage());
                throw new RuntimeException("Đồng bộ core banking thất bại: " + coreResponse.getMessage());
            }

            // Tạo KycProfile
            KycProfile kycProfile = KycProfile.builder()
                    .status(KycStatus.PENDING)
                    .identityNumber(customer.getIdentityNumber())
                    .fullName(customer.getFullName())
                    .dateOfBirth(customer.getDateOfBirth())
                    .gender(customer.getGender().toString())
                    .build();

            kycProfile.setCustomer(savedCustomer);
            savedCustomer.setKycProfile(kycProfile);
            kycProfileRepository.save(kycProfile);

            return new Response(true, "Đăng ký thành công, vui lòng hoàn tất xác minh KYC");
        } catch (Exception e) {
            log.error("Đăng ký thất bại, đang xóa người dùng Keycloak ID: {}", userId, e);
            deleteKeycloakUser(userId);
            throw new RuntimeException("Đăng ký thất bại: " + e.getMessage(), e);
        }
    }

    private UserRepresentation buildUserRepresentation(RegisterCustomerDTO request) {
        UserRepresentation user = new UserRepresentation();

        String keycloakUsername = (request.getUsername() != null && !request.getUsername().isEmpty())
                ? request.getUsername()
                : request.getPhoneNumber();
        user.setUsername(keycloakUsername);

        user.setEmail(request.getEmail());
        user.setEnabled(true);
        user.setEmailVerified(true);

        CredentialRepresentation passwordCred = new CredentialRepresentation();
        passwordCred.setTemporary(false);
        passwordCred.setType(CredentialRepresentation.PASSWORD);
        passwordCred.setValue(request.getPassword());
        user.setCredentials(Collections.singletonList(passwordCred));

        return user;
    }

    private String createKeycloakUser(RegisterCustomerDTO request) {
        try (Keycloak keycloak = KeycloakBuilder.builder()
                .serverUrl(keycloakUrl)
                .realm(realm)
                .clientId(clientId)
                .clientSecret(clientSecret)
                .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
                .build()) {

            log.info("Đang cố gắng tạo người dùng trong Keycloak với username: {}", request.getUsername());
            log.info("URL Keycloak: {}, Realm: {}, ClientId: {}", keycloakUrl, realm, clientId);

            UserRepresentation user = buildUserRepresentation(request);

            jakarta.ws.rs.core.Response response = keycloak.realm(realm).users().create(user);
            int status = response.getStatus();
            String responseBody = response.readEntity(String.class);

            log.info("Mã trạng thái phản hồi Keycloak: {}", status);
            log.info("Nội dung phản hồi Keycloak: {}", responseBody);
            log.info("Tiêu đề phản hồi Keycloak: {}", response.getHeaders());

            if (status == 201) {
                String userId = CreatedResponseUtil.getCreatedId(response);
                try {
                    RoleRepresentation role = keycloak.realm(realm).roles().get("CUSTOMER").toRepresentation();
                    keycloak.realm(realm).users().get(userId).roles().realmLevel().add(Collections.singletonList(role));
                    log.info("Đã tạo người dùng Keycloak với ID: {} và gán vai trò CUSTOMER", userId);
                } catch (Exception e) {
                    log.error("Lỗi khi gán vai trò CUSTOMER cho người dùng Keycloak ID: {}", userId, e);
                    keycloak.realm(realm).users().get(userId).remove();
                    throw new IllegalArgumentException("Không thể gán vai trò CUSTOMER: " + e.getMessage());
                }
                return userId;
            } else if (status == 409) {
                throw new AppException(ErrorCode.USERNAME_EXISTS);
            } else {
                ObjectMapper objectMapper = new ObjectMapper();
                try {
                    JsonNode errorJson = objectMapper.readTree(responseBody);
                    String errorMessage = errorJson.has("error_description")
                            ? errorJson.get("error_description").asText()
                            : errorJson.has("error")
                            ? errorJson.get("error").asText()
                            : "Lỗi Keycloak không xác định";
                    throw new IllegalArgumentException("Lỗi Keycloak: " + errorMessage + " (Mã trạng thái: " + status + ")");
                } catch (Exception e) {
                    throw new IllegalArgumentException("Lỗi Keycloak: Không thể phân tích phản hồi - Mã trạng thái: " + status + ", Nội dung: " + responseBody);
                }
            }
        } catch (Exception e) {
            log.error("Lỗi khi tạo người dùng Keycloak: {}", e.getMessage(), e);
            throw new RuntimeException("Không thể tạo người dùng Keycloak: " + e.getMessage(), e);
        }
    }

    private void deleteKeycloakUser(String userId) {
        try (Keycloak keycloak = KeycloakBuilder.builder()
                .serverUrl(keycloakUrl)
                .realm(realm)
                .clientId(clientId)
                .clientSecret(clientSecret)
                .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
                .build()) {
            keycloak.realm(realm).users().get(userId).remove();
            log.info("Đã xóa người dùng Keycloak với ID: {}", userId);
        } catch (Exception e) {
            log.error("Xóa người dùng Keycloak thất bại: {}", e.getMessage(), e);
        }
    }

    @Override
    public Response updateCustomer(UpdateCustomerDTO request) {
        Optional<Customer> customerOpt = customerRepository.findById(request.getId());
        if (customerOpt.isEmpty()) {
            return new Response(false, "Không tìm thấy khách hàng");
        }

        Customer customer = customerOpt.get();
        if (request.getFullName() != null) customer.setFullName(request.getFullName());
        if (request.getAddress() != null) customer.setAddress(request.getAddress());
        if (request.getPhoneNumber() != null) {
            if (customerRepository.findByPhoneNumber(request.getPhoneNumber()).isPresent() &&
                    !customer.getPhoneNumber().equals(request.getPhoneNumber())) {
                return new Response(false, "Số điện thoại đã tồn tại");
            }
            customer.setPhoneNumber(request.getPhoneNumber());
        }

        updateUserInKeycloak(customer.getUserId(), request);

        customerRepository.save(customer);
        return new Response(true, "Cập nhật thành công");
    }

    private void updateUserInKeycloak(String userId, UpdateCustomerDTO request) {
        try (Keycloak keycloak = KeycloakBuilder.builder()
                .serverUrl(keycloakUrl)
                .realm(realm)
                .clientId(clientId)
                .clientSecret(clientSecret)
                .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
                .build()) {

            UserResource userResource = keycloak.realm(realm).users().get(userId);
            UserRepresentation user = userResource.toRepresentation();
            user.setFirstName(request.getFullName());
            userResource.update(user);
            log.info("Updated Keycloak user with ID: {}", userId);
        } catch (Exception e) {
            log.error("Error updating Keycloak user: {}", e.getMessage());
            throw new RuntimeException("Failed to update Keycloak user", e);
        }
    }

    @Override
    public Response forgotPassword(String email) {
        try (Keycloak keycloak = KeycloakBuilder.builder()
                .serverUrl(keycloakUrl)
                .realm(realm)
                .clientId(clientId)
                .clientSecret(clientSecret)
                .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
                .build()) {

            List<UserRepresentation> users = keycloak.realm(realm).users().searchByEmail(email, true);
            if (users.isEmpty()) {
                return new Response(false, "Không tìm thấy email");
            }

            String userId = users.get(0).getId();
            keycloak.realm(realm).users().get(userId).executeActionsEmail(
                    Collections.singletonList("UPDATE_PASSWORD"),
                    3600 // Thời gian hết hạn (1 giờ)
            );
            return new Response(true, "Đã gửi liên kết đặt lại mật khẩu qua email");
        } catch (Exception e) {
            log.error("Error sending password reset email: {}", e.getMessage());
            return new Response(false, "Lỗi khi gửi email đặt lại mật khẩu");
        }
    }

    @Override
    public CustomerListResponse getCustomerList() {
        List<CustomerResponse> customers = customerRepository.findAll()
                .stream()
                .map(this::toCustomerResponse)
                .collect(Collectors.toList());
        CustomerListResponse response = new CustomerListResponse();
        response.setCustomers(customers);
        return response;
    }

    @Override
    public CustomerResponse getCustomerDetail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userID = authentication.getName();
        Optional<Customer> customerOpt = customerRepository.findByUserId(userID);
        return customerOpt.map(this::toCustomerResponse).orElse(null);
    }

    @Override
    public CustomerResponse getCustomerDetailByCifCode(String cifCode) {
        Optional<Customer> customerOpt = customerRepository.findByCifCode(cifCode);
        return customerOpt.map(this::toCustomerResponse).orElse(null);
    }

    @Override
    public Response updateCustomerPassword(ChangePasswordDTO request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userID = authentication.getName();
        Optional<Customer> customerOpt = customerRepository.findByUserId(userID);
        if (customerOpt.isEmpty()) {
            return new Response(false, "Không tìm thấy khách hàng");
        }

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            return new Response(false, "Mật khẩu xác nhận không khớp");
        }

        Customer customer = customerOpt.get();
        updateKeycloakPassword(customer.getUserId(), request.getNewPassword());
        return new Response(true, "Cập nhật mật khẩu thành công");
    }

    private void updateKeycloakPassword(String userId, String newPassword) {
        try (Keycloak keycloak = KeycloakBuilder.builder()
                .serverUrl(keycloakUrl)
                .realm(realm)
                .clientId(clientId)
                .clientSecret(clientSecret)
                .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
                .build()) {

            CredentialRepresentation newPasswordCred = new CredentialRepresentation();
            newPasswordCred.setType(CredentialRepresentation.PASSWORD);
            newPasswordCred.setValue(newPassword);
            newPasswordCred.setTemporary(false);

            keycloak.realm(realm).users().get(userId).resetPassword(newPasswordCred);
            log.info("Updated Keycloak password for userId: {}", userId);
        } catch (Exception e) {
            log.error("Error updating Keycloak password: {}", e.getMessage());
            throw new RuntimeException("Failed to update Keycloak password", e);
        }
    }

    @Override
    @Transactional
    public Response updateCustomerStatus(UpdateStatusRequest request) {
        Optional<Customer> customerOpt = customerRepository.findById(request.getId());
        if (customerOpt.isEmpty()) {
            return new Response(false, "Không tìm thấy khách hàng");
        }

        Customer customer = customerOpt.get();
        CustomerStatus newStatus = request.getStatus();

        if (newStatus == null) {
            return new Response(false, "Trạng thái không hợp lệ");
        }

        if (customer.getStatus() == CustomerStatus.CLOSED) {
            return new Response(false, "Không thể thay đổi trạng thái từ CLOSED");
        }

        if (!customer.getStatus().equals(newStatus)) {
            customer.setStatus(newStatus);
            customerRepository.save(customer);

            // Đồng bộ sang corebanking-service
            CoreCustomerDTO coreCustomerDTO = CoreCustomerDTO.builder()
                    .cifCode(customer.getCifCode())
                    .status(customer.getStatus().toString())
                    .build();

            log.info("Đang đồng bộ trạng thái khách hàng với CIF: {}", customer.getCifCode());
            CoreResponse coreResponse = coreBankingClient.syncCustomer(coreCustomerDTO);
            if (!coreResponse.isSuccess()) {
                log.error("Đồng bộ trạng thái khách hàng thất bại với CIF: {}. Lỗi: {}",
                        customer.getCifCode(), coreResponse.getMessage());
                throw new RuntimeException("Đồng bộ trạng thái khách hàng thất bại: " + coreResponse.getMessage());
            }
            log.info("Đồng bộ trạng thái khách hàng thành công với CIF: {}", customer.getCifCode());
        }

        return new Response(true, "Cập nhật trạng thái thành công");
    }

    @Transactional
    public KycResponse verifyKyc(KycRequest request) {
        try {
            if (!isValidKycRequest(request)) {
                return new KycResponse(false, "Dữ liệu đầu vào không hợp lệ", null);
            }

            Optional<Customer> customerOpt = customerRepository.findById(request.getCustomerId());
            if (customerOpt.isEmpty()) {
                return new KycResponse(false, "Không tìm thấy khách hàng", null);
            }
            Customer customer = customerOpt.get();

            Optional<KycProfile> kycProfileOpt = kycProfileRepository.findByCustomer(customer);
            if (kycProfileOpt.isPresent() && KycStatus.VERIFIED.equals(kycProfileOpt.get().getStatus())) {
                return new KycResponse(false, "Tài khoản đã được xác minh KYC", null);
            }

            String errorMessage = validateCustomerData(customer, request);
            if (errorMessage != null) {
                return new KycResponse(false, errorMessage, null);
            }

            KycResponse kycResponse = kycService.verifyIdentity(
                    request.getIdentityNumber(),
                    request.getFullName()
            );

            if (kycResponse.isVerified()) {
                kycService.saveKycInfo(
                        customer.getCustomerId(),
                        kycResponse,
                        request.getIdentityNumber(),
                        request.getFullName(),
                        request.getDateOfBirth(),
                        request.getGender()
                );
                customer.setStatus(CustomerStatus.ACTIVE);

                // Đồng bộ sang corebanking-service
                CoreCustomerDTO coreCustomerDTO = CoreCustomerDTO.builder()
                        .cifCode(customer.getCifCode())
                        .status(customer.getStatus().toString())
                        .build();

                log.info("Đang đồng bộ trạng thái KYC với CIF: {}", customer.getCifCode());
                CoreResponse coreResponse = coreBankingClient.syncCustomer(coreCustomerDTO);
                if (!coreResponse.isSuccess()) {
                    log.error("Đồng bộ trạng thái KYC thất bại với CIF: {}. Lỗi: {}",
                            customer.getCifCode(), coreResponse.getMessage());
                    throw new RuntimeException("Đồng bộ trạng thái KYC thất bại: " + coreResponse.getMessage());
                }

                customerRepository.save(customer);
                log.info("Đồng bộ trạng thái KYC thành công với CIF: {}", customer.getCifCode());
            }

            return kycResponse;
        } catch (Exception e) {
            log.error("Xác minh KYC thất bại cho customerId: {}. Lỗi: {}",
                    request.getCustomerId(), e.getMessage(), e);
            return new KycResponse(false, "Xác minh KYC thất bại: " + e.getMessage(), null);
        }
    }

    private boolean isValidKycRequest(KycRequest request) {
        return request.getCustomerId() != null &&
                request.getIdentityNumber() != null &&
                request.getFullName() != null &&
                request.getDateOfBirth() != null &&
                request.getGender() != null;
    }

    private String validateCustomerData(Customer customer, KycRequest request) {
        if (!Objects.equals(customer.getIdentityNumber(), request.getIdentityNumber())) {
            return "Số CMND/CCCD không khớp với thông tin đăng ký";
        }
        if (!Objects.equals(customer.getFullName(), request.getFullName())) {
            return "Họ tên không khớp với thông tin đăng ký";
        }
        if (!Objects.equals(customer.getDateOfBirth(), request.getDateOfBirth())) {
            return "Ngày sinh không khớp với thông tin đăng ký";
        }
        if (customer.getGender() == null || request.getGender() == null) {
            log.error("Gender is null: customer.gender={}, request.gender={}", customer.getGender(), request.getGender());
            return "Giới tính không hợp lệ";
        }
        if (!customer.getGender().name().toLowerCase().equals(request.getGender().toLowerCase())) {
            log.error("Gender mismatch: customer.gender={} (type={}), request.gender={} (type={})",
                    customer.getGender(), customer.getGender().getClass().getSimpleName(),
                    request.getGender(), request.getGender().getClass().getSimpleName());
            return "Giới tính không khớp với thông tin đăng ký";
        }
        return null;
    }

    public String generateCifCode(Long id, LocalDate dateOfBirth, Gender gender, String phoneNumber) {

        // Mã BIN quốc gia và ngân hàng
        String binCode = "970452";

        // Lấy YYMD từ ngày tháng sinh
        int dobPart = dateOfBirth.getYear() % 10;

        // Gán mã giới tính
        String genderCode = gender == Gender.male ? "1" : "0";

        // Lấy 3 số cuối điện thoại
        String phonePart = phoneNumber.substring(phoneNumber.length() - 3);

        // Format ID tăng dần thành 2 chữ số
        String idPart = String.format("%02d", id % 100);

        // Kết hợp tất cả thành mã CIF
        return binCode + dobPart + genderCode + phonePart + idPart;
    }

    private CustomerResponse toCustomerResponse(Customer customer) {
        CustomerResponse response = new CustomerResponse();
        response.setCifCode(customer.getCifCode());
        response.setFullName(customer.getFullName());
        response.setAddress(customer.getAddress());
        response.setEmail(customer.getEmail());
        response.setPhoneNumber(customer.getPhoneNumber());
        response.setStatus(customer.getStatus());

        // Lấy KycStatus từ KycProfile
        Optional<KycProfile> kycProfileOpt = kycProfileRepository.findByCustomer(customer);
        response.setKycStatus(kycProfileOpt.map(KycProfile::getStatus).orElse(null));

        return response;
    }
}