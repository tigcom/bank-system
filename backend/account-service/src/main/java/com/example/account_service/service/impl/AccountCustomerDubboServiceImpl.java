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
import java.util.stream.Collectors;

@DubboService
@RequiredArgsConstructor
@Slf4j
public class AccountCustomerDubboServiceImpl implements CustomerCommonService {

    private final AccountRepository accountRepository;
    private final RestTemplate restTemplate;

    @Override
    public List<AccountDTO> getAccountsByCifCode(String cifCode) {
        try {
            List<Account> accounts = accountRepository.findByCifCode(cifCode);
            return accounts.stream()
                    .map(this::mapToDto)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("[getAccountsByCifCode] Lỗi khi lấy danh sách account cho CIF: {}: {}", cifCode, e.getMessage(), e);
            throw new RuntimeException("Không thể lấy danh sách tài khoản: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public PaymentRequestResponse createPaymentInit(PaymentCreateDTO paymentRequest) {
        log.info("[createPaymentInit] Bắt đầu tạo payment account cho CIF: {}", paymentRequest.getCifCode());
        return createPaymentAccountDirectly(paymentRequest.getCifCode());
    }

    private PaymentRequestResponse createPaymentAccountDirectly(String cifCode) {
        try {
            // Tạo account trong database local
            AccountCreateReponse account = createPaymentAccountForCustomer(cifCode);

            log.info("[createPaymentAccountDirectly] Đã tạo payment account cho CIF: {}, AccountNumber: {}",
                    cifCode, account.getAccountNumber());

            return PaymentRequestResponse.builder()
                    .id(account.getId())
                    .cifCode(cifCode)
                    .accountType(AccountType.PAYMENT)
                    .status(PaymentRequestResponse.PaymentRequestStatus.APPROVED)
                    .build();

        } catch (Exception e) {
            log.error("[createPaymentAccountDirectly] Lỗi khi tạo payment account cho CIF: {}: {}", cifCode, e.getMessage(), e);
            throw new RuntimeException("Không thể tạo tài khoản thanh toán trực tiếp: " + e.getMessage(), e);
        }
    }

    private AccountCreateReponse createPaymentAccountForCustomer(String cifCode) {
        Account account = null;
        try {
            // 1. Tạo account trong database local
            account = Account.builder()
                    .accountType(AccountType.PAYMENT)
                    .cifCode(cifCode)
                    .status(AccountStatus.ACTIVE)
                    .build();

            account.setAccountNumber(generateAccountNumber(account));
            account = accountRepository.save(account);

            log.info("[createPaymentAccountForCustomer] Đã lưu account local cho CIF: {}, AccountNumber: {}",
                    cifCode, account.getAccountNumber());

            // 2. Đồng bộ với CoreBanking
            syncWithCoreBanking(account);

            return AccountCreateReponse.builder()
                    .accountNumber(account.getAccountNumber())
                    .cifCode(account.getCifCode())
                    .id(account.getId())
                    .accountType(account.getAccountType())
                    .status(account.getStatus())
                    .build();

        } catch (Exception e) {
            log.error("[createPaymentAccountForCustomer] Lỗi khi tạo account cho CIF: {}: {}", cifCode, e.getMessage(), e);

            // Rollback: Xóa account nếu đã tạo trong database nhưng sync CoreBanking thất bại
            if (account != null && account.getId() != null) {
                try {
                    accountRepository.deleteById(account.getId());
                    log.info("[createPaymentAccountForCustomer] Đã rollback account ID: {}", account.getId());
                } catch (Exception rollbackEx) {
                    log.error("[createPaymentAccountForCustomer] Lỗi khi rollback account ID: {}. Vui lòng kiểm tra thủ công!",
                            account.getId(), rollbackEx);
                    throw new RuntimeException("Lỗi khi rollback tài khoản: " + rollbackEx.getMessage(), rollbackEx);
                }
            }

            throw new RuntimeException("Không thể tạo tài khoản thanh toán: " + e.getMessage(), e);
        }
    }

    private void syncWithCoreBanking(Account account) {
        try {
            CoreAccountRequest coreAccount = CoreAccountRequest.builder()
                    .accountNumber(account.getAccountNumber())
                    .cifCode(account.getCifCode())
                    .balance(BigDecimal.ZERO)
                    .accountType(account.getAccountType())
                    .status(AccountStatus.ACTIVE)
                    .build();

            String url = "http://localhost:8083/corebanking/save-account";

            log.info("[syncWithCoreBanking] Gửi yêu cầu đồng bộ đến CoreBanking: URL={}, AccountNumber={}",
                    url, account.getAccountNumber());

            restTemplate.postForObject(url, coreAccount, Void.class);

            log.info("[syncWithCoreBanking] Đã đồng bộ thành công account với CoreBanking: {}",
                    account.getAccountNumber());

        } catch (Exception e) {
            log.error("[syncWithCoreBanking] Lỗi khi đồng bộ account {} với CoreBanking: {}",
                    account.getAccountNumber(), e.getMessage(), e);
            throw new RuntimeException("Không thể đồng bộ với hệ thống CoreBanking: " + e.getMessage(), e);
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