package com.example.account_service.scheduled;

import com.example.account_service.dto.request.SavingRequestCreateDTO;
import com.example.account_service.entity.Account;
import com.example.account_service.entity.SavingsAccount;
import com.example.account_service.entity.Term;
import com.example.account_service.exception.AppException;
import com.example.account_service.exception.ErrorCode;
import com.example.account_service.repository.AccountRepository;
import com.example.account_service.repository.SavingsAccountRepository;
import com.example.account_service.repository.SavingsRequestRepository;
import com.example.account_service.repository.TermRepository;
import com.example.common_service.constant.AccountStatus;
import com.example.common_service.constant.AccountType;
import com.example.common_service.constant.InterestPaymentType;
import com.example.common_service.constant.RenewOption;
import com.example.common_service.dto.CommonTransactionDTO;
import com.example.common_service.dto.CoreAccountRequest;
import com.example.common_service.dto.CoreAccountUpdateStatusRequest;
import com.example.common_service.dto.request.PayInterestRequest;
import com.example.common_service.dto.response.BalanceResponse;
import com.example.common_service.dto.response.SavingAccountResponse;
import com.example.common_service.services.CommonService;
import com.example.common_service.services.transactions.CommonTransactionService;
import lombok.RequiredArgsConstructor;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class MaturityInterestPaymentScheduler {
    private final AccountRepository accountRepository;
    private final SavingsAccountRepository savingsAccountRepository;
    private final TermRepository termRepository;

    @Value("${core-banking.base-url:http://localhost:8083/corebanking}")
    private String coreBankingBaseUrl;

    @DubboReference(timeout = 5000)
    private final CommonService commonService;

    @DubboReference(timeout = 5000)
    private final CommonTransactionService commonTransactionService;
    private final RestTemplate restTemplate;

    @Scheduled(cron = "0 54 15 * * ?")
    public void processSavingsAccounts() {
        log.info("Start checking your savings account for the maturity date or monthly interest payment date.");
        LocalDateTime today = LocalDateTime.now();
        //List này chưa có balance
        List<SavingsAccount> monthlyAccounts = savingsAccountRepository
                .findByInterestPaymentTypeAndStatus(InterestPaymentType.MONTHLY, AccountStatus.ACTIVE);

        for (SavingsAccount account : monthlyAccounts) {
            // Kiểm tra xem hôm nay có phải ngày trả lãi (dựa trên start_date)
            if (isMonthlyInterestDue(account.getCreatedDate().toLocalDate(), today)) {
                BigDecimal interest = (account.getInitialDeposit().multiply(account.getTerm().getInterestRate()))
                        .divide(BigDecimal.valueOf(12), 2, BigDecimal.ROUND_HALF_UP);
                /// thuc hien tra lãi tới tài khoản đích
                long period = calculatePeriod(account.getCreatedDate().toLocalDate(), today);
                String description = String.format("Tiền lãi tháng %d/%d kỳ %d ", today.getMonthValue(), today.getYear(), period);
                PayInterestRequest request = PayInterestRequest.builder()
                        .toAccountNumber(account.getAccountNumberSrc())
                        .amount(interest)
                        .currency("VND")
                        .description(description)
                        .build();
                try {
                    CommonTransactionDTO transactionDTO = commonTransactionService.payinterestInternal(request);
                    log.info("Pay success :" + transactionDTO.getStatus());
                } catch (Exception e) {
                    throw new AppException(ErrorCode.TRANSACTION_FAILED);
                }
            }
        }
        // 2. Xử lý sổ đáo hạn
        //Danh sách các sổ tiết kiệm tới ngày đáo hạn
        List<SavingsAccount> maturedAccountsTemp = savingsAccountRepository
                .findByMaturityDateAndStatus(today, AccountStatus.ACTIVE);

        List<SavingAccountResponse> maturedAccounts = maturedAccountsTemp.stream()
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
                            .accountNumberSrc(account.getAccountNumberSrc())
                            .build();
                })
                .collect(Collectors.toList());


        for (SavingAccountResponse account : maturedAccounts) {
            BigDecimal interest;
            if (account.getInterestPaymentType().equals(InterestPaymentType.MONTHLY)) {
                // Nếu trả lãi hàng tháng, không cần trả lãi khi đáo hạn
                interest = BigDecimal.ZERO;
            } else {
                // Tính lãi cuối kỳ (lãi đơn)
                // tĩnh lãi phải dựa vào balacne hiện tại
                 interest = (account.getBalance().multiply(account.getInterestRate())
                         .multiply(BigDecimal.valueOf(account.getTermValueMonths())))
                         .divide(BigDecimal.valueOf(12), 2, BigDecimal.ROUND_HALF_UP);

            }
            if (account.getRenewOption().equals(RenewOption.AUTO_RENEW)) {
                // Tái tục: Tạo sổ mới với số dư = gốc + lãi
                // create new saving in local
                Term term = termRepository.findByTermValueMonths(account.getTermValueMonths());
                if (term == null) {
                    throw new AppException(ErrorCode.TERM_NOT_FOUND);
                }

                // Calculate maturity date: current time + term months
                LocalDateTime currentTime = LocalDateTime.now();
                LocalDateTime maturityDate = currentTime.plusMonths(term.getTermValueMonths());

                SavingsAccount newAccount = SavingsAccount.builder()
                        .accountType(AccountType.SAVING)
                        .cifCode(account.getCifCode())
                        .status(AccountStatus.ACTIVE)
                        .initialDeposit(account.getBalance().add(interest))
                        .term(term)
                        .maturityDate(maturityDate)
                        .interestPaymentType(account.getInterestPaymentType())  // Default to AT_MATURITY
                        .renewOption(account.getRenewOption())  // Default to NO_RENEW
                        .accountNumberSrc(account.getAccountNumber())
                        .build();
                newAccount.setAccountNumber(generateAccountNumber(newAccount));
                savingsAccountRepository.save(newAccount);
                // create Core saving
                createCoreBankingAccount(newAccount);
            } else {
                // Không tái tục: Chuyển gốc và lãi
                String description = String.format("Tiền lãi cuối kỳ của sổ Tiết kiệm %d ",account.getAccountNumber());
                PayInterestRequest request = PayInterestRequest.builder()
                        .toAccountNumber(account.getAccountNumberSrc())
                        .amount(interest)
                        .currency("VND")
                        .description(description)
                        .build();
                try {
                    CommonTransactionDTO transactionDTO = commonTransactionService.payinterestInternal(request);
                    log.info("Pay success :" + transactionDTO.getStatus());
                } catch (Exception e) {
                    throw new AppException(ErrorCode.TRANSACTION_FAILED);
                }
            }
        }

        for (SavingsAccount account : maturedAccountsTemp)
        {
            //saving account local matured
            account.setStatus(AccountStatus.MATURED);
            savingsAccountRepository.save(account);
            // update account trên core nữa

            try {
                CoreAccountUpdateStatusRequest request = CoreAccountUpdateStatusRequest.builder()
                        .accountNumber(account.getAccountNumber())
                        .status(account.getStatus())
                        .build();
                String url = coreBankingBaseUrl + "/update-account-status";
                restTemplate.postForObject(url ,request,Void.class);

            } catch (Exception e) {
                log.error("Failed to create account in core banking system", e);
                throw new AppException(ErrorCode.CORE_BANKING_SERVICE_ERROR);
            }
        }

    }

    private boolean isMonthlyInterestDue(LocalDate startDate, LocalDateTime today) {
        return startDate.getDayOfMonth() == today.getDayOfMonth() &&
                !startDate.equals(today); // Tránh trả lãi ngay ngày mở sổ
    }
    private long calculatePeriod(LocalDate createdDate, LocalDateTime today) {
        return ChronoUnit.MONTHS.between(createdDate, today) + 1; // +1 để tính kỳ hiện tại
    }
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
    private void createCoreBankingAccount(SavingsAccount  request) {
        // Create simple account structure for Core Banking (only balance and status)
        String url = coreBankingBaseUrl + "/save-account";
        try {
            CoreAccountRequest coreAccount = CoreAccountRequest.builder()
                    .accountNumber(request.getAccountNumber())
                    .cifCode(request.getCifCode())
                    .balance(request.getInitialDeposit())
                    .accountType(request.getAccountType())
                    .status(AccountStatus.ACTIVE)
                    .build();
            log.info("corePaymentAccountDTO: {}", coreAccount);
            // Call API save account trên CoreBanking
            String saveurl = "http://localhost:8083/corebanking/save-account";
            restTemplate.postForObject(saveurl ,coreAccount,Void.class);

        } catch (Exception e) {
            log.error("Failed to create account in core banking system", e);
            throw new AppException(ErrorCode.CORE_BANKING_SERVICE_ERROR);
        }
    }
}
