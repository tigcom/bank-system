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
        // Validate input
        validateSavingRequestInput(savingRequestCreateDTO);
        
        // Check account balance
        validateAccountBalance(savingRequestCreateDTO.getAccountNumberSource(), savingRequestCreateDTO.getInitialDeposit());
        
        // Get and validate customer
        CustomerDTO currentCustomer = getCurrentValidatedCustomer();
        
        // Create and save savings request
        SavingsRequest savingsRequest = createSavingsRequest(savingRequestCreateDTO, currentCustomer);
        savingsRequestRepository.save(savingsRequest);

        return buildSavingsRequestResponse(savingsRequest);
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
     * Creates a SavingsRequest entity from DTO and customer data
     */
    private SavingsRequest createSavingsRequest(SavingRequestCreateDTO dto, CustomerDTO customer) {
        return SavingsRequest.builder()
                .cifCode(customer.getCifCode())
                .initialDeposit(dto.getInitialDeposit())
                .term(dto.getTerm())
                .accountNumberSource(dto.getAccountNumberSource())
                .status(SavingsRequestStatus.PENDING)
                .interestRate(dto.getInterestRate())
                .build();
    }

    /**
     * Builds response DTO from SavingsRequest entity
     */
    private SavingsRequestResponse buildSavingsRequestResponse(SavingsRequest savingsRequest) {
        return SavingsRequestResponse.builder()
                .id(savingsRequest.getId())
                .accountNumberSource(savingsRequest.getAccountNumberSource())
                .initialDeposit(savingsRequest.getInitialDeposit())
                .term(savingsRequest.getTerm())
                .status(savingsRequest.getStatus())
                .cifCode(savingsRequest.getCifCode())
                .interestRate(savingsRequest.getInterestRate())
                .build();
    }

    @Override
    public void sendOTP(String savingsRequestId) {
        SavingsRequest savingsRequest = getSavingsRequestById(savingsRequestId);
        log.info("Saving request: {}", savingsRequest);
        
        CustomerDTO customerDTO = commonService.getCustomerByCifCode(savingsRequest.getCifCode());
        if (customerDTO == null) {
            throw new AppException(ErrorCode.CUSTOMER_NOT_FOUND);
        }

        String otp = createOTP(savingsRequestId);
        log.info("OTP created: {}", otp);
        
        sendOTPEmail(customerDTO, otp);
    }

    @Override
    public SavingsRequestResponse confirmOTPandSave(ConfirmRequestDTO confirmRequestDTO) {
        // Validate OTP
        SavingsRequest savingsRequest = validateOTPAndGetRequest(confirmRequestDTO);
        
        // Process saving account creation
        Account account = processSavingAccountCreation(savingsRequest);
        
        // Update request status
        savingsRequest.setStatus(SavingsRequestStatus.APPROVED);
        savingsRequestRepository.save(savingsRequest);

        return buildSavingsRequestResponse(savingsRequest);
    }

    /**
     * Gets savings request by ID with validation
     */
    private SavingsRequest getSavingsRequestById(String savingsRequestId) {
        return savingsRequestRepository.findById(savingsRequestId)
                .orElseThrow(() -> new AppException(ErrorCode.SAVING_REQUEST_NOTEXISTED));
    }

    /**
     * Sends OTP email to customer
     */
    private void sendOTPEmail(CustomerDTO customer, String otp) {
        MailMessageDTO mailMessageDTO = MailMessageDTO.builder()
                .recipientName(customer.getFullName())
                .recipient(customer.getEmail())
                .body(otp)
                .subject("Xác thực OTP")
                .build();
        streamBridge.send("mail-out-0", mailMessageDTO);
    }

    /**
     * Validates OTP and returns the savings request if valid
     */
    private SavingsRequest validateOTPAndGetRequest(ConfirmRequestDTO confirmRequestDTO) {
        String keyOTP = "OTP:" + confirmRequestDTO.getSavingRequestID();
        String storedOtp = (String) redisTemplate.opsForValue().get(keyOTP);
        log.info("OTP on redis: {}", storedOtp);
        
        SavingsRequest savingsRequest = getSavingsRequestById(confirmRequestDTO.getSavingRequestID());
        
        if (!savingsRequest.getStatus().equals(SavingsRequestStatus.PENDING)) {
            throw new AppException(ErrorCode.SAVING_REQUEST_INVALID_STATUS);
        }
        
        if (storedOtp == null) {
            throw new AppException(ErrorCode.OTP_EXPIRED);
        }
        
        if (!storedOtp.equals(confirmRequestDTO.getOtpCode())) {
            handleOTPFailure(confirmRequestDTO.getSavingRequestID(), savingsRequest);
            throw new AppException(ErrorCode.INVALID_OTP);
        }
        
        // Clean up OTP from Redis after successful validation
        redisTemplate.delete(keyOTP);
        
        return savingsRequest;
    }

    /**
     * Handles OTP failure logic including failure counting
     */
    private void handleOTPFailure(String savingRequestID, SavingsRequest savingsRequest) {
        String keyFailCount = "OTP_FAIL_COUNT:" + savingRequestID;
        String failStr = (String) redisTemplate.opsForValue().get(keyFailCount);
        int failCount = (failStr == null) ? 0 : Integer.parseInt(failStr);

        failCount++;
        redisTemplate.opsForValue().set(keyFailCount, String.valueOf(failCount), Duration.ofSeconds(120));
        
        if (failCount > 3) {
            savingsRequest.setStatus(SavingsRequestStatus.REJECTED);
            savingsRequestRepository.save(savingsRequest);
            log.error("Transaction failed due to OTP entered incorrectly more than 3 times");
            throw new AppException(ErrorCode.OTP_WRONG_MANY);
        }
    }

    /**
     * Processes the complete saving account creation workflow
     */
    private Account processSavingAccountCreation(SavingsRequest savingsRequest) {
        // Transfer money from source account to master account
        CommonTransactionDTO transactionDTO = transferToMasterAccount(savingsRequest);
        
        // Create account in local database
        Account account = createLocalSavingAccount(savingsRequest);
        
        // Create account in core banking system
        createCoreBankingAccount(account, savingsRequest);
        
        accountRepository.save(account);
        log.info("Saving account created successfully: {}", account.getAccountNumber());
        
        return account;
    }

    /**
     * Transfers money from source account to master account
     */
    private CommonTransactionDTO transferToMasterAccount(SavingsRequest savingsRequest) {
        CommonTransactionDTO transactionDTO = commonTransactionService.createAccountSaving(
                CreateAccountSavingRequest.builder()
                        .fromAccountNumber(savingsRequest.getAccountNumberSource())
                        .amount(savingsRequest.getInitialDeposit())
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
    private Account createLocalSavingAccount(SavingsRequest savingsRequest) {
        Account account = Account.builder()
                .accountType(AccountType.SAVING)
                .cifCode(savingsRequest.getCifCode())
                .status(AccountStatus.ACTIVE)
                .build();
        account.setAccountNumber(generateAccountNumber(account));
        return account;
    }

    /**
     * Creates saving account in core banking system
     */
    private void createCoreBankingAccount(Account account, SavingsRequest savingsRequest) {
        CoreSavingAccountDTO coreSavingAccountDTO = CoreSavingAccountDTO.builder()
                .cifCode(account.getCifCode())
                .term(savingsRequest.getTerm())
                .initialDeposit(savingsRequest.getInitialDeposit())
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
     * Creates OTP and stores in Redis
     */
    public String createOTP(String savingsRequestId) {
        String keyOTP = "OTP:" + savingsRequestId;
        String otp = String.valueOf(100000 + new Random().nextInt(900000));
        redisTemplate.opsForValue().set(keyOTP, otp, Duration.ofSeconds(3600));
        return otp;
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

