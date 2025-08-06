package com.example.account_service.service.impl;


import com.example.account_service.dto.request.PaymentConfirmOtpDTO;
import com.example.account_service.dto.request.PaymentCreateDTO;
import com.example.account_service.dto.response.AccountCreateReponse;
import com.example.account_service.dto.response.CicResponse;
import com.example.account_service.dto.response.CreditRequestReponse;
import com.example.account_service.dto.response.PaymentRequestResponse;
import com.example.account_service.entity.*;
import com.example.account_service.exception.AppException;
import com.example.account_service.exception.ErrorCode;
import com.example.account_service.repository.*;
import com.example.account_service.service.AccountService;
import com.example.common_service.constant.AccountStatus;
import com.example.common_service.constant.AccountType;
import com.example.common_service.constant.CustomerStatus;
import com.example.common_service.dto.*;
import com.example.common_service.dto.request.LoanRequestDTO;
import com.example.common_service.dto.response.*;
import com.example.common_service.services.CommonService;
import com.example.common_service.services.customer.CustomerQueryService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.rpc.RpcContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
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
    private final CreditRequestRepository creditRequestRepository;
    private final LoanAccountRepository loanAccountRepository;
    private final JwtDecoder jwtDecoder;
    @DubboReference(timeout = 5000)
    private CommonService commonService;
    @DubboReference(timeout = 5000)
    private final CustomerQueryService customerQueryService;
    @Autowired
    @Qualifier("restTemplateInternal")
    private  RestTemplate restTemplateInternal;
    @Autowired
    @Qualifier("coreBankingRestTemplate")
    private RestTemplate coreBankingRestTemplate;

    private final RedisTemplate<Object, Object> redisTemplate;

    private final StreamBridge streamBridge;
    @Autowired
    private ObjectMapper objectMapper;
    @Value("${core-banking.base-url:http://localhost:8083/corebanking}")
    private String coreBankingBaseUrl;

    public List<AccountSummaryDTO> getAllAccountsbyCifCode() {
        // Lấy thông tin người dùng từ context bảo mật
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName();
        
        log.info("GET_ALL_ACCOUNTS_START - UserId: {}", userId);

        try {
            // Lấy thông tin khách hàng hiện tại
            CustomerDTO currentCustomer = commonService.getCurrentCustomer(userId);
            String cifCode = currentCustomer.getCifCode();
            
            log.info("CUSTOMER_INFO_RETRIEVED - UserId: {}, CifCode: {}, CustomerStatus: {}",
                    userId, cifCode, currentCustomer.getStatus());

            List<AccountSummaryDTO> result = new ArrayList<>();

            // Lấy Payment Accounts
            List<Account> paymentAccounts = accountRepository.findByCifCodeAndAccountTypeAndStatus(
                    cifCode, AccountType.PAYMENT, AccountStatus.ACTIVE);
            
            log.info("PAYMENT_ACCOUNTS_RETRIEVED - CifCode: {}, Count: {}",
                    cifCode, paymentAccounts.size());
            
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
            
            log.info("SAVINGS_ACCOUNTS_RETRIEVED - CifCode: {}, Count: {}",
                    cifCode, savingsAccounts.size());
            
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
            
            log.info("CREDIT_ACCOUNTS_RETRIEVED - CifCode: {}, Count: {}",
                    cifCode, creditAccounts.size());
            
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

            log.info("GET_ALL_ACCOUNTS_SUCCESS - UserId: {}, CifCode: {}, TotalAccounts: {}, PaymentAccounts: {}, SavingsAccounts: {}, CreditAccounts: {}",
                    userId, cifCode, result.size(), paymentAccounts.size(), savingsAccounts.size(), creditAccounts.size());

            return result;
        } catch (Exception e) {
            log.error("GET_ALL_ACCOUNTS_ERROR - UserId: {}, Error: {}",
                    userId, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public List<AccountPaymentResponse> getAllPaymentAccountsbyCifCode() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName();
        log.info("GET_PAYMENT_ACCOUNTS_START - UserId: {}", userId);
        try {
            // Lấy thông tin khách hàng hiện tại
            CustomerDTO currentCustomer = commonService.getCurrentCustomer(userId);
            
            log.info("CUSTOMER_INFO_RETRIEVED - UserId: {}, CifCode: {}, CustomerStatus: {}",
                    userId, currentCustomer.getCifCode(), currentCustomer.getStatus());
            
            // check trang thai cua Customer trươc
            if (!currentCustomer.getStatus().equals(CustomerStatus.ACTIVE)) {
                log.warn("GET_PAYMENT_ACCOUNTS_FAILED - UserId: {}, CifCode: {}, Reason: CUSTOMER_NOT_ACTIVE",
                        userId, currentCustomer.getCifCode());
                throw new AppException(ErrorCode.CUSTOMER_NOTACTIVE);
            }
            
            String cifCode = currentCustomer.getCifCode();

            // Lấy Payment Accounts từ local database
            List<Account> paymentAccounts = accountRepository.findByCifCodeAndAccountTypeAndStatus(
                    cifCode, AccountType.PAYMENT, AccountStatus.ACTIVE);
            log.info("PAYMENT_ACCOUNTS_FOUND - UserId: {}, CifCode: {}, Count: {}",
                    userId, cifCode, paymentAccounts.size());

            // Kết hợp thông tin local với balance từ Core Banking
            List<AccountPaymentResponse> result = paymentAccounts.stream()
                    .map(account -> {
                        BigDecimal balance = getBalanceFromCorebanking(account.getAccountNumber());
                        log.info("ACCOUNT_BALANCE_RETRIEVED - AccountNumber: {}, Balance: {}",
                                account.getAccountNumber(), balance);
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

            log.info("GET_PAYMENT_ACCOUNTS_SUCCESS - UserId: {}, CifCode: {}, TotalAccounts: {}",
                    userId, cifCode, result.size());

            return result;
        } catch (Exception e) {
            log.error("GET_PAYMENT_ACCOUNTS_ERROR - UserId: {}, Error: {}",
                    userId, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public AccountPaymentResponse getAccountPaymentbyID(String id) {
        log.info("GET_PAYMENT_ACCOUNT_BY_ID_START - AccountNumber: {}", id);

        try {
            // Lấy thông tin account từ local database
            Account account = accountRepository.findByAccountNumber(id);
            if (account == null) {
                log.warn("GET_PAYMENT_ACCOUNT_BY_ID_FAILED - AccountNumber: {}, Reason: ACCOUNT_NOT_FOUND",
                        id);
                throw new AppException(ErrorCode.ACCOUNT_NOT_FOUND);
            }

            log.info("ACCOUNT_FOUND - AccountNumber: {}, CifCode: {}, AccountType: {}, Status: {}",
                    id, account.getCifCode(), account.getAccountType(), account.getStatus());

            // Lấy balance từ Core Banking
            BigDecimal balance = getBalanceFromCorebanking(account.getAccountNumber());
            
            log.info("ACCOUNT_BALANCE_RETRIEVED - AccountNumber: {}, Balance: {}",
                    account.getAccountNumber(), balance);

            // Kết hợp thông tin
            AccountPaymentResponse response = AccountPaymentResponse.builder()
                    .accountNumber(account.getAccountNumber())
                    .cifCode(account.getCifCode())
                    .accountType(account.getAccountType())
                    .balance(balance)
                    .status(account.getStatus())
                    .openedDate(account.getCreatedDate().toLocalDate())
                    .build();

            log.info("GET_PAYMENT_ACCOUNT_BY_ID_SUCCESS - AccountNumber: {}, CifCode: {}",
                    id, account.getCifCode());

            return response;
        } catch (Exception e) {
            log.error("GET_PAYMENT_ACCOUNT_BY_ID_ERROR - AccountNumber: {}, Error: {}",
                    id, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public List<SavingAccountResponse> getAllSavingAccountbyCifCode() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName();
        log.info("GET_SAVING_ACCOUNTS_START - UserId: {}", userId);
        try {
            // Lấy thông tin khách hàng hiện tại
            CustomerDTO currentCustomer = commonService.getCurrentCustomer(userId);
            String cifCode = currentCustomer.getCifCode();

            log.info("CUSTOMER_INFO_RETRIEVED - UserId: {}, CifCode: {}, CustomerStatus: {}",
                    userId, cifCode, currentCustomer.getStatus());
            if (currentCustomer.getStatus().equals(CustomerStatus.CLOSED)) {
                throw  new AppException(ErrorCode.CUSTOMER_NOTACTIVE);
            }
            List<SavingsAccount> savingsAccounts = savingsAccountRepository.findActiveSavingsAccountsByCifCode(cifCode);

            log.info("SAVINGS_ACCOUNTS_FOUND - UserId: {}, CifCode: {}, Count: {}",
                    userId, cifCode, savingsAccounts.size());

            // Kết hợp thông tin local với balance từ Core Banking
            List<SavingAccountResponse> result = savingsAccounts.stream()
                    .map(account -> {
                        BigDecimal balance = getBalanceFromCorebanking(account.getAccountNumber());
                        log.info("SAVINGS_ACCOUNT_BALANCE_RETRIEVED - AccountNumber: {}, Balance: {}",
                                account.getAccountNumber(), balance);
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
                                .accountNumberSrc(account.getAccountNumberSrc())
                                .build();
                    })
                    .collect(Collectors.toList());

            log.info("GET_SAVING_ACCOUNTS_SUCCESS - UserId: {}, CifCode: {}, TotalAccounts: {}",
                    userId, cifCode, result.size());

            return result;
        } catch (Exception e) {
            log.error("GET_SAVING_ACCOUNTS_ERROR - UserId: {}, Error: {}",
                    userId, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public List<CreditAccountResponse> getAllCreditAccountbyCifCode() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName();
        log.info("User id: " + userId);
        // Lấy thông tin khách hàng hiện tại
        CustomerDTO currentCustomer = commonService.getCurrentCustomer(userId);
        if (currentCustomer.getStatus().equals(CustomerStatus.CLOSED)) {
            throw  new AppException(ErrorCode.CUSTOMER_NOTACTIVE);
        }
        String cifCode = currentCustomer.getCifCode();
        // Lấy Savings Accounts từ local database
        List<CreditAccount> creditAccounts = creditAccountRepository.findActiveCreditAccountsByCifCode(cifCode);
        return creditAccounts.stream()
                .map(account -> {
                    BigDecimal balance = getBalanceFromCorebanking(account.getAccountNumber());
                    return CreditAccountResponse.builder()
                            .status(account.getStatus().name())
                            .accountNumber(account.getAccountNumber())
                            .cifCode(account.getCifCode())
                            .accountType(account.getAccountType().name())
                            .balance(balance)
                            .imageUrl(account.getCreditCardType().getImageUrl())
                            .creditLimit(account.getCreditLimit())
                            .currentDebt(account.getCurrentDebt())
                            .openedDate(account.getCreatedDate().toLocalDate())
                            .typeName(account.getCreditCardType().getTypeName())
                            .cardID(account.getCreditCardType().getId())
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<CreditAccountResponse> getAllCreditAccountNonbyCifCode() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName();
        log.info("User id: " + userId);
        // Lấy thông tin khách hàng hiện tại
        CustomerDTO currentCustomer = commonService.getCurrentCustomer(userId);
        if (!currentCustomer.getStatus().equals(CustomerStatus.ACTIVE)) {
            throw  new AppException(ErrorCode.CUSTOMER_NOTACTIVE);
        }
        String cifCode = currentCustomer.getCifCode();
        // Lấy Savings Accounts từ local database
        List<CreditAccount> creditAccounts = creditAccountRepository.findCreditAccountsByCifCode(cifCode);
        return creditAccounts.stream()
                .map(account -> {
                    return CreditAccountResponse.builder()
                            .status(account.getStatus().name())
                            .accountNumber(account.getAccountNumber())
                            .cifCode(account.getCifCode())
                            .accountType(account.getAccountType().name())
                            .imageUrl(account.getCreditCardType().getImageUrl())
                            .creditLimit(account.getCreditLimit())
                            .currentDebt(account.getCurrentDebt())
                            .openedDate(account.getCreatedDate().toLocalDate())
                            .typeName(account.getCreditCardType().getTypeName())
                            .cardID(account.getCreditCardType().getId())
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
            ResponseEntity<CicResponse> response = coreBankingRestTemplate.postForEntity(url, entity, CicResponse.class);
            log.info("Response : " + response.getBody());
            return response.getBody();
        } catch (RestClientException e) {
            log.info(e.getMessage());
            return null;
        }
    }


    @Override
    public PaymentRequestResponse createPaymentInit(PaymentCreateDTO paymentRequest) {
        String cifCode = paymentRequest.getCifCode();
        
        log.info("CREATE_PAYMENT_INIT_START - CifCode: {}", cifCode);

        try {
            CustomerDTO customer = commonService.getCustomerByCifCode(cifCode);
            if (customer == null) {
                log.warn("CREATE_PAYMENT_INIT_FAILED - CifCode: {}, Reason: CUSTOMER_NOT_FOUND", cifCode);
                throw new AppException(ErrorCode.CUSTOMER_NOT_FOUND);
            }
            
            log.info("CUSTOMER_FOUND - CifCode: {}, CustomerStatus: {}", cifCode, customer.getStatus());

            // Check trạng thái customer
            if (customer.getStatus() != CustomerStatus.ACTIVE) {
                log.warn("CREATE_PAYMENT_INIT_FAILED - CifCode: {}, CustomerStatus: {}, Reason: CUSTOMER_NOT_ACTIVE",
                        cifCode, customer.getStatus());
                throw new AppException(ErrorCode.CUSTOMER_NOTACTIVE);
            }

            PaymentRequestResponse response = createPaymentAccountDirectly(cifCode);
            
            log.info("CREATE_PAYMENT_INIT_SUCCESS - CifCode: {}, AccountNumber: {}, Status: {}",
                    cifCode, response.getId(), response.getStatus());

            return response;
        } catch (Exception e) {
            log.error("CREATE_PAYMENT_INIT_ERROR - CifCode: {}, Error: {}", cifCode, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public CustomerDTO getCustomerByAccountNumber(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber);
        if(account==null) throw new AppException(ErrorCode.USER_NOTEXISTED);
        return customerQueryService.getCustomerByCifCode(account.getCifCode());
    }

    @Override
    public List<CreditRequestReponse> getAllCreditRequestPending() {
        log.info("GET_ALL_CREDIT_REQUEST_PENDING_START");

        try {
            List<CreditRequest> list = creditRequestRepository.findAllByStatus();
            
            log.info("CREDIT_REQUESTS_FOUND - Count: {}", list.size());

            return list.stream()
                    .map(creditRequest -> {
                        try {
                            // Lấy thông tin khách hàng qua Dubbo service
                            CustomerDTO customer = commonService.getCustomerByCifCode(creditRequest.getCifCode());
                            return maptoCreditRequestReponse(creditRequest, customer);
                        } catch (Exception e) {
                            log.warn("CUSTOMER_INFO_NOT_FOUND - CifCode: {}, Error: {}", 
                                    creditRequest.getCifCode(), e.getMessage());
                            // Nếu không lấy được thông tin customer, vẫn trả về response nhưng không có fullname và email
                            return maptoCreditRequestReponse(creditRequest, null);
                        }
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("GET_ALL_CREDIT_REQUEST_PENDING_ERROR - Error: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    private CreditRequestReponse maptoCreditRequestReponse(CreditRequest creditRequest, CustomerDTO customer) {
        CreditRequestReponse.CreditRequestReponseBuilder builder = CreditRequestReponse.builder()
                .id(creditRequest.getId())
                .status(creditRequest.getStatus())
                .cartTypeId(creditRequest.getCartTypeId())
                .monthlyIncome(creditRequest.getMonthlyIncome())
                .occupation(creditRequest.getOccupation())
                .cifCode(creditRequest.getCifCode())
                .reason(creditRequest.getReason())
                .accountNumber(creditRequest.getAccountNumber());
        
        // Thêm thông tin khách hàng nếu có
        if (customer != null) {
            builder.fullname(customer.getFullName())
                   .email(customer.getEmail())
                    .dateOfBirth(customer.getDateOfBirth())
                    .identityNumber(customer.getIdentityNumber())
                    .phoneNumber(customer.getPhoneNumber());
                    ;
        }
        
        return builder.build();
    }

    @Override
    public Page<CreditRequestReponse> getAllCreditRequestPendingPaginated(Pageable pageable) {
        log.info("GET_ALL_CREDIT_REQUEST_PENDING_PAGINATED_START - Page: {}, Size: {}", 
                pageable.getPageNumber(), pageable.getPageSize());

        try {
            Page<CreditRequest> creditRequestPage = creditRequestRepository.findAllByStatusWithPagination(pageable);
            
            log.info("CREDIT_REQUESTS_FOUND_PAGINATED - TotalElements: {}, TotalPages: {}, CurrentPage: {}", 
                    creditRequestPage.getTotalElements(), 
                    creditRequestPage.getTotalPages(), 
                    creditRequestPage.getNumber());

            List<CreditRequestReponse> responseList = creditRequestPage.getContent().stream()
                    .map(creditRequest -> {
                        try {
                            // Lấy thông tin khách hàng qua Dubbo service
                            CustomerDTO customer = commonService.getCustomerByCifCode(creditRequest.getCifCode());
                            return maptoCreditRequestReponse(creditRequest, customer);
                        } catch (Exception e) {
                            log.warn("CUSTOMER_INFO_NOT_FOUND_PAGINATED - CifCode: {}, Error: {}", 
                                    creditRequest.getCifCode(), e.getMessage());
                            // Nếu không lấy được thông tin customer, vẫn trả về response nhưng không có fullname và email
                            return maptoCreditRequestReponse(creditRequest, null);
                        }
                    })
                    .collect(Collectors.toList());

            Page<CreditRequestReponse> result = new PageImpl<>(
                    responseList, 
                    pageable, 
                    creditRequestPage.getTotalElements()
            );

            log.info("GET_ALL_CREDIT_REQUEST_PENDING_PAGINATED_SUCCESS - TotalElements: {}, TotalPages: {}, CurrentPage: {}, ContentSize: {}", 
                    result.getTotalElements(), 
                    result.getTotalPages(), 
                    result.getNumber(),
                    result.getContent().size());

            return result;
        } catch (Exception e) {
            log.error("GET_ALL_CREDIT_REQUEST_PENDING_PAGINATED_ERROR - Error: {}", e.getMessage(), e);
            throw e;
        }
    }

    private CreditRequestReponse maptoCreditRequestReponse(CreditRequest creditRequest) {
        CreditRequestReponse creditRequestReponse = CreditRequestReponse.builder()
                .id(creditRequest.getId())
                .status(creditRequest.getStatus())
                .cartTypeId(creditRequest.getCartTypeId())
                .monthlyIncome(creditRequest.getMonthlyIncome())
                .occupation(creditRequest.getOccupation())
                .cifCode(creditRequest.getCifCode())
                .reason(creditRequest.getReason())
                .accountNumber(creditRequest.getAccountNumber())
                .build();
        return creditRequestReponse;
    }


    @Override
    public PaymentRequestResponse createPaymentRequest(String cifCode) {
        log.info("CREATE_PAYMENT_REQUEST_START - CifCode: {}", cifCode);

        try {
            // Lấy thông tin customer theo cifCode
            CustomerDTO customer = commonService.getCustomerByCifCode(cifCode);
            if (customer == null) {
                log.warn("CREATE_PAYMENT_REQUEST_FAILED - CifCode: {}, Reason: CUSTOMER_NOT_FOUND", cifCode);
                throw new AppException(ErrorCode.CUSTOMER_NOT_FOUND);
            }
            
            log.info("CUSTOMER_FOUND - CifCode: {}, CustomerStatus: {}", cifCode, customer.getStatus());

            // Check trạng thái customer
            if (customer.getStatus() != CustomerStatus.ACTIVE) {
                log.warn("CREATE_PAYMENT_REQUEST_FAILED - CifCode: {}, CustomerStatus: {}, Reason: CUSTOMER_NOT_ACTIVE",
                        cifCode, customer.getStatus());
                throw new AppException(ErrorCode.CUSTOMER_NOTACTIVE);
            }

            // Check KYC status from CustomerDTO
            if (!customer.isKycVerified()) {
                log.warn("CREATE_PAYMENT_REQUEST_FAILED - CifCode: {}, Reason: KYC_NOT_VERIFIED", cifCode);
                throw new AppException(ErrorCode.KYC_INVALID);
            }
            
            log.info("KYC_CHECK_SUCCESS - CifCode: {}, KycVerified: true", cifCode);

            PaymentRequestResponse result = createPaymentRequestWithOtp(cifCode);
            
            log.info("CREATE_PAYMENT_REQUEST_SUCCESS - CifCode: {}, TempRequestId: {}, Status: {}",
                    cifCode, result.getId(), result.getStatus());

            return result;
        } catch (Exception e) {
            log.error("CREATE_PAYMENT_REQUEST_ERROR - CifCode: {}, Error: {}", cifCode, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public AccountCreateReponse confirmOtpAndCreatePayment(PaymentConfirmOtpDTO paymentConfirmOtpDTO) {
        String tempRequestId = paymentConfirmOtpDTO.getPaymentRequestId();
        
        log.info("CONFIRM_OTP_CREATE_PAYMENT_START - TempRequestId: {}", tempRequestId);

        try {
            // Validate OTP
            PaymentCreateDTO tempRequest = validateOTPAndGetTempRequest(paymentConfirmOtpDTO);

            // Lấy thông tin customer
            String cifCode = extractCifFromTempKey(tempRequestId);
            
            log.info("CIF_CODE_EXTRACTED - TempRequestId: {}, CifCode: {}", tempRequestId, cifCode);
            
            CustomerDTO customerDTO = commonService.getCustomerByCifCode(cifCode);
            
            log.info("CUSTOMER_RETRIEVED - CifCode: {}, CustomerName: {}", cifCode, customerDTO.getFullName());

            // Tạo Payment Account
            AccountCreateReponse response = createPaymentAccountForCustomer(cifCode);

            // Cleanup temp data
            redisTemplate.delete(tempRequestId);
            redisTemplate.delete("OTP:PAYMENT:" + tempRequestId);
            
            log.info("TEMP_DATA_CLEANED - TempRequestId: {}, RedisKeysDeleted: 2", tempRequestId);

            log.info("CONFIRM_OTP_CREATE_PAYMENT_SUCCESS - TempRequestId: {}, CifCode: {}, AccountNumber: {}",
                    tempRequestId, cifCode, response.getAccountNumber());
            
            return response;
        } catch (Exception e) {
            log.error("CONFIRM_OTP_CREATE_PAYMENT_ERROR - TempRequestId: {}, Error: {}",
                    tempRequestId, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public void resendPaymentOtp(String tempRequestKey) {
        log.info("RESEND_PAYMENT_OTP_START - TempRequestKey: {}", tempRequestKey);

        try {
            // Kiểm tra temp request có tồn tại không
            Object tempRequest = redisTemplate.opsForValue().get(tempRequestKey);
            if (tempRequest == null) {
                log.warn("RESEND_PAYMENT_OTP_FAILED - TempRequestKey: {}, Reason: TEMP_REQUEST_NOT_FOUND", tempRequestKey);
                throw new AppException(ErrorCode.UNCATERROR_ERROR);
            }

            log.info("TEMP_REQUEST_EXISTS - TempRequestKey: {}", tempRequestKey);

            // Lấy thông tin customer từ temp key
            String cifCode = extractCifFromTempKey(tempRequestKey);
            
            log.info("CIF_CODE_EXTRACTED - TempRequestKey: {}, CifCode: {}", tempRequestKey, cifCode);
            
            CustomerDTO customerDTO = commonService.getCustomerByCifCode(cifCode);

            if (customerDTO == null) {
                log.warn("RESEND_PAYMENT_OTP_FAILED - TempRequestKey: {}, CifCode: {}, Reason: CUSTOMER_NOT_FOUND", 
                        tempRequestKey, cifCode);
                throw new AppException(ErrorCode.CUSTOMER_NOT_FOUND);
            }

            log.info("CUSTOMER_FOUND - TempRequestKey: {}, CifCode: {}, CustomerName: {}", 
                    tempRequestKey, cifCode, customerDTO.getFullName());

            String otp = generateAndStoreOTP(tempRequestKey);
            sendOTPEmail(customerDTO, otp);

            log.info("RESEND_PAYMENT_OTP_SUCCESS - TempRequestKey: {}, CifCode: {}, Email: {}", 
                    tempRequestKey, cifCode, customerDTO.getEmail());
        } catch (Exception e) {
            log.error("RESEND_PAYMENT_OTP_ERROR - TempRequestKey: {}, Error: {}", 
                    tempRequestKey, e.getMessage(), e);
            throw e;
        }
    }


    private PaymentRequestResponse createPaymentAccountDirectly(String cifCode) {
        log.info("CREATE_PAYMENT_ACCOUNT_DIRECTLY_START - CifCode: {}", cifCode);
        
        try {
            // Tạo account luôn không cần OTP
            AccountCreateReponse account = createPaymentAccountForCustomer(cifCode);

            PaymentRequestResponse response = PaymentRequestResponse.builder()
                    .id(account.getId())
                    .cifCode(cifCode)
                    .accountType(AccountType.PAYMENT)
                    .status(PaymentRequestResponse.PaymentRequestStatus.APPROVED)
                    .build();

            log.info("CREATE_PAYMENT_ACCOUNT_DIRECTLY_SUCCESS - CifCode: {}, AccountNumber: {}",
                    cifCode, account.getAccountNumber());

            return response;
        } catch (Exception e) {
            log.error("CREATE_PAYMENT_ACCOUNT_DIRECTLY_ERROR - CifCode: {}, Error: {}",
                    cifCode, e.getMessage(), e);
            throw e;
        }
    }

    private PaymentRequestResponse createPaymentRequestWithOtp(String cifCode) {
        log.info("CREATE_PAYMENT_REQUEST_WITH_OTP_START - CifCode: {}", cifCode);
        try {
            // Tạo temporary key để lưu thông tin request trước khi verify OTP
            String tempRequestKey = "TEMP_PAYMENT_REQUEST:" + cifCode + ":" + System.currentTimeMillis();
            log.info("TEMP_REQUEST_KEY_GENERATED - CifCode: {}, TempKey: {}", cifCode, tempRequestKey);
            // Lưu thông tin request vào Redis (expire sau 1 giờ)
            PaymentCreateDTO tempRequest = PaymentCreateDTO.builder().cifCode(cifCode).build();
            redisTemplate.opsForValue().set(tempRequestKey, tempRequest, Duration.ofMinutes(60));
            log.info("TEMP_REQUEST_STORED_REDIS - CifCode: {}, TempKey: {}, ExpiryMinutes: 60",cifCode, tempRequestKey);
            // Lấy thông tin customer để gửi OTP
            CustomerDTO customer = commonService.getCustomerByCifCode(cifCode);
            // Tạo và gửi OTP
            String otp = generateAndStoreOTP(tempRequestKey);
            sendOTPEmail(customer, otp);
            log.info("OTP: {}:",otp);
            log.info("OTP_PROCESS_COMPLETED - CifCode: {}, TempKey: {}, EmailSent: true",cifCode, tempRequestKey);
            // Trả về response với temp key để client có thể confirm OTP
            PaymentRequestResponse response = PaymentRequestResponse.builder()
                    .id(tempRequestKey).cifCode(cifCode)
                    .accountType(AccountType.PAYMENT)
                    .status(PaymentRequestResponse.PaymentRequestStatus.PENDING)
                    .build();
            log.info("CREATE_PAYMENT_REQUEST_WITH_OTP_SUCCESS - CifCode: {}, TempKey: {}, Status: PENDING",cifCode, tempRequestKey);
            return response;
        } catch (Exception e) {
            log.error("CREATE_PAYMENT_REQUEST_WITH_OTP_ERROR - CifCode: {}, Error: {}",cifCode, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Validates OTP and returns the payment request if valid
     */
    private PaymentCreateDTO validateOTPAndGetTempRequest(PaymentConfirmOtpDTO confirmOtpDTO) {
        String tempRequestId = confirmOtpDTO.getPaymentRequestId();
        String keyOTP = "OTP:PAYMENT:" + tempRequestId;
        String providedOtp = confirmOtpDTO.getOtpCode();
        log.info("OTP_VALIDATION_START - TempRequestId: {}, OtpKey: {}", tempRequestId, keyOTP);
        try {
            String storedOtp = (String) redisTemplate.opsForValue().get(keyOTP);
            log.info("OTP_RETRIEVED_FROM_REDIS - TempRequestId: {}, OtpExists: {}", tempRequestId, storedOtp != null);
            if (storedOtp == null) {
                log.warn("OTP_VALIDATION_FAILED - TempRequestId: {}, Reason: OTP_EXPIRED", tempRequestId);
                throw new AppException(ErrorCode.OTP_EXPIRED);
            }
            if (!storedOtp.equals(providedOtp)) {
                log.warn("OTP_VALIDATION_FAILED - TempRequestId: {}, Reason: INVALID_OTP", tempRequestId);
                handleOTPFailure(tempRequestId);
                throw new AppException(ErrorCode.INVALID_OTP);
            }
            log.info("OTP_VALIDATION_SUCCESS - TempRequestId: {}", tempRequestId);
            // Lấy temp request
            PaymentCreateDTO tempRequest = (PaymentCreateDTO) redisTemplate.opsForValue().get(tempRequestId);
            if (tempRequest == null) {
                log.warn("TEMP_REQUEST_NOT_FOUND - TempRequestId: {}, Reason: TEMP_REQUEST_EXPIRED", tempRequestId);
                throw new AppException(ErrorCode.UNCATERROR_ERROR);
            }
            log.info("TEMP_REQUEST_RETRIEVED - TempRequestId: {}, CifCode: {}", tempRequestId, tempRequest.getCifCode());
            return tempRequest;
        } catch (Exception e) {
            log.error("OTP_VALIDATION_ERROR - TempRequestId: {}, Error: {}", tempRequestId, e.getMessage(), e);
            throw e;
        }
    }
    @Override
    public AccountDTO createLoanAccount(LoanRequestDTO dto) {
        long startTime = System.currentTimeMillis();
        log.info("[createLoanAccount] Start processing loan account creation");
        String username = null;
        try {
            // Validate loanId
            if (dto.getLoanId() == null) {
                log.error("[createLoanAccount] ERROR: loanId is null in request");
                throw new IllegalArgumentException("loanId cannot be null when creating loan account");
            }
            
            username =RpcContext.getContext().getAttachment("username");
            log.info("user name: {}",username);
            // Lấy số tài khoản từ coreBanking
            String urlGetAccountNumber = coreBankingBaseUrl + "/getAccountNumber/{typeAccount}";
            log.info("[createLoanAccount] Calling coreBanking API: GET {}", urlGetAccountNumber);
            String number = coreBankingRestTemplate.getForObject(urlGetAccountNumber, String.class, AccountType.LOAN);
            log.info("[createLoanAccount] Received account number: {}", number);

            // Lấy thông tin khách hàng
            log.info("[createLoanAccount] Fetching customer info for username: {}", username);
            CustomerDTO customer = commonService.getCurrentCustomer(username);
            log.info("[createLoanAccount] Customer data: {}", objectMapper.writeValueAsString(customer));

            // Khởi tạo đối tượng LoanAccount
            LoanAccount loanAccount = new LoanAccount();
            loanAccount.setAccountNumber(number);
            loanAccount.setAccountType(AccountType.LOAN);
            loanAccount.setCifCode(customer.getCifCode());
            loanAccount.setStatus(AccountStatus.ACTIVE);
            loanAccount.setLoanAmount(dto.getAmount());
            loanAccount.setTermMonths(dto.getTermMonths());
            loanAccount.setCreatedBy(customer.getUsername());
            loanAccount.setInterestRate(dto.getInterestRate());
            loanAccount.setLoanId(dto.getLoanId()); // Set loanId để link với loan
            log.info("[createLoanAccount] Set loanId: {} for account: {}", dto.getLoanId(), number);
            // Tính tổng số tiền phải trả (gốc + lãi dự kiến)
            BigDecimal amount = dto.getAmount();
            BigDecimal interestRate = dto.getInterestRate();
            int termMonths = dto.getTermMonths() != null ? dto.getTermMonths() : 0;
            // lãi dự kiến = amount * interestRate * termMonths / 12 / 100
            BigDecimal interest = amount.multiply(interestRate)
                    .multiply(BigDecimal.valueOf(termMonths))
                    .divide(BigDecimal.valueOf(12), 2, BigDecimal.ROUND_HALF_UP)
                    .divide(BigDecimal.valueOf(100), 2, BigDecimal.ROUND_HALF_UP);
            loanAccount.setOutstandingDebt(amount.add(interest));
            loanAccount.setCreatedDate(LocalDateTime.now());

            log.info("[createLoanAccount] LoanAccount entity to save: {}", objectMapper.writeValueAsString(loanAccount));

            loanAccountRepository.save(loanAccount);
            log.info("[createLoanAccount] LoanAccount saved successfully. Account number: {}, LoanId: {}", number, dto.getLoanId());

            // Gửi dữ liệu lên CoreBanking
            CoreAccountRequest coreAccount = CoreAccountRequest.builder()
                    .accountNumber(number)
                    .cifCode(customer.getCifCode())
                    .balance(BigDecimal.ZERO)
                    .accountType(AccountType.LOAN)
                    .status(AccountStatus.ACTIVE)
                    .build();

            String url = coreBankingBaseUrl + "/save-account";
            log.info("[createLoanAccount] Sending account to coreBanking. URL: {}", url);
            log.info("[createLoanAccount] Payload: {}", objectMapper.writeValueAsString(coreAccount));

            coreBankingRestTemplate.postForObject(url, coreAccount, Void.class);
            log.info("[createLoanAccount] CoreBanking API call completed");
            AccountDTO accountDTO = AccountDTO.builder().accountNumber(loanAccount.getAccountNumber()).accountType(loanAccount.getAccountType().name()).balance(loanAccount.getLoanAmount()).cifCode(loanAccount.getCifCode()).status(loanAccount.getStatus().name()).build();

            return  accountDTO;
        } catch (Exception e) {
            log.error("[createLoanAccount] Exception occurred: {}", e.getMessage(), e);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            log.info("[createLoanAccount] Finished processing in {} ms", duration);
        }

        return null;
    }


    private AccountCreateReponse createPaymentAccountForCustomer(String cifCode) {
        log.info("CREATE_PAYMENT_ACCOUNT_FOR_CUSTOMER_START - CifCode: {}", cifCode);

        try {
            Account account = Account.builder()
                    .accountType(AccountType.PAYMENT)
                    .cifCode(cifCode)
                    .status(AccountStatus.ACTIVE)
                    .build();
            String urlGetAccountNumber = coreBankingBaseUrl + "/getAccountNumber/{typeAccount}";
            String number = coreBankingRestTemplate.getForObject(urlGetAccountNumber, String.class, account.getAccountType().name());
            account.setAccountNumber(number);
            log.info("ACCOUNT_NUMBER_GENERATED - CifCode: {}, AccountNumber: {}, AccountType: {}",
                    cifCode, number, AccountType.PAYMENT);
            
            accountRepository.save(account);
            
            log.info("ACCOUNT_SAVED_LOCAL - CifCode: {}, AccountNumber: {}, AccountId: {}",
                    cifCode, account.getAccountNumber(), account.getId());

            CoreAccountRequest coreAccount = CoreAccountRequest.builder()
                    .accountNumber(number)
                    .cifCode(cifCode)
                    .balance(BigDecimal.ZERO)
                    .accountType(account.getAccountType())
                    .status(AccountStatus.ACTIVE)
                    .build();
            
            log.info("CORE_BANKING_SYNC_START - CifCode: {}, AccountNumber: {}, Balance: {}",
                    cifCode, account.getAccountNumber(), BigDecimal.ZERO);
            String url = coreBankingBaseUrl + "/save-account";
            coreBankingRestTemplate.postForObject(url, coreAccount, Void.class);
            
            log.info("CORE_BANKING_SYNC_SUCCESS - CifCode: {}, AccountNumber: {}, CoreBankingUrl: {}",
                    cifCode, account.getAccountNumber(), url);

            AccountCreateReponse response = AccountCreateReponse.builder()
                    .accountNumber(account.getAccountNumber())
                    .cifCode(account.getCifCode())
                    .id(account.getId())
                    .accountType(account.getAccountType())
                    .status(account.getStatus())
                    .build();

            log.info("CREATE_PAYMENT_ACCOUNT_FOR_CUSTOMER_SUCCESS - CifCode: {}, AccountNumber: {}, AccountId: {}",
                    cifCode, account.getAccountNumber(), account.getId());

            return response;
        } catch (Exception e) {
            log.error("CREATE_PAYMENT_ACCOUNT_FOR_CUSTOMER_ERROR - CifCode: {}, Error: {}",
                    cifCode, e.getMessage(), e);
            throw e;
        }
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
     * Handles OTP failure logic including failure counting
     */
    private void handleOTPFailure(String tempRequestKey) {
        String keyFailCount = "OTP_FAIL_COUNT:PAYMENT:" + tempRequestKey;
        
        log.info("OTP_FAILURE_HANDLING_START - TempRequestKey: {}, FailCountKey: {}", tempRequestKey, keyFailCount);

        try {
            String failStr = (String) redisTemplate.opsForValue().get(keyFailCount);
            int failCount = (failStr == null) ? 0 : Integer.parseInt(failStr);
            int newFailCount = failCount + 1;

            log.info("OTP_FAIL_COUNT_INCREMENTED - TempRequestKey: {}, OldCount: {}, NewCount: {}", 
                    tempRequestKey, failCount, newFailCount);

            redisTemplate.opsForValue().set(keyFailCount, String.valueOf(newFailCount), Duration.ofMinutes(5));

            if (newFailCount >= 3) {
                // Xóa temp request
                redisTemplate.delete(tempRequestKey);
                redisTemplate.delete("OTP:PAYMENT:" + tempRequestKey);
                
                log.warn("OTP_FAILURE_LIMIT_EXCEEDED - TempRequestKey: {}, FailCount: {}, Action: TEMP_DATA_DELETED", 
                        tempRequestKey, newFailCount);
                
                throw new AppException(ErrorCode.OTP_WRONG_MANY);
            }

            log.info("OTP_FAILURE_HANDLED - TempRequestKey: {}, FailCount: {}, RemainingAttempts: {}", 
                    tempRequestKey, newFailCount, (3 - newFailCount));
        } catch (Exception e) {
            log.error("OTP_FAILURE_HANDLING_ERROR - TempRequestKey: {}, Error: {}", tempRequestKey, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Generates OTP and stores in Redis
     */
    private String generateAndStoreOTP(String key) {
        String keyOTP = "OTP:PAYMENT:" + key;
        String otp = String.valueOf(100000 + new Random().nextInt(900000));
        
        log.info("OTP_GENERATION_START - TempKey: {}, OtpKey: {}", key, keyOTP);
        
        redisTemplate.opsForValue().set(keyOTP, otp, Duration.ofMinutes(3)); // OTP có hiệu lực 3 phút
        
        log.info("OTP_GENERATION_SUCCESS - TempKey: {}, OtpKey: {}, ExpiryMinutes: 3", key, keyOTP);
        return otp;
    }

    /**
     * Sends OTP email to customer
     */
    private void sendOTPEmail(CustomerDTO customer, String otp) {
        log.info("SEND_OTP_EMAIL_START - Email: {}, CustomerName: {}", customer.getEmail(), customer.getFullName());
        
        try {
            MailMessageDTO mailMessageDTO = MailMessageDTO.builder()
                    .recipientName(customer.getFullName())
                    .recipient(customer.getEmail())
                    .body(otp)
                    .subject("Xác thực OTP - Tạo tài khoản thanh toán")
                    .build();
            
            boolean sent = streamBridge.send("mail-out-0", mailMessageDTO);
            if (sent) {
                log.info("SEND_OTP_EMAIL_SUCCESS - Email: {}, KafkaTopic: mail-out-0", customer.getEmail());
            } else {
                log.error("SEND_OTP_EMAIL_FAILED - Email: {}, Reason: KAFKA_SEND_FAILED", customer.getEmail());
                throw new AppException(ErrorCode.UNCATERROR_ERROR);
            }
        } catch (Exception e) {
            log.error("SEND_OTP_EMAIL_ERROR - Email: {}, Error: {}", customer.getEmail(), e.getMessage(), e);
            throw e;
        }
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
    @Override
    public BigDecimal getBalanceFromCorebanking(String accountNumber) {
        log.info("GET_BALANCE_FROM_COREBANKING_START - AccountNumber: {}", accountNumber);

        try {
            String url = coreBankingBaseUrl + "/get-balance-by-accountNumber/" + accountNumber;
            
            log.info("CORE_BANKING_BALANCE_REQUEST - AccountNumber: {}, Url: {}", accountNumber, url);
            
            ResponseEntity<BalanceResponse> response = coreBankingRestTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<BalanceResponse>() {}
            );

            BalanceResponse balanceResponse = response.getBody();
            if (balanceResponse != null && balanceResponse.getBalance() != null) {
                log.info("GET_BALANCE_FROM_COREBANKING_SUCCESS - AccountNumber: {}, Balance: {}", 
                        accountNumber, balanceResponse.getBalance());
                return balanceResponse.getBalance();
            }
            
            log.warn("GET_BALANCE_FROM_COREBANKING_NULL_RESPONSE - AccountNumber: {}, UsingZeroBalance: true", accountNumber);
            return BigDecimal.ZERO;
        } catch (Exception e) {
            log.warn("GET_BALANCE_FROM_COREBANKING_ERROR - AccountNumber: {}, Error: {}, UsingZeroBalance: true", 
                    accountNumber, e.getMessage(), e);
            return BigDecimal.ZERO;
        }
    }

    @Override
    public void updateAccountFromLoan(LoanRequestDTO dto) {
        LoanAccount loanAccount = null;
        
        // Tìm loan account bằng loanId trước (ưu tiên)
        if (dto.getLoanId() != null) {
            loanAccount = loanAccountRepository.findByLoanId(dto.getLoanId());
            if (loanAccount != null) {
                log.info("[updateAccountFromLoan] Found loan account by loanId: {}", dto.getLoanId());
            }
        }
        
        // Nếu không tìm thấy bằng loanId, tìm bằng account number
        if (loanAccount == null) {
            loanAccount = loanAccountRepository.findByAccountNumber(dto.getDisbursementAccountNumber());
            if (loanAccount != null) {
                log.info("[updateAccountFromLoan] Found loan account by account number: {}", dto.getDisbursementAccountNumber());
            }
        }
        
        if (loanAccount == null) {
            log.warn("[updateAccountFromLoan] Không tìm thấy tài khoản vay với loanId: {} hoặc account number: {}", 
                dto.getLoanId(), dto.getDisbursementAccountNumber());
            return;
        }

        CustomerResponse customer = customerQueryService.getCurrentCustomer();
        loanAccount.setLoanAmount(dto.getAmount());
        loanAccount.setTermMonths(dto.getTermMonths());
        loanAccount.setInterestRate(dto.getInterestRate());
        loanAccount.setStatus(dto.getStatus().equals("CLOSED") ? AccountStatus.CLOSED : AccountStatus.ACTIVE);
        
        // Xử lý outstanding debt
        if (dto.getStatus().equals("CLOSED")) {
            // Khi đóng khoản vay, outstanding debt = 0
            loanAccount.setOutstandingDebt(BigDecimal.ZERO);
            log.info("[updateAccountFromLoan] Đóng khoản vay - outstanding debt set to 0 cho account: {}", loanAccount.getAccountNumber());
        } else {
            // Cập nhật outstanding debt bình thường
            BigDecimal paidAmount = dto.getPaidAmount() ;
            log.info("[updateAccountFromLoan] paidAmount: {}", paidAmount);
            BigDecimal newOutstanding = loanAccount.getOutstandingDebt().subtract(paidAmount);
            loanAccount.setOutstandingDebt(newOutstanding.max(BigDecimal.ZERO));
            log.info("[updateAccountFromLoan] Cập nhật dư nợ cho account {} (loanId: {}): {}", 
                loanAccount.getAccountNumber(), dto.getLoanId(), newOutstanding);
        }
        
        loanAccount.setLastModifiedBy(customer.getUserId());
        loanAccount.setLastModifiedDate(LocalDateTime.now());
        loanAccountRepository.save(loanAccount);
    }
}
 