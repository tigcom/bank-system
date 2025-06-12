package com.example.account_service.service.impl;

import com.example.account_service.dto.request.ConfirmRequestDTO;
import com.example.account_service.dto.request.SavingRequestCreateDTO;
import com.example.account_service.dto.response.SavingsRequestResponse;
import com.example.account_service.dto.response.withdrawSavingResponse;
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
import com.example.common_service.dto.request.SavingUpdateRequest;
import com.example.common_service.dto.request.WithdrawAccountSavingRequest;
import com.example.common_service.dto.response.AccountPaymentResponse;
import com.example.common_service.dto.response.AccountSavingUpdateResponse;
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
import org.springframework.http.*;
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
        String otp = generateAndStoreOTP(tempRequestKey, "SAVING");
        MailMessageDTO mailMessageDTO = MailMessageDTO.builder()
                .recipientName(currentCustomer.getFullName())
                .recipient(currentCustomer.getEmail())
                .body(otp)
                .subject("Xác thực OTP - Tạo tài khoản tiết kiệm")
                .build();
        sendOTPEmail(currentCustomer, otp,mailMessageDTO);
        
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

        // Xác định loại request từ temp key
        String requestType = tempRequestKey.contains("SAVING_REQUEST") ? "SAVING" : "WITHDRAW";
        String otp = generateAndStoreOTP(tempRequestKey, requestType);
        String emailSubject = requestType.equals("SAVING") ? 
            "Xác thực OTP - Tạo tài khoản tiết kiệm" : 
            "Xác thực OTP - Yêu cầu rút tiền tiết kiệm";
            
        MailMessageDTO mailMessageDTO = MailMessageDTO.builder()
                .recipientName(customerDTO.getFullName())
                .recipient(customerDTO.getEmail())
                .body(otp)
                .subject(emailSubject)
                .build();
        sendOTPEmail(customerDTO, otp,mailMessageDTO);
        
        log.info("OTP resent successfully for temp request: {}", tempRequestKey);
    }

    @Override
    public SavingsRequestResponse confirmOTPAndCreateSavingAccount(ConfirmRequestDTO confirmRequestDTO) {
        log.info("Confirming OTP and creating saving account: {}", confirmRequestDTO.getSavingRequestID());
        
        // Validate OTP
        SavingRequestCreateDTO tempRequest = validateOTPAndGetTempRequest(confirmRequestDTO, "SAVING");
        
        // Lấy thông tin customer
        String cifCode = extractCifFromTempKey(confirmRequestDTO.getSavingRequestID());
        log.info("Creating saving account for CIF Code: {}", cifCode);
        CustomerDTO customerDTO = commonService.getCustomerByCifCode(cifCode);
        
        // Tạo Saving Account trực tiếp (không cần tạo SavingsRequest entity)
        Account account = processSavingAccountCreation(tempRequest, cifCode);
        
        // Cleanup temp data
        cleanupTempData(confirmRequestDTO.getSavingRequestID(), "SAVING");
        
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

    @Override
    public withdrawSavingResponse createWithDrawRequest(WithdrawSavingDTO request) {
        log.info("Starting create withdraw from Saving with input: {}", request);

        // Validate input
        validatewithdrawSavingRequest(request);
        // Get and validate customer
        CustomerDTO currentCustomer = getCurrentValidatedCustomer();
        // Tạo temporary key để lưu thông tin request trước khi verify OTP
        String tempRequestKey = "TEMP_WITHDRAW_REQUEST:" + currentCustomer.getCifCode() + ":" + System.currentTimeMillis();

        // Lưu thông tin request vào Redis (expire sau 1 giờ)
        WithdrawSavingDTO tempRequest = WithdrawSavingDTO.builder()
                .withdrawAmount(request.getWithdrawAmount())
                .amountOriginal(request.getAmountOriginal())
                .withdrawType(request.getWithdrawType())
                .destinationAccountNumber(request.getDestinationAccountNumber())
                .savingsAccountNumber(request.getSavingsAccountNumber())
                .build();

        redisTemplate.opsForValue().set(tempRequestKey, tempRequest, Duration.ofMinutes(60));

        // Tạo và gửi OTP
        String otp = generateAndStoreOTP(tempRequestKey, "WITHDRAW");
        MailMessageDTO mailMessageDTO = MailMessageDTO.builder()
                .recipientName(currentCustomer.getFullName())
                .recipient(currentCustomer.getEmail())
                .body(otp)
                .subject("Xác thực OTP - Yêu cầu rút tiền tiết kiệm")
                .build();
        sendOTPEmail(currentCustomer, otp, mailMessageDTO);

        log.info("OTP sent for withdraw request creation. Temp key: {}", tempRequestKey);
        // Trả về response với temp key để client có thể confirm OTP
        return withdrawSavingResponse.builder()
                .id(tempRequestKey)
                .amountOriginal(request.getAmountOriginal())
                .withdrawAmount(request.getWithdrawAmount())
                .withdrawType(tempRequest.getWithdrawType())
                .destinationAccountNumber(tempRequest.getDestinationAccountNumber())
                .savingsAccountNumber(tempRequest.getSavingsAccountNumber())
                .build();
    }

    /**
     * Confirms OTP and processes withdraw from saving account
     */
    @Override
    public withdrawSavingResponse confirmOTPAndProcessWithdraw(ConfirmRequestDTO confirmRequestDTO) {
        log.info("Confirming OTP and processing withdraw: {}", confirmRequestDTO.getSavingRequestID());
        
        // Validate OTP
        WithdrawSavingDTO tempRequest = validateOTPAndGetTempRequest(confirmRequestDTO, "WITHDRAW");
        
        // Lấy thông tin customer
        String cifCode = extractCifFromTempKey(confirmRequestDTO.getSavingRequestID());
        log.info("Processing withdraw for CIF Code: {}", cifCode);
        CustomerDTO customerDTO = commonService.getCustomerByCifCode(cifCode);
        
        // Process withdraw transaction
        processWithdrawTransaction(tempRequest);
        
        // Cleanup temp data
        cleanupTempData(confirmRequestDTO.getSavingRequestID(), "WITHDRAW");
        
        log.info("Withdraw from saving account processed successfully");
        
        // Trả về response với thông tin đã xử lý
        return withdrawSavingResponse.builder()
                .id("COMPLETED")
                .amountOriginal(tempRequest.getAmountOriginal())
                .withdrawAmount(tempRequest.getWithdrawAmount())
                .withdrawType(tempRequest.getWithdrawType())
                .destinationAccountNumber(tempRequest.getDestinationAccountNumber())
                .savingsAccountNumber(tempRequest.getSavingsAccountNumber())
                .build();
    }

    /**
     * Processes withdraw transaction from saving to destination account
     */
    private void processWithdrawTransaction(WithdrawSavingDTO request) {
        // Implementation for withdraw transaction
        // This would involve calling core banking service to process the withdrawal
        log.info("Processing withdraw transaction: {} from {} to {}", 
                request.getWithdrawAmount(), request.getSavingsAccountNumber(), request.getDestinationAccountNumber());
        
        // TODO: Implement actual withdraw logic by calling core banking service
        // For now, we'll just log the operation
        String url = coreBankingBaseUrl +"/get-account-by-id/" + request.getSavingsAccountNumber();
        ResponseEntity<AccountPaymentResponse> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<AccountPaymentResponse>() {}
        );
        AccountPaymentResponse thisSaving = response.getBody();

        if (request.getWithdrawType().equals("full"))
        {
            log.info("Withdraw all monney from saving", request.getWithdrawType());
            //DTO chuyen tien
            processTransaction(request);

            //chuyen tien  thanh cong se update account local , va core banking sang closed

            //update account saving local
            Account account = accountRepository.findByAccountNumber(request.getSavingsAccountNumber());
            if (account == null) {
                throw new AppException(ErrorCode.ACCOUNT_NOT_FOUND);
            }
            account.setStatus(AccountStatus.CLOSED);
            accountRepository.save(account);

            SavingUpdateRequest  savingUpdateRequest = SavingUpdateRequest.builder()
                    .balance(BigDecimal.ZERO)
                    .status(AccountStatus.CLOSED)
                    .build();
            log.info("Saving update: {}", savingUpdateRequest);
                 String urlUpdate = coreBankingBaseUrl +"/update-balance-account-saving/" + request.getSavingsAccountNumber();
//            restTemplate.put(urlUpdate, savingUpdateRequest);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<SavingUpdateRequest> entity = new HttpEntity<>(savingUpdateRequest, headers);
            ResponseEntity<AccountSavingUpdateResponse> newresponse = restTemplate.exchange(
                    urlUpdate,
                    HttpMethod.PUT,
                    entity,
                    new ParameterizedTypeReference<AccountSavingUpdateResponse>() {}
            );
        }
        else
        {
            log.info("Withdraw a part of  monney from saving" + request.getWithdrawType());
            // chuyen tien
           processTransaction(request);
            // neu chuyen tien thanh cong thi update
            //update account core
            BigDecimal newBalance = thisSaving.getBalance().subtract(request.getAmountOriginal());
            SavingUpdateRequest  savingUpdateRequest = SavingUpdateRequest.builder()
                    .balance(newBalance)
                    .status(AccountStatus.ACTIVE)
                    .build();
            log.info("Saving update: {}", savingUpdateRequest);
            String urlUpdate = coreBankingBaseUrl +"/update-balance-account-saving/" + request.getSavingsAccountNumber();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<SavingUpdateRequest> entity = new HttpEntity<>(savingUpdateRequest, headers);
            ResponseEntity<AccountSavingUpdateResponse> newresponse = restTemplate.exchange(
                    urlUpdate,
                    HttpMethod.PUT,
                    entity,
                    new ParameterizedTypeReference<AccountSavingUpdateResponse>() {}
            );
            //get account saving

        }

    }

    private void processTransaction(WithdrawSavingDTO request) {
        WithdrawAccountSavingRequest withdrawAccountSavingRequest = WithdrawAccountSavingRequest.builder()
                .toAccountNumber(request.getDestinationAccountNumber())
                .amount(request.getWithdrawAmount())
                .currency("VND")
                .description("Rút tiền tiết kiệm")
                .build();
        
        // Backup current security context before calling Dubbo service
        Authentication currentAuth = SecurityContextHolder.getContext().getAuthentication();
        try {
            // Clear context to avoid JwtAuthenticationToken serialization issues with Dubbo
            SecurityContextHolder.clearContext();
            CommonTransactionDTO transactionDTO = commonTransactionService.withdrawAccountSaving(withdrawAccountSavingRequest);
            if (transactionDTO == null)
            {
                throw new AppException(ErrorCode.TRANSACTION_FAILED);
            }
            if (!transactionDTO.getStatus().equals("COMPLETED"))
            {
                throw new AppException(ErrorCode.TRANSACTION_FAILED);
            }
        } finally {
            // Restore security context
            if (currentAuth != null) {
                SecurityContextHolder.getContext().setAuthentication(currentAuth);
            }
        }
    }

    /**
     * Extracts CIF code from temporary key
     */
    private String extractCifFromTempKey(String tempKey) {
        // Format: TEMP_SAVING_REQUEST:{cifCode}:{timestamp} or TEMP_WITHDRAW_REQUEST:{cifCode}:{timestamp}
        String[] parts = tempKey.split(":");
        if (parts.length >= 3) {
            return parts[1];
        }
        throw new AppException(ErrorCode.SAVING_REQUEST_NOTEXISTED);
    }

    /**
     * Generic method to validate OTP and return temp request
     */
    @SuppressWarnings("unchecked")
    private <T> T validateOTPAndGetTempRequest(ConfirmRequestDTO confirmRequestDTO, String requestType) {
        String keyOTP = "OTP:" + requestType + ":" + confirmRequestDTO.getSavingRequestID();
        String storedOtp = (String) redisTemplate.opsForValue().get(keyOTP);
        log.info("Validating OTP for temp request: {} with type: {}", confirmRequestDTO.getSavingRequestID(), requestType);
        
        if (storedOtp == null) {
            throw new AppException(ErrorCode.OTP_EXPIRED);
        }
        
        if (!storedOtp.equals(confirmRequestDTO.getOtpCode())) {
            handleOTPFailure(confirmRequestDTO.getSavingRequestID(), requestType);
            throw new AppException(ErrorCode.INVALID_OTP);
        }
        
        // Lấy temp request
        T tempRequest = (T) redisTemplate.opsForValue().get(confirmRequestDTO.getSavingRequestID());
        if (tempRequest == null) {
            throw new AppException(ErrorCode.SAVING_REQUEST_NOTEXISTED);
        }
        
        log.info("OTP validated successfully for temp request: {}", confirmRequestDTO.getSavingRequestID());
        return tempRequest;
    }

    /**
     * Generic method to handle OTP failure logic including failure counting
     */
    private void handleOTPFailure(String tempRequestKey, String requestType) {
        String keyFailCount = "OTP_FAIL_COUNT:" + requestType + ":" + tempRequestKey;
        String failStr = (String) redisTemplate.opsForValue().get(keyFailCount);
        int failCount = (failStr == null) ? 0 : Integer.parseInt(failStr);

        failCount++;
        redisTemplate.opsForValue().set(keyFailCount, String.valueOf(failCount), Duration.ofMinutes(5));
        
        if (failCount >= 3) {
            // Xóa temp request
            cleanupTempData(tempRequestKey, requestType);
            log.error("{} request failed due to OTP entered incorrectly more than 3 times", requestType);
            throw new AppException(ErrorCode.OTP_WRONG_MANY);
        }
    }

    /**
     * Generic method to generate OTP and store in Redis
     */
    private String generateAndStoreOTP(String key, String requestType) {
        String keyOTP = "OTP:" + requestType + ":" + key;
        String otp = String.valueOf(100000 + new Random().nextInt(900000));
        redisTemplate.opsForValue().set(keyOTP, otp, Duration.ofMinutes(10)); // OTP có hiệu lực 10 phút
        log.info("OTP generated and stored for key: {} with type: {}", key, requestType);
        return otp;
    }

    /**
     * Generic method to cleanup temporary data
     */
    private void cleanupTempData(String tempRequestKey, String requestType) {
        redisTemplate.delete(tempRequestKey);
        redisTemplate.delete("OTP:" + requestType + ":" + tempRequestKey);
        redisTemplate.delete("OTP_FAIL_COUNT:" + requestType + ":" + tempRequestKey);
        log.info("Cleanup completed for temp request: {} with type: {}", tempRequestKey, requestType);
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

        // Backup current security context before calling Dubbo service
        Authentication currentAuth = SecurityContextHolder.getContext().getAuthentication();
        CustomerDTO currentCustomer;
        try {
            // Clear context to avoid JwtAuthenticationToken serialization issues with Dubbo
            SecurityContextHolder.clearContext();
            currentCustomer = commonService.getCurrentCustomer(userId);
        } finally {
            // Restore security context
            if (currentAuth != null) {
                SecurityContextHolder.getContext().setAuthentication(currentAuth);
            }
        }
        
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
        // Backup current security context before calling Dubbo service
        Authentication currentAuth = SecurityContextHolder.getContext().getAuthentication();
        try {
            // Clear context to avoid JwtAuthenticationToken serialization issues with Dubbo
            SecurityContextHolder.clearContext();
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
        } finally {
            // Restore security context
            if (currentAuth != null) {
                SecurityContextHolder.getContext().setAuthentication(currentAuth);
            }
        }
    }

    /**
     * Creates a local saving account entity
     */
    private Account createLocalSavingAccount(SavingRequestCreateDTO tempRequest, String cifCode) {
        Account account = Account.builder()
                .accountType(AccountType.SAVING)
                .cifCode(cifCode)
                .status(AccountStatus.ACTIVE)
                .srcAccountNumber(tempRequest.getAccountNumberSource())
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
                .srcAccountNumber(tempRequest.getAccountNumberSource())
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

    private void validatewithdrawSavingRequest(WithdrawSavingDTO request) {
        Account accountSRC = accountRepository.findByAccountNumber(request.getDestinationAccountNumber());
        Account accountSaving = accountRepository.findByAccountNumber(request.getSavingsAccountNumber());

        if (accountSRC == null && accountSaving == null) {
            throw new AppException(ErrorCode.ACCOUNT_NOT_FOUND);
        }
        if(!accountSRC.getStatus().equals(AccountStatus.ACTIVE) && !accountSaving.getStatus().equals(AccountStatus.ACTIVE) ) {
            throw new AppException(ErrorCode.ACCOUNT_NOT_ACTIVE);
        }
        validateAccountBalance(request.getSavingsAccountNumber(), request.getAmountOriginal());
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

    /**
     * Sends OTP email to customer
     */
    private void sendOTPEmail(CustomerDTO customer, String otp,MailMessageDTO mailMessageDTO) {
        streamBridge.send("mail-out-0", mailMessageDTO);
        log.info("OTP email sent to: {}", customer.getEmail());
    }
}

