package com.example.account_service.service.impl;

import com.example.account_service.dto.response.AccountCreateReponse;
import com.example.account_service.entity.Account;
import com.example.account_service.repository.AccountRepository;
import com.example.common_service.constant.AccountStatus;
import com.example.common_service.constant.AccountType;
import com.example.common_service.dto.AccountDTO;
import com.example.common_service.dto.CoreAccountRequest;
import com.example.common_service.dto.PaymentCreateDTO;
import com.example.common_service.dto.response.PaymentRequestResponse;
import com.example.common_service.services.customer.CustomerCommonService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

@DubboService
@RequiredArgsConstructor
@Slf4j
public class AccountCustomerDubboServiceImpl implements CustomerCommonService {

    private final AccountRepository accountRepository;
    private final RestTemplate restTemplate;

    @Override
    public List<AccountDTO> getAccountsByCifCode(String cifCode) {
        String requestId = UUID.randomUUID().toString();
        log.info("GET_ACCOUNTS_BY_CIF_REQUEST - RequestId: {}, CifCode: {}", requestId, cifCode);
        try {
            List<Account> accounts = accountRepository.findByCifCode(cifCode);
            List<AccountDTO> result = accounts.stream()
                    .map(this::mapToDto)
                    .collect(Collectors.toList());
            log.info("GET_ACCOUNTS_BY_CIF_SUCCESS - RequestId: {}, CifCode: {}, Count: {}", requestId, cifCode, result.size());
            return result;
        } catch (Exception e) {
            log.error("GET_ACCOUNTS_BY_CIF_FAILED - RequestId: {}, CifCode: {}, Error: {}", requestId, cifCode, e.getMessage(), e);
            throw new RuntimeException("Unable to get account list: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public PaymentRequestResponse createPaymentInit(PaymentCreateDTO paymentRequest) {
        String requestId = UUID.randomUUID().toString();
        log.info("CREATE_PAYMENT_INIT_REQUEST - RequestId: {}, CifCode: {}", requestId, paymentRequest.getCifCode());
        return createPaymentAccountDirectly(paymentRequest.getCifCode(), requestId);
    }

    private PaymentRequestResponse createPaymentAccountDirectly(String cifCode, String requestId) {
        try {
            // Create account in local database
            AccountCreateReponse account = createPaymentAccountForCustomer(cifCode, requestId);

            log.info("CREATE_PAYMENT_ACCOUNT_DIRECTLY_SUCCESS - RequestId: {}, CifCode: {}, AccountNumber: {}", requestId, cifCode, account.getAccountNumber());

            return PaymentRequestResponse.builder()
                    .id(account.getId())
                    .cifCode(cifCode)
                    .accountType(AccountType.PAYMENT)
                    .status(PaymentRequestResponse.PaymentRequestStatus.APPROVED)
                    .build();

        } catch (Exception e) {
            log.error("CREATE_PAYMENT_ACCOUNT_DIRECTLY_FAILED - RequestId: {}, CifCode: {}, Error: {}", requestId, cifCode, e.getMessage(), e);
            throw new RuntimeException("Unable to create payment account directly: " + e.getMessage(), e);
        }
    }

    private AccountCreateReponse createPaymentAccountForCustomer(String cifCode, String requestId) {
        Account account = null;
        try {
            // 1. Create account in local database
            account = Account.builder()
                    .accountType(AccountType.PAYMENT)
                    .cifCode(cifCode)
                    .status(AccountStatus.ACTIVE)
                    .build();

            account.setAccountNumber(generateAccountNumber(account));
            account = accountRepository.save(account);

            log.info("CREATE_PAYMENT_ACCOUNT_LOCAL_SUCCESS - RequestId: {}, CifCode: {}, AccountNumber: {}", requestId, cifCode, account.getAccountNumber());

            // 2. Sync with CoreBanking
            syncWithCoreBanking(account, requestId);

            return AccountCreateReponse.builder()
                    .accountNumber(account.getAccountNumber())
                    .cifCode(account.getCifCode())
                    .id(account.getId())
                    .accountType(account.getAccountType())
                    .status(account.getStatus())
                    .build();

        } catch (Exception e) {
            log.error("CREATE_PAYMENT_ACCOUNT_FOR_CUSTOMER_FAILED - RequestId: {}, CifCode: {}, Error: {}", requestId, cifCode, e.getMessage(), e);

            // Rollback: Delete account if created in DB but sync failed
            if (account != null && account.getId() != null) {
                try {
                    accountRepository.deleteById(account.getId());
                    log.info("ROLLBACK_ACCOUNT_SUCCESS - RequestId: {}, AccountId: {}", requestId, account.getId());
                } catch (Exception rollbackEx) {
                    log.error("ROLLBACK_ACCOUNT_FAILED - RequestId: {}, AccountId: {}, Error: {}", requestId, account.getId(), rollbackEx.getMessage(), rollbackEx);
                    throw new RuntimeException("Rollback account failed: " + rollbackEx.getMessage(), rollbackEx);
                }
            }

            throw new RuntimeException("Unable to create payment account: " + e.getMessage(), e);
        }
    }

    private void syncWithCoreBanking(Account account, String requestId) {
        try {
            CoreAccountRequest coreAccount = CoreAccountRequest.builder()
                    .accountNumber(account.getAccountNumber())
                    .cifCode(account.getCifCode())
                    .balance(BigDecimal.ZERO)
                    .accountType(account.getAccountType())
                    .status(AccountStatus.ACTIVE)
                    .build();

            String url = "http://localhost:8083/corebanking/save-account";

            log.info("SYNC_WITH_COREBANKING_REQUEST - RequestId: {}, URL: {}, AccountNumber: {}", requestId, url, account.getAccountNumber());

            restTemplate.postForObject(url, coreAccount, Void.class);

            log.info("SYNC_WITH_COREBANKING_SUCCESS - RequestId: {}, AccountNumber: {}", requestId, account.getAccountNumber());

        } catch (Exception e) {
            log.error("SYNC_WITH_COREBANKING_FAILED - RequestId: {}, AccountNumber: {}, Error: {}", requestId, account.getAccountNumber(), e.getMessage(), e);
            throw new RuntimeException("Unable to sync with CoreBanking: " + e.getMessage(), e);
        }
    }

    public String generateAccountNumber(Account dto) {
        String cif = dto.getCifCode();
        int typeCode;

        switch (dto.getAccountType().name()) {
            case "PAYMENT":
                typeCode = 0;
                break;
            case "CREDIT":
                typeCode = 1;
                break;
            default:
                typeCode = 2;
        }

        String randomPart = String.format("%03d", new Random().nextInt(1000));
        return cif + typeCode + randomPart;
    }

    private AccountDTO mapToDto(Account account) {
        return AccountDTO.builder()
                .accountNumber(account.getAccountNumber())
                .cifCode(account.getCifCode())
                .accountType(account.getAccountType().toString())
                .status(account.getStatus().toString())
                .build();
    }
}