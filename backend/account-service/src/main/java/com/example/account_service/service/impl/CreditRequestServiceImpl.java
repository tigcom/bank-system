package com.example.account_service.service.impl;

import com.example.account_service.dto.request.CreditRequestCreateDTO;
import com.example.account_service.dto.request.CreditRequestConfirmDTO;
import com.example.account_service.dto.request.PaymentCreateDTO;
import com.example.account_service.dto.request.SavingCreateDTO;
import com.example.account_service.dto.response.AccountCreateReponse;
import com.example.account_service.dto.response.CicResponse;
import com.example.account_service.dto.response.CreditRequestReponse;
import com.example.account_service.dto.request.CreateSimpleAccountRequest;
import com.example.account_service.dto.request.UpdateBalanceRequest;
import com.example.account_service.entity.Account;
import com.example.account_service.entity.CreditAccount;
import com.example.account_service.entity.CreditCardType;
import com.example.account_service.entity.CreditRequest;
import com.example.account_service.exception.AppException;
import com.example.account_service.exception.ErrorCode;
import com.example.account_service.mapper.AccountMapper;
import com.example.account_service.repository.AccountRepository;
import com.example.account_service.repository.CreditAccountRepository;
import com.example.account_service.repository.CreditCardTypeRepository;
import com.example.account_service.repository.CreditRequestRepository;
import com.example.account_service.service.AccountService;
import com.example.account_service.service.CreditRequestService;
import com.example.account_service.utils.AccountNumberUtils;
import com.example.common_service.constant.AccountStatus;
import com.example.common_service.constant.AccountType;
import com.example.common_service.constant.CreditRequestStatus;
import com.example.common_service.constant.CustomerStatus;
import com.example.common_service.dto.*;
import com.example.common_service.services.CommonService;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.Period;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class CreditRequestServiceImpl implements CreditRequestService {
    private final AccountRepository accountRepository;
    private final CreditAccountRepository creditAccountRepository;
    private final CreditCardTypeRepository creditCardTypeRepository;

    @DubboReference(timeout = 5000)
    private final CommonService commonService;


    private final CreditRequestRepository creditRequestRepository;

    private final AccountNumberUtils    accountNumberUtils;
    private final RestTemplate restTemplate;
    private final StreamBridge streamBridge;
    private final RedisTemplate<Object, Object> redisTemplate;
    private static final String[] STABLE_OCCUPATIONS = {"Engineer", "Doctor", "Teacher", "Government Employee"};
    @Value("${core-banking.base-url:http://localhost:8083/corebanking}")
    private String coreBankingBaseUrl;

    @Override
    public CreditRequestReponse createCreditRequest(CreditRequestCreateDTO creditRequestCreateDTO) {
        log.info("Starting createCreditRequest with input: {}", creditRequestCreateDTO);

        CustomerDTO currentCustomer = getCurrentValidatedCustomer();
        validateCustomerForCreditRequest(currentCustomer);

        // Tạo temporary key để lưu thông tin request trước khi verify OTP
        String tempRequestKey = "TEMP_CREDIT_REQUEST:" + currentCustomer.getCifCode() + ":" + System.currentTimeMillis();

        // Lưu thông tin request vào Redis (expire sau 1 giờ)
        CreditRequestCreateDTO tempRequest = CreditRequestCreateDTO.builder()
                .occupation(creditRequestCreateDTO.getOccupation())
                .monthlyIncome(creditRequestCreateDTO.getMonthlyIncome())
                .cartTypeId(creditRequestCreateDTO.getCartTypeId())
                .build();

        redisTemplate.opsForValue().set(tempRequestKey, tempRequest, Duration.ofMinutes(60));

        // Tạo và gửi OTP
        String otp = generateAndStoreOTP(tempRequestKey);
        sendOTPEmail(currentCustomer, otp);

        log.info("OTP sent for credit request creation. Temp key: {}", tempRequestKey);

        // Trả về response với temp key để client có thể confirm OTP
        return CreditRequestReponse.builder()
                .id(tempRequestKey) // Sử dụng temp key làm ID tạm thời
                .cifCode(currentCustomer.getCifCode())
                .occupation(creditRequestCreateDTO.getOccupation())
                .cartTypeId(creditRequestCreateDTO.getCartTypeId())
                .monthlyIncome(creditRequestCreateDTO.getMonthlyIncome())
                .status(CreditRequestStatus.PENDING) // Trạng thái pending OTP
                .build();
    }

    @Override
    public void sendOTP(String tempRequestKey) {
        log.info("Resending OTP for temp request key: {}", tempRequestKey);

        // Kiểm tra temp request có tồn tại không
        Object tempRequest = redisTemplate.opsForValue().get(tempRequestKey);
        if (tempRequest == null) {
            throw new AppException(ErrorCode.CREDIT_REQUEST_NOTEXISTED);
        }

        // Lấy thông tin customer từ temp key
        String cifCode = extractCifFromTempKey(tempRequestKey);
        log.info("OTP sent for credit request. CIF code: {}", cifCode);
        CustomerDTO customerDTO = commonService.getCustomerByCifCode(cifCode);

        if (customerDTO == null) {
            throw new AppException(ErrorCode.CUSTOMER_NOT_FOUND);
        }

        String otp = generateAndStoreOTP(tempRequestKey);
        sendOTPEmail(customerDTO, otp);

        log.info("OTP resent successfully for temp request: {}", tempRequestKey);
    }

    @Override
    public CreditRequestReponse confirmOTPAndCreateAccount(CreditRequestConfirmDTO creditRequestConfirmDTO) {
        log.info("Confirming OTP and creating credit request: {}", creditRequestConfirmDTO.getCreditRequestId());

        // Validate OTP
        CreditRequestCreateDTO tempRequest = validateOTPAndGetTempRequest(creditRequestConfirmDTO);

        // Lấy thông tin customer
        String cifCode = extractCifFromTempKey(creditRequestConfirmDTO.getCreditRequestId());
        log.info("Cif Code : {}", cifCode);
        CustomerDTO customerDTO = commonService.getCustomerByCifCode(cifCode);
        /// Check CIC Gia Lap
        log.info("CCCD: " +customerDTO.getIdentityNumber());
        CicResponse cicResponse = checkCIC(customerDTO.getIdentityNumber());
        // Tính điểm tín dụng tổng hợp
        int finalScore = calculateCreditScore(tempRequest.getMonthlyIncome(), tempRequest.getOccupation(), cicResponse.getCreditScore());
        //
        CreditRequest creditRequest = CreditRequest.builder()
                .cifCode(cifCode)
                .occupation(tempRequest.getOccupation())
                .cartTypeId(tempRequest.getCartTypeId())
                .monthlyIncome(tempRequest.getMonthlyIncome())
                .build();
        if(cicResponse.getDebtGroup()>=3)
        {
            // cap nhat trang thai rejected va send email tu choi -->>
            return autoRejectCreditRequest(creditRequest,creditRequestConfirmDTO.getCreditRequestId());
        }
        if (finalScore >= 80) {
            // cap nhat trang thai Approved va send email duyet the  -->>
            return   autoApproveCreditRequest(creditRequest,creditRequestConfirmDTO.getCreditRequestId());

        }
     else if (finalScore >= 50) {
            // cap nhat trang thai Pending voi cac truong hop dac biet va chuyen  sang admin duyệt -->>
            creditRequest.setStatus(CreditRequestStatus.PENDING);
            creditRequestRepository.save(creditRequest);
            redisTemplate.delete(creditRequestConfirmDTO.getCreditRequestId());
            redisTemplate.delete("OTP:CREDIT:" + creditRequestConfirmDTO.getCreditRequestId());
        return mapToDto(creditRequest);
    } else {
             return autoRejectCreditRequest(creditRequest,creditRequestConfirmDTO.getCreditRequestId());
    }
    }

    private CreditRequestReponse autoApproveCreditRequest(CreditRequest creditRequest,  String creditRequestId) {
        creditRequest.setStatus(CreditRequestStatus.APPROVED);
        creditRequestRepository.save(creditRequest);
        redisTemplate.delete(creditRequestId);
        redisTemplate.delete("OTP:CREDIT:" + creditRequestId);
        // tao luon credit
        Account account = createCreditAccount(creditRequest);
        createCoreBankingCreditAccount(account, creditRequest);
        // Gửi email thông báo phê duyệt
        sendApprovalNotification(creditRequest, account);

        log.info("Credit request approved and account created: {}", account.getAccountNumber());
        return mapToDto(creditRequest);
    }

    private CreditRequestReponse autoRejectCreditRequest(CreditRequest creditRequest, String creditRequestId) {
        creditRequest.setStatus(CreditRequestStatus.REJECTED);
        creditRequestRepository.save(creditRequest);
        // Cleanup temp data
        redisTemplate.delete(creditRequestId);
        redisTemplate.delete("OTP:CREDIT:" + creditRequestId);
        //gui email tu choi
        sendRejectionNotification(creditRequest, "Hiện tại quý khách chưa được hỗ trợ mở thẻ tín dụng. ");
        log.info("Credit request rejected: {}", creditRequest.getId());
        return mapToDto(creditRequest);
    }

    private int calculateCreditScore(BigDecimal monthlyIncome, String occupation, int creditScore) {
        int score = 0;
        if (Arrays.asList(STABLE_OCCUPATIONS).contains(occupation)) {
            score += 30;
        } else if (occupation.equalsIgnoreCase("Freelancer")) {
            score += 10;
        } else {
            score += 20; // Nghề khác
        }

        // Điểm dựa trên lương (30%)
        if (monthlyIncome.compareTo(BigDecimal.valueOf(15000000)) > 0) {
            score += 30;
        } else if (monthlyIncome.compareTo(BigDecimal.valueOf(8000000))>0) {
            score += 20;
        } else {
            score += 10;
        }

        // Điểm dựa trên CIC (40%)
        score += (creditScore / 1000.0) * 40; // Quy đổi điểm CIC (0-1000) thành 0-40

        return score;

    }

    private CicResponse checkCIC(String idNumber) {
        String url = "http://localhost:8089/api/cic/check";

        Map<String, String> request = new HashMap<>();
        request.put("idNumber", idNumber);
        request.put("name", "Nguyen Van A");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, String>> entity = new HttpEntity<>(request, headers);

        try {
            ResponseEntity<CicResponse> response = restTemplate.postForEntity(url, entity, CicResponse.class);
            log.info("Response from CIC : " + response.getBody());
            return response.getBody();
        } catch (RestClientException e) {
            log.info(e.getMessage());
            return null;
        }
    }
    @Override
    public AccountCreateReponse approveCreditRequest(String id) {
        log.info("Admin approving credit request with id: {}", id);

        CreditRequest creditRequest = getCreditRequestById(id);
        validateCreditRequestStatus(creditRequest, CreditRequestStatus.PENDING);

        // Validate business rules
        try {
            validateCreditRequestBusinessRules(creditRequest);
        } catch (AppException e) {
            // Nếu không đạt yêu cầu, tự động reject
           // rejectCreditRequestInternal(id, "Không đáp ứng yêu cầu: " + e.getMessage());
            // Throw exception để không tạo tài khoản
            throw e;
        }

        // Tạo tài khoản tín dụng
        Account account = createCreditAccount(creditRequest);
        createCoreBankingCreditAccount(account, creditRequest);

        // Update status
        creditRequest.setStatus(CreditRequestStatus.APPROVED);
        creditRequestRepository.save(creditRequest);

        // Gửi email thông báo phê duyệt
        sendApprovalNotification(creditRequest, account);

        log.info("Credit request approved and account created: {}", account.getAccountNumber());
        return buildAccountCreateResponse(account);
    }

    @Override
    public List<CreditRequestReponse> getAllCreditRequest() {
        return creditRequestRepository.findAll()
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    public CreditRequestReponse rejectCreditRequest(String id) {
        return rejectCreditRequestInternal(id, "Không đáp ứng yêu cầu về chính sách ngân hàng");
    }

    // === Private Helper Methods ===

    private CreditRequestReponse rejectCreditRequestInternal(String id, String reason) {
        log.info("Rejecting credit request with id: {}", id);

        CreditRequest creditRequest = getCreditRequestById(id);
        creditRequest.setStatus(CreditRequestStatus.REJECTED);
        creditRequestRepository.save(creditRequest);

        // Gửi email thông báo từ chối
        sendRejectionNotification(creditRequest, reason);

        log.info("Credit request rejected: {}", id);
        return mapToDto(creditRequest);
    }

    private void sendApprovalNotification(CreditRequest creditRequest, Account account) {
        try {
            CustomerDTO customer = commonService.getCustomerByCifCode(creditRequest.getCifCode());
            String urlGetCard = coreBankingBaseUrl + "/get-cart-type/" + creditRequest.getCartTypeId();
            CartTypeDTO cartTypeDTO = restTemplate.getForObject(urlGetCard, CartTypeDTO.class);

            CreditNotificationDTO notification = CreditNotificationDTO.builder()
                    .customerName(customer.getFullName())
                    .customerEmail(customer.getEmail())
                    .cardType(cartTypeDTO != null ? cartTypeDTO.getTypeName() : creditRequest.getCartTypeId())
                    .accountNumber(account.getAccountNumber())
                    .templateType("approval")
                    .subject("🎉 Chúc mừng! Yêu cầu thẻ tín dụng được phê duyệt - Ngân hàng ABC")
                    .build();

            streamBridge.send("send-credit-notification", notification);
            log.info("Approval notification sent to: {}", customer.getEmail());
        } catch (Exception e) {
            log.error("Failed to send approval notification", e);
        }
    }

    private void sendRejectionNotification(CreditRequest creditRequest, String reason) {
        try {
            CustomerDTO customer = commonService.getCustomerByCifCode(creditRequest.getCifCode());
            String urlGetCard = coreBankingBaseUrl + "/get-cart-type/" + creditRequest.getCartTypeId();
            CartTypeDTO cartTypeDTO = restTemplate.getForObject(urlGetCard, CartTypeDTO.class);

            CreditNotificationDTO notification = CreditNotificationDTO.builder()
                    .customerName(customer.getFullName())
                    .customerEmail(customer.getEmail())
                    .cardType(cartTypeDTO != null ? cartTypeDTO.getTypeName() : creditRequest.getCartTypeId())
                    .rejectionReason(reason)
                    .templateType("rejection")
                    .subject("Thông báo về yêu cầu thẻ tín dụng - Ngân hàng ABC")
                    .build();

            streamBridge.send("send-credit-notification", notification);
            log.info("Rejection notification sent to: {}", customer.getEmail());
        } catch (Exception e) {
            log.error("Failed to send rejection notification", e);
        }
    }

    private String extractCifFromTempKey(String tempKey) {
        // Format: TEMP_CREDIT_REQUEST:{cifCode}:{timestamp}
        String[] parts = tempKey.split(":");
        if (parts.length >= 3) {
            return parts[1];
        }
        throw new AppException(ErrorCode.CREDIT_REQUEST_NOTEXISTED);
    }

    private CreditRequestCreateDTO validateOTPAndGetTempRequest(CreditRequestConfirmDTO confirmDTO) {
        String keyOTP = "OTP:CREDIT:" + confirmDTO.getCreditRequestId();
        String storedOtp = (String) redisTemplate.opsForValue().get(keyOTP);
        log.info("Validating OTP for temp request: {}", confirmDTO.getCreditRequestId());

        if (storedOtp == null) {
            throw new AppException(ErrorCode.OTP_EXPIRED);
        }

        if (!storedOtp.equals(confirmDTO.getOtpCode())) {
            handleOTPFailure(confirmDTO.getCreditRequestId());
            throw new AppException(ErrorCode.INVALID_OTP);
        }

        // Lấy temp request

        CreditRequestCreateDTO tempRequest = (CreditRequestCreateDTO) redisTemplate.opsForValue().get(confirmDTO.getCreditRequestId());
        if (tempRequest == null) {
            throw new AppException(ErrorCode.CREDIT_REQUEST_NOTEXISTED);
        }

        log.info("OTP validated successfully for temp request: {}", confirmDTO.getCreditRequestId());
        return tempRequest;
    }

    private void handleOTPFailure(String tempRequestKey) {
        String keyFailCount = "OTP_FAIL_COUNT:CREDIT:" + tempRequestKey;
        String failStr = (String) redisTemplate.opsForValue().get(keyFailCount);
        int failCount = (failStr == null) ? 0 : Integer.parseInt(failStr);

        failCount++;
        redisTemplate.opsForValue().set(keyFailCount, String.valueOf(failCount), Duration.ofMinutes(5));

        if (failCount >= 3) {
            // Xóa temp request
            redisTemplate.delete(tempRequestKey);
            redisTemplate.delete("OTP:CREDIT:" + tempRequestKey);
            log.error("Credit request creation failed due to OTP entered incorrectly more than 3 times");
            throw new AppException(ErrorCode.OTP_WRONG_MANY);
        }
    }

    /**
     * Gets current authenticated customer with validation
     */
    private CustomerDTO getCurrentValidatedCustomer() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            log.error("No authentication found in security context");
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        String userId = authentication.getName();
        log.info("Authenticated userId: {}", userId);

        CustomerDTO currentCustomer = commonService.getCurrentCustomer(userId);
        log.info("Current Customer retrieved: {}", currentCustomer);
        return currentCustomer;
    }

    /**
     * Validates customer eligibility for credit request
     */
    private void validateCustomerForCreditRequest(CustomerDTO customer) {
        if (customer.getStatus() != CustomerStatus.ACTIVE) {
            log.warn("Customer status is not ACTIVE: {}", customer.getStatus());
            throw new AppException(ErrorCode.CUSTOMER_NOTACTIVE);
        }
        ///  check KYC status cua khach hang
    }

    /**
     * Gets credit request by ID with validation
     */
    private CreditRequest getCreditRequestById(String id) {
        return creditRequestRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("CreditRequest not found with id: {}", id);
                    return new AppException(ErrorCode.CREDIT_REQUEST_NOTEXISTED);
                });
    }

    /**
     * Validates credit request status
     */
    private void validateCreditRequestStatus(CreditRequest creditRequest, CreditRequestStatus expectedStatus) {
        if (creditRequest.getStatus() != expectedStatus) {
            throw new AppException(ErrorCode.CREDIT_REQUEST_STATUS_INVALID);
        }
    }

    /**
     * Generates OTP and stores in Redis
     */
    private String generateAndStoreOTP(String key) {
        String keyOTP = "OTP:CREDIT:" + key;
        String otp = String.valueOf(100000 + new Random().nextInt(900000));
        redisTemplate.opsForValue().set(keyOTP, otp, Duration.ofMinutes(10)); // OTP có hiệu lực 10 phút
        log.info("OTP generated and stored for key: {}", key);
        return otp;
    }

    /**
     * Sends OTP email to customer
     */
    private void sendOTPEmail(CustomerDTO customer, String otp) {
        MailMessageDTO mailMessageDTO = MailMessageDTO.builder()
                .recipientName(customer.getFullName())
                .recipient(customer.getEmail())
                .body(otp)
                .subject("Xác thực OTP - Yêu cầu thẻ tín dụng")
                .build();
        streamBridge.send("send-mail-html", mailMessageDTO); // Sử dụng HTML template
        log.info("OTP email sent to: {}", customer.getEmail());
    }

    /**
     * Validates credit request business rules (income, age, etc.)
     */
    private void validateCreditRequestBusinessRules(CreditRequest creditRequest) {
        CustomerDTO customerDTO = commonService.getCustomerByCifCode(creditRequest.getCifCode());
        log.info("Validating business rules for cifCode: {}", creditRequest.getCifCode());

        // Get credit card type from local repository
        CreditCardType creditCardType = creditCardTypeRepository.findById(creditRequest.getCartTypeId())
                .orElseThrow(() -> new AppException(ErrorCode.CORE_BANKING_SERVICE_ERROR));

        log.info("Validating business rules for credit card type: {}", creditCardType.getTypeName());

        // Validate age
        int age = Period.between(customerDTO.getDateOfBirth(), LocalDate.now()).getYears();
        log.info("Customer age: {} years", age);

        if (age < 21) {
            log.warn("Customer age is below required minimum: {}", age);
            throw new AppException(ErrorCode.AGE_INVALID);
        }

        // Validate income
        if (creditRequest.getMonthlyIncome().compareTo(creditCardType.getMinimumIncome()) < 0) {
            log.warn("Monthly income is insufficient: {} < {}",
                creditRequest.getMonthlyIncome(), creditCardType.getMinimumIncome());
            throw new AppException(ErrorCode.INCOME_INVALID);
        }

        log.info("Business rules validation passed for credit request: {}", creditRequest.getId());
    }

    /**
     * Creates credit account in local database
     */
    private Account createCreditAccount(CreditRequest creditRequest) {
        // Get credit card type from local repository
        CreditCardType creditCardType = creditCardTypeRepository.findById(creditRequest.getCartTypeId())
                .orElseThrow(() -> new AppException(ErrorCode.CORE_BANKING_SERVICE_ERROR));

        // Calculate credit limit based on income
        BigDecimal creditLimit = calculateCreditLimit(creditRequest.getMonthlyIncome(), creditCardType.getDefaultCreditLimit());

        CreditAccount creditAccount = CreditAccount.builder()
                .cifCode(creditRequest.getCifCode())
                .status(AccountStatus.ACTIVE)
                .accountType(AccountType.CREDIT)
                .creditLimit(creditLimit)
                .currentDebt(BigDecimal.ZERO)
                .creditCardType(creditCardType)
                .build();

        creditAccount.setAccountNumber(accountNumberUtils.generateAccountNumber(creditAccount));

        // Save credit account (Account will be saved automatically due to inheritance)
        CreditAccount savedAccount = creditAccountRepository.save(creditAccount);

        log.info("Credit account created in local database: {}", savedAccount.getAccountNumber());
        return savedAccount;
    }

    /**
     * Calculates credit limit based on monthly income
     */
    private BigDecimal calculateCreditLimit(BigDecimal monthlyIncome, BigDecimal defaultLimit) {
        if (monthlyIncome.compareTo(BigDecimal.valueOf(10000000)) < 0) {
            return defaultLimit.multiply(BigDecimal.valueOf(0.6)); // 60% default limit
        } else if (monthlyIncome.compareTo(BigDecimal.valueOf(20000000)) < 0) {
            return defaultLimit.multiply(BigDecimal.valueOf(0.8)); // 80% default limit
        } else {
            return defaultLimit; // 100% default limit
        }
    }

    /**
     * Creates simple account in core banking system (only for balance and transaction management)
     */
    private void createCoreBankingCreditAccount(Account account, CreditRequest creditRequest) {
        // Create simple account structure for Core Banking (only balance and status)
        String url = coreBankingBaseUrl + "/api/v1/accounts/simple/create";
        try {
            CreateSimpleAccountRequest request = CreateSimpleAccountRequest.builder()
                .accountNumber(account.getAccountNumber())
                    .status(account.getStatus())
                .build();

            log.info("Creating simple core banking account for credit: {}", request);
            restTemplate.postForObject(url, request, Void.class);

            log.info("Credit account created successfully in core banking system: {}", account.getAccountNumber());
        } catch (Exception e) {
            log.error("Failed to create account in core banking system", e);
            throw new AppException(ErrorCode.CORE_BANKING_SERVICE_ERROR);
        }
    }

    /**
     * Builds account create response
     */
    private AccountCreateReponse buildAccountCreateResponse(Account account) {
        return AccountCreateReponse.builder()
                .accountNumber(account.getAccountNumber())
                .cifCode(account.getCifCode())
                .id(account.getId())
                .accountType(account.getAccountType())
                .status(account.getStatus())
                .build();
    }

    /**
     * Maps CreditRequest entity to response DTO
     */
    private CreditRequestReponse mapToDto(CreditRequest request) {
        return CreditRequestReponse.builder()
                .id(request.getId())
                .cifCode(request.getCifCode())
                .occupation(request.getOccupation())
                .cartTypeId(request.getCartTypeId())
                .monthlyIncome(request.getMonthlyIncome())
                .status(request.getStatus())
                .build();
    }
}

