package com.example.account_service.service.impl;

import com.example.account_service.dto.response.AccountCreateReponse;
import com.example.account_service.entity.Account;
import com.example.account_service.repository.AccountRepository;
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
}
