package com.example.account_service.service.impl;
import com.example.account_service.service.AccountService;
import lombok.extern.slf4j.Slf4j;
import com.example.account_service.entity.Account;
import com.example.account_service.repository.AccountRepository;
import com.example.common_service.constant.AccountStatus;
import com.example.common_service.constant.AccountType;
import com.example.common_service.dto.AccountDTO;
import com.example.common_service.dto.CustomerDTO;
import com.example.common_service.dto.response.AccountPaymentResponse;
import com.example.common_service.services.CommonService;
import com.example.common_service.services.customer.CustomerCommonService;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;
@Slf4j
@DubboService
@RequiredArgsConstructor
public class AccountCustomerDubboServiceImpl implements CustomerCommonService {

    private final AccountRepository accountRepository;
    private final AccountService accountService;
    @DubboReference(timeout = 5000)
    private CommonService commonService;
    @Override
    public List<AccountDTO> getAccountsByCifCode(String cifCode) {
        List<Account> accounts = accountRepository.findByCifCode(cifCode);
        return accounts.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    private AccountDTO mapToDto(Account account) {
        return AccountDTO.builder()
                .accountNumber(account.getAccountNumber())
                .cifCode(account.getCifCode())
                .accountType(account.getAccountType().toString())
                .status(account.getStatus().toString())
                .build();
    }
    @Override
    public List<AccountPaymentResponse> getAllPaymentAccountsbyUserId(String userId) {
        try {
            // Lấy thông tin khách hàng hiện tại
            CustomerDTO currentCustomer = commonService.getCurrentCustomer(userId);

            log.info("CUSTOMER_INFO_RETRIEVED - UserId: {}, CifCode: {}, CustomerStatus: {}",
                    userId, currentCustomer.getCifCode(), currentCustomer.getStatus());
            String cifCode = currentCustomer.getCifCode();
            // Lấy Payment Accounts từ local database
            List<Account> paymentAccounts = accountRepository.findByCifCodeAndAccountTypeAndStatus(
                    cifCode, AccountType.PAYMENT, AccountStatus.ACTIVE);
            log.info("PAYMENT_ACCOUNTS_FOUND - UserId: {}, CifCode: {}, Count: {}",
                    userId, cifCode, paymentAccounts.size());

            // Kết hợp thông tin local với balance từ Core Banking
            List<AccountPaymentResponse> result = paymentAccounts.stream()
                    .map(account -> {
                        BigDecimal balance = accountService.getBalanceFromCorebanking(account.getAccountNumber());
                        log.debug("ACCOUNT_BALANCE_RETRIEVED - AccountNumber: {}, Balance: {}",
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

}
