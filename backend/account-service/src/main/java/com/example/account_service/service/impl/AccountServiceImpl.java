package com.example.account_service.service.impl;


import com.example.account_service.dto.request.PaymentConfirmOtpDTO;
import com.example.account_service.dto.request.PaymentCreateDTO;
import com.example.account_service.dto.response.AccountCreateReponse;
import com.example.common_service.dto.response.BalanceResponse;
import com.example.account_service.dto.response.CicResponse;
import com.example.account_service.dto.response.PaymentRequestResponse;
import com.example.account_service.entity.Account;
import com.example.account_service.entity.SavingsAccount;
import com.example.account_service.entity.CreditAccount;
import com.example.account_service.entity.CreditCardType;
import com.example.account_service.exception.AppException;
import com.example.account_service.exception.ErrorCode;
import com.example.account_service.repository.AccountRepository;
import com.example.account_service.repository.SavingsAccountRepository;
import com.example.account_service.repository.CreditAccountRepository;
import com.example.account_service.repository.CreditCardTypeRepository;
import com.example.account_service.service.AccountService;
import com.example.common_service.constant.AccountStatus;
import com.example.common_service.constant.AccountType;
import com.example.common_service.constant.CustomerStatus;
import com.example.common_service.dto.*;
import com.example.common_service.dto.response.AccountPaymentResponse;
import com.example.common_service.dto.response.AccountSummaryDTO;
import com.example.common_service.dto.response.SavingAccountResponse;
import com.example.common_service.services.CommonService;
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
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.ArrayList;

@Service
@Slf4j
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {
    private final AccountRepository accountRepository;
    private final SavingsAccountRepository savingsAccountRepository;
    private final CreditAccountRepository creditAccountRepository;
    private final CreditCardTypeRepository creditCardTypeRepository;

    @DubboReference(timeout = 5000)
    private final CommonService commonService;
    private final RestTemplate restTemplate;
    private final RedisTemplate<Object, Object> redisTemplate;
    private final StreamBridge streamBridge;
    @Value("${core-banking.base-url:http://localhost:8083/corebanking}")
    private String coreBankingBaseUrl;
    @Override
    public AccountCreateReponse createPayment() {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName();
        CustomerDTO currentCustomer = commonService.getCurrentCustomer(userId);
        String token = ((JwtAuthenticationToken) SecurityContextHolder.getContext().getAuthentication()).getToken().getTokenValue();
        log.info("Current Customer : {}", currentCustomer);

        log.info("Create payment request received");

        if (currentCustomer.getStatus() == CustomerStatus.ACTIVE) {
            Account account = Account.builder()
                    .accountType(AccountType.PAYMENT)
                    .cifCode(currentCustomer.getCifCode())
                    .status(AccountStatus.ACTIVE)
                    .build();
            account.setAccountNumber(generateAccountNumber(account));
            log.info("Account : " + account);

            CorePaymentAccountDTO corePaymentAccountDTO = CorePaymentAccountDTO.builder()
                    .cifCode(account.getCifCode())
                    .accountNumber(account.getAccountNumber())
                    .build();
            log.info("corePaymentAccountDTO: {}", corePaymentAccountDTO);
            //dung dubbo luu account payment len core
//            commonServiceCore.createCoreAccountPayment(corePaymentAccountDTO);

            //// dung restTemplate call API save account tren CoreBanking
            String url = coreBankingBaseUrl+ "/create-payment-account";
            restTemplate.postForObject(url ,corePaymentAccountDTO,Void.class);

            accountRepository.save(account);

            return AccountCreateReponse.builder()
                    .accountNumber(account.getAccountNumber())
                    .cifCode(account.getCifCode())
                    .id(account.getId())
                    .accountType(account.getAccountType())
                    .status(account.getStatus())
                    .build();
        }
        throw new AppException(ErrorCode.CUSTOMER_NOTACTIVE);
    }

    public List<AccountSummaryDTO> getAllAccountsbyCifCode() {
        // Lấy thông tin người dùng từ context bảo mật
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName();
        log.info("User id: " + userId);

        // Lấy thông tin khách hàng hiện tại
        CustomerDTO currentCustomer = commonService.getCurrentCustomer(userId);
        String cifCode = currentCustomer.getCifCode();

        List<AccountSummaryDTO> result = new ArrayList<>();

        // Lấy Payment Accounts
        List<Account> paymentAccounts = accountRepository.findByCifCodeAndAccountTypeAndStatus(
                cifCode, AccountType.PAYMENT, AccountStatus.ACTIVE);
        for (Account account : paymentAccounts) {
            BigDecimal balance = getBalanceFromCorebanking(account.getAccountNumber());
            result.add(AccountSummaryDTO.builder()
                    .accountNumber(account.getAccountNumber())
                    .cifCode(account.getCifCode())
                    .accountType(account.getAccountType())
                    .balance(balance)
                    .status(account.getStatus())
                    .openedDate(account.getCreatedDate().toLocalDate())
                    .build());
        }

        // Lấy Savings Accounts
        List<SavingsAccount> savingsAccounts = savingsAccountRepository.findActiveSavingsAccountsByCifCode(cifCode);
        for (SavingsAccount account : savingsAccounts) {
            BigDecimal balance = getBalanceFromCorebanking(account.getAccountNumber());
            result.add(AccountSummaryDTO.builder()
                    .accountNumber(account.getAccountNumber())
                    .cifCode(account.getCifCode())
                    .accountType(account.getAccountType())
                    .balance(balance)
                    .status(account.getStatus())
                    .openedDate(account.getCreatedDate().toLocalDate())
                    .initialDeposit(account.getInitialDeposit())
                    .termValueMonths(account.getTerm().getTermValueMonths())
                    .interestRate(account.getTerm().getInterestRate())
                    .maturityDate(account.getMaturityDate())
                    .interestPaymentType(account.getInterestPaymentType())
                    .renewOption(account.getRenewOption())
                    .build());
        }

        // Lấy Credit Accounts
        List<CreditAccount> creditAccounts = creditAccountRepository.findActiveCreditAccountsByCifCode(cifCode);
        for (CreditAccount account : creditAccounts) {
            BigDecimal balance = getBalanceFromCorebanking(account.getAccountNumber());
            result.add(AccountSummaryDTO.builder()
                    .accountNumber(account.getAccountNumber())
                    .cifCode(account.getCifCode())
                    .accountType(account.getAccountType())
                    .balance(balance)
                    .status(account.getStatus())
                    .openedDate(account.getCreatedDate().toLocalDate())
                    .creditLimit(account.getCreditLimit())
                    .currentDebt(account.getCurrentDebt())
                    .creditCardType(account.getCreditCardType().getTypeName())
                    .build());
        }

        return result;
    }

    @Override
    public List<AccountPaymentResponse> getAllPaymentAccountsbyCifCode() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName();
        log.info("User id: " + userId);

        // Lấy thông tin khách hàng hiện tại
        CustomerDTO currentCustomer = commonService.getCurrentCustomer(userId);
        // check trang thai cua  Customer trươc
        if (!currentCustomer.getStatus().equals(CustomerStatus.ACTIVE)) {
            throw  new AppException(ErrorCode.CUSTOMER_NOTACTIVE);
        }
        String cifCode = currentCustomer.getCifCode();

        // Lấy Payment Accounts từ local database
        List<Account> paymentAccounts = accountRepository.findByCifCodeAndAccountTypeAndStatus(
                cifCode, AccountType.PAYMENT, AccountStatus.ACTIVE);

        // Kết hợp thông tin local với balance từ Core Banking
        return paymentAccounts.stream()
                .map(account -> {
                    BigDecimal balance = getBalanceFromCorebanking(account.getAccountNumber());
                    return AccountPaymentResponse.builder()
                            .accountNumber(account.getAccountNumber())
                            .cifCode(account.getCifCode())
                            .accountType(account.getAccountType())
                            .balance(balance)
                            .status(account.getStatus())
                            .openedDate(account.getCreatedDate().toLocalDate())
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Override
    public AccountPaymentResponse getAccountPaymentbyID(String id) {
        // Lấy thông tin account từ local database
        Account account = accountRepository.findByAccountNumber(id);
        if (account == null) {
            throw new AppException(ErrorCode.ACCOUNT_NOT_FOUND);
        }

        // Lấy balance từ Core Banking
        BigDecimal balance = getBalanceFromCorebanking(account.getAccountNumber());

        // Kết hợp thông tin
        return AccountPaymentResponse.builder()
                .accountNumber(account.getAccountNumber())
                .cifCode(account.getCifCode())
                .accountType(account.getAccountType())
                .balance(balance)
                .status(account.getStatus())
                .openedDate(account.getCreatedDate().toLocalDate())
                .build();
    }

    @Override
    public List<SavingAccountResponse> getAllSavingAccountbyCifCode() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName();
        log.info("User id: " + userId);

        // Lấy thông tin khách hàng hiện tại
        CustomerDTO currentCustomer = commonService.getCurrentCustomer(userId);
        String cifCode = currentCustomer.getCifCode();

        // Lấy Savings Accounts từ local database
        List<SavingsAccount> savingsAccounts = savingsAccountRepository.findActiveSavingsAccountsByCifCode(cifCode);

        // Kết hợp thông tin local với balance từ Core Banking
        return savingsAccounts.stream()
                .map(account -> {
                    BigDecimal balance = getBalanceFromCorebanking(account.getAccountNumber());
                    return SavingAccountResponse.builder()
                            .status(account.getStatus().name())
                            .accountNumber(account.getAccountNumber())
                            .cifCode(account.getCifCode())
                            .accountType(account.getAccountType().name())
                            .balance(balance)
                            .initialDeposit(account.getInitialDeposit())
                            .termValueMonths(account.getTerm().getTermValueMonths())
                            .interestRate(account.getTerm().getInterestRate())
                            .openedDate(account.getCreatedDate().toLocalDate())
                            .maturityDate(account.getMaturityDate())
                            .interestPaymentType(account.getInterestPaymentType())
                            .renewOption(account.getRenewOption())
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<CreditCardDTO> getAllCreditCard() {
        // Lấy Credit Card Types từ local database
        List<CreditCardType> creditCardTypes = creditCardTypeRepository.findAllOrderByAnnualFee();

        // Map to DTO
        return creditCardTypes.stream()
                .map(this::mapToCreditCardDTO)
                .collect(Collectors.toList());
    }

    private CreditCardDTO mapToCreditCardDTO(CreditCardType creditCardType) {
        return CreditCardDTO.builder()
                .cardID(creditCardType.getId())
                .typeName(creditCardType.getTypeName())
                .defaultCreditLimit(creditCardType.getDefaultCreditLimit())
                .interestRate(creditCardType.getInterestRate())
                .annualFee(creditCardType.getAnnualFee())
                .minimumIncome(creditCardType.getMinimumIncome())
                .imgURL(creditCardType.getImageUrl())
                .build();
    }

    @Override
    public CicResponse checkCIC(String idNumber) {
        String url = "http://localhost:8089/api/cic/check";

        Map<String, String> request = new HashMap<>();
        request.put("idNumber", idNumber);
        request.put("name", "Nguyen Van A");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, String>> entity = new HttpEntity<>(request, headers);

        try {
            ResponseEntity<CicResponse> response = restTemplate.postForEntity(url, entity, CicResponse.class);
            log.info("Response : " + response.getBody());
            return response.getBody();
        } catch (RestClientException e) {
            log.info(e.getMessage());
            return null;
        }
    }

    @Override
    public PaymentRequestResponse createPaymentInit(PaymentCreateDTO paymentRequest) {
        log.info("Tạo tài khoản  bắt đầu");

        return createPaymentAccountDirectly(paymentRequest.getCifCode());
    }

    @Override
    public PaymentRequestResponse createPaymentRequest(String cifCode) {
        log.info("Starting createPaymentRequest with cifCode: {}", cifCode);

        // Lấy thông tin customer theo cifCode
        CustomerDTO customer = commonService.getCustomerByCifCode(cifCode);
        if (customer == null) {
            throw new AppException(ErrorCode.CUSTOMER_NOT_FOUND);
        }

        // Check trạng thái customer
        if (customer.getStatus() != CustomerStatus.ACTIVE) {
            throw new AppException(ErrorCode.CUSTOMER_NOTACTIVE);
        }

        // Kiểm tra xem customer đã có tài khoản payment nào chưa
        List<AccountPaymentResponse> existingPaymentAccounts = getAllPaymentAccountsbyCifCode();

        if (existingPaymentAccounts.isEmpty()) {
            // Lần đầu tạo tài khoản payment - tạo luôn không cần OTP
            log.info("First time creating payment account for cifCode: {}. Creating directly without OTP.", cifCode);
            return createPaymentAccountDirectly(cifCode);
        } else {
            // Đã có tài khoản payment - cần OTP
            log.info("Customer already has payment accounts. Requiring OTP verification.");
            return createPaymentRequestWithOtp(cifCode);
        }
    }

    @Override
    public AccountCreateReponse confirmOtpAndCreatePayment(PaymentConfirmOtpDTO paymentConfirmOtpDTO) {
        log.info("Confirming OTP and creating payment account: {}", paymentConfirmOtpDTO.getPaymentRequestId());

        // Validate OTP
        PaymentCreateDTO tempRequest = validateOTPAndGetTempRequest(paymentConfirmOtpDTO);

        // Lấy thông tin customer
        String cifCode = extractCifFromTempKey(paymentConfirmOtpDTO.getPaymentRequestId());
        log.info("Creating payment account for CIF Code: {}", cifCode);
        CustomerDTO customerDTO = commonService.getCustomerByCifCode(cifCode);

        // Tạo Payment Account
        AccountCreateReponse response = createPaymentAccountForCustomer(cifCode);

        // Cleanup temp data
        redisTemplate.delete(paymentConfirmOtpDTO.getPaymentRequestId());
        redisTemplate.delete("OTP:PAYMENT:" + paymentConfirmOtpDTO.getPaymentRequestId());

        log.info("Payment account created successfully: {}", response.getAccountNumber());
        return response;
    }

    @Override
    public void resendPaymentOtp(String tempRequestKey) {
        log.info("Resending OTP for temp request key: {}", tempRequestKey);

        // Kiểm tra temp request có tồn tại không
        Object tempRequest = redisTemplate.opsForValue().get(tempRequestKey);
        if (tempRequest == null) {
            throw new AppException(ErrorCode.CUSTOMER_NOT_FOUND); // Sử dụng error code có sẵn
        }

        // Lấy thông tin customer từ temp key
        String cifCode = extractCifFromTempKey(tempRequestKey);
        log.info("Resending OTP for payment request. CIF code: {}", cifCode);
        CustomerDTO customerDTO = commonService.getCustomerByCifCode(cifCode);

        if (customerDTO == null) {
            throw new AppException(ErrorCode.CUSTOMER_NOT_FOUND);
        }

        String otp = generateAndStoreOTP(tempRequestKey);
        sendOTPEmail(customerDTO, otp);

        log.info("OTP resent successfully for temp request: {}", tempRequestKey);
    }


    private PaymentRequestResponse createPaymentAccountDirectly(String cifCode) {
        // Tạo account luôn không cần OTP
        AccountCreateReponse account = createPaymentAccountForCustomer(cifCode);

        return PaymentRequestResponse.builder()
                .id(account.getId())
                .cifCode(cifCode)
                .accountType(AccountType.PAYMENT)
                .status(PaymentRequestResponse.PaymentRequestStatus.APPROVED)
                .build();
    }

    private PaymentRequestResponse createPaymentRequestWithOtp(String cifCode) {
        // Tạo temporary key để lưu thông tin request trước khi verify OTP
        String tempRequestKey = "TEMP_PAYMENT_REQUEST:" + cifCode + ":" + System.currentTimeMillis();

        // Lưu thông tin request vào Redis (expire sau 1 giờ)
        PaymentCreateDTO tempRequest = PaymentCreateDTO.builder()
                .cifCode(cifCode)
                .build();

        redisTemplate.opsForValue().set(tempRequestKey, tempRequest, Duration.ofMinutes(60));

        // Lấy thông tin customer để gửi OTP
        CustomerDTO customer = commonService.getCustomerByCifCode(cifCode);

        // Tạo và gửi OTP
        String otp = generateAndStoreOTP(tempRequestKey);
        sendOTPEmail(customer, otp);

        log.info("OTP sent for payment request creation. Temp key: {}", tempRequestKey);

        // Trả về response với temp key để client có thể confirm OTP
        return PaymentRequestResponse.builder()
                .id(tempRequestKey)
                .cifCode(cifCode)
                .accountType(AccountType.PAYMENT)
                .status(PaymentRequestResponse.PaymentRequestStatus.PENDING)
                .build();
    }

    private AccountCreateReponse createPaymentAccountForCustomer(String cifCode) {
        Account account = Account.builder()
                .accountType(AccountType.PAYMENT)
                .cifCode(cifCode)
                .status(AccountStatus.ACTIVE)
                .build();
        account.setAccountNumber(generateAccountNumber(account));
        log.info("Account : " + account);
        accountRepository.save(account);
        CoreAccountRequest coreAccount = CoreAccountRequest.builder()
                .accountNumber(account.getAccountNumber())
                .cifCode(cifCode)
                .balance(BigDecimal.ZERO)
                .accountType(account.getAccountType())
                .status(AccountStatus.ACTIVE)
                .build();
        log.info("corePaymentAccountDTO: {}", coreAccount);

        // Call API save account trên CoreBanking
        String url = "http://localhost:8083/corebanking/save-account";
        restTemplate.postForObject(url ,coreAccount,Void.class);

        return AccountCreateReponse.builder()
                .accountNumber(account.getAccountNumber())
                .cifCode(account.getCifCode())
                .id(account.getId())
                .accountType(account.getAccountType())
                .status(account.getStatus())
                .build();
    }

    /**
     * Extracts CIF code from temporary key
     */
    private String extractCifFromTempKey(String tempKey) {
        // Format: TEMP_PAYMENT_REQUEST:{cifCode}:{timestamp}
        String[] parts = tempKey.split(":");
        if (parts.length >= 3) {
            return parts[1];
        }
        throw new AppException(ErrorCode.UNCATERROR_ERROR);
    }

    /**
     * Validates OTP and returns the payment request if valid
     */
    private PaymentCreateDTO validateOTPAndGetTempRequest(PaymentConfirmOtpDTO confirmOtpDTO) {
        String keyOTP = "OTP:PAYMENT:" + confirmOtpDTO.getPaymentRequestId();
        log.info("keyOTP: {}", keyOTP);
        String storedOtp = (String) redisTemplate.opsForValue().get(keyOTP);
        log.info("storedOtp: {}", storedOtp);
        log.info("Validating OTP for temp request: {}", confirmOtpDTO.getPaymentRequestId());

            if (storedOtp == null) {
                throw new AppException(ErrorCode.OTP_EXPIRED); // Sử dụng error code có sẵn thay vì OTP_EXPIRED
            }

        if (!storedOtp.equals(confirmOtpDTO.getOtpCode())) {
            handleOTPFailure(confirmOtpDTO.getPaymentRequestId());
            throw new AppException(ErrorCode.INVALID_OTP); // Sử dụng error code có sẵn thay vì INVALID_OTP
        }

        // Lấy temp request
        PaymentCreateDTO tempRequest = (PaymentCreateDTO) redisTemplate.opsForValue().get(confirmOtpDTO.getPaymentRequestId());
        if (tempRequest == null) {
            throw new AppException(ErrorCode.UNCATERROR_ERROR);
        }

        log.info("OTP validated successfully for temp request: {}", confirmOtpDTO.getPaymentRequestId());
        return tempRequest;
    }

    /**
     * Handles OTP failure logic including failure counting
     */
    private void handleOTPFailure(String tempRequestKey) {
        String keyFailCount = "OTP_FAIL_COUNT:PAYMENT:" + tempRequestKey;
        String failStr = (String) redisTemplate.opsForValue().get(keyFailCount);
        int failCount = (failStr == null) ? 0 : Integer.parseInt(failStr);

        failCount++;
        redisTemplate.opsForValue().set(keyFailCount, String.valueOf(failCount), Duration.ofMinutes(5));

        if (failCount >= 3) {
            // Xóa temp request
            redisTemplate.delete(tempRequestKey);
            redisTemplate.delete("OTP:PAYMENT:" + tempRequestKey);
            log.error("Payment request creation failed due to OTP entered incorrectly more than 3 times");
            throw new AppException(ErrorCode.OTP_WRONG_MANY); // Sử dụng error code có sẵn thay vì OTP_WRONG_MANY
        }
    }

    /**
     * Generates OTP and stores in Redis
     */
    private String generateAndStoreOTP(String key) {
        String keyOTP = "OTP:PAYMENT:" + key;
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
                .subject("Xác thực OTP - Tạo tài khoản thanh toán")
                .build();
        streamBridge.send("mail-out-0", mailMessageDTO);
        log.info("OTP email sent to: {}", customer.getEmail());
    }

    public String generateAccountNumber(Account dto) {
        String cif = dto.getCifCode();
        int typeCode;
        if (dto.getAccountType().name().equals("PAYMENT")) {
            typeCode = 0;
        } else if (dto.getAccountType().name().equals("CREDIT")) {
            typeCode = 1;
        } else {
            typeCode = 2;
        }
        String randomPart = String.format("%03d", new Random().nextInt(1000));
        return cif + typeCode + randomPart;
    }

    /**
     * Lấy balance từ Core Banking Service
     */
    private BigDecimal getBalanceFromCorebanking(String accountNumber) {
        try {
            String url = coreBankingBaseUrl + "/get-balance-by-accountNumber/"+accountNumber;
            ResponseEntity<BalanceResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<BalanceResponse>() {}
            );

            BalanceResponse balanceResponse = response.getBody();
            if (balanceResponse != null && balanceResponse.getBalance() != null) {
                return balanceResponse.getBalance();
            }
            return BigDecimal.ZERO;
        } catch (Exception e) {
            log.warn("Failed to get balance from core banking for account: {}. Using zero balance.", accountNumber, e);
            return BigDecimal.ZERO;
        }
    }
}
