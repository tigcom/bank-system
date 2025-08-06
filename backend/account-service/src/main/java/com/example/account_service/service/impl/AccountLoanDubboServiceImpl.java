package com.example.account_service.service.impl;

import com.example.account_service.dto.response.AccountCreateReponse;
import com.example.account_service.entity.Account;
import com.example.account_service.entity.LoanAccount;
import com.example.account_service.repository.AccountRepository;
import com.example.account_service.repository.LoanAccountRepository;
import com.example.account_service.service.AccountService;
import com.example.common_service.constant.AccountStatus;
import com.example.common_service.constant.AccountType;
import com.example.common_service.dto.AccountDTO;
import com.example.common_service.dto.CoreAccountRequest;
import com.example.common_service.dto.CustomerDTO;
import com.example.common_service.dto.PaymentCreateDTO;
import com.example.common_service.dto.request.LoanRequestDTO;
import com.example.common_service.dto.response.AccountPaymentResponse;
import com.example.common_service.dto.response.PaymentRequestResponse;
import com.example.common_service.services.CommonService;
import com.example.common_service.services.account.AccountDubboService;
import com.example.common_service.services.customer.CustomerCommonService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@DubboService
@RequiredArgsConstructor
public class AccountLoanDubboServiceImpl implements AccountDubboService {

    private final AccountRepository accountRepository;
    private final AccountService accountService;
     private final LoanAccountRepository loanAccountRepository;
    @DubboReference(timeout = 5000)
    private CommonService commonService;
    @Autowired
    @Qualifier("coreBankingRestTemplate")
    private RestTemplate restTemplate;


    @Override
    public AccountDTO createLoanAccount(LoanRequestDTO dto) {
        return accountService.createLoanAccount(dto);
    }

    @Override
    public void updateAccountFromLoan(LoanRequestDTO dto) {
        log.info("DUBBO updateAccountFromLoan - loanId: {}, repaymentAccount: {}", dto.getLoanId(), dto.getRepaymentAccountNumber());
        accountService.updateAccountFromLoan(dto);
    }

    @Override
    public void updateOutstandingDebt(Long loanId, BigDecimal outstandingDebt) {
        log.info("DUBBO updateOutstandingDebt - loanId: {}, outstandingDebt: {}", loanId, outstandingDebt);
        // Tìm loan account theo loanId
        LoanAccount loanAccount = loanAccountRepository.findByLoanId(loanId)
        if (loanAccount == null) {
            log.warn("Không tìm thấy tài khoản vay với loanId: {}", loanId);
            return;
        }
        accountRepository.save(loanAccount);
        log.info("Đã cập nhật dư nợ cho loan account {}: {}", loanAccount.getAccountNumber(), outstandingDebt);
    }
}
