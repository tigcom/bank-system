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
import org.springframework.web.client.RestTemplate;

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
    private final RestTemplate restTemplate;

    @Override
    public CreditRequestReponse createCreditRequest(CreditRequestCreateDTO creditRequestCreateDTO) {
        log.info("Starting createCreditRequest with input: {}", creditRequestCreateDTO);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            log.error("No authentication found in security context");
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        String userId = authentication.getName();
        log.info("Authenticated userId: {}", userId);

        CustomerDTO currentCustomer = commonService.getCurrentCustomer(userId);
        log.info("Current Customer retrieved: {}", currentCustomer);

        if (currentCustomer.getStatus() != CustomerStatus.ACTIVE) {
            log.warn("Customer status is not ACTIVE: {}", currentCustomer.getStatus());
            throw new AppException(ErrorCode.CUSTOMER_NOTACTIVE);
        }

        CreditRequest creditRequest = CreditRequest.builder()
                .cifCode(currentCustomer.getCifCode())
                .occupation(creditRequestCreateDTO.getOccupation())
                .cartTypeId(creditRequestCreateDTO.getCartTypeId())
                .monthlyIncome(creditRequestCreateDTO.getMonthlyIncome())
                .status(CreditRequestStatus.PENDING)
                .build();

        log.info("Saving new CreditRequest: {}", creditRequest);
        creditRequestRepository.save(creditRequest);
        log.info("CreditRequest saved successfully with id: {}", creditRequest.getId());

        CreditRequestReponse response = CreditRequestReponse.builder()
                .id(creditRequest.getId())
                .cifCode(creditRequest.getCifCode())
                .occupation(creditRequest.getOccupation())
                .cartTypeId(creditRequest.getCartTypeId())
                .monthlyIncome(creditRequest.getMonthlyIncome())
                .status(creditRequest.getStatus())
                .build();

        log.info("Returning response: {}", response);
        return response;
    }


    @Override
    public AccountCreateReponse approveCreditRequest(String id) {
        log.info("Starting approval process for credit request with id: {}", id);

        CreditRequest creditRequest = creditRequestRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("CreditRequest not found with id: {}", id);
                    return new AppException(ErrorCode.CREDIT_REQUEST_NOTEXISTED);
                });
        if (creditRequest.getStatus() != CreditRequestStatus.PENDING) {
            throw new AppException(ErrorCode.CREDIT_REQUEST_STATUS_INVALID);
        }

        log.info("Found CreditRequest: {}", creditRequest);

        CustomerDTO customerDTO = commonService.getCustomerByCifCode(creditRequest.getCifCode());
        log.info("Retrieved customer information for cifCode: {}", creditRequest.getCifCode());

        //dung dubbo call ham get cart info by id
       /// CartTypeDTO cartTypeDTO = commonServiceCore.getCartTypebyID(creditRequest.getCartTypeId());

        /// call api get cart type tren core Banking by rest template
        String urlGetCard = "http://localhost:8083/corebanking/get-cart-type/" + creditRequest.getCartTypeId();
        CartTypeDTO cartTypeDTO = restTemplate.getForObject(urlGetCard, CartTypeDTO.class);

        log.info("Retrieved CartType information for id: {}", creditRequest.getCartTypeId());

        int age = Period.between(customerDTO.getDateOfBirth(), LocalDate.now()).getYears();
        log.info("Calculated customer age: {} years", age);

        if (creditRequest.getMonthlyIncome().compareTo(cartTypeDTO.getMinimumIncome()) < 0) {
            log.warn("Monthly income is insufficient: {} < {}", creditRequest.getMonthlyIncome(), cartTypeDTO.getMinimumIncome());
            throw new AppException(ErrorCode.INCOME_INVALID);
        } else if (age < 21) {
            log.warn("Customer age is below required minimum: {}", age);
            throw new AppException(ErrorCode.AGE_INVALID);
        }

        log.info("Creating new account for credit request");

        Account account = Account.builder()
                .cifCode(creditRequest.getCifCode())
                .status(AccountStatus.ACTIVE)
                .accountType(AccountType.CREDIT)
                .build();

        account.setAccountNumber(accountNumberUtils.generateAccountNumber(account));
        accountRepository.save(account);
        log.info("Successfully saved account with accountNumber: {}", account.getAccountNumber());

        com.example.common_service.dto.coreCreditAccountDTO coreCreditAccountDTO = com.example.common_service.dto.coreCreditAccountDTO.builder()
                .accountNumber(account.getAccountNumber())
                .cifCode(account.getCifCode())
                .cartTypeId(creditRequest.getCartTypeId())
                .monthlyIncome(creditRequest.getMonthlyIncome())
                .build();

        log.info("Sending account information to core banking service: {}", coreCreditAccountDTO);
        /// dung dubbo goi ham tao credit account tren core banking
       /// commonServiceCore.createCoreAccountCredit(coreCreditAccountDTO);
        String url = "http://localhost:8083/corebanking/create-credit-account";
        restTemplate.postForObject(url ,coreCreditAccountDTO,Void.class);
        log.info("Successfully sent account information to core banking");

        creditRequest.setStatus(CreditRequestStatus.APPROVED);
        creditRequestRepository.save(creditRequest);
        log.info("Updated credit request status to APPROVED");

        AccountCreateReponse response = AccountCreateReponse.builder()
                .accountNumber(account.getAccountNumber())
                .cifCode(account.getCifCode())
                .id(account.getId())
                .accountType(account.getAccountType())
                .status(account.getStatus())
                .build();

        log.info("Returning response: {}", response);
        return response;
    }


    @Override
    public List<CreditRequestReponse> getAllCreditRequest() {
        return creditRequestRepository.findAll()
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    public CreditRequestReponse rejectCreditRequest(String id) {
        CreditRequest creditRequest = creditRequestRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("CreditRequest not found with id: {}", id);
                    return new AppException(ErrorCode.CREDIT_REQUEST_NOTEXISTED);
                });
        creditRequest.setStatus(CreditRequestStatus.REJECTED);
        //Gui email thong bao
        creditRequestRepository.save(creditRequest);

        return CreditRequestReponse.builder()
                .id(creditRequest.getId())
                .cifCode(creditRequest.getCifCode())
                .occupation(creditRequest.getOccupation())
                .status(creditRequest.getStatus())
                .monthlyIncome(creditRequest.getMonthlyIncome())
                .cartTypeId(creditRequest.getCartTypeId())
                .build();
    }

    public CreditRequestReponse mapToDto(CreditRequest request) {
            CreditRequestReponse creditRequestReponse = CreditRequestReponse.builder()
                    .id(request.getId())
                    .cifCode(request.getCifCode())
                    .occupation(request.getOccupation())
                    .cartTypeId(request.getCartTypeId())
                    .monthlyIncome(request.getMonthlyIncome())
                    .status(request.getStatus())
                    .build();
        return creditRequestReponse;
    }
}

