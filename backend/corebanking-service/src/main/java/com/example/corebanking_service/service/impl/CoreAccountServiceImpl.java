package com.example.corebanking_service.service.impl;

import com.example.common_service.constant.AccountStatus;
import com.example.common_service.constant.AccountType;
import com.example.common_service.dto.CartTypeDTO;
import com.example.common_service.dto.CorePaymentAccountDTO;
import com.example.common_service.dto.coreCreditAccountDTO;
import com.example.common_service.dto.CoreSavingAccountDTO;
import com.example.common_service.dto.response.AccountPaymentResponse;
import com.example.common_service.dto.response.AccountSummaryDTO;
import com.example.common_service.dto.response.CoreTermDTO;
import com.example.common_service.services.CommonServiceCore;
import com.example.corebanking_service.entity.*;
import com.example.corebanking_service.exception.AppException;
import com.example.corebanking_service.exception.ErrorCode;
import com.example.corebanking_service.repository.*;
import com.example.corebanking_service.service.CoreAccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CoreAccountServiceImpl implements CommonServiceCore, CoreAccountService {
    private final CoreCustomerRepo coreCustomerRepo;
    private final CoreAccountRepo coreAccountRepo;
    private final CoreAccountSavingRepo coreAccountSavingRepo;
    private final CoreTermRepo coreTermRepo;
    private final CoreCreditCartTypeRepo coreCreditCartTypeRepo;
    private final CoreAccountCreditRepo coreAccountCreditRepo;
    @Override
    public void createCoreAccountPayment(CorePaymentAccountDTO dto) {
            CoreAccount coreAccount = CoreAccount.builder()
                    .accountNumber(dto.getAccountNumber())
                    .accountType(dto.getAccountType())
                    .balance(BigDecimal.ZERO)
                    .status(dto.getStatus())
                    .openedDate(LocalDate.now())
                    .coreCustomer(coreCustomerRepo.getCoreCustomerByCifCode(dto.getCifCode()))
                    .build();
            log.info("createCoreAccountPayment:{}", coreAccount);
            coreAccountRepo.save(coreAccount);
    }

    @Override
    public void createCoreAccountSaving(CoreSavingAccountDTO dto) {
        CoreSavingsAccount coreSavingsAccount=CoreSavingsAccount.builder()
                .accountNumber(dto.getAccountNumber())
                .accountType(dto.getAccountType())
                .balance(dto.getInitialDeposit())
                .status(AccountStatus.ACTIVE)
                .openedDate(LocalDate.now())
                .coreCustomer(coreCustomerRepo.getCoreCustomerByCifCode(dto.getCifCode()))
                .initialDeposit(dto.getInitialDeposit())
                .coreTerm(coreTermRepo.getCoreTermsByTermValueMonths(dto.getTerm()))
                .build();
        coreAccountSavingRepo.save(coreSavingsAccount);
    }

    @Override
    public CartTypeDTO getCartTypebyID(String id) {
        CoreCreditCardType coreCreditCardType = coreCreditCartTypeRepo.findById(id).orElseThrow(()->
                new AppException(ErrorCode.CARTCREDIT_TYPE_NOTEXISTED));
        return CartTypeDTO.builder()
                .annualFee(coreCreditCardType.getAnnualFee())
                .defaultCreditLimit(coreCreditCardType.getDefaultCreditLimit())
                .interestRate(coreCreditCardType.getInterestRate())
                .minimumIncome(coreCreditCardType.getMinimumIncome())
                .typeName(coreCreditCardType.getTypeName())
                .build();
    }

    @Override
    public void createCoreAccountCredit(coreCreditAccountDTO dto) {
        CoreCreditCardType coreCreditCardType = coreCreditCartTypeRepo.findById(dto.getCartTypeId()).orElseThrow(()->
                new AppException(ErrorCode.CARTCREDIT_TYPE_NOTEXISTED));

        BigDecimal defaultLimit = coreCreditCardType.getDefaultCreditLimit();
        BigDecimal monthlyIncome = dto.getMonthlyIncome();

        BigDecimal creditLimit;

        if (monthlyIncome.compareTo(BigDecimal.valueOf(10000000)) < 0) {
            creditLimit = defaultLimit.multiply(BigDecimal.valueOf(0.6)); // 60% hạn mức mặc định
        } else if (monthlyIncome.compareTo(BigDecimal.valueOf(20000000)) < 0) {
            creditLimit = defaultLimit.multiply(BigDecimal.valueOf(0.8)); // 80%
        } else {
            creditLimit = defaultLimit; // 100%
        }
        CoreCreditAccount coreCreditAccount = CoreCreditAccount.builder()
                .accountNumber(dto.getAccountNumber())
                .coreCustomer(coreCustomerRepo.getCoreCustomerByCifCode(dto.getCifCode()))
                .balance(BigDecimal.ZERO)
                .status(AccountStatus.ACTIVE)
                .openedDate(LocalDate.now())
                .coreCreditCardType(coreCreditCardType)
                .creditLimit(creditLimit)
                .currentDebt(BigDecimal.ZERO)
                .accountType(AccountType.CREDIT)
                .build();
        coreAccountCreditRepo.save(coreCreditAccount);
    }

    @Override
    public List<AccountSummaryDTO> getAllAccountsByCif(String id) {
            return coreAccountRepo.getAllAccountsByCif(id);

    }

    @Override
    public List<AccountPaymentResponse> getAllPaymentAccountsByCif(String cifCode) {
        List<CoreAccount> list = coreAccountRepo.getAllCorePaymentAccounts(cifCode);
        return list.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<CoreTermDTO> getAllCoreTerm() {
            List<CoreTerm> list = coreTermRepo.getAllCoreTermActive();

        return list.stream()
                .map(this:: mapToCoreTermDTO)
                .collect(Collectors.toList());
    }

    public AccountPaymentResponse mapToDto(CoreAccount request) {
        AccountPaymentResponse response = AccountPaymentResponse.builder()
                .accountNumber(request.getAccountNumber())
                .accountType(request.getAccountType())
                .openedDate(request.getOpenedDate())
                .balance(request.getBalance())
                .status(request.getStatus())
                .cifCode(request.getCoreCustomer().getCifCode())
                .build();
        return response;

    }
    public  CoreTermDTO mapToCoreTermDTO(CoreTerm request) {
        CoreTermDTO response = CoreTermDTO.builder()
                .termId(request.getTermId())
                .termValueMonths(request.getTermValueMonths())
                .interestRate(request.getInterestRate())
                .isActive(request.getIsActive())
                .build();
        return response;

    }
}
