package com.example.customer_service.services.Impl;

import com.example.common_service.constant.CustomerStatus;
import com.example.customer_service.dtos.*;
import com.example.customer_service.exceptions.AppException;
import com.example.customer_service.exceptions.ErrorCode;
import com.example.customer_service.models.*;
import com.example.customer_service.repositories.CustomerRepository;
import com.example.customer_service.repositories.KycProfileRepository;
import com.example.customer_service.responses.*;
import com.example.customer_service.services.CustomerService;
import com.example.customer_service.services.KycService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

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

    @Value("${idp.url}")
    private String keycloakUrl;

    @Value("${idp.realm}")
    private String realm;

    @Value("${idp.client-id}")
    private String clientId;

    @Value("${idp.client-secret}")
    private String clientSecret;

    @Override
    @Transactional
    public Response register(RegisterCustomerDTO request) {

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
        String userId = createKeycloakUser(request);

        Customer customer = Customer.builder()
                .userId(userId)
                .username(request.getUsername())
                .fullName(request.getFullName())
                .address(request.getAddress())
                .identityNumber(request.getIdentityNumber())
                .email(request.getEmail())
                .phoneNumber(request.getPhoneNumber())
                .status(com.example.common_service.constant.CustomerStatus.SUSPENDED)
                .dateOfBirth(request.getDateOfBirth())
                .gender(request.getGender())
                .cifCode("TEMP_" + UUID.randomUUID().toString().substring(0, 10))
                .build();

        Customer savedCustomer = customerRepository.save(customer);
        savedCustomer.setCifCode(generateCifCode(savedCustomer.getCustomerId()));
        customerRepository.save(savedCustomer);

        // Tạo KycProfile
        KycProfile kycProfile = KycProfile.builder()
                .status(KycStatus.PENDING)
                .identityNumber(customer.getIdentityNumber())
                .fullName(customer.getFullName())
                .dateOfBirth(customer.getDateOfBirth())
                .gender(customer.getGender().toString())
                .build();

        kycProfile.setCustomer(customer);
        customer.setKycProfile(kycProfile);

        kycProfileRepository.save(kycProfile);

        return new Response(true, "Đăng ký thành công, vui lòng hoàn tất xác minh KYC");
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
            log.info("Keycloak URL: {}, Realm: {}, ClientId: {}", keycloakUrl, realm, clientId);

            UserRepresentation user = buildUserRepresentation(request);

            jakarta.ws.rs.core.Response response = keycloak.realm(realm).users().create(user);
            int status = response.getStatus();
            String errorBody = response.readEntity(String.class);
            log.info("Trạng thái phản hồi từ Keycloak khi tạo người dùng: {}", status);
            log.info("Nội dung phản hồi từ Keycloak khi tạo người dùng: {}", errorBody);

            if (response.getStatus() == 201) {
                String userId = CreatedResponseUtil.getCreatedId(response);
                RoleRepresentation role = keycloak.realm(realm).roles().get("CUSTOMER").toRepresentation();
                keycloak.realm(realm).users().get(userId).roles().realmLevel().add(Collections.singletonList(role));
                log.info("Đã tạo người dùng Keycloak với ID: {}", userId);
                return userId;
            } else if (response.getStatus() == 409) {
                throw new AppException(ErrorCode.USERNAME_EXISTS);
            } else {
                throw new AppException(ErrorCode.KEYCLOAK_ERROR);
            }
        } catch (Exception e) {
            log.error("Lỗi khi tạo người dùng Keycloak: {}", e.getMessage(), e);
            throw new RuntimeException("Không thể tạo người dùng Keycloak", e);
        }
    }

    private String getKeycloakToken(String usernameOrPhone, String password) throws Exception {
        String tokenEndpoint = keycloakUrl + "/realms/" + realm + "/protocol/openid-connect/token";

        MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<>();
        requestBody.add("grant_type", "password");
        requestBody.add("client_id", clientId);
        requestBody.add("client_secret", clientSecret);
        requestBody.add("username", usernameOrPhone);
        requestBody.add("password", password);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(requestBody, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(tokenEndpoint, request, Map.class);

        if (response.getStatusCode() == HttpStatus.OK) {
            return (String) response.getBody().get("access_token");
        } else if (response.getStatusCode() == HttpStatus.UNAUTHORIZED) {
            throw new IllegalArgumentException("Sai tên đăng nhập hoặc mật khẩu");
        } else {
            throw new Exception("Không thể lấy token từ Keycloak: " + response.getStatusCode());
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
    public CustomerResponse getCustomerDetail(String cifCode) {
        Optional<Customer> customerOpt = customerRepository.findByCifCode(cifCode);
        return customerOpt.map(this::toCustomerResponse).orElse(null);
    }

    @Override
    public Response updateCustomerPassword(ChangePasswordDTO request) {
        Optional<Customer> customerOpt = customerRepository.findById(request.getCustomerId());
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
    public Response updateCustomerStatus(UpdateStatusRequest request) {
        Optional<Customer> customerOpt = customerRepository.findById(request.getId());
        if (customerOpt.isEmpty()) {
            return new Response(false, "Không tìm thấy khách hàng");
        }

        Customer customer = customerOpt.get();
        com.example.common_service.constant.CustomerStatus newStatus = request.getStatus();

        if (newStatus == null) {
            return new Response(false, "Trạng thái không hợp lệ");
        }

        if (customer.getStatus() == com.example.common_service.constant.CustomerStatus.CLOSED) {
            return new Response(false, "Không thể thay đổi trạng thái từ CLOSED");
        }

        if (!customer.getStatus().equals(newStatus)) {
            customer.setStatus(newStatus);
            customerRepository.save(customer);
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
                customerRepository.save(customer);
            }

            return kycResponse;
        } catch (Exception e) {
            log.error("KYC verification failed for customerId: {} - {}", request.getCustomerId(), e.getMessage());
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

    private String generateCifCode(Long id) {
        return String.format("CIF%08d", id);
    }

    private CustomerResponse toCustomerResponse(Customer customer) {
        CustomerResponse response = new CustomerResponse();
        response.setId(customer.getCustomerId());
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