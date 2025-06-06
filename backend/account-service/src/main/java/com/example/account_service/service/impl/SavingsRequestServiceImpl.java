package com.example.account_service.service.impl;

import com.example.account_service.dto.request.ConfirmRequestDTO;
import com.example.account_service.dto.request.SavingRequestCreateDTO;
import com.example.account_service.dto.response.SavingsRequestResponse;
import com.example.account_service.entity.Account;
import com.example.account_service.entity.SavingsRequest;
import com.example.account_service.exception.AppException;
import com.example.account_service.exception.ErrorCode;
import com.example.account_service.repository.AccountRepository;
import com.example.account_service.repository.CreditRequestRepository;
import com.example.account_service.repository.SavingsRequestRepository;
import com.example.account_service.service.SavingRequestService;
import com.example.account_service.utils.AccountNumberUtils;
import com.example.common_service.constant.*;
import com.example.common_service.dto.*;
import com.example.common_service.dto.request.CreateAccountSavingRequest;
import com.example.common_service.dto.response.ApiResponse;
import com.example.common_service.dto.response.CoreTermDTO;
import com.example.common_service.services.CommonService;
import com.example.common_service.services.CommonServiceCore;
import com.example.common_service.services.transactions.CommonTransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Random;

@Service
@Slf4j
@RequiredArgsConstructor
public class SavingsRequestServiceImpl implements SavingRequestService {
    
    private final AccountRepository accountRepository;

    @DubboReference(timeout = 5000)
    private final CommonService commonService;

    @DubboReference(timeout = 5000)
    private final CommonServiceCore commonServiceCore;

    @DubboReference(timeout = 5000)
    private final CommonTransactionService commonTransactionService;

    private final CreditRequestRepository creditRequestRepository;
    private final AccountNumberUtils accountNumberUtils;
    private final RestTemplate restTemplate;
    private final SavingsRequestRepository savingsRequestRepository;
    private final StreamBridge streamBridge;
    private final RedisTemplate<Object, Object> redisTemplate;

    @Value("${core-banking.base-url:http://localhost:8083/corebanking}")
    private String coreBankingBaseUrl;

    @Override
    public SavingsRequestResponse CreateSavingRequest(SavingRequestCreateDTO savingRequestCreateDTO) {
        log.info("Starting createSavingRequest with input: {}", savingRequestCreateDTO);
        
        // Validate input
        validateSavingRequestInput(savingRequestCreateDTO);
        
        // Check account balance
        validateAccountBalance(savingRequestCreateDTO.getAccountNumberSource(), savingRequestCreateDTO.getInitialDeposit());
        
        // Get and validate customer
        CustomerDTO currentCustomer = getCurrentValidatedCustomer();
        
        // Tạo temporary key để lưu thông tin request trước khi verify OTP
        String tempRequestKey = "TEMP_SAVING_REQUEST:" + currentCustomer.getCifCode() + ":" + System.currentTimeMillis();
        
        // Lưu thông tin request vào Redis (expire sau 1 giờ)
        SavingRequestCreateDTO tempRequest = SavingRequestCreateDTO.builder()
                .accountNumberSource(savingRequestCreateDTO.getAccountNumberSource())
                .initialDeposit(savingRequestCreateDTO.getInitialDeposit())
                .term(savingRequestCreateDTO.getTerm())
                .build();
        
        redisTemplate.opsForValue().set(tempRequestKey, tempRequest, Duration.ofMinutes(60));
        
        // Tạo và gửi OTP
        String otp = generateAndStoreOTP(tempRequestKey);
        sendOTPEmail(currentCustomer, otp);
        
        log.info("OTP sent for saving request creation. Temp key: {}", tempRequestKey);
        
        // Trả về response với temp key để client có thể confirm OTP
        return SavingsRequestResponse.builder()
                .id(tempRequestKey) // Sử dụng temp key làm ID tạm thời
                .cifCode(currentCustomer.getCifCode())
                .accountNumberSource(savingRequestCreateDTO.getAccountNumberSource())
                .initialDeposit(savingRequestCreateDTO.getInitialDeposit())
                .term(savingRequestCreateDTO.getTerm())
                .status(SavingsRequestStatus.PENDING) // Trạng thái pending OTP
                .build();
    }

    @Override
    public void resendOTP(String tempRequestKey) {
        log.info("Resending OTP for temp request key: {}", tempRequestKey);
        
        // Kiểm tra temp request có tồn tại không
        Object tempRequest = redisTemplate.opsForValue().get(tempRequestKey);
        if (tempRequest == null) {
            throw new AppException(ErrorCode.SAVING_REQUEST_NOTEXISTED);
        }
        
        // Lấy thông tin customer từ temp key
        String cifCode = extractCifFromTempKey(tempRequestKey);
        log.info("Resending OTP for saving request. CIF code: {}", cifCode);
        CustomerDTO customerDTO = commonService.getCustomerByCifCode(cifCode);
        
        if (customerDTO == null) {
            throw new AppException(ErrorCode.CUSTOMER_NOT_FOUND);
        }

        String otp = generateAndStoreOTP(tempRequestKey);
        sendOTPEmail(customerDTO, otp);
        
        log.info("OTP resent successfully for temp request: {}", tempRequestKey);
    }

    @Override
    public SavingsRequestResponse confirmOTPAndCreateSavingAccount(ConfirmRequestDTO confirmRequestDTO) {
        log.info("Confirming OTP and creating saving account: {}", confirmRequestDTO.getSavingRequestID());
        
        // Validate OTP
        SavingRequestCreateDTO tempRequest = validateOTPAndGetTempRequest(confirmRequestDTO);
        
        // Lấy thông tin customer
        String cifCode = extractCifFromTempKey(confirmRequestDTO.getSavingRequestID());
        log.info("Creating saving account for CIF Code: {}", cifCode);
        CustomerDTO customerDTO = commonService.getCustomerByCifCode(cifCode);
        
        // Tạo Saving Account trực tiếp (không cần tạo SavingsRequest entity)
        Account account = processSavingAccountCreation(tempRequest, cifCode);
        
        // Cleanup temp data
        redisTemplate.delete(confirmRequestDTO.getSavingRequestID());
        redisTemplate.delete("OTP:SAVING:" + confirmRequestDTO.getSavingRequestID());
        
        log.info("Saving account created successfully: {}", account.getAccountNumber());
        
        // Trả về response với thông tin account đã tạo
        return SavingsRequestResponse.builder()
                .id(account.getId())
                .cifCode(cifCode)
                .accountNumberSource(tempRequest.getAccountNumberSource())
                .initialDeposit(tempRequest.getInitialDeposit())
                .term(tempRequest.getTerm())
                .status(SavingsRequestStatus.APPROVED) // Đã tạo thành công
                .build();
    }

    /**
     * Extracts CIF code from temporary key
     */
    private String extractCifFromTempKey(String tempKey) {
        // Format: TEMP_SAVING_REQUEST:{cifCode}:{timestamp}
        String[] parts = tempKey.split(":");
        if (parts.length >= 3) {
            return parts[1];
        }
        throw new AppException(ErrorCode.SAVING_REQUEST_NOTEXISTED);
    }

    /**
     * Validates OTP and returns the savings request if valid
     */
    private SavingRequestCreateDTO validateOTPAndGetTempRequest(ConfirmRequestDTO confirmRequestDTO) {
        String keyOTP = "OTP:SAVING:" + confirmRequestDTO.getSavingRequestID();
        String storedOtp = (String) redisTemplate.opsForValue().get(keyOTP);
        log.info("Validating OTP for temp request: {}", confirmRequestDTO.getSavingRequestID());
        
        if (storedOtp == null) {
            throw new AppException(ErrorCode.OTP_EXPIRED);
        }
        
        if (!storedOtp.equals(confirmRequestDTO.getOtpCode())) {
            handleOTPFailure(confirmRequestDTO.getSavingRequestID());
            throw new AppException(ErrorCode.INVALID_OTP);
        }
        
        // Lấy temp request
        SavingRequestCreateDTO tempRequest = (SavingRequestCreateDTO) redisTemplate.opsForValue().get(confirmRequestDTO.getSavingRequestID());
        if (tempRequest == null) {
            throw new AppException(ErrorCode.SAVING_REQUEST_NOTEXISTED);
        }
        
        log.info("OTP validated successfully for temp request: {}", confirmRequestDTO.getSavingRequestID());
        return tempRequest;
    }

    /**
     * Handles OTP failure logic including failure counting
     */
    private void handleOTPFailure(String tempRequestKey) {
        String keyFailCount = "OTP_FAIL_COUNT:SAVING:" + tempRequestKey;
        String failStr = (String) redisTemplate.opsForValue().get(keyFailCount);
        int failCount = (failStr == null) ? 0 : Integer.parseInt(failStr);

        failCount++;
        redisTemplate.opsForValue().set(keyFailCount, String.valueOf(failCount), Duration.ofMinutes(5));
        
        if (failCount >= 3) {
            // Xóa temp request
            redisTemplate.delete(tempRequestKey);
            redisTemplate.delete("OTP:SAVING:" + tempRequestKey);
            log.error("Saving request creation failed due to OTP entered incorrectly more than 3 times");
            throw new AppException(ErrorCode.OTP_WRONG_MANY);
        }
    }

    /**
     * Generates OTP and stores in Redis
     */
    private String generateAndStoreOTP(String key) {
        String keyOTP = "OTP:SAVING:" + key;
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
                .subject("Xác thực OTP - Tạo tài khoản tiết kiệm")
                .build();
        streamBridge.send("mail-out-0", mailMessageDTO);
        log.info("OTP email sent to: {}", customer.getEmail());
    }

    /**
     * Validates the savings request input data
     */
    private void validateSavingRequestInput(SavingRequestCreateDTO dto) {
        if (dto == null) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }
        if (dto.getAccountNumberSource() == null || dto.getAccountNumberSource().trim().isEmpty()) {
            throw new AppException(ErrorCode.INVALID_ACCOUNT_NUMBER);
        }
        if (dto.getInitialDeposit() == null || dto.getInitialDeposit().compareTo(BigDecimal.ZERO) <= 0) {
            throw new AppException(ErrorCode.INVALID_AMOUNT);
        }
        if (dto.getTerm() == null || dto.getTerm() <= 0) {
            throw new AppException(ErrorCode.INVALID_TERM);
        }
    }

    /**
     * Validates account balance against required deposit amount
     */
    private void validateAccountBalance(String accountNumber, BigDecimal requiredAmount) {
        String balanceUrl = String.format("%s/api/core-bank/get-balance/%s", coreBankingBaseUrl, accountNumber);
        
        try {
            ResponseEntity<ApiResponse<BigDecimal>> response = restTemplate.exchange(
                    balanceUrl,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<ApiResponse<BigDecimal>>() {}
            );
            
            if (response.getBody() == null || response.getBody().getResult() == null) {
                log.error("Invalid response when checking balance for account: {}", accountNumber);
                throw new AppException(ErrorCode.ACCOUNT_NOT_FOUND);
            }
            
            BigDecimal balance = response.getBody().getResult();
            log.info("Balance of source account {}: {}", accountNumber, balance);
            
            if (balance.compareTo(requiredAmount) < 0) {
                log.warn("Insufficient balance. Required: {}, Available: {}", requiredAmount, balance);
                throw new AppException(ErrorCode.BALANCE_NOT_ENOUGH);
            }
        } catch (Exception e) {
            log.error("Error checking account balance for account: {}", accountNumber, e);
            throw new AppException(ErrorCode.CORE_BANKING_SERVICE_ERROR);
        }
    }

    /**
     * Gets and validates the current authenticated customer
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

        if (currentCustomer == null) {
            log.error("Customer not found for userId: {}", userId);
            throw new AppException(ErrorCode.CUSTOMER_NOT_FOUND);
        }

        if (currentCustomer.getStatus() != CustomerStatus.ACTIVE) {
            log.warn("Customer status is not ACTIVE: {}", currentCustomer.getStatus());
            throw new AppException(ErrorCode.CUSTOMER_NOTACTIVE);
        }
        
        return currentCustomer;
    }

    /**
     * Processes the complete saving account creation workflow
     */
    private Account processSavingAccountCreation(SavingRequestCreateDTO tempRequest, String cifCode) {
        // Transfer money from source account to master account
        CommonTransactionDTO transactionDTO = transferToMasterAccount(tempRequest);
        
        // Create account in local database
        Account account = createLocalSavingAccount(tempRequest, cifCode);
        
        // Create account in core banking system
        createCoreBankingAccount(account, tempRequest);
        
        accountRepository.save(account);
        log.info("Saving account created successfully: {}", account.getAccountNumber());
        
        return account;
    }

    /**
     * Transfers money from source account to master account
     */
    private CommonTransactionDTO transferToMasterAccount(SavingRequestCreateDTO tempRequest) {
        CommonTransactionDTO transactionDTO = commonTransactionService.createAccountSaving(
                CreateAccountSavingRequest.builder()
                        .fromAccountNumber(tempRequest.getAccountNumberSource())
                        .amount(tempRequest.getInitialDeposit())
                        .currency("VND")
                        .description("Gửi tiền tiết kiệm")
                        .build()
        );
        
        if (!"COMPLETED".equals(transactionDTO.getStatus())) {
            log.error("Transaction failed with status: {}", transactionDTO.getStatus());
            throw new AppException(ErrorCode.TRANSACTION_FAILED);
        }
        
        return transactionDTO;
    }

    /**
     * Creates a local saving account entity
     */
    private Account createLocalSavingAccount(SavingRequestCreateDTO tempRequest, String cifCode) {
        Account account = Account.builder()
                .accountType(AccountType.SAVING)
                .cifCode(cifCode)
                .status(AccountStatus.ACTIVE)
                .build();
        account.setAccountNumber(generateAccountNumber(account));
        return account;
    }

    /**
     * Creates saving account in core banking system
     */
    private void createCoreBankingAccount(Account account, SavingRequestCreateDTO tempRequest) {
        CoreSavingAccountDTO coreSavingAccountDTO = CoreSavingAccountDTO.builder()
                .cifCode(account.getCifCode())
                .term(tempRequest.getTerm())
                .initialDeposit(tempRequest.getInitialDeposit())
                .accountNumber(account.getAccountNumber())
                .build();
                
        log.info("Creating core banking account: {}", coreSavingAccountDTO);
        
        String url = coreBankingBaseUrl + "/create-savings-account";
        try {
            restTemplate.postForObject(url, coreSavingAccountDTO, Void.class);
        } catch (Exception e) {
            log.error("Failed to create account in core banking system", e);
            throw new AppException(ErrorCode.CORE_BANKING_SERVICE_ERROR);
        }
    }

    @Override
    public List<CoreTermDTO> getAllTerm() {
        String url = coreBankingBaseUrl + "/get-all-term-isactive";
        try {
            ResponseEntity<List<CoreTermDTO>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<CoreTermDTO>>() {}
            );
            return response.getBody();
        } catch (Exception e) {
            log.error("Failed to get terms from core banking", e);
            throw new AppException(ErrorCode.CORE_BANKING_SERVICE_ERROR);
        }
    }

    /**
     * Generates account number based on account type and CIF
     */
    public String generateAccountNumber(Account account) {
        String cif = account.getCifCode();
        int typeCode;
        switch (account.getAccountType()) {
            case PAYMENT:
                typeCode = 0;
                break;
            case CREDIT:
                typeCode = 1;
                break;
            case SAVING:
                typeCode = 2;
                break;
            default:
                typeCode = 9; // Unknown type
        }
        String randomPart = String.format("%03d", new Random().nextInt(1000));
        return cif + typeCode + randomPart;
    }
}

