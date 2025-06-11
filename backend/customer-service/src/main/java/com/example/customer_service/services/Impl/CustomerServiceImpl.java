package com.example.customer_service.services.Impl;

import com.example.common_service.constant.CustomerStatus;
import com.example.common_service.dto.MailMessageDTO;
import com.example.common_service.dto.customer.CoreCustomerDTO;
import com.example.common_service.dto.customer.CoreResponse;
import com.example.customer_service.dtos.*;
import com.example.customer_service.exceptions.*;
import com.example.customer_service.models.*;
import com.example.customer_service.repositories.CustomerRepository;
import com.example.customer_service.repositories.KycProfileRepository;
import com.example.customer_service.responses.*;
import com.example.customer_service.services.CoreBankingClient;
import com.example.customer_service.services.CustomerService;
import com.example.customer_service.services.KycService;
import com.example.customer_service.services.OtpCacheService;
import com.example.customer_service.ultils.MessageKeys;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

    private static final Logger log = LoggerFactory.getLogger("ACCESS_LOG");

    private final CustomerRepository customerRepository;
    private final KycService kycService;
    private final KycProfileRepository kycProfileRepository;
    private final RestTemplate restTemplate;
    private final CoreBankingClient coreBankingClient;
    private final StreamBridge streamBridge;
    private final OtpCacheService otpCacheService;
    private final MessageSource messageSource;

    @Value("${idp.url}")
    private String keycloakUrl;

    @Value("${idp.realm}")
    private String realm;

    @Value("${idp.client-id}")
    private String clientId;

    @Value("${idp.client-secret}")
    private String clientSecret;

//    @Override
//    public ApiResponseWrapper<?> login(LoginCustomerDTO request) {
//        HttpHeaders headers = new HttpHeaders();
//        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
//
//        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
//        body.add("grant_type", "password");
//        body.add("client_id", "customer-service");
//        body.add("client_secret", "vF8VYOn3m3g63csOanjpBqG9AxQNUEQX");
//        body.add("username", request.getUsername());
//        body.add("password", request.getPassword());
//
//        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(body, headers);
//
//        try {
//            ResponseEntity<Map> keycloakResponse = restTemplate.exchange(
//                    "http://localhost:8081/realms/myrealm/protocol/openid-connect/token",
//                    HttpMethod.POST,
//                    entity,
//                    Map.class
//            );
//
//            Map<String, Object> keycloakToken = keycloakResponse.getBody();
//            String accessToken = (String) keycloakToken.get("access_token");
//
//            return ApiResponseWrapper.success(
//                    getMessage(MessageKeys.LOGIN_SUCCESSFULLY),
//                    accessToken
//            );
//
//        } catch (HttpClientErrorException e) {
//            log.error("Login failed for username: {}", request.getUsername(), e);
//            throw new BusinessException(getMessage(MessageKeys.LOGIN_FAILED));
//        }
//    }

    @Override
    public void sentOtpRegister(RegisterCustomerDTO request) {
        validateDuplicate(request);
        String otp = String.format("%06d", new Random().nextInt(1000000));
        otpCacheService.saveOtp(request.getEmail(), otp, request);

        try {
            MailMessageDTO mailMessage = new MailMessageDTO();
            mailMessage.setSubject("Mã xác thực đăng ký");
            mailMessage.setRecipient(request.getEmail());
            mailMessage.setRecipientName(request.getFullName());
            mailMessage.setBody(String.format(otp));
            boolean sent = streamBridge.send("mail-register-out-0", mailMessage);
            if (!sent) {
                log.error("Failed to send message to Kafka for email: {}", request.getEmail());
                otpCacheService.clearOtp(request.getEmail());
                throw new BusinessException(getMessage(MessageKeys.KAFKA_FAILED));
            }
            log.info("Sent OTP to email: {}", request.getEmail());
        } catch (Exception e) {
            log.error("Failed to send OTP email to: {}", request.getEmail(), e);
            otpCacheService.clearOtp(request.getEmail());
            throw new BusinessException(getMessage(MessageKeys.OTP_SEND_FAILED));
        }
    }

    @Override
    @Transactional
    public ApiResponseWrapper<?> confirmRegister(String email, String otp) {
        if (!otpCacheService.isValidOtp(email, otp)) {
            throw new IllegalArgumentException(getMessage(MessageKeys.INVALID_OTP));
        }

        RegisterCustomerDTO request = otpCacheService.getRegisterData(email);
        ApiResponseWrapper<?> response = register(request);
        otpCacheService.clearOtp(email);
        return response;
    }

    public ApiResponseWrapper<?> register(RegisterCustomerDTO request) {
        String userId = createKeycloakUser(request);

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
            Customer savedCustomer = customerRepository.save(customer);

            CoreCustomerDTO coreCustomerDTO = CoreCustomerDTO.builder()
                    .cifCode(savedCustomer.getCifCode())
                    .status(savedCustomer.getStatus().toString())
                    .build();

            log.info("Syncing with core banking for CIF: {}", savedCustomer.getCifCode());
            CoreResponse coreResponse = coreBankingClient.syncCustomer(coreCustomerDTO);
            if (!coreResponse.isSuccess()) {
                log.error("Core banking sync failed for CIF: {}. Error: {}", savedCustomer.getCifCode(), coreResponse.getMessage());
                throw new BusinessException(getMessage(MessageKeys.CORE_BANKING_SYNC_FAILED, coreResponse.getMessage()));
            }

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

            return new ApiResponseWrapper<>(HttpStatus.OK.value(), getMessage(MessageKeys.REGISTER_SUCCESSFULLY), request);
        } catch (Exception e) {
            log.error("Registration failed, deleting Keycloak user ID: {}", userId, e);
            deleteKeycloakUser(userId);
            throw new BusinessException(getMessage(MessageKeys.REGISTER_FAILED, e.getMessage()));
        }
    }

    private void validateDuplicate(RegisterCustomerDTO request) {
        if (customerRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new IllegalArgumentException(getMessage(MessageKeys.USER_EXISTS));
        }
        if (customerRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException(getMessage(MessageKeys.EMAIL_EXISTS));
        }
        if (customerRepository.findByIdentityNumber(request.getIdentityNumber()).isPresent()) {
            throw new IllegalArgumentException(getMessage(MessageKeys.IDENTITY_NUMBER_EXISTS));
        }
        if (customerRepository.findByPhoneNumber(request.getPhoneNumber()).isPresent()) {
            throw new IllegalArgumentException(getMessage(MessageKeys.PHONE_EXISTS));
        }
        LocalDate dateOfBirth = request.getDateOfBirth();
        if (dateOfBirth != null) {
            LocalDate today = LocalDate.now();
            LocalDate sixteenYearsAgo = today.minusYears(16);
            if (dateOfBirth.isAfter(sixteenYearsAgo)) {
                throw new IllegalArgumentException(getMessage(MessageKeys.AGE_UNDER_16));
            }
        } else {
            throw new IllegalArgumentException(getMessage(MessageKeys.NOT_NULL_DOB));
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

            log.info("Attempting to create user in Keycloak with username: {}", request.getUsername());
            UserRepresentation user = buildUserRepresentation(request);

            jakarta.ws.rs.core.Response response = keycloak.realm(realm).users().create(user);
            int status = response.getStatus();
            String responseBody = response.readEntity(String.class);

            log.info("Keycloak response status: {}", status);
            log.info("Keycloak response body: {}", responseBody);

            if (status == 201) {
                String userId = CreatedResponseUtil.getCreatedId(response);
                try {
                    RoleRepresentation role = keycloak.realm(realm).roles().get("CUSTOMER").toRepresentation();
                    keycloak.realm(realm).users().get(userId).roles().realmLevel().add(Collections.singletonList(role));
                    log.info("Created Keycloak user with ID: {} and assigned CUSTOMER role", userId);
                } catch (Exception e) {
                    log.error("Failed to assign CUSTOMER role for Keycloak user ID: {}", userId, e);
                    keycloak.realm(realm).users().get(userId).remove();
                    throw new BusinessException(getMessage(MessageKeys.KEYCLOAK_ROLE_FAILED, e.getMessage()));
                }
                return userId;
            } else if (status == 400) {
                throw new IllegalArgumentException(getMessage(MessageKeys.INVALID_REQUEST));
            } else if (status == 401) {
                throw new BusinessException(getMessage(MessageKeys.UNAUTHORIZED_ACCESS));
            } else if (status == 409) {
                throw new IllegalArgumentException(getMessage(MessageKeys.USER_EXISTS));
            } else {
                ObjectMapper objectMapper = new ObjectMapper();
                try {
                    JsonNode errorJson = objectMapper.readTree(responseBody);
                    String errorMessage = errorJson.has("error_description")
                            ? errorJson.get("error_description").asText()
                            : errorJson.has("error")
                            ? errorJson.get("error").asText()
                            : getMessage(MessageKeys.KEYCLOAK_UNKNOWN);
                    throw new BusinessException(getMessage(MessageKeys.KEYCLOAK_ERROR, errorMessage, status));
                } catch (Exception e) {
                    throw new BusinessException(getMessage(MessageKeys.KEYCLOAK_PARSE_ERROR, status, responseBody));
                }
            }
        } catch (Exception e) {
            log.error("Failed to create Keycloak user: {}", e.getMessage(), e);
            throw new BusinessException(getMessage(MessageKeys.KEYCLOAK_CREATE_FAILED, e.getMessage()));
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
            log.info("Deleted Keycloak user with ID: {}", userId);
        } catch (Exception e) {
            log.error("Failed to delete Keycloak user: {}", e.getMessage(), e);
        }
    }

    @Override
    public ApiResponseWrapper<?> updateCustomer(UpdateCustomerDTO request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUserId = authentication.getName();
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));

        Optional<Customer> customerOpt = customerRepository.findByUserId(currentUserId);
        if (customerOpt.isEmpty()) {
            throw new EntityNotFoundException(getMessage(MessageKeys.USER_NOT_FOUND));
        }

        Customer customer = customerOpt.get();
        if (!isAdmin && !customer.getUserId().equals(currentUserId)) {
            log.warn("User {} attempted unauthorized access to customer {}", currentUserId, currentUserId);
            throw new BusinessException(getMessage(MessageKeys.UNAUTHORIZED_ACCESS));
        }

        if (customer.getStatus() != CustomerStatus.ACTIVE) {
            throw new BusinessException(getMessage(MessageKeys.ACCOUNT_NOT_ACTIVE));
        }

        if (request.getFullName() != null) customer.setFullName(request.getFullName());
        if (request.getAddress() != null) customer.setAddress(request.getAddress());
        if (request.getGender() != null) customer.setGender(request.getGender());
        if (request.getDateOfBirth() != null) customer.setDateOfBirth(request.getDateOfBirth());
        if (request.getEmail() != null) {
            if (customerRepository.findByEmail(request.getEmail()).isPresent() &&
                    !customer.getEmail().equals(request.getEmail())) {
                return new ApiResponseWrapper<>(HttpStatus.BAD_REQUEST.value(), getMessage(MessageKeys.EMAIL_EXISTS), null);
            }
            customer.setEmail(request.getEmail());
        }
        if (request.getPhoneNumber() != null) {
            if (customerRepository.findByPhoneNumber(request.getPhoneNumber()).isPresent() &&
                    !customer.getPhoneNumber().equals(request.getPhoneNumber())) {
                return new ApiResponseWrapper<>(HttpStatus.BAD_REQUEST.value(), getMessage(MessageKeys.PHONE_EXISTS), null);
            }
            customer.setPhoneNumber(request.getPhoneNumber());
        }

        updateUserInKeycloak(customer.getUserId(), request);

        customerRepository.save(customer);
        return new ApiResponseWrapper<>(HttpStatus.OK.value(), getMessage(MessageKeys.SUCCESS_UPDATE), customer.getFullName());
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
            log.error("Error updating Keycloak user: {}", e.getMessage(), e);
            throw new BusinessException(getMessage(MessageKeys.KEYCLOAK_UPDATE_FAILED, e.getMessage()));
        }
    }

    @Override
    public void sentEmailForgotPassword(String email) {
        log.info("request email: {}", email);
        Customer customer = customerRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException(getMessage(MessageKeys.EMAIL_NOT_FOUND)));

        String resetToken = UUID.randomUUID().toString();
        LocalDateTime expiry = LocalDateTime.now().plusMinutes(15);

        customer.setResetToken(resetToken);
        customer.setResetTokenExpiry(expiry);
        customerRepository.save(customer);

        String resetLink = String.format("http://localhost:4200/reset-password?token=%s", resetToken);

        try {
            MailMessageDTO mailMessage = new MailMessageDTO();
            mailMessage.setSubject("Khôi phục mật khẩu");
            mailMessage.setRecipient(email);
            mailMessage.setRecipientName(customer.getFullName());
            mailMessage.setBody(resetLink);

            streamBridge.send("mail-forgotPassword-out-0", mailMessage);
            log.info("Sent password reset email to {}", email);
        } catch (Exception e) {
            log.error("Failed to send reset email", e);
            throw new BusinessException(getMessage(MessageKeys.EMAIL_SEND_FAILED, e.getMessage()));
        }
    }

    @Override
    public ApiResponseWrapper<?> resetPassword(ResetPasswordDTO request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException(getMessage(MessageKeys.INVALID_CONFIRM_PASSWORD));
        }

        Customer customer = customerRepository.findByResetToken(request.getToken())
                .orElseThrow(() -> new EntityNotFoundException(getMessage(MessageKeys.EXPIRED_TOKEN)));

        if (customer.getResetTokenExpiry() == null || customer.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new BusinessException(getMessage(MessageKeys.EXPIRED_TOKEN));
        }

        updateKeycloakPassword(customer.getUserId(), request.getNewPassword());

        customer.setResetToken(null);
        customer.setResetTokenExpiry(null);
        customerRepository.save(customer);

        log.info("User {} successfully reset password", customer.getUserId());
        return new ApiResponseWrapper<>(HttpStatus.OK.value(), getMessage(MessageKeys.PASSWORD_RESET_SUCCESS), null);
    }

    @Override
    public Response forgotPassword(String email) {
        Optional<Customer> optionalCustomer = customerRepository.findByEmail(email);
        if (optionalCustomer.isEmpty() || optionalCustomer.get().getStatus() != CustomerStatus.ACTIVE) {
            throw new EntityNotFoundException(getMessage(MessageKeys.ACCOUNT_ERROR));
        }

        Customer customer = optionalCustomer.get();

        try (Keycloak keycloak = KeycloakBuilder.builder()
                .serverUrl(keycloakUrl)
                .realm(realm)
                .clientId(clientId)
                .clientSecret(clientSecret)
                .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
                .build()) {

            List<UserRepresentation> users = keycloak.realm(realm).users().searchByEmail(email, true);
            if (users.isEmpty()) {
                throw new EntityNotFoundException(getMessage(MessageKeys.KEYCLOAK_USER_EMAIL_FAILED, email));
            }

            String userId = users.get(0).getId();

            keycloak.realm(realm)
                    .users()
                    .get(userId)
                    .executeActionsEmail(
                            clientId,
                            null,
                            3600,
                            List.of("UPDATE_PASSWORD")
                    );

            log.info("Sent password reset link to email: {}", email);
            return new Response(true, getMessage(MessageKeys.FORGOT_PASSWORD_NOTIFICATION));
        } catch (Exception e) {
            log.error("Failed to send password reset email for: {}", email, e);
            throw new BusinessException(getMessage(MessageKeys.EMAIL_SEND_FAILED, e.getMessage()));
        }
    }

    @Override
    public CustomerListResponse getCustomerList() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin) {
            throw new BusinessException(getMessage(MessageKeys.ADMIN_ONLY));
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
    public CustomerResponse getCustomerDetail(String userId) {
        Optional<Customer> customerOpt = customerRepository.findByUserId(userId);
        if (customerOpt.isEmpty()) {
            throw new EntityNotFoundException(getMessage(MessageKeys.USER_NOT_FOUND));
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
            throw new EntityNotFoundException(getMessage(MessageKeys.USER_NOT_FOUND));
        }

        Customer customer = customerOpt.get();
        if (!isAdmin && !customer.getUserId().equals(currentUserId)) {
            log.warn("User {} attempted unauthorized access to customer {}", currentUserId, cifCode);
            throw new BusinessException(getMessage(MessageKeys.UNAUTHORIZED_ACCESS));
        }

        return toCustomerResponse(customer);
    }

    @Override
    public ApiResponseWrapper<?> updateCustomerPassword(ChangePasswordDTO request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUserId = authentication.getName();

        Optional<Customer> customerOpt = customerRepository.findByUserId(currentUserId);
        if (customerOpt.isEmpty()) {
            throw new EntityNotFoundException(getMessage(MessageKeys.USER_NOT_FOUND));
        }

        Customer customer = customerOpt.get();
        if (customer.getStatus() != CustomerStatus.ACTIVE) {
            throw new BusinessException(getMessage(MessageKeys.ACCOUNT_NOT_ACTIVE));
        }

        // Kiểm tra mật khẩu hiện tại qua Keycloak
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("grant_type", "password");
            body.add("client_id", clientId);
            body.add("client_secret", clientSecret);
            body.add("username", customer.getUsername());
            body.add("password", request.getCurrentPassword());

            HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<Map> keycloakResponse = restTemplate.exchange(
                    keycloakUrl + "/realms/" + realm + "/protocol/openid-connect/token",
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            if (keycloakResponse.getStatusCode() != HttpStatus.OK) {
                throw new IllegalArgumentException(getMessage(MessageKeys.INVALID_CURRENT_PASSWORD));
            }
        } catch (HttpClientErrorException e) {
            log.error("Failed to verify current password for user {}: {}", currentUserId, e.getMessage());
            throw new IllegalArgumentException(getMessage(MessageKeys.INVALID_CURRENT_PASSWORD));
        } catch (Exception e) {
            log.error("Error verifying current password for user {}: {}", currentUserId, e.getMessage());
            throw new BusinessException(getMessage(MessageKeys.PASSWORD_VERIFICATION_FAILED));
        }

        // Kiểm tra mật khẩu mới và xác nhận mật khẩu
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException(getMessage(MessageKeys.INVALID_CONFIRM_PASSWORD));
        }

        updateKeycloakPassword(currentUserId, request.getNewPassword());
        log.info("User {} updated password", currentUserId);
        return new ApiResponseWrapper<>(HttpStatus.OK.value(), getMessage(MessageKeys.SUCCESS_UPDATE), null);
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
            log.error("Failed to update Keycloak password for userId: {}", userId, e);
            throw new BusinessException(getMessage(MessageKeys.KEYCLOAK_PASSWORD_UPDATE_FAILED, e.getMessage()));
        }
    }

    @Transactional
    @Override
    public ApiResponseWrapper<?> updateCustomerStatus(UpdateStatusRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin) {
            throw new BusinessException(getMessage(MessageKeys.ADMIN_ONLY));
        }

        Optional<Customer> customerOpt = customerRepository.findByCifCode(request.getCifCode());
        if (customerOpt.isEmpty()) {
            throw new EntityNotFoundException(getMessage(MessageKeys.USER_NOT_FOUND));
        }

        Customer customer = customerOpt.get();
        CustomerStatus newStatus = request.getStatus();

        if (newStatus == null) {
            throw new IllegalArgumentException(getMessage(MessageKeys.INVALID_STATUS));
        }

        if (customer.getStatus() == CustomerStatus.CLOSED) {
            throw new BusinessException(getMessage(MessageKeys.CLOSED_ACCOUNT_STATUS));
        }

        if (!customer.getStatus().equals(newStatus)) {

            customer.setStatus(newStatus);
            customerRepository.save(customer);

            KycResponse kycResponse = kycService.verifyIdentity(
                    customer.getIdentityNumber(),
                    customer.getFullName()
            );
            kycService.saveKycInfo(
                    customer.getCustomerId(),
                    kycResponse,
                    customer.getIdentityNumber(),
                    customer.getFullName(),
                    customer.getDateOfBirth(),
                    customer.getGender().toString()
            );

            CoreCustomerDTO coreCustomerDTO = CoreCustomerDTO.builder()
                    .cifCode(customer.getCifCode())
                    .status(customer.getStatus().toString())
                    .build();

            log.info("Syncing customer status for CIF: {}", customer.getCifCode());
            CoreResponse coreResponse = coreBankingClient.syncCustomer(coreCustomerDTO);
            if (!coreResponse.isSuccess()) {
                log.error("Failed to sync customer status for CIF: {}. Error: {}", customer.getCifCode(), coreResponse.getMessage());
                throw new BusinessException(getMessage(MessageKeys.STATUS_SYNC_FAILED, coreResponse.getMessage()));
            }
            log.info("Successfully synced customer status for CIF: {}", customer.getCifCode());
        }

        return new ApiResponseWrapper<>(HttpStatus.OK.value(), getMessage(MessageKeys.SUCCESS_UPDATE), customer.getStatus());
    }

    @Transactional
    public KycResponse verifyKyc(String userId, KycRequest request) {
        try {
            System.out.println(request);
            if (!isValidKycRequest(request)) {
                throw new IllegalArgumentException(getMessage(MessageKeys.INVALID_KYC_DATA));
            }

            Customer customer = customerRepository.findCustomerByUserId(userId);

            if (customer == null) {
                throw new EntityNotFoundException(getMessage(MessageKeys.USER_NOT_FOUND));
            }

            Optional<KycProfile> kycProfileOpt = kycProfileRepository.findByCustomer(customer);
            if (kycProfileOpt.isPresent() && KycStatus.VERIFIED.equals(kycProfileOpt.get().getStatus())) {
                throw new BusinessException(getMessage(MessageKeys.ACCOUNT_ALREADY_VERIFIED));
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
                        request.getGender().toString()
                );
                customer.setStatus(CustomerStatus.ACTIVE);

                CoreCustomerDTO coreCustomerDTO = CoreCustomerDTO.builder()
                        .cifCode(customer.getCifCode())
                        .status(customer.getStatus().toString())
                        .build();

                log.info("Syncing KYC status for CIF: {}", customer.getCifCode());
                CoreResponse coreResponse = coreBankingClient.syncCustomer(coreCustomerDTO);

                if (!coreResponse.isSuccess()) {
                    log.error("Failed to sync KYC status for CIF: {}. Error: {}", customer.getCifCode(), coreResponse.getMessage());
                    throw new BusinessException(getMessage(MessageKeys.KYC_SYNC_FAILED, coreResponse.getMessage()));
                }

                customerRepository.save(customer);

                log.info("Successfully synced KYC status for CIF: {}", customer.getCifCode());
            }

            return kycResponse;
        } catch (Exception e) {
            log.error("KYC verification failed for customerId: {}. Error: {}", userId, e.getMessage(), e);
            throw new BusinessException(getMessage(MessageKeys.KYC_VERIFICATION_FAILED, e.getMessage()));
        }
    }

    private boolean isValidKycRequest(KycRequest request) {
        return request.getIdentityNumber() != null &&
                request.getFullName() != null &&
                request.getDateOfBirth() != null &&
                request.getGender() != null;
    }

    private String validateCustomerData(Customer customer, KycRequest request) {
        if (!Objects.equals(customer.getIdentityNumber(), request.getIdentityNumber())) {
            return getMessage(MessageKeys.KYC_MISMATCH_IDENTITY);
        }
        if (!Objects.equals(customer.getFullName(), request.getFullName())) {
            return getMessage(MessageKeys.KYC_MISMATCH_NAME);
        }
        if (!Objects.equals(customer.getDateOfBirth(), request.getDateOfBirth())) {
            return getMessage(MessageKeys.KYC_MISMATCH_DOB);
        }
        try {
            Gender requestGender = Gender.valueOf(request.getGender().toString());
            if (!customer.getGender().equals(requestGender)) {
                return getMessage(MessageKeys.KYC_MISMATCH_GENDER);
            }
        } catch (IllegalArgumentException e) {
            return getMessage(MessageKeys.INVALID_GENDER);
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
        response.setIdentityNumber(customer.getIdentityNumber());
        response.setPhoneNumber(customer.getPhoneNumber());
        response.setDateOfBirth(customer.getDateOfBirth());
        response.setStatus(customer.getStatus());
        response.setGender(customer.getGender());
        Optional<KycProfile> kycProfileOpt = kycProfileRepository.findByCustomer(customer);
        response.setKycStatus(kycProfileOpt.map(KycProfile::getStatus).orElse(null));
        return response;
    }

    private String getMessage(String key, Object... args) {
        return messageSource.getMessage(key, args, LocaleContextHolder.getLocale());
    }
}