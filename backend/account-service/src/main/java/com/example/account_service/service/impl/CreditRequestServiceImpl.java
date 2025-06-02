package com.example.account_service.service.impl;

import com.example.account_service.dto.request.CreditRequestCreateDTO;
import com.example.account_service.dto.request.CreditRequestConfirmDTO;
import com.example.account_service.dto.request.PaymentCreateDTO;
import com.example.account_service.dto.request.SavingCreateDTO;
import com.example.account_service.dto.response.AccountCreateReponse;
import com.example.account_service.dto.response.CreditRequestReponse;
import com.example.account_service.entity.Account;
import com.example.account_service.entity.CreditRequest;
import com.example.account_service.exception.AppException;
import com.example.account_service.exception.ErrorCode;
import com.example.account_service.mapper.AccountMapper;
import com.example.account_service.repository.AccountRepository;
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
import com.example.common_service.services.CommonServiceCore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class CreditRequestServiceImpl implements CreditRequestService {
    private final AccountRepository accountRepository;

    @DubboReference(timeout = 5000)
    private final CommonService commonService;

    @DubboReference(timeout = 5000)
    private final CommonServiceCore commonServiceCore;

    private final CreditRequestRepository creditRequestRepository;

    private final AccountNumberUtils    accountNumberUtils;
    private final RestTemplate restTemplate;
    private final StreamBridge streamBridge;
    private final RedisTemplate<Object, Object> redisTemplate;

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
        
        // Tạo Credit Request trong database
        CreditRequest creditRequest = CreditRequest.builder()
                .cifCode(cifCode)
                .occupation(tempRequest.getOccupation())
                .cartTypeId(tempRequest.getCartTypeId())
                .monthlyIncome(tempRequest.getMonthlyIncome())
                .status(CreditRequestStatus.PENDING) // Chờ admin review
                .build();
        
        creditRequestRepository.save(creditRequest);
        
        // Cleanup temp data
        redisTemplate.delete(creditRequestConfirmDTO.getCreditRequestId());
        redisTemplate.delete("OTP:CREDIT:" + creditRequestConfirmDTO.getCreditRequestId());
        
        log.info("Credit request created successfully and waiting for admin review: {}", creditRequest.getId());
        
        // Return  Credit response with pending status (chờ admin review)
        return CreditRequestReponse.builder()
                .id(creditRequest.getId()) // Sử dụng temp key làm ID tạm thời
                .cifCode(creditRequest.getCifCode())
                .occupation(creditRequest.getOccupation())
                .cartTypeId(creditRequest.getCartTypeId())
                .monthlyIncome(creditRequest.getMonthlyIncome())
                .status(CreditRequestStatus.PENDING) // Trạng thái pending OTP
                .build();
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

        // Get cart type information
        String urlGetCard = coreBankingBaseUrl + "/get-cart-type/" + creditRequest.getCartTypeId();
        log.info("URL : "+ urlGetCard);
        CartTypeDTO cartTypeDTO = restTemplate.getForObject(urlGetCard, CartTypeDTO.class);
        log.info("Validating business rules for cartType: {}", cartTypeDTO);
        
        if (cartTypeDTO == null) {
            throw new AppException(ErrorCode.CORE_BANKING_SERVICE_ERROR);
        }

        // Validate age
        int age = Period.between(customerDTO.getDateOfBirth(), LocalDate.now()).getYears();
        log.info("Customer age: {} years", age);
        
        if (age < 21) {
            log.warn("Customer age is below required minimum: {}", age);
            throw new AppException(ErrorCode.AGE_INVALID);
        }

        // Validate income
        if (creditRequest.getMonthlyIncome().compareTo(cartTypeDTO.getMinimumIncome()) < 0) {
            log.warn("Monthly income is insufficient: {} < {}", 
                creditRequest.getMonthlyIncome(), cartTypeDTO.getMinimumIncome());
            throw new AppException(ErrorCode.INCOME_INVALID);
        }
        
        log.info("Business rules validation passed for credit request: {}", creditRequest.getId());
    }

    /**
     * Creates credit account in local database
     */
    private Account createCreditAccount(CreditRequest creditRequest) {
        Account account = Account.builder()
                .cifCode(creditRequest.getCifCode())
                .status(AccountStatus.ACTIVE)
                .accountType(AccountType.CREDIT)
                .build();

        account.setAccountNumber(accountNumberUtils.generateAccountNumber(account));
        accountRepository.save(account);
        
        log.info("Credit account created in local database: {}", account.getAccountNumber());
        return account;
    }

    /**
     * Creates credit account in core banking system
     */
    private void createCoreBankingCreditAccount(Account account, CreditRequest creditRequest) {
        coreCreditAccountDTO coreAccountDTO = coreCreditAccountDTO.builder()
                .accountNumber(account.getAccountNumber())
                .cifCode(account.getCifCode())
                .cartTypeId(creditRequest.getCartTypeId())
                .monthlyIncome(creditRequest.getMonthlyIncome())
                .build();

        log.info("Creating account in core banking system: {}", coreAccountDTO);
        
        String url = coreBankingBaseUrl + "/create-credit-account";
        try {
            restTemplate.postForObject(url, coreAccountDTO, Void.class);
            log.info("Account created successfully in core banking system");
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

