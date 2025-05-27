package com.example.account_service.service.impl;

import com.example.account_service.dto.request.CreditRequestCreateDTO;
import com.example.account_service.dto.request.PaymentCreateDTO;
import com.example.account_service.dto.request.SavingCreateDTO;
import com.example.account_service.dto.response.AccountCreateReponse;
import com.example.account_service.dto.response.CreditRequestReponse;
import com.example.account_service.entity.Account;
import com.example.account_service.entity.CreditRequest;
import com.example.account_service.exception.AppException;
import com.example.account_service.exception.ErrorCode;
import com.example.account_service.mapper.AccountMapper;
import com.example.account_service.repository.AccountRepository;
import com.example.account_service.repository.CreditRequestRepository;
import com.example.account_service.service.AccountService;
import com.example.account_service.service.CreditRequestService;
import com.example.account_service.utils.AccountNumberUtils;
import com.example.common_service.constant.AccountStatus;
import com.example.common_service.constant.AccountType;
import com.example.common_service.constant.CreditRequestStatus;
import com.example.common_service.constant.CustomerStatus;
import com.example.common_service.dto.*;
import com.example.common_service.services.CommonService;
import com.example.common_service.services.CommonServiceCore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor

public class CreditRequestServiceImpl implements CreditRequestService {
    private final AccountRepository accountRepository;

    @DubboReference(timeout = 5000)
    private final CommonService commonService;

    @DubboReference(timeout = 5000)
    private final CommonServiceCore commonServiceCore;

    private final CreditRequestRepository creditRequestRepository;

    private final AccountNumberUtils    accountNumberUtils;
    @Override
    public CreditRequestReponse createCreditRequest(CreditRequestCreateDTO creditRequestCreateDTO) {
        /// get current Customer
        ///
        log.info("Create credit request" + creditRequestCreateDTO);


        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName();
        log.info("User id " + userId);
        CustomerDTO currentCustomer = commonService.getCurrentCustomer(userId);
        log.info("Current Customer : {}", currentCustomer);
        if (currentCustomer.getStatus() == CustomerStatus.ACTIVE) {
            CreditRequest creditRequest = CreditRequest.builder()
                    .cifCode(currentCustomer.getCifCode())
                    .occupation(creditRequestCreateDTO.getOccupation())
                    .cartTypeId(creditRequestCreateDTO.getCartTypeId())
                    .monthlyIncome(creditRequestCreateDTO.getMonthlyIncome())
                    .status(CreditRequestStatus.PENDING)
                    .build();
            creditRequestRepository.save(creditRequest);
            return CreditRequestReponse.builder()
                    .id(creditRequest.getId())
                    .cifCode(creditRequest.getCifCode())
                    .occupation(creditRequest.getOccupation())
                    .cartTypeId(creditRequest.getCartTypeId())
                    .createdAt(creditRequest.getCreatedAt())
                    .monthlyIncome(creditRequest.getMonthlyIncome())
                    .status(creditRequest.getStatus())
                    .build();
        }
        throw new AppException(ErrorCode.CUSTOMER_NOTACTIVE);
    }

    @Override
    public AccountCreateReponse approveCreditRequest(String id) {
        /// Check credit request conditon
        log.info("Approve credit request" + id);
        CreditRequest creditRequest = creditRequestRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.CREDIT_REQUEST_NOTEXISTED));
        CustomerDTO customerDTO = commonService.getCustomerByCifCode(creditRequest.getCifCode());
        CartTypeDTO cartTypeDTO = commonServiceCore.getCartTypebyID(creditRequest.getCartTypeId());
        int age = Period.between(customerDTO.getDateOfBirth(), LocalDate.now()).getYears();
        if(creditRequest.getMonthlyIncome().compareTo(cartTypeDTO.getMinimumIncome()) < 0)
        {
            throw new AppException(ErrorCode.INCOME_INVALID);
        } else if (age < 21) {
            throw new AppException(ErrorCode.AGE_INVALID);
        }


        ///  Create Account and save to account db
         Account account = Account.builder()
                 .cifCode(creditRequest.getCifCode())
                 .status(AccountStatus.ACTIVE)
                 .accountType(AccountType.CREDIT)
                 .build();
         account.setAccountNumber(accountNumberUtils.generateAccountNumber(account));
         accountRepository.save(account);

         /// Send account and save in db core banking
        coreCreditAccountDTO coreCreditAccountDTO = com.example.common_service.dto.coreCreditAccountDTO.builder()
                .accountNumber(account.getAccountNumber())
                .cifCode(account.getCifCode())
                .cartTypeId(creditRequest.getCartTypeId())
                .monthlyIncome(creditRequest.getMonthlyIncome())
                .build();
        commonServiceCore.createCoreAccountCredit(coreCreditAccountDTO);

        /// Update status of  that credit request : to APPROVED
        creditRequest.setStatus(CreditRequestStatus.APPROVED);
        creditRequestRepository.save(creditRequest);

        return  AccountCreateReponse.builder()
                .accountNumber(account.getAccountNumber())
                .cifCode(account.getCifCode())
                .id(account.getId())
                .accountType(account.getAccountType())
                .status(account.getStatus())
                .build();
    }

    @Override
    public List<CreditRequestReponse> getAllCreditRequest() {
        return creditRequestRepository.findAll()
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }
    public CreditRequestReponse mapToDto(CreditRequest request) {
            CreditRequestReponse creditRequestReponse = CreditRequestReponse.builder()
                    .id(request.getId())
                    .cifCode(request.getCifCode())
                    .occupation(request.getOccupation())
                    .cartTypeId(request.getCartTypeId())
                    .monthlyIncome(request.getMonthlyIncome())
                    .status(request.getStatus())
                    .createdAt(request.getCreatedAt())
                    .build();
        return creditRequestReponse;
    }
}

