package com.example.account_service.service.impl;

import com.example.account_service.dto.request.CreditRequestCreateDTO;
import com.example.account_service.dto.request.SavingRequestCreateDTO;
import com.example.account_service.dto.response.AccountCreateReponse;
import com.example.account_service.dto.response.CreditRequestReponse;
import com.example.account_service.dto.response.SavingsRequestResponse;
import com.example.account_service.entity.Account;
import com.example.account_service.entity.CreditRequest;
import com.example.account_service.entity.SavingsRequest;
import com.example.account_service.exception.AppException;
import com.example.account_service.exception.ErrorCode;
import com.example.account_service.repository.AccountRepository;
import com.example.account_service.repository.CreditRequestRepository;
import com.example.account_service.repository.SavingsRequestRepository;
import com.example.account_service.service.CreditRequestService;
import com.example.account_service.service.SavingRequestService;
import com.example.account_service.utils.AccountNumberUtils;
import com.example.common_service.constant.*;
import com.example.common_service.dto.CartTypeDTO;
import com.example.common_service.dto.CustomerDTO;
import com.example.common_service.dto.MailMessageDTO;
import com.example.common_service.dto.coreCreditAccountDTO;
import com.example.common_service.services.CommonService;
import com.example.common_service.services.CommonServiceCore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor

public class SavingsRequestServiceImpl implements SavingRequestService {
    private final AccountRepository accountRepository;

    @DubboReference(timeout = 5000)
    private final CommonService commonService;

    @DubboReference(timeout = 5000)
    private final CommonServiceCore commonServiceCore;

    private final CreditRequestRepository creditRequestRepository;

    private final AccountNumberUtils    accountNumberUtils;
    private final RestTemplate restTemplate;
    private final SavingsRequestRepository savingsRequestRepository;
    private final StreamBridge streamBridge;


    @Override
    public SavingsRequestResponse CreateSavingRequest(SavingRequestCreateDTO savingRequestCreateDTO) {
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
        SavingsRequest savingsRequest = SavingsRequest.builder()
                .cifCode(currentCustomer.getCifCode())
                .initialDeposit(savingRequestCreateDTO.getInitialDeposit())
                .term(savingRequestCreateDTO.getTerm())
                .accountNumberSource(savingRequestCreateDTO.getAccountNumberSource())
                .status(SavingsRequestStatus.PENDING)
                .interestRate(savingRequestCreateDTO.getInterestRate())
                .build();
        savingsRequestRepository.save(savingsRequest);

        return SavingsRequestResponse.builder()
                .id(savingsRequest.getId())
                .accountNumberSource(savingsRequest.getAccountNumberSource())
                .initialDeposit(savingsRequest.getInitialDeposit())
                .term(savingsRequest.getTerm())
                .status(savingsRequest.getStatus())
                .cifCode(savingsRequest.getCifCode())
                .interestRate(savingRequestCreateDTO.getInterestRate())
                .build();
    }

    @Override
    public void sendOTP(String savingsRequestId) {
        SavingsRequest savingsRequest = savingsRequestRepository.findById(savingsRequestId).orElseThrow(
                ()->  new AppException(ErrorCode.SAVING_REQUEST_NOTEXISTED)
        );
        log.info("Saving request: {}", savingsRequest);
        CustomerDTO customerDTO = commonService.getCustomerByCifCode(savingsRequest.getCifCode());

        MailMessageDTO  mailMessageDTO= MailMessageDTO.builder()
                .recipientName(customerDTO.getFullName())
                .recipient(customerDTO.getEmail())
                .body("Mã OTP")
                .subject("Xác thực OTP")
                .build();
        streamBridge.send("mail-out-0", mailMessageDTO);

    }
}

