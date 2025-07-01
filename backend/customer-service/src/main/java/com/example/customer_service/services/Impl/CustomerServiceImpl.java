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
import lombok.Builder;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
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
        log.info("[initiateRegister] Bắt đầu đăng ký cho email: {}, username: {}", request.getEmail(), request.getUsername());
        validateDuplicate(request);
        registrationCacheService.saveRegistrationData(request.getEmail(), request);
        log.info("[initiateRegister] Đã lưu dữ liệu đăng ký cho email: {}, username: {}", request.getEmail(), request.getUsername());
        return new ApiResponseWrapper<>(HttpStatus.OK.value(),
                getMessage(MessageKeys.REGISTER_DATA_SAVED),
                request);
    }

    @Override
    public ApiResponseWrapper<?> processKycAndSendOtp(String email, KycRequest kycRequest) {
        log.info("[processKycAndSendOtp] Bắt đầu xác minh KYC và gửi OTP cho email: {}, identityNumber: {}", email, kycRequest.getIdentityNumber());
        RegisterCustomerDTO registerData = registrationCacheService.getRegistrationData(email);
        if (registerData == null) {
            log.error("[processKycAndSendOtp] Không tìm thấy dữ liệu đăng ký cho email: {}", email);
            throw new BusinessException(getMessage(MessageKeys.REGISTRATION_DATA_NOT_FOUND));
        }
        String errorMessage = validateKycDataWithRegistration(registerData, kycRequest);
        if (errorMessage != null) {
            log.error("[processKycAndSendOtp] Dữ liệu KYC không khớp cho email: {}, identityNumber: {}. Lỗi: {}", email, kycRequest.getIdentityNumber(), errorMessage);
            throw new IllegalArgumentException(errorMessage);
        }
        KycResponse kycResponse = kycService.verifyIdentity(
                kycRequest.getIdentityNumber(),
                kycRequest.getFullName(),
                kycRequest.getDateOfBirth(),
                kycRequest.getGender().toString()
        );
        if (!kycResponse.isVerified()) {
            log.warn("[processKycAndSendOtp] KYC thất bại cho email: {}, identityNumber: {}. Lý do: {}", email, kycRequest.getIdentityNumber(), kycResponse.getMessage());
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
                log.error("[processKycAndSendOtp] Không gửi được tin nhắn đến Kafka cho email: {}, otp: {}", email, otp);
                otpCacheService.clearOtp(email);
                registrationCacheService.clearRegistrationData(email);
                throw new BusinessException(getMessage(MessageKeys.KAFKA_FAILED));
            }
            log.info("[processKycAndSendOtp] Đã gửi OTP tới email: {} sau khi xác minh KYC, otp: {}", email, otp);
            return new ApiResponseWrapper<>(HttpStatus.OK.value(),
                    getMessage(MessageKeys.OTP_SENT),
                    "KYC thành công. OTP đã được gửi đến email của bạn.");
        } catch (Exception e) {
            log.error("[processKycAndSendOtp] Gửi OTP thất bại cho email: {}, otp: {}. Lỗi: {}", email, otp, e.getMessage(), e);
            otpCacheService.clearOtp(email);
            registrationCacheService.clearRegistrationData(email);
            throw new BusinessException(getMessage(MessageKeys.OTP_SEND_FAILED));
        }
    }

    @Override
    public ApiResponseWrapper<?> reSendOtp(String email) {
        log.info("[reSendOtp] Bắt đầu gửi lại OTP cho email: {}", email);
        RegisterCustomerDTO registerData = registrationCacheService.getRegistrationData(email);
        if (registerData == null) {
            log.error("[reSendOtp] Không tìm thấy dữ liệu đăng ký cho email: {}", email);
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
                log.error("[reSendOtp] Không gửi được tin nhắn đến Kafka cho email: {}, otp: {}", email, otp);
                otpCacheService.clearOtp(email);
                registrationCacheService.clearRegistrationData(email);
                throw new BusinessException(getMessage(MessageKeys.KAFKA_FAILED));
            }
            log.info("[reSendOtp] Đã gửi lại OTP tới email: {}, otp: {}", email, otp);
            return new ApiResponseWrapper<>(HttpStatus.OK.value(),
                    getMessage(MessageKeys.OTP_SENT),
                    "KYC thành công. OTP đã được gửi đến email của bạn.");
        } catch (Exception e) {
            log.error("[reSendOtp] Gửi lại OTP thất bại cho email: {}, otp: {}. Lỗi: {}", email, otp, e.getMessage(), e);
            otpCacheService.clearOtp(email);
            registrationCacheService.clearRegistrationData(email);
            throw new BusinessException(getMessage(MessageKeys.OTP_SEND_FAILED));
        }
    }

    @Override
    @Transactional
    public ApiResponseWrapper<?> confirmRegister(String email, String otp) {
        log.info("[confirmRegister] Xác nhận đăng ký cho email: {}, otp: {}", email, otp);
        if (!otpCacheService.isValidOtp(email, otp)) {
            log.error("[confirmRegister] OTP không hợp lệ cho email: {}, otp: {}", email, otp);
            throw new IllegalArgumentException(getMessage(MessageKeys.INVALID_OTP));
        }
        RegisterCustomerDTO request = registrationCacheService.getRegistrationData(email);
        if (request == null) {
            log.error("[confirmRegister] Không tìm thấy dữ liệu đăng ký cho email: {}", email);
            throw new BusinessException(getMessage(MessageKeys.REGISTRATION_DATA_NOT_FOUND));
        }
        KycRequest kycData = registrationCacheService.getKycData(email);
        if (kycData == null) {
            log.error("[confirmRegister] Không tìm thấy dữ liệu KYC cho email: {}", email);
            throw new BusinessException(getMessage(MessageKeys.KYC_DATA_NOT_FOUND));
        }
        ApiResponseWrapper<?> response = completeRegistration(request, kycData);
        otpCacheService.clearOtp(email);
        registrationCacheService.clearRegistrationData(email);
        log.info("[confirmRegister] Đăng ký thành công cho email: {}, username: {}", email, request.getUsername());
        return response;
    }

    @Transactional
    private ApiResponseWrapper<?> completeRegistration(RegisterCustomerDTO request, KycRequest kycData) {
        log.info("[completeRegistration] Bắt đầu hoàn tất đăng ký cho email: {}, username: {}", request.getEmail(), request.getUsername());
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
            log.info("[completeRegistration] Đã lưu khách hàng với userId: {}, email: {}", savedCustomer.getUserId(), savedCustomer.getEmail());
            CoreCustomerDTO coreCustomerDTO = CoreCustomerDTO.builder()
                    .cifCode(savedCustomer.getCifCode())
                    .status(savedCustomer.getStatus().toString())
                    .build();
            log.info("[completeRegistration] Đồng bộ với core banking cho CIF: {}, userId: {}", savedCustomer.getCifCode(), savedCustomer.getUserId());
            CoreResponse coreResponse = coreBankingClient.syncCustomer(coreCustomerDTO);
            if (!coreResponse.isSuccess()) {
                log.error("[completeRegistration] Đồng bộ core banking thất bại cho CIF: {}, userId: {}. Lỗi: {}", savedCustomer.getCifCode(), savedCustomer.getUserId(), coreResponse.getMessage());
                throw new BusinessException(getMessage(MessageKeys.CORE_BANKING_SYNC_FAILED, coreResponse.getMessage()));
            }
            PaymentCreateDTO paymentCreateDTO = PaymentCreateDTO.builder()
                    .cifCode(savedCustomer.getCifCode())
                    .build();
            PaymentRequestResponse paymentResponse = customerCommonService.createPaymentInit(paymentCreateDTO);
            log.info("[completeRegistration] Đã tạo tài khoản thanh toán với CifCode: {}", paymentResponse.getCifCode());
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
            log.info("[completeRegistration] Đăng ký thành công cho email: {}, userId: {}", request.getEmail(), savedCustomer.getUserId());
            return new ApiResponseWrapper<>(HttpStatus.OK.value(),
                    getMessage(MessageKeys.REGISTER_SUCCESSFULLY),
                    toCustomerResponse(savedCustomer));
        } catch (Exception e) {
            log.error("[completeRegistration] Đăng ký thất bại, xóa user Keycloak ID: {}. Lỗi: {}", userId, e.getMessage(), e);
            deleteKeycloakUser(userId);
            throw new BusinessException(getMessage(MessageKeys.REGISTER_FAILED, e.getMessage())) {
            };
        }
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

    private String createKeycloakUser(RegisterCustomerDTO request) {
        log.info("[createKeycloakUser] Bắt đầu tạo user Keycloak với username: {}, email: {}", request.getUsername(), request.getEmail());
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
            log.info("[createKeycloakUser] Kết quả trả về từ Keycloak: status={}, body={}, username: {}", status, responseBody, request.getUsername());
            if (status == 201) {
                String userId = CreatedResponseUtil.getCreatedId(response);
                try {
                    RoleRepresentation role = keycloak.realm(realm).roles().get("CUSTOMER").toRepresentation();
                    keycloak.realm(realm).users().get(userId).roles().realmLevel().add(Collections.singletonList(role));
                    log.info("[createKeycloakUser] Đã tạo user Keycloak thành công với ID: {}, username: {} và gán role CUSTOMER", userId, request.getUsername());
                } catch (Exception e) {
                    log.error("[createKeycloakUser] Lỗi khi gán role CUSTOMER cho user Keycloak ID: {}, username: {}. Lỗi: {}", userId, request.getUsername(), e.getMessage(), e);
                    keycloak.realm(realm).users().get(userId).remove();
                    throw new BusinessException(getMessage(MessageKeys.KEYCLOAK_ROLE_FAILED, e.getMessage()));
                }
                log.info("[createKeycloakUser] Hoàn tất tạo user Keycloak với ID: {}, username: {}", userId, request.getUsername());
                return userId;
            } else if (status == 400) {
                log.error("[createKeycloakUser] Lỗi 400 - Request không hợp lệ khi tạo user Keycloak cho username: {}", request.getUsername());
                throw new IllegalArgumentException(getMessage(MessageKeys.INVALID_REQUEST));
            } else if (status == 401) {
                log.error("[createKeycloakUser] Lỗi 401 - Không có quyền truy cập khi tạo user Keycloak cho username: {}", request.getUsername());
                throw new BusinessException(getMessage(MessageKeys.UNAUTHORIZED_ACCESS));
            } else if (status == 409) {
                log.error("[createKeycloakUser] Lỗi 409 - User đã tồn tại trên Keycloak với username: {}", request.getUsername());
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
                    log.error("[createKeycloakUser] Lỗi không xác định từ Keycloak khi tạo user: {}, email: {}. Lỗi: {}", request.getUsername(), request.getEmail(), errorMessage);
                    throw new BusinessException(getMessage(MessageKeys.KEYCLOAK_ERROR, errorMessage, status));
                } catch (Exception e) {
                    log.error("[createKeycloakUser] Lỗi khi parse response từ Keycloak cho user: {}, email: {}. Lỗi: {}", request.getUsername(), request.getEmail(), e.getMessage(), e);
                    throw new BusinessException(getMessage(MessageKeys.KEYCLOAK_PARSE_ERROR, status, responseBody));
                }
            }
        } catch (Exception e) {
            log.error("[createKeycloakUser] Lỗi ngoại lệ khi tạo user Keycloak cho username: {}, email: {}. Lỗi: {}", request.getUsername(), request.getEmail(), e.getMessage(), e);
            throw new BusinessException(getMessage(MessageKeys.KEYCLOAK_CREATE_FAILED, e.getMessage()));
        }
    }

    private void deleteKeycloakUser(String userId) {
        log.info("[deleteKeycloakUser] Bắt đầu xóa user Keycloak với ID: {}", userId);
        try (Keycloak keycloak = KeycloakBuilder.builder()
                .serverUrl(keycloakUrl)
                .realm(realm)
                .clientId(clientId)
                .clientSecret(clientSecret)
                .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
                .build()) {
            keycloak.realm(realm).users().get(userId).remove();
            log.info("[deleteKeycloakUser] Đã xóa user Keycloak với ID: {} thành công", userId);
        } catch (Exception e) {
            log.error("[deleteKeycloakUser] Lỗi khi xóa user Keycloak với ID: {}. Lỗi: {}", userId, e.getMessage(), e);
        }
    }

    @Override
    public ApiResponseWrapper<?> updateCustomer(UpdateCustomerDTO request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUserId = authentication.getName();
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
        String targetUserId = isAdmin && request.getUserId() != null ? request.getUserId() : currentUserId;
        log.info("[updateCustomer] Bắt đầu cập nhật thông tin cho userId: {} (thực hiện bởi: {}, isAdmin: {})", targetUserId, currentUserId, isAdmin);
        Optional<Customer> customerOpt = customerRepository.findByUserId(targetUserId);
        if (customerOpt.isEmpty()) {
            log.error("[updateCustomer] Không tìm thấy khách hàng với userId: {} (thực hiện bởi: {})", targetUserId, currentUserId);
            throw new EntityNotFoundException(getMessage(MessageKeys.USER_NOT_FOUND));
        }
        Customer customer = customerOpt.get();
        if (!isAdmin && !customer.getUserId().equals(currentUserId)) {
            log.warn("[updateCustomer] Người dùng {} cố truy cập trái phép userId: {}", currentUserId, targetUserId);
            throw new BusinessException(getMessage(MessageKeys.UNAUTHORIZED_ACCESS));
        }
        if (customer.getStatus() != CustomerStatus.ACTIVE) {
            log.warn("[updateCustomer] Tài khoản userId: {} không ở trạng thái ACTIVE", targetUserId);
            throw new BusinessException(getMessage(MessageKeys.ACCOUNT_NOT_ACTIVE));
        }
        if (request.getFullName() != null) customer.setFullName(request.getFullName());
        if (request.getAddress() != null) customer.setAddress(request.getAddress());
        if (request.getGender() != null) customer.setGender(request.getGender());
        if (request.getDateOfBirth() != null) customer.setDateOfBirth(request.getDateOfBirth());
        if (request.getEmail() != null) {
            Optional<Customer> emailOwner = customerRepository.findByEmail(request.getEmail());
            if (emailOwner.isPresent() && !customer.getEmail().equals(request.getEmail())) {
                log.error("[updateCustomer] Email đã tồn tại: {}, userId: {}", request.getEmail(), targetUserId);
                throw new BusinessException(getMessage(MessageKeys.EMAIL_EXISTS));
            }
            customer.setEmail(request.getEmail());
        }
        if (request.getPhoneNumber() != null) {
            Optional<Customer> phoneNumberOwner = customerRepository.findByPhoneNumber(request.getPhoneNumber());
            if (phoneNumberOwner.isPresent() && !customer.getPhoneNumber().equals(request.getPhoneNumber())) {
                log.error("[updateCustomer] Số điện thoại đã tồn tại: {}, userId: {}", request.getPhoneNumber(), targetUserId);
                throw new BusinessException(getMessage(MessageKeys.PHONE_EXISTS));
            }
            customer.setPhoneNumber(request.getPhoneNumber());
        }
        updateUserInKeycloak(customer.getUserId(), request);
        customerRepository.save(customer);
        log.info("[updateCustomer] Cập nhật thông tin thành công cho userId: {} (thực hiện bởi: {})", targetUserId, currentUserId);
        return new ApiResponseWrapper<>(HttpStatus.OK.value(), getMessage(MessageKeys.SUCCESS_UPDATE), customer);
    }

    private void updateUserInKeycloak(String userId, UpdateCustomerDTO request) {
        log.info("[updateUserInKeycloak] Bắt đầu cập nhật user Keycloak với ID: {}, email mới: {}", userId, request.getEmail());
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
            log.info("[updateUserInKeycloak] Đã cập nhật user Keycloak với ID: {} thành công, email mới: {}", userId, request.getEmail());
        } catch (Exception e) {
            log.error("[updateUserInKeycloak] Lỗi khi cập nhật user Keycloak với ID: {}, email mới: {}. Lỗi: {}", userId, request.getEmail(), e.getMessage(), e);
            throw new BusinessException(getMessage(MessageKeys.KEYCLOAK_UPDATE_FAILED, e.getMessage()));
        }
    }

    @Override
    public void sentEmailForgotPassword(String email) {
        log.info("[sentEmailForgotPassword] Yêu cầu gửi email quên mật khẩu cho: {}", email);
        Customer customer = customerRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.error("[sentEmailForgotPassword] Không tìm thấy khách hàng với email: {}", email);
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
            log.info("[sentEmailForgotPassword] Đã gửi email reset mật khẩu cho {}, userId: {}", email, customer.getUserId());
        } catch (Exception e) {
            log.error("[sentEmailForgotPassword] Gửi email reset thất bại cho email: {}, userId: {}. Lỗi: {}", email, customer.getUserId(), e.getMessage(), e);
            throw new BusinessException(getMessage(MessageKeys.EMAIL_SEND_FAILED, e.getMessage()));
        }
    }

    @Override
    public ApiResponseWrapper<?> resetPassword(ResetPasswordDTO request) {
        log.info("[resetPassword] Bắt đầu reset mật khẩu cho token: {}", request.getToken());
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            log.error("[resetPassword] Xác nhận mật khẩu không khớp cho token: {}", request.getToken());
            throw new IllegalArgumentException(getMessage(MessageKeys.INVALID_CONFIRM_PASSWORD));
        }
        Customer customer = customerRepository.findByResetToken(request.getToken())
                .orElseThrow(() -> {
                    log.error("[resetPassword] Token hết hạn hoặc không hợp lệ: {}", request.getToken());
                    return new EntityNotFoundException(getMessage(MessageKeys.EXPIRED_TOKEN));
                });
        if (customer.getResetTokenExpiry() == null || customer.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
            log.error("[resetPassword] Token hết hạn cho email: {}, userId: {}", customer.getEmail(), customer.getUserId());
            throw new BusinessException(getMessage(MessageKeys.EXPIRED_TOKEN));
        }
        updateKeycloakPassword(customer.getUserId(), request.getNewPassword());
        customer.setResetToken(null);
        customer.setResetTokenExpiry(null);
        customerRepository.save(customer);
        log.info("[resetPassword] Đặt lại mật khẩu thành công cho userId: {}, email: {}", customer.getUserId(), customer.getEmail());
        return new ApiResponseWrapper<>(HttpStatus.OK.value(), getMessage(MessageKeys.PASSWORD_RESET_SUCCESS), null);
    }

    @Override
    public CustomerListResponse getCustomerList(int page, int size, String keyword) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUserId = authentication.getName();
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin) {
            log.warn("[getCustomerList] Người dùng không có quyền ADMIN, userId: {}", currentUserId);
            throw new BusinessException(getMessage(MessageKeys.ADMIN_ONLY));
        }
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Customer> customerPage;
        if (keyword != null && !keyword.trim().isEmpty()) {
            log.info("[getCustomerList] Tìm kiếm khách hàng với từ khóa: {}, userId: {}", keyword, currentUserId);
            customerPage = customerRepository.searchCustomers(keyword.trim(), pageable);
        } else {
            log.info("[getCustomerList] Lấy danh sách tất cả khách hàng, userId: {}", currentUserId);
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
        log.info("[getCustomerList] Trả về {} khách hàng, trang {}/{} cho userId: {}", customerResponses.size(), response.getCurrentPage() + 1, response.getTotalPages(), currentUserId);
        return response;
    }

    @Override
    public CustomerResponse getCustomerDetail(String userId) {
        log.info("[getCustomerDetail] Lấy thông tin khách hàng với userId: {}", userId);
        Optional<Customer> customerOpt = customerRepository.findByUserId(userId);
        if (customerOpt.isEmpty()) {
            log.error("[getCustomerDetail] Không tìm thấy khách hàng với userId: {}", userId);
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
        log.info("[getCustomerDetailByCifCode] Lấy thông tin khách hàng với cifCode: {} (thực hiện bởi: {}, isAdmin: {})", cifCode, currentUserId, isAdmin);
        Optional<Customer> customerOpt = customerRepository.findByCifCode(cifCode);
        if (customerOpt.isEmpty()) {
            log.error("[getCustomerDetailByCifCode] Không tìm thấy khách hàng với cifCode: {} (thực hiện bởi: {})", cifCode, currentUserId);
            throw new EntityNotFoundException(getMessage(MessageKeys.USER_NOT_FOUND));
        }
        Customer customer = customerOpt.get();
        if (!isAdmin && !customer.getUserId().equals(currentUserId)) {
            log.warn("[getCustomerDetailByCifCode] Người dùng {} cố truy cập trái phép cifCode: {}", currentUserId, cifCode);
            throw new BusinessException(getMessage(MessageKeys.UNAUTHORIZED_ACCESS));
        }
        return toCustomerResponse(customer);
    }

    @Override
    public ApiResponseWrapper<?> updateCustomerPassword(ChangePasswordDTO request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUserId = authentication.getName();
        log.info("[updateCustomerPassword] Bắt đầu đổi mật khẩu cho userId: {}", currentUserId);
        Optional<Customer> customerOpt = customerRepository.findByUserId(currentUserId);
        if (customerOpt.isEmpty()) {
            log.error("[updateCustomerPassword] Không tìm thấy khách hàng với userId: {}", currentUserId);
            throw new EntityNotFoundException(getMessage(MessageKeys.USER_NOT_FOUND));
        }
        Customer customer = customerOpt.get();
        if (customer.getStatus() != CustomerStatus.ACTIVE) {
            log.warn("[updateCustomerPassword] Tài khoản userId: {} không ở trạng thái ACTIVE", currentUserId);
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
                log.error("[updateCustomerPassword] Mật khẩu hiện tại không đúng cho userId: {}, username: {}", currentUserId, customer.getUsername());
                throw new IllegalArgumentException(getMessage(MessageKeys.INVALID_CURRENT_PASSWORD));
            }
        } catch (HttpClientErrorException e) {
            log.error("[updateCustomerPassword] Xác thực mật khẩu hiện tại thất bại cho userId: {}, username: {}. Lỗi: {}", currentUserId, customer.getUsername(), e.getMessage(), e);
            throw new IllegalArgumentException(getMessage(MessageKeys.INVALID_CURRENT_PASSWORD));
        } catch (Exception e) {
            log.error("[updateCustomerPassword] Lỗi xác thực mật khẩu hiện tại cho userId: {}, username: {}. Lỗi: {}", currentUserId, customer.getUsername(), e.getMessage(), e);
            throw new BusinessException(getMessage(MessageKeys.PASSWORD_VERIFICATION_FAILED));
        }
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            log.error("[updateCustomerPassword] Xác nhận mật khẩu mới không khớp cho userId: {}, username: {}", currentUserId, customer.getUsername());
            throw new IllegalArgumentException(getMessage(MessageKeys.INVALID_CONFIRM_PASSWORD));
        }
        updateKeycloakPassword(currentUserId, request.getNewPassword());
        log.info("[updateCustomerPassword] Đổi mật khẩu thành công cho userId: {}, username: {}", currentUserId, customer.getUsername());
        return new ApiResponseWrapper<>(HttpStatus.OK.value(), getMessage(MessageKeys.SUCCESS_UPDATE), null);
    }

    private void updateKeycloakPassword(String userId, String newPassword) {
        log.info("[updateKeycloakPassword] Bắt đầu cập nhật mật khẩu Keycloak cho userId: {}", userId);
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
            log.info("[updateKeycloakPassword] Đã cập nhật mật khẩu Keycloak thành công cho userId: {}", userId);
        } catch (Exception e) {
            log.error("[updateKeycloakPassword] Lỗi khi cập nhật mật khẩu Keycloak cho userId: {}. Lỗi: {}", userId, e.getMessage(), e);
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

        if (customer.getStatus().equals(newStatus)) {
            log.info("No status change needed for CIF: {}", customer.getCifCode());
            return new ApiResponseWrapper<>(HttpStatus.OK.value(), getMessage(MessageKeys.SUCCESS_UPDATE), customer.getStatus());
        }

        // Cập nhật trạng thái
        customer.setStatus(newStatus);
        customerRepository.save(customer);
        log.info("Updated customer status for CIF: {} from {} to {}", customer.getCifCode(), customer.getStatus(), newStatus);

        // Cập nhật KYC tương ứng với status
        KycStatus kycStatusToUpdate = switch (newStatus) {
            case ACTIVE -> KycStatus.VERIFIED;
            case SUSPENDED -> KycStatus.PENDING;
            case CLOSED -> KycStatus.REJECTED;
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

        log.info("KYC status updated to {} for CIF: {}", kycStatusToUpdate, customer.getCifCode());

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

        return new ApiResponseWrapper<>(HttpStatus.OK.value(), getMessage(MessageKeys.SUCCESS_UPDATE), customer.getStatus());
    }

    @Transactional
    @Override
    public KycResponse verifyKyc(String userId, KycRequest request) {
        try {
            log.info("[verifyKyc] Yêu cầu KYC từ userId: {}, identityNumber: {}", userId, request.getIdentityNumber());
            if (!isValidKycRequest(request)) {
                log.error("[verifyKyc] Dữ liệu KYC không hợp lệ cho userId: {}", userId);
                throw new IllegalArgumentException(getMessage(MessageKeys.INVALID_KYC_DATA));
            }

            Customer customer = customerRepository.findCustomerByUserId(userId);
            if (customer == null) {
                log.error("[verifyKyc] Không tìm thấy khách hàng với userId: {}", userId);
                throw new EntityNotFoundException(getMessage(MessageKeys.USER_NOT_FOUND));
            }

            Optional<KycProfile> kycProfileOpt = kycProfileRepository.findByCustomer(customer);
            if (kycProfileOpt.isPresent() && KycStatus.VERIFIED.equals(kycProfileOpt.get().getStatus())) {
                log.warn("[verifyKyc] Tài khoản đã được xác minh KYC cho userId: {}", userId);
                throw new BusinessException(getMessage(MessageKeys.ACCOUNT_ALREADY_VERIFIED));
            }

            String errorMessage = validateCustomerData(customer, request);
            if (errorMessage != null) {
                log.error("[verifyKyc] Dữ liệu KYC không khớp cho userId: {}. Lỗi: {}", userId, errorMessage);
                throw new IllegalArgumentException(errorMessage);
            }
            KycProfile kycProfile = kycProfileOpt.get();
            kycProfile.setStatus(KycStatus.PENDING);
            kycProfile.setReason("");

//            // Lưu thông tin KYC với trạng thái PENDING
//            KycProfile kycProfile = KycProfile.builder()
//                    .status(KycStatus.PENDING)
//                    .identityNumber(request.getIdentityNumber())
//                    .fullName(request.getFullName())
//                    .dateOfBirth(request.getDateOfBirth())
//                    .gender(request.getGender().toString())
//                    .createdAt(LocalDateTime.now())
//                    .build();
//            kycProfile.setCustomer(customer);
//            kycProfileRepository.save(kycProfile);

            // Lưu lịch sử KYC
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

            // Gửi thông báo KYC đang chờ duyệt
            MailMessageDTO mailMessage = new MailMessageDTO();
            mailMessage.setSubject("Yêu cầu KYC đang được xem xét");
            mailMessage.setRecipient(customer.getEmail());
            mailMessage.setRecipientName(customer.getFullName());
            mailMessage.setBody("Yêu cầu KYC của bạn đã được gửi và đang chờ admin duyệt. Chúng tôi sẽ thông báo kết quả sớm nhất.");
            streamBridge.send("mail-kyc-pending-out-0", mailMessage);

            KycResponse kycResponse = new KycResponse();
            kycResponse.setVerified(false);
            kycResponse.setStatus(KycStatus.PENDING);
            kycResponse.setMessage("Yêu cầu KYC đã được gửi, đang chờ admin duyệt");
            kycResponse.setDetails("{\"status\": \"pending\", \"submitted_at\": \"" + LocalDateTime.now() + "\"}");

            log.info("[verifyKyc] Yêu cầu KYC cho userId: {} đã được lưu với trạng thái PENDING", userId);
            return kycResponse;
        } catch (Exception e) {
            log.error("[verifyKyc] Gửi yêu cầu KYC thất bại cho userId: {}. Lỗi: {}", userId, e.getMessage(), e);
            throw new BusinessException(getMessage(MessageKeys.KYC_VERIFICATION_FAILED, e.getMessage()));
        }
    }

    @Transactional
    @Override
    public KycResponse approveKyc(String cifCode, KycStatus status, String reason) {
        log.info("[approveKyc] Bắt đầu duyệt KYC cho cifCode: {}, status: {}", cifCode, status);
        Optional<Customer> customerOpt = customerRepository.findByCifCode(cifCode);
        if (customerOpt.isEmpty()) {
            log.error("[approveKyc] Không tìm thấy khách hàng với cifCode: {}", cifCode);
            throw new EntityNotFoundException(getMessage(MessageKeys.USER_NOT_FOUND));
        }

        Customer customer = customerOpt.get();
        Optional<KycProfile> kycProfileOpt = kycProfileRepository.findByCustomer(customer);
        if (kycProfileOpt.isEmpty()) {
            log.error("[approveKyc] Không tìm thấy hồ sơ KYC cho cifCode: {}", cifCode);
            throw new EntityNotFoundException(getMessage(MessageKeys.KYC_DATA_NOT_FOUND));
        }

        KycProfile kycProfile = kycProfileOpt.get();
        if (!KycStatus.PENDING.equals(kycProfile.getStatus())) {
            log.warn("[approveKyc] Hồ sơ KYC của cifCode: {} không ở trạng thái PENDING", cifCode);
            throw new BusinessException(getMessage(MessageKeys.KYC_NOT_PENDING));
        }

        kycProfile.setStatus(status);
        kycProfile.setUpdatedAt(LocalDateTime.now());
        kycProfile.setReason(status == KycStatus.REJECTED ? reason : null);
        kycProfileRepository.save(kycProfile);

        // Lưu lịch sử KYC
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

        // Cập nhật trạng thái khách hàng
        customer.setStatus(status == KycStatus.VERIFIED ? CustomerStatus.ACTIVE : CustomerStatus.SUSPENDED);
        customerRepository.save(customer);

        // Đồng bộ với core banking
        CoreCustomerDTO coreCustomerDTO = CoreCustomerDTO.builder()
                .cifCode(customer.getCifCode())
                .status(customer.getStatus().toString())
                .build();
        CoreResponse coreResponse = coreBankingClient.syncCustomer(coreCustomerDTO);
        if (!coreResponse.isSuccess()) {
            log.error("[approveKyc] Đồng bộ core banking thất bại cho cifCode: {}. Lỗi: {}", cifCode, coreResponse.getMessage());
            throw new BusinessException(getMessage(MessageKeys.KYC_SYNC_FAILED, coreResponse.getMessage()));
        }

        // Gửi thông báo cho khách hàng
        MailMessageDTO mailMessage = new MailMessageDTO();
        mailMessage.setSubject(status == KycStatus.VERIFIED ? "KYC đã được duyệt" : "KYC bị từ chối");
        mailMessage.setRecipient(customer.getEmail());
        mailMessage.setRecipientName(customer.getFullName());
        mailMessage.setBody(status == KycStatus.VERIFIED ?
                "Yêu cầu KYC của bạn đã được duyệt thành công." :
                "Yêu cầu KYC của bạn bị từ chối. Lý do: " + (reason != null ? reason : "Không được cung cấp"));
        streamBridge.send("mail-kyc-result-out-0", mailMessage);

        KycResponse kycResponse = new KycResponse();
        kycResponse.setVerified(status == KycStatus.VERIFIED);
        kycResponse.setStatus(status);
        kycResponse.setMessage(status == KycStatus.VERIFIED ? "KYC được duyệt thành công" : "KYC bị từ chối: " + (reason != null ? reason : ""));
        kycResponse.setDetails("{\"updated_at\": \"" + LocalDateTime.now() + "\", \"reason\": \"" + (reason != null ? reason : "") + "\"}");

        log.info("[approveKyc] KYC cho cifCode: {} đã được duyệt với trạng thái: {}", cifCode, status);
        return kycResponse;
    }

    @Override
    public KycListResponse getPendingKycRequests(int page, int size) {
        log.info("[getPendingKycRequests] Lấy danh sách KYC đang chờ duyệt, page: {}, size: {}", page, size);
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<KycProfile> kycPage = kycProfileRepository.findByStatus(KycStatus.PENDING, pageable);

        List<KycResponse> kycResponses = kycPage.getContent().stream()
                .map(kyc -> {
                    KycResponse response = new KycResponse();
                    response.setVerified(false);
                    response.setStatus(kyc.getStatus());
                    response.setMessage("Đang chờ duyệt");
                    response.setDetails("{\"identityNumber\": \"" + kyc.getIdentityNumber() + "\", \"fullName\": \"" + kyc.getFullName() + "\", \"submitted_at\": \"" + kyc.getCreatedAt() + "\"}");
                    return response;
                })
                .collect(Collectors.toList());

        KycListResponse response = new KycListResponse();
        response.setKycRequests(kycResponses);
        response.setTotalElements(kycPage.getTotalElements());
        response.setTotalPages(kycPage.getTotalPages());
        response.setCurrentPage(kycPage.getNumber());
        log.info("[getPendingKycRequests] Trả về {} yêu cầu KYC, trang {}/{}", kycResponses.size(), response.getCurrentPage() + 1, response.getTotalPages());
        return response;
    }

    @Override
    public KycStatisticsResponse getKycStatistics(LocalDate startDate, LocalDate endDate) {
        log.info("[getKycStatistics] Lấy thống kê KYC từ {} đến {}", startDate, endDate);
        KycStatisticsResponse response = new KycStatisticsResponse();

        if (startDate == null) startDate = LocalDate.now().minusDays(30); // Mặc định 30 ngày
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

        log.info("[getKycStatistics] Thống kê KYC: total={}, successful={}, failed={}, pending={}", total, successful, failed, pending);
        return response;
    }

    @Override
    public CustomerGrowthResponse getCustomerGrowth(LocalDate startDate, LocalDate endDate) {
        log.info("[getCustomerGrowth] Lấy thống kê tăng trưởng khách hàng từ {} đến {}", startDate, endDate);
        CustomerGrowthResponse response = new CustomerGrowthResponse();

        if (startDate == null) startDate = LocalDate.now().minusDays(30); // Mặc định 30 ngày
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

        log.info("[getCustomerGrowth] Tăng trưởng khách hàng: newCustomers={}, growthRate={}%", newCustomers, growthRate);
        return response;
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