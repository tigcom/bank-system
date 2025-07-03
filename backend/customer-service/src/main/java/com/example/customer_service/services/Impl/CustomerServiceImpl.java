package com.example.customer_service.services.Impl;

import com.example.common_service.constant.CustomerStatus;
import com.example.common_service.dto.MailMessageDTO;
import com.example.common_service.dto.PaymentCreateDTO;
import com.example.common_service.dto.customer.CoreCustomerDTO;
import com.example.common_service.dto.customer.CoreResponse;
import com.example.common_service.dto.response.PaymentRequestResponse;
import com.example.common_service.services.customer.CustomerCommonService;
import com.example.customer_service.dtos.*;
import com.example.customer_service.exceptions.*;
import com.example.customer_service.models.*;
import com.example.customer_service.repositories.CustomerRepository;
import com.example.customer_service.repositories.KycHistoryRepository;
import com.example.customer_service.repositories.KycProfileRepository;
import com.example.customer_service.responses.*;
import com.example.customer_service.services.*;
import com.example.customer_service.ultils.MessageKeys;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboReference;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

    private static final Logger log = LoggerFactory.getLogger("ACCESS_LOG");

    private final CustomerRepository customerRepository;
    private final KycService kycService;
    private final KycProfileRepository kycProfileRepository;
    private final KycHistoryRepository kycHistoryRepository;
    private final RestTemplate restTemplate;
    private final CoreBankingClient coreBankingClient;
    private final StreamBridge streamBridge;
    private final OtpCacheService otpCacheService;
    private final MessageSource messageSource;
    private final RegistrationCacheService registrationCacheService;

    @DubboReference(timeout = 5000)
    private CustomerCommonService customerCommonService;

    @Value("${idp.url}")
    private String keycloakUrl;

    @Value("${idp.realm}")
    private String realm;

    @Value("${idp.client-id}")
    private String clientId;

    @Value("${idp.client-secret}")
    private String clientSecret;

    @Override
    public ApiResponseWrapper<?> initiateRegister(RegisterCustomerDTO request) {
        // Generate a unique request ID for tracing
        String requestId = UUID.randomUUID().toString();
        log.info("[initiateRegister] INITIATE_REGISTRATION - RequestId: {}, Email: {}, Username: {}", 
                requestId, request.getEmail(), request.getUsername());
        validateDuplicate(request);
        registrationCacheService.saveRegistrationData(request.getEmail(), request);
        log.info("[initiateRegister] REGISTRATION_DATA_SAVED - RequestId: {}, Email: {}, Username: {}", 
                requestId, request.getEmail(), request.getUsername());
        return new ApiResponseWrapper<>(HttpStatus.OK.value(),
                getMessage(MessageKeys.REGISTER_DATA_SAVED),
                request);
    }

    @Override
    public ApiResponseWrapper<?> processKycAndSendOtp(String email, KycRequest kycRequest) {
        String requestId = UUID.randomUUID().toString();
        log.info("[processKycAndSendOtp] PROCESS_KYC_START - RequestId: {}, Email: {}, IdentityNumber: {}", 
                requestId, email, kycRequest.getIdentityNumber());
        RegisterCustomerDTO registerData = registrationCacheService.getRegistrationData(email);
        if (registerData == null) {
            log.error("[processKycAndSendOtp] REGISTRATION_DATA_NOT_FOUND - RequestId: {}, Email: {}", 
                    requestId, email);
            throw new BusinessException(getMessage(MessageKeys.REGISTRATION_DATA_NOT_FOUND));
        }
        String errorMessage = validateKycDataWithRegistration(registerData, kycRequest);
        if (errorMessage != null) {
            log.error("[processKycAndSendOtp] KYC_DATA_MISMATCH - RequestId: {}, Email: {}, IdentityNumber: {}, Error: {}", 
                    requestId, email, kycRequest.getIdentityNumber(), errorMessage);
            throw new IllegalArgumentException(errorMessage);
        }
        KycResponse kycResponse = kycService.verifyIdentity(
                kycRequest.getIdentityNumber(),
                kycRequest.getFullName(),
                kycRequest.getDateOfBirth(),
                kycRequest.getGender().toString()
        );
        if (!kycResponse.isVerified()) {
            log.warn("[processKycAndSendOtp] KYC_VERIFICATION_FAILED - RequestId: {}, Email: {}, IdentityNumber: {}, Reason: {}", 
                    requestId, email, kycRequest.getIdentityNumber(), kycResponse.getMessage());
            registrationCacheService.clearRegistrationData(email);
            throw new BusinessException(getMessage(MessageKeys.KYC_VERIFICATION_FAILED, kycResponse.getMessage()));
        }
        registrationCacheService.updateRegistrationWithKyc(email, kycRequest);
        String otp = String.format("%06d", new Random().nextInt(1000000));
        otpCacheService.saveOtp(email, otp, registerData);
        try {
            MailMessageDTO mailMessage = new MailMessageDTO();
            mailMessage.setSubject("Mã xác thực đăng ký");
            mailMessage.setRecipient(email);
            mailMessage.setRecipientName(registerData.getFullName());
            mailMessage.setBody(String.format("Mã OTP của bạn là: %s", otp));
            boolean sent = streamBridge.send("mail-register-out-0", mailMessage);
            if (!sent) {
                log.error("[processKycAndSendOtp] KAFKA_SEND_FAILED - RequestId: {}, Email: {}", 
                        requestId, email);
                otpCacheService.clearOtp(email);
                registrationCacheService.clearRegistrationData(email);
                throw new BusinessException(getMessage(MessageKeys.KAFKA_FAILED));
            }
            log.info("[processKycAndSendOtp] OTP_SENT - RequestId: {}, Email: {}", 
                    requestId, email);
            return new ApiResponseWrapper<>(HttpStatus.OK.value(),
                    getMessage(MessageKeys.OTP_SENT),
                    "KYC thành công. OTP đã được gửi đến email của bạn.");
        } catch (Exception e) {
            log.error("[processKycAndSendOtp] OTP_SEND_FAILED - RequestId: {}, Email: {}, Error: {}", 
                    requestId, email, e.getMessage(), e);
            otpCacheService.clearOtp(email);
            registrationCacheService.clearRegistrationData(email);
            throw new BusinessException(getMessage(MessageKeys.OTP_SEND_FAILED));
        }
    }

    @Override
    public ApiResponseWrapper<?> reSendOtp(String email) {
        String requestId = UUID.randomUUID().toString();
        log.info("[reSendOtp] RESEND_OTP_START - RequestId: {}, Email: {}", 
                requestId, email);
        RegisterCustomerDTO registerData = registrationCacheService.getRegistrationData(email);
        if (registerData == null) {
            log.error("[reSendOtp] REGISTRATION_DATA_NOT_FOUND - RequestId: {}, Email: {}", 
                    requestId, email);
            throw new BusinessException(getMessage(MessageKeys.REGISTRATION_DATA_NOT_FOUND));
        }
        String otp = String.format("%06d", new Random().nextInt(1000000));
        otpCacheService.saveOtp(email, otp, registerData);
        try {
            MailMessageDTO mailMessage = new MailMessageDTO();
            mailMessage.setSubject("Mã xác thực đăng ký");
            mailMessage.setRecipient(email);
            mailMessage.setRecipientName(registerData.getFullName());
            mailMessage.setBody(String.format("Mã OTP của bạn là: %s", otp));
            boolean sent = streamBridge.send("mail-register-out-0", mailMessage);
            if (!sent) {
                log.error("[reSendOtp] KAFKA_SEND_FAILED - RequestId: {}, Email: {}", 
                        requestId, email);
                otpCacheService.clearOtp(email);
                registrationCacheService.clearRegistrationData(email);
                throw new BusinessException(getMessage(MessageKeys.KAFKA_FAILED));
            }
            log.info("[reSendOtp] OTP_RESENT - RequestId: {}, Email: {}", 
                    requestId, email);
            return new ApiResponseWrapper<>(HttpStatus.OK.value(),
                    getMessage(MessageKeys.OTP_SENT),
                    "KYC thành công. OTP đã được gửi đến email của bạn.");
        } catch (Exception e) {
            log.error("[reSendOtp] OTP_RESEND_FAILED - RequestId: {}, Email: {}, Error: {}", 
                    requestId, email, e.getMessage(), e);
            otpCacheService.clearOtp(email);
            registrationCacheService.clearRegistrationData(email);
            throw new BusinessException(getMessage(MessageKeys.OTP_SEND_FAILED));
        }
    }

    @Override
    @Transactional
    public ApiResponseWrapper<?> confirmRegister(String email, String otp) {
        String requestId = UUID.randomUUID().toString();
        log.info("[confirmRegister] CONFIRM_REGISTRATION_START - RequestId: {}, Email: {}", 
                requestId, email);
        if (!otpCacheService.isValidOtp(email, otp)) {
            log.error("[confirmRegister] INVALID_OTP - RequestId: {}, Email: {}", 
                    requestId, email);
            throw new IllegalArgumentException(getMessage(MessageKeys.INVALID_OTP));
        }
        RegisterCustomerDTO request = registrationCacheService.getRegistrationData(email);
        if (request == null) {
            log.error("[confirmRegister] REGISTRATION_DATA_NOT_FOUND - RequestId: {}, Email: {}", 
                    requestId, email);
            throw new BusinessException(getMessage(MessageKeys.REGISTRATION_DATA_NOT_FOUND));
        }
        KycRequest kycData = registrationCacheService.getKycData(email);
        if (kycData == null) {
            log.error("[confirmRegister] KYC_DATA_NOT_FOUND - RequestId: {}, Email: {}", 
                    requestId, email);
            throw new BusinessException(getMessage(MessageKeys.KYC_DATA_NOT_FOUND));
        }
        ApiResponseWrapper<?> response = completeRegistration(request, kycData);
        otpCacheService.clearOtp(email);
        registrationCacheService.clearRegistrationData(email);
        log.info("[confirmRegister] REGISTRATION_COMPLETED - RequestId: {}, Email: {}, Username: {}", 
                requestId, email, request.getUsername());
        return response;
    }

    @Transactional

        protected ApiResponseWrapper<?> completeRegistration(RegisterCustomerDTO request, KycRequest kycData) {
        String requestId = UUID.randomUUID().toString();
        log.info("[completeRegistration] COMPLETE_REGISTRATION_START - RequestId: {}, Email: {}, Username: {}", 
                requestId, request.getEmail(), request.getUsername());
        String userId = createKeycloakUser(request);
        Customer customer = Customer.builder()
                .userId(userId)
                .username(request.getUsername())
                .fullName(request.getFullName())
                .address(request.getAddress())
                .identityNumber(request.getIdentityNumber())
                .email(request.getEmail())
                .phoneNumber(request.getPhoneNumber())
                .status(CustomerStatus.ACTIVE)
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
            log.info("[completeRegistration] CUSTOMER_SAVED - RequestId: {}, UserId: {}, Email: {}, CifCode: {}", 
                    requestId, savedCustomer.getUserId(), savedCustomer.getEmail(), savedCustomer.getCifCode());
            CoreCustomerDTO coreCustomerDTO = CoreCustomerDTO.builder()
                    .cifCode(savedCustomer.getCifCode())
                    .status(savedCustomer.getStatus().toString())
                    .build();
            log.info("[completeRegistration] SYNC_CORE_BANKING_START - RequestId: {}, CifCode: {}, UserId: {}", 
                    requestId, savedCustomer.getCifCode(), savedCustomer.getUserId());
            CoreResponse coreResponse = coreBankingClient.syncCustomer(coreCustomerDTO);
            if (!coreResponse.isSuccess()) {
                log.error("[completeRegistration] CORE_BANKING_SYNC_FAILED - RequestId: {}, CifCode: {}, UserId: {}, Error: {}", 
                        requestId, savedCustomer.getCifCode(), savedCustomer.getUserId(), coreResponse.getMessage());
                throw new BusinessException(getMessage(MessageKeys.CORE_BANKING_SYNC_FAILED, coreResponse.getMessage()));
            }
            PaymentCreateDTO paymentCreateDTO = PaymentCreateDTO.builder()
                    .cifCode(savedCustomer.getCifCode())
                    .build();
            // Log payment account creation
            log.info("[completeRegistration] CREATE_PAYMENT_ACCOUNT_START - RequestId: {}, CifCode: {}, UserId: {}", 
                    requestId, savedCustomer.getCifCode(), savedCustomer.getUserId());
            PaymentRequestResponse paymentResponse = customerCommonService.createPaymentInit(paymentCreateDTO);
            log.info("[completeRegistration] PAYMENT_ACCOUNT_CREATED - RequestId: {}, CifCode: {}, UserId: {}", 
                    requestId, paymentResponse.getCifCode(), savedCustomer.getUserId());
            KycProfile kycProfile = KycProfile.builder()
                    .status(KycStatus.VERIFIED)
                    .identityNumber(kycData.getIdentityNumber())
                    .fullName(kycData.getFullName())
                    .dateOfBirth(kycData.getDateOfBirth())
                    .gender(kycData.getGender().toString())
                    .build();
            kycProfile.setCustomer(savedCustomer);
            savedCustomer.setKycProfile(kycProfile);
            kycProfileRepository.save(kycProfile);
            log.info("[completeRegistration] KYC_PROFILE_SAVED - RequestId: {}, CifCode: {}, UserId: {}, KycStatus: {}", 
                    requestId, savedCustomer.getCifCode(), savedCustomer.getUserId(), kycProfile.getStatus());
            log.info("[completeRegistration] REGISTRATION_SUCCESS - RequestId: {}, Email: {}, UserId: {}, CifCode: {}", 
                    requestId, request.getEmail(), savedCustomer.getUserId(), savedCustomer.getCifCode());
            return new ApiResponseWrapper<>(HttpStatus.OK.value(),
                    getMessage(MessageKeys.REGISTER_SUCCESSFULLY),
                    toCustomerResponse(savedCustomer));
        } catch (Exception e) {
            log.error("[completeRegistration] REGISTRATION_FAILED - RequestId: {}, Email: {}, UserId: {}, Error: {}", 
                    requestId, request.getEmail(), userId, e.getMessage(), e);
            deleteKeycloakUser(userId);
            throw new BusinessException(getMessage(MessageKeys.REGISTER_FAILED, e.getMessage()));
        }
    }

    private String createKeycloakUser(RegisterCustomerDTO request) {
        String requestId = UUID.randomUUID().toString();
        log.info("[createKeycloakUser] CREATE_KEYCLOAK_USER_START - RequestId: {}, Username: {}, Email: {}", 
                requestId, request.getUsername(), request.getEmail());
        try (Keycloak keycloak = KeycloakBuilder.builder()
                .serverUrl(keycloakUrl)
                .realm(realm)
                .clientId(clientId)
                .clientSecret(clientSecret)
                .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
                .build()) {
            UserRepresentation user = buildUserRepresentation(request);
            jakarta.ws.rs.core.Response response = keycloak.realm(realm).users().create(user);
            int status = response.getStatus();
            String responseBody = response.readEntity(String.class);
            log.info("[createKeycloakUser] KEYCLOAK_RESPONSE - RequestId: {}, Username: {}, Status: {}, ResponseBody: {}", 
                    requestId, request.getUsername(), status, responseBody);
            if (status == 201) {
                String userId = CreatedResponseUtil.getCreatedId(response);
                try {
                    RoleRepresentation role = keycloak.realm(realm).roles().get("CUSTOMER").toRepresentation();
                    keycloak.realm(realm).users().get(userId).roles().realmLevel().add(Collections.singletonList(role));
                    log.info("[createKeycloakUser] KEYCLOAK_ROLE_ASSIGNED - RequestId: {}, UserId: {}, Username: {}, Role: CUSTOMER", 
                            requestId, userId, request.getUsername());
                } catch (Exception e) {
                    log.error("[createKeycloakUser] KEYCLOAK_ROLE_ASSIGNMENT_FAILED - RequestId: {}, UserId: {}, Username: {}, Error: {}", 
                            requestId, userId, request.getUsername(), e.getMessage(), e);
                    keycloak.realm(realm).users().get(userId).remove();
                    throw new BusinessException(getMessage(MessageKeys.KEYCLOAK_ROLE_FAILED, e.getMessage()));
                }
                log.info("[createKeycloakUser] KEYCLOAK_USER_CREATED - RequestId: {}, UserId: {}, Username: {}, Email: {}", 
                        requestId, userId, request.getUsername(), request.getEmail());
                return userId;
            } else {
                ObjectMapper objectMapper = new ObjectMapper();
                try {
                    JsonNode errorJson = objectMapper.readTree(responseBody);
                    String errorMessage = errorJson.has("error_description")
                            ? errorJson.get("error_description").asText()
                            : errorJson.has("error")
                            ? errorJson.get("error").asText()
                            : getMessage(MessageKeys.KEYCLOAK_UNKNOWN);
                    log.error("[createKeycloakUser] KEYCLOAK_ERROR - RequestId: {}, Username: {}, Email: {}, Status: {}, Error: {}", 
                            requestId, request.getUsername(), request.getEmail(), status, errorMessage);
                    if (status == 400) {
                        throw new IllegalArgumentException(getMessage(MessageKeys.INVALID_REQUEST));
                    } else if (status == 401) {
                        throw new BusinessException(getMessage(MessageKeys.UNAUTHORIZED_ACCESS));
                    } else if (status == 409) {
                        throw new IllegalArgumentException(getMessage(MessageKeys.USER_EXISTS));
                    } else {
                        throw new BusinessException(getMessage(MessageKeys.KEYCLOAK_ERROR, errorMessage, status));
                    }
                } catch (Exception e) {
                    log.error("[createKeycloakUser] KEYCLOAK_RESPONSE_PARSE_FAILED - RequestId: {}, Username: {}, Email: {}, Status: {}, ResponseBody: {}, Error: {}", 
                            requestId, request.getUsername(), request.getEmail(), status, responseBody, e.getMessage(), e);
                    throw new BusinessException(getMessage(MessageKeys.KEYCLOAK_PARSE_ERROR, status, responseBody));
                }
            }
        } catch (Exception e) {
            log.error("[createKeycloakUser] KEYCLOAK_CREATE_FAILED - RequestId: {}, Username: {}, Email: {}, Error: {}", 
                    requestId, request.getUsername(), request.getEmail(), e.getMessage(), e);
            throw new BusinessException(getMessage(MessageKeys.KEYCLOAK_CREATE_FAILED, e.getMessage()));
        }
    }

    private void deleteKeycloakUser(String userId) {
        String requestId = UUID.randomUUID().toString();
        log.info("[deleteKeycloakUser] DELETE_KEYCLOAK_USER_START - RequestId: {}, UserId: {}", 
                requestId, userId);
        try (Keycloak keycloak = KeycloakBuilder.builder()
                .serverUrl(keycloakUrl)
                .realm(realm)
                .clientId(clientId)
                .clientSecret(clientSecret)
                .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
                .build()) {
            keycloak.realm(realm).users().get(userId).remove();
            log.info("[deleteKeycloakUser] KEYCLOAK_USER_DELETED - RequestId: {}, UserId: {}", 
                    requestId, userId);
        } catch (Exception e) {
            log.error("[deleteKeycloakUser] KEYCLOAK_DELETE_FAILED - RequestId: {}, UserId: {}, Error: {}", 
                    requestId, userId, e.getMessage(), e);
        }
    }

    @Override
    public ApiResponseWrapper<?> updateCustomer(UpdateCustomerDTO request) {
        String requestId = UUID.randomUUID().toString();
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUserId = authentication.getName();
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
        String targetUserId = isAdmin && request.getUserId() != null ? request.getUserId() : currentUserId;
        log.info("[updateCustomer] UPDATE_CUSTOMER_START - RequestId: {}, TargetUserId: {}, CurrentUserId: {}, IsAdmin: {}", 
                requestId, targetUserId, currentUserId, isAdmin);
        Optional<Customer> customerOpt = customerRepository.findByUserId(targetUserId);
        if (customerOpt.isEmpty()) {
            log.error("[updateCustomer] USER_NOT_FOUND - RequestId: {}, TargetUserId: {}, CurrentUserId: {}", 
                    requestId, targetUserId, currentUserId);
            throw new EntityNotFoundException(getMessage(MessageKeys.USER_NOT_FOUND));
        }
        Customer customer = customerOpt.get();
        if (!isAdmin && !customer.getUserId().equals(currentUserId)) {
            log.warn("[updateCustomer] UNAUTHORIZED_ACCESS - RequestId: {}, CurrentUserId: {}, TargetUserId: {}", 
                    requestId, currentUserId, targetUserId);
            throw new BusinessException(getMessage(MessageKeys.UNAUTHORIZED_ACCESS));
        }
        if (customer.getStatus() != CustomerStatus.ACTIVE) {
            log.warn("[updateCustomer] ACCOUNT_NOT_ACTIVE - RequestId: {}, TargetUserId: {}, CurrentStatus: {}", 
                    requestId, targetUserId, customer.getStatus());
            throw new BusinessException(getMessage(MessageKeys.ACCOUNT_NOT_ACTIVE));
        }
        if (request.getFullName() != null) customer.setFullName(request.getFullName());
        if (request.getAddress() != null) customer.setAddress(request.getAddress());
        if (request.getGender() != null) customer.setGender(request.getGender());
        if (request.getDateOfBirth() != null) customer.setDateOfBirth(request.getDateOfBirth());
        if (request.getEmail() != null) {
            Optional<Customer> emailOwner = customerRepository.findByEmail(request.getEmail());
            if (emailOwner.isPresent() && !customer.getEmail().equals(request.getEmail())) {
                log.error("[updateCustomer] EMAIL_EXISTS - RequestId: {}, TargetUserId: {}, Email: {}", 
                        requestId, targetUserId, request.getEmail());
                throw new BusinessException(getMessage(MessageKeys.EMAIL_EXISTS));
            }
            customer.setEmail(request.getEmail());
        }
        if (request.getPhoneNumber() != null) {
            Optional<Customer> phoneNumberOwner = customerRepository.findByPhoneNumber(request.getPhoneNumber());
            if (phoneNumberOwner.isPresent() && !customer.getPhoneNumber().equals(request.getPhoneNumber())) {
                log.error("[updateCustomer] PHONE_EXISTS - RequestId: {}, TargetUserId: {}, PhoneNumber: {}", 
                        requestId, targetUserId, request.getPhoneNumber());
                throw new BusinessException(getMessage(MessageKeys.PHONE_EXISTS));
            }
            customer.setPhoneNumber(request.getPhoneNumber());
        }
        updateUserInKeycloak(customer.getUserId(), request);
        customerRepository.save(customer);
        log.info("[updateCustomer] CUSTOMER_UPDATED - RequestId: {}, TargetUserId: {}, Email: {}, CifCode: {}", 
                requestId, targetUserId, customer.getEmail(), customer.getCifCode());
        return new ApiResponseWrapper<>(HttpStatus.OK.value(), getMessage(MessageKeys.SUCCESS_UPDATE), customer);
    }

    private void updateUserInKeycloak(String userId, UpdateCustomerDTO request) {
        String requestId = UUID.randomUUID().toString();
        log.info("[updateUserInKeycloak] UPDATE_KEYCLOAK_USER_START - RequestId: {}, UserId: {}, Email: {}", 
                requestId, userId, request.getEmail());
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
            user.setEmail(request.getEmail());
            userResource.update(user);
            log.info("[updateUserInKeycloak] KEYCLOAK_USER_UPDATED - RequestId: {}, UserId: {}, Email: {}", 
                    requestId, userId, request.getEmail());
        } catch (Exception e) {
            log.error("[updateUserInKeycloak] KEYCLOAK_UPDATE_FAILED - RequestId: {}, UserId: {}, Email: {}, Error: {}", 
                    requestId, userId, request.getEmail(), e.getMessage(), e);
            throw new BusinessException(getMessage(MessageKeys.KEYCLOAK_UPDATE_FAILED, e.getMessage()));
        }
    }

    @Override
    public void sentEmailForgotPassword(String email) {
        String requestId = UUID.randomUUID().toString();
        log.info("[sentEmailForgotPassword] FORGOT_PASSWORD_EMAIL_START - RequestId: {}, Email: {}", 
                requestId, email);
        Customer customer = customerRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.error("[sentEmailForgotPassword] EMAIL_NOT_FOUND - RequestId: {}, Email: {}", 
                            requestId, email);
                    return new EntityNotFoundException(getMessage(MessageKeys.EMAIL_NOT_FOUND));
                });
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
            log.info("[sentEmailForgotPassword] FORGOT_PASSWORD_EMAIL_SENT - RequestId: {}, Email: {}, UserId: {}, CifCode: {}", 
                    requestId, email, customer.getUserId(), customer.getCifCode());
        } catch (Exception e) {
            log.error("[sentEmailForgotPassword] EMAIL_SEND_FAILED - RequestId: {}, Email: {}, UserId: {}, Error: {}", 
                    requestId, email, customer.getUserId(), e.getMessage(), e);
            throw new BusinessException(getMessage(MessageKeys.EMAIL_SEND_FAILED, e.getMessage()));
        }
    }

    @Override
    public ApiResponseWrapper<?> resetPassword(ResetPasswordDTO request) {
        String requestId = UUID.randomUUID().toString();
        log.info("[resetPassword] RESET_PASSWORD_START - RequestId: {}", 
                requestId);
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            log.error("[resetPassword] INVALID_CONFIRM_PASSWORD - RequestId: {}", 
                    requestId);
            throw new IllegalArgumentException(getMessage(MessageKeys.INVALID_CONFIRM_PASSWORD));
        }
        Customer customer = customerRepository.findByResetToken(request.getToken())
                .orElseThrow(() -> {
                    log.error("[resetPassword] EXPIRED_TOKEN - RequestId: {}, Token: {}", 
                            requestId, request.getToken());
                    return new EntityNotFoundException(getMessage(MessageKeys.EXPIRED_TOKEN));
                });
        if (customer.getResetTokenExpiry() == null || customer.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
            log.error("[resetPassword] EXPIRED_TOKEN - RequestId: {}, Email: {}, UserId: {}, CifCode: {}", 
                    requestId, customer.getEmail(), customer.getUserId(), customer.getCifCode());
            throw new BusinessException(getMessage(MessageKeys.EXPIRED_TOKEN));
        }
        updateKeycloakPassword(customer.getUserId(), request.getNewPassword());
        customer.setResetToken(null);
        customer.setResetTokenExpiry(null);
        customerRepository.save(customer);
        log.info("[resetPassword] PASSWORD_RESET_SUCCESS - RequestId: {}, Email: {}, UserId: {}, CifCode: {}", 
                requestId, customer.getEmail(), customer.getUserId(), customer.getCifCode());
        return new ApiResponseWrapper<>(HttpStatus.OK.value(), getMessage(MessageKeys.PASSWORD_RESET_SUCCESS), null);
    }

    @Override
    public CustomerListResponse getCustomerList(int page, int size, String keyword) {
        String requestId = UUID.randomUUID().toString();
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUserId = authentication.getName();
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
        log.info("[getCustomerList] GET_CUSTOMER_LIST_START - RequestId: {}, CurrentUserId: {}, IsAdmin: {}, Page: {}, Size: {}, Keyword: {}", 
                requestId, currentUserId, isAdmin, page, size, keyword);
        if (!isAdmin) {
            log.warn("[getCustomerList] ADMIN_ACCESS_DENIED - RequestId: {}, CurrentUserId: {}", 
                    requestId, currentUserId);
            throw new BusinessException(getMessage(MessageKeys.ADMIN_ONLY));
        }
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Customer> customerPage;
        if (keyword != null && !keyword.trim().isEmpty()) {
            log.info("[getCustomerList] SEARCH_CUSTOMERS - RequestId: {}, CurrentUserId: {}, Keyword: {}", 
                    requestId, currentUserId, keyword);
            customerPage = customerRepository.searchCustomers(keyword.trim(), pageable);
        } else {
            log.info("[getCustomerList] FETCH_ALL_CUSTOMERS - RequestId: {}, CurrentUserId: {}", 
                    requestId, currentUserId);
            customerPage = customerRepository.findAll(pageable);
        }
        List<CustomerResponse> customerResponses = customerPage.getContent()
                .stream()
                .map(this::toCustomerResponse)
                .collect(Collectors.toList());
        CustomerListResponse response = new CustomerListResponse();
        response.setCustomers(customerResponses);
        response.setTotalElements(customerPage.getTotalElements());
        response.setTotalPages(customerPage.getTotalPages());
        response.setCurrentPage(customerPage.getNumber());
        log.info("[getCustomerList] CUSTOMER_LIST_RETRIEVED - RequestId: {}, CurrentUserId: {}, TotalCustomers: {}, CurrentPage: {}, TotalPages: {}", 
                requestId, currentUserId, customerResponses.size(), response.getCurrentPage() + 1, response.getTotalPages());
        return response;
    }

    @Override
    public CustomerResponse getCustomerDetail(String userId) {
        String requestId = UUID.randomUUID().toString();
        log.info("[getCustomerDetail] GET_CUSTOMER_DETAIL_START - RequestId: {}, UserId: {}", 
                requestId, userId);
        Optional<Customer> customerOpt = customerRepository.findByUserId(userId);
        if (customerOpt.isEmpty()) {
            log.error("[getCustomerDetail] USER_NOT_FOUND - RequestId: {}, UserId: {}", 
                    requestId, userId);
            throw new EntityNotFoundException(getMessage(MessageKeys.USER_NOT_FOUND));
        }
        CustomerResponse response = toCustomerResponse(customerOpt.get());
        log.info("[getCustomerDetail] CUSTOMER_DETAIL_RETRIEVED - RequestId: {}, UserId: {}, CifCode: {}", 
                requestId, userId, response.getCifCode());
        return response;
    }

    @Override
    public CustomerResponse getCustomerDetailByCifCode(String cifCode) {
        String requestId = UUID.randomUUID().toString();
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUserId = authentication.getName();
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
        log.info("[getCustomerDetailByCifCode] GET_CUSTOMER_BY_CIF_START - RequestId: {}, CifCode: {}, CurrentUserId: {}, IsAdmin: {}", 
                requestId, cifCode, currentUserId, isAdmin);
        Optional<Customer> customerOpt = customerRepository.findByCifCode(cifCode);
        if (customerOpt.isEmpty()) {
            log.error("[getCustomerDetailByCifCode] USER_NOT_FOUND - RequestId: {}, CifCode: {}, CurrentUserId: {}", 
                    requestId, cifCode, currentUserId);
            throw new EntityNotFoundException(getMessage(MessageKeys.USER_NOT_FOUND));
        }
        Customer customer = customerOpt.get();
        if (!isAdmin && !customer.getUserId().equals(currentUserId)) {
            log.warn("[getCustomerDetailByCifCode] UNAUTHORIZED_ACCESS - RequestId: {}, CurrentUserId: {}, CifCode: {}, TargetUserId: {}", 
                    requestId, currentUserId, cifCode, customer.getUserId());
            throw new BusinessException(getMessage(MessageKeys.UNAUTHORIZED_ACCESS));
        }
        CustomerResponse response = toCustomerResponse(customer);
        log.info("[getCustomerDetailByCifCode] CUSTOMER_DETAIL_RETRIEVED - RequestId: {}, CifCode: {}, UserId: {}, CurrentUserId: {}", 
                requestId, cifCode, customer.getUserId(), currentUserId);
        return response;
    }

    @Override
    public ApiResponseWrapper<?> updateCustomerPassword(ChangePasswordDTO request) {
        String requestId = UUID.randomUUID().toString();
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUserId = authentication.getName();
        log.info("[updateCustomerPassword] UPDATE_PASSWORD_START - RequestId: {}, UserId: {}", 
                requestId, currentUserId);
        Optional<Customer> customerOpt = customerRepository.findByUserId(currentUserId);
        if (customerOpt.isEmpty()) {
            log.error("[updateCustomerPassword] USER_NOT_FOUND - RequestId: {}, UserId: {}", 
                    requestId, currentUserId);
            throw new EntityNotFoundException(getMessage(MessageKeys.USER_NOT_FOUND));
        }
        Customer customer = customerOpt.get();
        if (customer.getStatus() != CustomerStatus.ACTIVE) {
            log.warn("[updateCustomerPassword] ACCOUNT_NOT_ACTIVE - RequestId: {}, UserId: {}, CurrentStatus: {}", 
                    requestId, currentUserId, customer.getStatus());
            throw new BusinessException(getMessage(MessageKeys.ACCOUNT_NOT_ACTIVE));
        }
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
                log.error("[updateCustomerPassword] INVALID_CURRENT_PASSWORD - RequestId: {}, UserId: {}, Username: {}, Status: {}", 
                        requestId, currentUserId, customer.getUsername(), keycloakResponse.getStatusCode());
                throw new IllegalArgumentException(getMessage(MessageKeys.INVALID_CURRENT_PASSWORD));
            }
        } catch (HttpClientErrorException e) {
            log.error("[updateCustomerPassword] AUTHENTICATION_FAILED - RequestId: {}, UserId: {}, Username: {}, Status: {}, Error: {}", 
                    requestId, currentUserId, customer.getUsername(), e.getStatusCode(), e.getMessage(), e);
            throw new IllegalArgumentException(getMessage(MessageKeys.INVALID_CURRENT_PASSWORD));
        } catch (Exception e) {
            log.error("[updateCustomerPassword] PASSWORD_VERIFICATION_FAILED - RequestId: {}, UserId: {}, Username: {}, Error: {}", 
                    requestId, currentUserId, customer.getUsername(), e.getMessage(), e);
            throw new BusinessException(getMessage(MessageKeys.PASSWORD_VERIFICATION_FAILED));
        }
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            log.error("[updateCustomerPassword] INVALID_CONFIRM_PASSWORD - RequestId: {}, UserId: {}, Username: {}", 
                    requestId, currentUserId, customer.getUsername());
            throw new IllegalArgumentException(getMessage(MessageKeys.INVALID_CONFIRM_PASSWORD));
        }
        updateKeycloakPassword(currentUserId, request.getNewPassword());
        log.info("[updateCustomerPassword] PASSWORD_UPDATED - RequestId: {}, UserId: {}, Username: {}, CifCode: {}", 
                requestId, currentUserId, customer.getUsername(), customer.getCifCode());
        return new ApiResponseWrapper<>(HttpStatus.OK.value(), getMessage(MessageKeys.SUCCESS_UPDATE), null);
    }

    private void updateKeycloakPassword(String userId, String newPassword) {
        String requestId = UUID.randomUUID().toString();
        log.info("[updateKeycloakPassword] UPDATE_KEYCLOAK_PASSWORD_START - RequestId: {}, UserId: {}", 
                requestId, userId);
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
            log.info("[updateKeycloakPassword] KEYCLOAK_PASSWORD_UPDATED - RequestId: {}, UserId: {}", 
                    requestId, userId);
        } catch (Exception e) {
            log.error("[updateKeycloakPassword] KEYCLOAK_PASSWORD_UPDATE_FAILED - RequestId: {}, UserId: {}, Error: {}", 
                    requestId, userId, e.getMessage(), e);
            throw new BusinessException(getMessage(MessageKeys.KEYCLOAK_PASSWORD_UPDATE_FAILED, e.getMessage()));
        }
    }

    @Transactional
    @Override
    public ApiResponseWrapper<?> updateCustomerStatus(UpdateStatusRequest request) {
        String requestId = UUID.randomUUID().toString();
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUserId = authentication.getName();
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
        log.info("[updateCustomerStatus] UPDATE_STATUS_START - RequestId: {}, CifCode: {}, CurrentUserId: {}, IsAdmin: {}", 
                requestId, request.getCifCode(), currentUserId, isAdmin);
        if (!isAdmin) {
            log.warn("[updateCustomerStatus] ADMIN_ACCESS_DENIED - RequestId: {}, CurrentUserId: {}", 
                    requestId, currentUserId);
            throw new BusinessException(getMessage(MessageKeys.ADMIN_ONLY));
        }
        Optional<Customer> customerOpt = customerRepository.findByCifCode(request.getCifCode());
        if (customerOpt.isEmpty()) {
            log.error("[updateCustomerStatus] USER_NOT_FOUND - RequestId: {}, CifCode: {}", 
                    requestId, request.getCifCode());
            throw new EntityNotFoundException(getMessage(MessageKeys.USER_NOT_FOUND));
        }
        Customer customer = customerOpt.get();
        CustomerStatus newStatus = request.getStatus();
        CustomerStatus oldStatus = customer.getStatus();
        if (newStatus == null) {
            log.error("[updateCustomerStatus] INVALID_STATUS - RequestId: {}, CifCode: {}, UserId: {}", 
                    requestId, customer.getCifCode(), customer.getUserId());
            throw new IllegalArgumentException(getMessage(MessageKeys.INVALID_STATUS));
        }
        if (customer.getStatus() == CustomerStatus.CLOSED) {
            log.warn("[updateCustomerStatus] CLOSED_ACCOUNT_STATUS - RequestId: {}, CifCode: {}, UserId: {}", 
                    requestId, customer.getCifCode(), customer.getUserId());
            throw new BusinessException(getMessage(MessageKeys.CLOSED_ACCOUNT_STATUS));
        }
        if (customer.getStatus().equals(newStatus)) {
            log.info("[updateCustomerStatus] NO_STATUS_CHANGE - RequestId: {}, CifCode: {}, UserId: {}, CurrentStatus: {}", 
                    requestId, customer.getCifCode(), customer.getUserId(), customer.getStatus());
            return new ApiResponseWrapper<>(HttpStatus.OK.value(), getMessage(MessageKeys.SUCCESS_UPDATE), customer.getStatus());
        }
        // Update customer status
        customer.setStatus(newStatus);
        customerRepository.save(customer);
        log.info("[updateCustomerStatus] CUSTOMER_STATUS_UPDATED - RequestId: {}, CifCode: {}, UserId: {}, OldStatus: {}, NewStatus: {}", 
                requestId, customer.getCifCode(), customer.getUserId(), oldStatus, newStatus);
        // Update corresponding KYC status
        KycStatus kycStatusToUpdate = switch (newStatus) {
            case ACTIVE -> KycStatus.VERIFIED;
            case SUSPENDED, CLOSED -> KycStatus.REJECTED;
        };
        KycResponse kycResponse = new KycResponse();
        kycResponse.setVerified(kycStatusToUpdate == KycStatus.VERIFIED);
        kycResponse.setStatus(kycStatusToUpdate);
        kycResponse.setMessage("Updated due to status change: " + newStatus);
        kycResponse.setDetails("{\"auto_update\": true}");
        kycService.saveKycInfo(
                customer.getCustomerId(),
                kycResponse,
                customer.getIdentityNumber(),
                customer.getFullName(),
                customer.getDateOfBirth(),
                customer.getGender().toString()
        );
        log.info("[updateCustomerStatus] KYC_STATUS_UPDATED - RequestId: {}, CifCode: {}, UserId: {}, KycStatus: {}", 
                requestId, customer.getCifCode(), customer.getUserId(), kycStatusToUpdate);
        // Sync with core banking
        CoreCustomerDTO coreCustomerDTO = CoreCustomerDTO.builder()
                .cifCode(customer.getCifCode())
                .status(customer.getStatus().toString())
                .build();
        log.info("[updateCustomerStatus] SYNC_CORE_BANKING_START - RequestId: {}, CifCode: {}, UserId: {}", 
                requestId, customer.getCifCode(), customer.getUserId());
        CoreResponse coreResponse = coreBankingClient.syncCustomer(coreCustomerDTO);
        if (!coreResponse.isSuccess()) {
            log.error("[updateCustomerStatus] CORE_BANKING_SYNC_FAILED - RequestId: {}, CifCode: {}, UserId: {}, Error: {}", 
                    requestId, customer.getCifCode(), customer.getUserId(), coreResponse.getMessage());
            throw new BusinessException(getMessage(MessageKeys.STATUS_SYNC_FAILED, coreResponse.getMessage()));
        }
        log.info("[updateCustomerStatus] CORE_BANKING_SYNC_SUCCESS - RequestId: {}, CifCode: {}, UserId: {}", 
                requestId, customer.getCifCode(), customer.getUserId());
        return new ApiResponseWrapper<>(HttpStatus.OK.value(), getMessage(MessageKeys.SUCCESS_UPDATE), customer.getStatus());
    }

    @Transactional
    @Override
    public KycResponse verifyKyc(String userId, KycRequest request) {
        String requestId = UUID.randomUUID().toString();
        log.info("[verifyKyc] VERIFY_KYC_START - RequestId: {}, UserId: {}, IdentityNumber: {}",
                requestId, userId, request.getIdentityNumber());
        if (!isValidKycRequest(request)) {
            log.error("[verifyKyc] INVALID_KYC_DATA - RequestId: {}, UserId: {}",
                    requestId, userId);
            throw new IllegalArgumentException(getMessage(MessageKeys.INVALID_KYC_DATA));
        }
        Customer customer = customerRepository.findCustomerByUserId(userId);
        if (customer == null) {
            log.error("[verifyKyc] USER_NOT_FOUND - RequestId: {}, UserId: {}",
                    requestId, userId);
            throw new EntityNotFoundException(getMessage(MessageKeys.USER_NOT_FOUND));
        }
        Optional<KycProfile> kycProfileOpt = kycProfileRepository.findByCustomer(customer);
        if (kycProfileOpt.isPresent() && KycStatus.VERIFIED.equals(kycProfileOpt.get().getStatus())) {
            log.warn("[verifyKyc] ACCOUNT_ALREADY_VERIFIED - RequestId: {}, UserId: {}, CifCode: {}",
                    requestId, userId, customer.getCifCode());
            throw new BusinessException(getMessage(MessageKeys.ACCOUNT_ALREADY_VERIFIED));
        }
        String errorMessage = validateCustomerData(customer, request);
        if (errorMessage != null) {
            log.error("[verifyKyc] KYC_DATA_MISMATCH - RequestId: {}, UserId: {}, CifCode: {}, Error: {}",
                    requestId, userId, customer.getCifCode(), errorMessage);
            throw new IllegalArgumentException(errorMessage);
        }
        KycProfile kycProfile = kycProfileOpt.get();
        kycProfile.setStatus(KycStatus.PENDING);
        kycProfile.setReason("");
        kycProfileRepository.save(kycProfile);
        // Save KYC history
        KycHistory kycHistory = KycHistory.builder()
                .customerId(customer.getCustomerId())
                .cifCode(customer.getCifCode())
                .status(KycStatus.PENDING)
                .identityNumber(request.getIdentityNumber())
                .fullName(request.getFullName())
                .dateOfBirth(request.getDateOfBirth())
                .gender(request.getGender().toString())
                .createdAt(LocalDateTime.now())
                .build();
        kycHistoryRepository.save(kycHistory);
        log.info("[verifyKyc] KYC_HISTORY_SAVED - RequestId: {}, UserId: {}, CifCode: {}, KycStatus: PENDING",
                requestId, userId, customer.getCifCode());
        // Send KYC pending notification
        MailMessageDTO mailMessage = new MailMessageDTO();
        mailMessage.setSubject("Yêu cầu KYC đang được xem xét");
        mailMessage.setRecipient(customer.getEmail());
        mailMessage.setRecipientName(customer.getFullName());
        mailMessage.setBody("Yêu cầu KYC của bạn đã được gửi và đang chờ admin duyệt. Chúng tôi sẽ thông báo kết quả sớm nhất.");
        streamBridge.send("mail-kyc-pending-out-0", mailMessage);
        log.info("[verifyKyc] KYC_PENDING_NOTIFICATION_SENT - RequestId: {}, UserId: {}, CifCode: {}, Email: {}",
                requestId, userId, customer.getCifCode(), customer.getEmail());
        KycResponse kycResponse = new KycResponse();
        kycResponse.setVerified(false);
        kycResponse.setStatus(KycStatus.PENDING);
        kycResponse.setMessage("Yêu cầu KYC đã được gửi, đang chờ admin duyệt");
        kycResponse.setDetails("{\"status\": \"pending\", \"submitted_at\": \"" + LocalDateTime.now() + "\"}");
        log.info("[verifyKyc] KYC_REQUEST_SUBMITTED - RequestId: {}, UserId: {}, CifCode: {}, KycStatus: PENDING",
                requestId, userId, customer.getCifCode());
        return kycResponse;
    }

    @Transactional
    @Override
    public KycResponse approveKyc(String cifCode, KycStatus status, String reason) {
        String requestId = UUID.randomUUID().toString();
        log.info("[approveKyc] APPROVE_KYC_START - RequestId: {}, CifCode: {}, KycStatus: {}", 
                requestId, cifCode, status);
        Optional<Customer> customerOpt = customerRepository.findByCifCode(cifCode);
        if (customerOpt.isEmpty()) {
            log.error("[approveKyc] USER_NOT_FOUND - RequestId: {}, CifCode: {}", 
                    requestId, cifCode);
            throw new EntityNotFoundException(getMessage(MessageKeys.USER_NOT_FOUND));
        }
        Customer customer = customerOpt.get();
        Optional<KycProfile> kycProfileOpt = kycProfileRepository.findByCustomer(customer);
        if (kycProfileOpt.isEmpty()) {
            log.error("[approveKyc] KYC_DATA_NOT_FOUND - RequestId: {}, CifCode: {}, UserId: {}", 
                    requestId, cifCode, customer.getUserId());
            throw new EntityNotFoundException(getMessage(MessageKeys.KYC_DATA_NOT_FOUND));
        }
        KycProfile kycProfile = kycProfileOpt.get();
        if (!KycStatus.PENDING.equals(kycProfile.getStatus())) {
            log.warn("[approveKyc] KYC_NOT_PENDING - RequestId: {}, CifCode: {}, UserId: {}, CurrentKycStatus: {}", 
                    requestId, cifCode, customer.getUserId(), kycProfile.getStatus());
            throw new BusinessException(getMessage(MessageKeys.KYC_NOT_PENDING));
        }
        kycProfile.setStatus(status);
        kycProfile.setUpdatedAt(LocalDateTime.now());
        kycProfile.setReason(status == KycStatus.REJECTED ? reason : null);
        kycProfileRepository.save(kycProfile);
        log.info("[approveKyc] KYC_PROFILE_UPDATED - RequestId: {}, CifCode: {}, UserId: {}, KycStatus: {}", 
                requestId, cifCode, customer.getUserId(), status);
        // Save KYC history
        KycHistory kycHistory = KycHistory.builder()
                .customerId(customer.getCustomerId())
                .cifCode(customer.getCifCode())
                .status(status)
                .identityNumber(kycProfile.getIdentityNumber())
                .fullName(kycProfile.getFullName())
                .dateOfBirth(kycProfile.getDateOfBirth())
                .gender(kycProfile.getGender())
                .reason(status == KycStatus.REJECTED ? reason : null)
                .createdAt(LocalDateTime.now())
                .build();
        kycHistoryRepository.save(kycHistory);
        log.info("[approveKyc] KYC_HISTORY_SAVED - RequestId: {}, CifCode: {}, UserId: {}, KycStatus: {}", 
                requestId, cifCode, customer.getUserId(), status);
        // Update customer status
        CustomerStatus newCustomerStatus = status == KycStatus.VERIFIED ? CustomerStatus.ACTIVE : CustomerStatus.SUSPENDED;
        CustomerStatus oldCustomerStatus = customer.getStatus();
        customer.setStatus(newCustomerStatus);
        customerRepository.save(customer);
        log.info("[approveKyc] CUSTOMER_STATUS_UPDATED - RequestId: {}, CifCode: {}, UserId: {}, OldStatus: {}, NewStatus: {}", 
                requestId, cifCode, customer.getUserId(), oldCustomerStatus, newCustomerStatus);
        // Sync with core banking
        CoreCustomerDTO coreCustomerDTO = CoreCustomerDTO.builder()
                .cifCode(customer.getCifCode())
                .status(customer.getStatus().toString())
                .build();
        log.info("[approveKyc] SYNC_CORE_BANKING_START - RequestId: {}, CifCode: {}, UserId: {}", 
                requestId, cifCode, customer.getUserId());
        CoreResponse coreResponse = coreBankingClient.syncCustomer(coreCustomerDTO);
        if (!coreResponse.isSuccess()) {
            log.error("[approveKyc] CORE_BANKING_SYNC_FAILED - RequestId: {}, CifCode: {}, UserId: {}, Error: {}", 
                    requestId, cifCode, customer.getUserId(), coreResponse.getMessage());
            throw new BusinessException(getMessage(MessageKeys.KYC_SYNC_FAILED, coreResponse.getMessage()));
        }
        log.info("[approveKyc] CORE_BANKING_SYNC_SUCCESS - RequestId: {}, CifCode: {}, UserId: {}", 
                requestId, cifCode, customer.getUserId());
        // Send notification
        MailMessageDTO mailMessage = new MailMessageDTO();
        mailMessage.setSubject(status == KycStatus.VERIFIED ? "KYC đã được duyệt" : "KYC bị từ chối");
        mailMessage.setRecipient(customer.getEmail());
        mailMessage.setRecipientName(customer.getFullName());
        mailMessage.setBody(status == KycStatus.VERIFIED ?
                "Yêu cầu KYC của bạn đã được duyệt thành công." :
                "Yêu cầu KYC của bạn bị từ chối. Lý do: " + (reason != null ? reason : "Không được cung cấp"));
        streamBridge.send("mail-kyc-result-out-0", mailMessage);
        log.info("[approveKyc] KYC_NOTIFICATION_SENT - RequestId: {}, CifCode: {}, UserId: {}, Email: {}, KycStatus: {}", 
                requestId, cifCode, customer.getUserId(), customer.getEmail(), status);
        KycResponse kycResponse = new KycResponse();
        kycResponse.setVerified(status == KycStatus.VERIFIED);
        kycResponse.setStatus(status);
        kycResponse.setMessage(status == KycStatus.VERIFIED ? "KYC được duyệt thành công" : "KYC bị từ chối: " + (reason != null ? reason : ""));
        kycResponse.setDetails("{\"updated_at\": \"" + LocalDateTime.now() + "\", \"reason\": \"" + (reason != null ? reason : "") + "\"}");
        log.info("[approveKyc] KYC_APPROVAL_COMPLETED - RequestId: {}, CifCode: {}, UserId: {}, KycStatus: {}", 
                requestId, cifCode, customer.getUserId(), status);
        return kycResponse;
    }

    @Override
    public KycListResponse getPendingKycRequests(int page, int size, String keyword) {
        String requestId = UUID.randomUUID().toString();
        log.info("[getPendingKycRequests] GET_PENDING_KYC_START - RequestId: {}, Page: {}, Size: {}, Keyword: {}",
                requestId, page, size, keyword);
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<KycProfile> kycPage;
        if (keyword == null || keyword.trim().isEmpty()) {
            kycPage = kycProfileRepository.findByStatus(KycStatus.PENDING, pageable);
        } else {
            kycPage = kycProfileRepository.findByStatusAndKeyword(KycStatus.PENDING, keyword, pageable);
        }

        List<KycResponse> kycResponses = kycPage.getContent().stream()
                .map(kyc -> {
                    KycResponse response = new KycResponse();
                    response.setVerified(false);
                    response.setStatus(kyc.getStatus());
                    response.setMessage("Đang chờ duyệt");
                    response.setDetails("{\"cifCode\": \"" + kyc.getCustomer().getCifCode() +
                            "\", \"identityNumber\": \"" + kyc.getIdentityNumber() +
                            "\", \"fullName\": \"" + kyc.getFullName() +
                            "\", \"submitted_at\": \"" + kyc.getUpdatedAt() + "\"}");
                    return response;
                })
                .collect(Collectors.toList());

        KycListResponse response = new KycListResponse();
        response.setKycRequests(kycResponses);
        response.setTotalElements(kycPage.getTotalElements());
        response.setTotalPages(kycPage.getTotalPages());
        response.setCurrentPage(kycPage.getNumber());

        log.info("[getPendingKycRequests] PENDING_KYC_RETRIEVED - RequestId: {}, TotalRequests: {}, CurrentPage: {}, TotalPages: {}",
                requestId, kycResponses.size(), response.getCurrentPage() + 1, response.getTotalPages());

        return response;
    }

    @Override
    public KycStatisticsResponse getKycStatistics(LocalDate startDate, LocalDate endDate) {
        String requestId = UUID.randomUUID().toString();
        log.info("[getKycStatistics] GET_KYC_STATISTICS_START - RequestId: {}, StartDate: {}, EndDate: {}", 
                requestId, startDate, endDate);
        KycStatisticsResponse response = new KycStatisticsResponse();
        if (startDate == null) startDate = LocalDate.now().minusDays(30);
        if (endDate == null) endDate = LocalDate.now();
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);
        long total = kycHistoryRepository.countByCreatedAtBetween(startDateTime, endDateTime);
        long successful = kycHistoryRepository.countByStatusAndCreatedAtBetween(KycStatus.VERIFIED, startDateTime, endDateTime);
        long failed = kycHistoryRepository.countByStatusAndCreatedAtBetween(KycStatus.REJECTED, startDateTime, endDateTime);
        long pending = kycHistoryRepository.countByStatusAndCreatedAtBetween(KycStatus.PENDING, startDateTime, endDateTime);
        response.setTotalKycRequests(total);
        response.setSuccessfulKyc(successful);
        response.setFailedKyc(failed);
        response.setPendingKyc(pending);
        log.info("[getKycStatistics] KYC_STATISTICS_RETRIEVED - RequestId: {}, Total: {}, Successful: {}, Failed: {}, Pending: {}", 
                requestId, total, successful, failed, pending);
        return response;
    }

    @Override
    public CustomerGrowthResponse getCustomerGrowth(LocalDate startDate, LocalDate endDate) {
        String requestId = UUID.randomUUID().toString();
        log.info("[getCustomerGrowth] GET_CUSTOMER_GROWTH_START - RequestId: {}, StartDate: {}, EndDate: {}", 
                requestId, startDate, endDate);
        CustomerGrowthResponse response = new CustomerGrowthResponse();
        if (startDate == null) startDate = LocalDate.now().minusDays(30);
        if (endDate == null) endDate = LocalDate.now();
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);
        long newCustomers = customerRepository.countByCreatedAtBetween(startDateTime, endDateTime);
        LocalDateTime prevStart = startDateTime.minusDays(30);
        LocalDateTime prevEnd = startDateTime.minusNanos(1);
        long previousPeriodCustomers = customerRepository.countByCreatedAtBetween(prevStart, prevEnd);
        double growthRate = previousPeriodCustomers == 0 ? 0 :
                ((double) (newCustomers - previousPeriodCustomers) / previousPeriodCustomers) * 100;
        response.setTotalNewCustomers(newCustomers);
        response.setPreviousPeriodCustomers(previousPeriodCustomers);
        response.setGrowthRate(Math.round(growthRate * 100.0) / 100.0);
        log.info("[getCustomerGrowth] CUSTOMER_GROWTH_RETRIEVED - RequestId: {}, NewCustomers: {}, PreviousPeriod: {}, GrowthRate: {}%", 
                requestId, newCustomers, previousPeriodCustomers, growthRate);
        return response;
    }

    private String validateKycDataWithRegistration(RegisterCustomerDTO registerData, KycRequest kycData) {
        if (!Objects.equals(registerData.getIdentityNumber(), kycData.getIdentityNumber())) {
            return getMessage(MessageKeys.KYC_MISMATCH_IDENTITY);
        }
        if (!Objects.equals(registerData.getFullName(), kycData.getFullName())) {
            return getMessage(MessageKeys.KYC_MISMATCH_NAME);
        }
        if (!Objects.equals(registerData.getDateOfBirth(), kycData.getDateOfBirth())) {
            return getMessage(MessageKeys.KYC_MISMATCH_DOB);
        }
        try {
            Gender requestGender = Gender.valueOf(kycData.getGender().toString());
            if (!registerData.getGender().equals(requestGender)) {
                return getMessage(MessageKeys.KYC_MISMATCH_GENDER);
            }
        } catch (IllegalArgumentException e) {
            return getMessage(MessageKeys.INVALID_GENDER);
        }
        return null;
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
        String keycloakUsername = request.getUsername();
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

    @Override
    public ApiResponseWrapper<?> getCustomerDetailById(Long customerId) {
        Customer cus = customerRepository.findById(customerId).orElse(null);
        return new ApiResponseWrapper<>(HttpStatus.OK.value(), getMessage(MessageKeys.PASSWORD_RESET_SUCCESS), cus);
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
        response.setUserId(customer.getUserId());
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