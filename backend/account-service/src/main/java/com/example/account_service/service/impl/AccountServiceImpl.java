package com.example.account_service.service.impl;

import com.example.account_service.dto.request.PaymentCreateDTO;
import com.example.account_service.dto.request.SavingCreateDTO;
import com.example.account_service.dto.response.AccountCreateReponse;
import com.example.account_service.entity.Account;
//import com.example.account_service.mapper.AccountMapper;
import com.example.account_service.exception.AppException;
import com.example.account_service.exception.ErrorCode;
import com.example.account_service.mapper.AccountMapper;
import com.example.account_service.repository.AccountRepository;
import com.example.account_service.service.AccountService;
import com.example.account_service.service.BaseAccountCreateDTO;
import com.example.common_service.constant.AccountStatus;
import com.example.common_service.constant.CustomerStatus;
import com.example.common_service.dto.CorePaymentAccountDTO;
import com.example.common_service.dto.CustomerDTO;
import com.example.common_service.dto.coreSavingAccountDTO;
import com.example.common_service.services.CommonService;
import com.example.common_service.services.CommonServiceCore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Random;

@Service
@Slf4j
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {
    private final AccountRepository accountRepository;

    @DubboReference(timeout = 5000)
    private final CommonService commonService;

    @DubboReference(timeout = 5000)
    private final CommonServiceCore commonServiceCore;

    private final AccountMapper accountMapper;

    @Override
    public AccountCreateReponse createPayment(PaymentCreateDTO paymentCreateDTO) {
        /// Kiem tra cif code , check status tai khoan- call api cua custommer- lay dc status account neu hop le
        /// cif se get tu current Customer logged
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName();
        log.info("User id "+ userId);
        log.info(" Recive payment request");
        CustomerDTO currentCustomer = commonService.getCurrentCustomer(userId);
        log.info("Current Customer : {}", currentCustomer);
        log.info("Create payment request received");
        log.info("Payment createDTO: {}", paymentCreateDTO);
//        log.info("checked : " + commonService.checkCustomer(paymentCreateDTO.getCifCode()));
//        boolean isActive = commonService.checkCustomer(paymentCreateDTO.getCifCode());
        if (currentCustomer.getStatus() == CustomerStatus.ACTIVE) {
            /// /
            /// Map tu request ve account
            //Account account = accountMapper.toEntityFromPayment(paymentCreateDTO);
            Account account = Account.builder()
                    .accountType(paymentCreateDTO.getAccountType())
                    .cifCode(currentCustomer.getCifCode())
                    .status(AccountStatus.ACTIVE)
                    .build();
            account.setAccountNumber(generateAccountNumber(account));
            log.info("Account : " + account);
            /// set up  account
            /// Call core banking create Account tren db core setup balance or terms
            ///  Map paymendto sang core
            CorePaymentAccountDTO corePaymentAccountDTO = CorePaymentAccountDTO.builder()
                    .cifCode(account.getCifCode())
                    .accountNumber(account.getAccountNumber())
                    .build();
            log.info("corePaymentAccountDTO: {}", corePaymentAccountDTO);
            commonServiceCore.createCoreAccountPayment(corePaymentAccountDTO);
            accountRepository.save(account);

            return AccountCreateReponse.builder()
                    .accountNumber(account.getAccountNumber())
                    .cifCode(account.getCifCode())
                    .id(account.getId())
                    .accountType(account.getAccountType())
                    .status(account.getStatus())
                    .build();
            ///  save account
            ///  map account ve response va tra ve
        }
        throw new AppException(ErrorCode.CUSTOMER_NOTACTIVE);
    }

    @Override
    public AccountCreateReponse createSaving(SavingCreateDTO savingCreateDTO) {
        log.info(" Recive create saving account request");
        /// Get Current Customer
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName();
        log.info("User id "+ userId);
        CustomerDTO currentCustomer = commonService.getCurrentCustomer(userId);
//        boolean isActive = commonService.checkCustomer(savingCreateDTO.getCifCode());
        //Check customer stattus
        if (currentCustomer.getStatus() == CustomerStatus.ACTIVE) {
            ///  Create account
            Account account = Account.builder()
                    .accountType(savingCreateDTO.getAccountType())
                    .cifCode(currentCustomer.getCifCode())
                    .status(AccountStatus.ACTIVE)
                    .build();
            account.setAccountNumber(generateAccountNumber(account));
            ///  got accountnumberSource from DTO
            log.info("Account number Soucre : " + savingCreateDTO.getAccountNumberSource());

            coreSavingAccountDTO coreSavingAccountDTO = com.example.common_service.dto.coreSavingAccountDTO.builder()
                    .cifCode(account.getCifCode())
                    .term(savingCreateDTO.getTerm())
                    .initialDeposit(savingCreateDTO.getInitialDeposit())
                    .accountNumber(account.getAccountNumber())
                    .build();
            log.info("coreSavingAccountDTO: {}", coreSavingAccountDTO);
;
            commonServiceCore.createCoreAccountSaving(coreSavingAccountDTO);
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

    @Override
    public List<AccountCreateReponse> getAllAccountsbyCifCode() {
        return List.of();
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
}
