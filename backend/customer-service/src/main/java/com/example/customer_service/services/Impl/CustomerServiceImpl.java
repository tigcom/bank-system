package com.example.customer_service.services.Impl;

import com.example.common_service.constant.CustomerStatus;
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
import jakarta.persistence.EntityNotFoundException;
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

    @Override
    @Transactional
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
                .cifCode(generateCifCode(
                        customerRepository.getNextId(),
                        request.getDateOfBirth(),
                        request.getGender(),
                        request.getPhoneNumber()
                ))
                .build();

        try {
            // Lưu khách hàng
            Customer savedCustomer = customerRepository.save(customer);

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
            } else if (status == 400) {
                throw new AppException(ErrorCode.INVALID_REQUEST);
            } else if (status == 401) {
                throw new AppException(ErrorCode.UNAUTHORIZED);
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
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUserId = authentication.getName();
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));

        Optional<Customer> customerOpt = customerRepository.findByUserId(currentUserId);
        if (customerOpt.isEmpty()) {
            throw new IllegalArgumentException("Không tìm thấy khách hàng");
        }

        Customer customer = customerOpt.get();
        if (!isAdmin && !customer.getUserId().equals(currentUserId)) {
            log.warn("Người dùng {} cố gắng chỉnh sửa khách hàng {} không được phép", currentUserId, currentUserId);
            throw new IllegalArgumentException("Bạn không có quyền chỉnh sửa thông tin này");
        }

        if (customer.getStatus() != CustomerStatus.ACTIVE) {
            throw new IllegalArgumentException("Tài khoản không ở trạng thái hoạt động");
        }

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
            throw new IllegalArgumentException("Failed to update Keycloak user", e);
        }
    }

    @Override
    public Response forgotPassword(String email) {

        Optional<Customer> customerOpt = customerRepository.findByEmail(email);
        if (customerOpt.isEmpty() || customerOpt.get().getStatus() != CustomerStatus.ACTIVE) {
            throw new EntityNotFoundException("Tài khoản không tồn tại hoặc không hoạt động");
        }

        try (Keycloak keycloak = KeycloakBuilder.builder()
                .serverUrl(keycloakUrl)
                .realm(realm)
                .clientId(clientId)
                .clientSecret(clientSecret)
                .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
                .build()) {

            List<UserRepresentation> users = keycloak.realm(realm).users().searchByEmail(email, true);
            if (users.isEmpty()) {
                throw new IllegalArgumentException("Không tìm thấy email");
            }

            String userId = users.get(0).getId();
            keycloak.realm(realm).users().get(userId).executeActionsEmail(
                    Collections.singletonList("UPDATE_PASSWORD"),
                    3600
            );
            return new Response(true, "Đã gửi liên kết đặt lại mật khẩu qua email");
        } catch (Exception e) {
            log.error("Lỗi khi gửi email đặt lại mật khẩu");
            throw new IllegalArgumentException("Lỗi khi gửi email đặt lại mật khẩu: " + e.getMessage());
        }
    }

    @Override
    public CustomerListResponse getCustomerList() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin) {
            throw new IllegalArgumentException("Chỉ admin mới có quyền xem danh sách khách hàng");
        }

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
        String currentUserId = authentication.getName();

        Optional<Customer> customerOpt = customerRepository.findByUserId(currentUserId);
        if (customerOpt.isEmpty()) {
            throw new EntityNotFoundException("Không tìm thấy khách hàng");
        }

        return toCustomerResponse(customerOpt.get());
    }

    @Override
    public CustomerResponse getCustomerDetailByCifCode(String cifCode) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUserId = authentication.getName();
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));

        Optional<Customer> customerOpt = customerRepository.findByCifCode(cifCode);
        if (customerOpt.isEmpty()) {
            throw new EntityNotFoundException("Không tìm thấy khách hàng");
        }

        Customer customer = customerOpt.get();
        if (!isAdmin && !customer.getUserId().equals(currentUserId)) {
            log.warn("Người dùng {} cố gắng xem thông tin khách hàng {} không được phép", currentUserId, cifCode);
            throw new IllegalArgumentException("Bạn không có quyền xem thông tin này");
        }

        return toCustomerResponse(customer);
    }

    @Override
    public Response updateCustomerPassword(ChangePasswordDTO request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUserId = authentication.getName();

        Optional<Customer> customerOpt = customerRepository.findByUserId(currentUserId);
        if (customerOpt.isEmpty()) {
            throw new EntityNotFoundException("Không tìm thấy khách hàng");
        }

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Mật khẩu xác nhận không khớp");
        }

        if (request.getNewPassword().length() < 8) {
            throw new IllegalArgumentException("Mật khẩu phải có ít nhất 8 ký tự");
        }

        Customer customer = customerOpt.get();
        if (customer.getStatus() != CustomerStatus.ACTIVE) {
            throw new IllegalArgumentException("Tài khoản không ở trạng thái hoạt động");
        }

        updateKeycloakPassword(currentUserId, request.getNewPassword());
        log.info("Người dùng {} đã cập nhật mật khẩu", currentUserId);
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
            log.info("Đã cập nhật mật khẩu Keycloak cho userId: {}", userId);
        } catch (Exception e) {
            log.error("Lỗi khi cập nhật mật khẩu Keycloak");
            throw new IllegalArgumentException("Không thể cập nhật mật khẩu Keycloak: " + e.getMessage());
        }
    }

    @Transactional
    @Override
    public Response updateCustomerStatus(UpdateStatusRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin) {
            throw new IllegalArgumentException("Chỉ admin mới có quyền cập nhật trạng thái khách hàng");
        }

        Optional<Customer> customerOpt = customerRepository.findById(request.getId());
        if (customerOpt.isEmpty()) {
            throw new EntityNotFoundException("Không tìm thấy khách hàng");
        }

        Customer customer = customerOpt.get();
        CustomerStatus newStatus = request.getStatus();

        if (newStatus == null) {
            throw new IllegalArgumentException("Trạng thái không hợp lệ");
        }

        if (customer.getStatus() == CustomerStatus.CLOSED) {
            throw new IllegalArgumentException("Không thể thay đổi trạng thái từ CLOSED");
        }

        if (!customer.getStatus().equals(newStatus)) {
            customer.setStatus(newStatus);
            customerRepository.save(customer);

            CoreCustomerDTO coreCustomerDTO = CoreCustomerDTO.builder()
                    .cifCode(customer.getCifCode())
                    .status(customer.getStatus().toString())
                    .build();

            log.info("Đang đồng bộ trạng thái khách hàng với CIF: {}", customer.getCifCode());
            CoreResponse coreResponse = coreBankingClient.syncCustomer(coreCustomerDTO);
            if (!coreResponse.isSuccess()) {
                log.error("Đồng bộ trạng thái khách hàng thất bại với CIF: {}. Lỗi: {}", customer.getCifCode(), coreResponse.getMessage());
                throw new IllegalArgumentException("Đồng bộ trạng thái khách hàng thất bại: " + coreResponse.getMessage());
            }
            log.info("Đồng bộ trạng thái khách hàng thành công với CIF: {}", customer.getCifCode());
        }

        return new Response(true, "Cập nhật trạng thái thành công");
    }

    @Transactional
    public KycResponse verifyKyc(KycRequest request) {
        try {
            if (!isValidKycRequest(request)) {
                throw new IllegalArgumentException("Dữ liệu đầu vào không hợp lệ");
            }

            Optional<Customer> customerOpt = customerRepository.findById(request.getCustomerId());
            if (customerOpt.isEmpty()) {
                throw new IllegalArgumentException("Không tìm thấy khách hàng");
            }
            Customer customer = customerOpt.get();

            Optional<KycProfile> kycProfileOpt = kycProfileRepository.findByCustomer(customer);
            if (kycProfileOpt.isPresent() && KycStatus.VERIFIED.equals(kycProfileOpt.get().getStatus())) {
                throw new IllegalArgumentException("Tài khoản đã được xác minh KYC");
            }

            String errorMessage = validateCustomerData(customer, request);
            if (errorMessage != null) {
                throw new IllegalArgumentException(errorMessage);
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
                    throw new IllegalArgumentException("Đồng bộ trạng thái KYC thất bại: " + coreResponse.getMessage());
                }

                customerRepository.save(customer);
                log.info("Đồng bộ trạng thái KYC thành công với CIF: {}", customer.getCifCode());
            }

            return kycResponse;
        } catch (Exception e) {
            log.error("Xác minh KYC thất bại cho customerId: {}. Lỗi: {}",
                    request.getCustomerId(), e.getMessage(), e);
            throw new IllegalArgumentException("Xác minh KYC thất bại: " + e.getMessage());
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
        try {
            Gender requestGender = Gender.valueOf(request.getGender());
            if (!customer.getGender().equals(requestGender)) {
                return "Giới tính không khớp với thông tin đăng ký";
            }
        } catch (IllegalArgumentException e) {
            return "Giới tính không hợp lệ";
        }
        return null;
    }

    public String generateCifCode(Long id, LocalDate dateOfBirth, Gender gender, String phoneNumber) {
        String binCode = "970452";
        int dobPart = dateOfBirth.getYear() % 10;
        String genderCode = gender == Gender.male ? "1" : "0";
        String phonePart = phoneNumber.substring(phoneNumber.length() - 3);
        String idPart = String.format("%02d", id % 100);
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
        Optional<KycProfile> kycProfileOpt = kycProfileRepository.findByCustomer(customer);
        response.setKycStatus(kycProfileOpt.map(KycProfile::getStatus).orElse(null));
        return response;
    }
}