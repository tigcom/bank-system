package com.example.corebanking_service.service.impl;

import com.example.common_service.dto.CommonTransactionDTO;
import com.example.corebanking_service.dto.request.NapasTransferRequest;
import com.example.corebanking_service.dto.request.TransactionRequest;
import com.example.corebanking_service.dto.resonse.ApiResponse;
import com.example.corebanking_service.dto.resonse.NapasTransferResponse;
import com.example.corebanking_service.entity.CoreAccount;
import com.example.corebanking_service.entity.CoreTransaction;
import com.example.corebanking_service.exception.AppException;
import com.example.corebanking_service.exception.ErrorCode;
import com.example.corebanking_service.repository.CoreAccountRepo;
import com.example.corebanking_service.repository.CoreTransactionRepo;
import com.example.corebanking_service.service.CoreTransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@DubboService
@RequiredArgsConstructor
@Slf4j
public class CoreTransactionServiceImpl implements CoreTransactionService {

    private final CoreAccountRepo accountRepo;
    private final CoreTransactionRepo transactionRepo;

    private final RestTemplate restTemplate;

    @Override
    @Transactional
    public CommonTransactionDTO performTransfer(TransactionRequest request) {
        CoreAccount fromAccount = accountRepo.findByAccountNumberWithLock(request.getFromAccountNumber());
        if (fromAccount == null ) {
            throw new AppException(ErrorCode.FROM_ACCOUNT_NOT_EXIST);
        }
        if(!fromAccount.getStatus().name().equals("ACTIVE")){
            throw new AppException(ErrorCode.FROM_ACCOUNT_NOT_ACTIVE);
        }
        if(fromAccount.getBalance().compareTo(request.getAmount())<0){
            log.warn("Tài khoản {} không đủ tiền. Số dư: {}, Số tiền yêu cầu: {}",
                    fromAccount.getAccountNumber(),
                    fromAccount.getBalance(),
                    request.getAmount());
            throw new AppException(ErrorCode.INSUFFICIENT_FUNDS);
        }
        if(request.getType().equals("EXTERNAL_TRANSFER")){
            String urlNapasTransfer = "http://localhost:8089/mock-napas/transfer";
            debit(fromAccount,request.getAmount());
            NapasTransferRequest napasTransferRequest = NapasTransferRequest.builder()
                    .toAccountNumber(request.getToAccountNumber())
                    .amount(request.getAmount())
                    .description(request.getDescription())
                    .bankCode(request.getDestinationBankCode())
                    .build();
            HttpEntity<NapasTransferRequest> entity = new HttpEntity<>(napasTransferRequest);
            try {
                ParameterizedTypeReference<ApiResponse<NapasTransferResponse>> responseType =
                        new ParameterizedTypeReference<>() {};

                // Dùng exchange để gọi API
                ResponseEntity<ApiResponse<NapasTransferResponse>> responseEntity =
                        restTemplate.exchange(urlNapasTransfer, HttpMethod.POST, entity, responseType);

                ApiResponse<NapasTransferResponse> apiResponse = responseEntity.getBody();
                if(apiResponse.getResult().getStatus().equals("FAILED")){
                    log.error("Giao dịch thất bại: {}", apiResponse.getResult().getMessage());
                    throw new AppException(ErrorCode.NAPAS_SERVER_ERROR);
                }
                CoreTransaction transaction = CoreTransaction.builder()
                        .amount(request.getAmount())
                        .transactionType(request.getType())
                        .timestamp(request.getTimestamp())
                        .status("COMPLETED")
                        .fromAccount(fromAccount)
                        .destinationAccountNumber(request.getToAccountNumber())
                        .destinationBankCode(request.getDestinationBankCode())
                        .referenceCode(request.getReferenceCode())
                        .description(request.getDescription())
                        .build();
                transactionRepo.save(transaction);
                CommonTransactionDTO transactionDTO = CommonTransactionDTO.builder()
                        .amount(transaction.getAmount())
                        .type(transaction.getTransactionType())
                        .timestamp(transaction.getTimestamp())
                        .status(transaction.getStatus())
                        .fromAccountNumber(transaction.getFromAccount().getAccountNumber())
                        .toAccountNumber(transaction.getDestinationAccountNumber())
                        .referenceCode(transaction.getReferenceCode())
                        .build();
                return transactionDTO;

            }  catch (RestClientException e) {
                // Bắt lỗi 5xx hoặc lỗi kết nối
                log.error("Lỗi server khi gọi API NAPAS {}", e.getMessage());
                throw new AppException(ErrorCode.NAPAS_SERVER_ERROR);
            }

        }else{
            CoreAccount toAccount = accountRepo.findByAccountNumberWithLock(request.getToAccountNumber());
            if (toAccount == null) {
                throw new AppException(ErrorCode.TO_ACCOUNT_NOT_EXIST);
            }
            if(!toAccount.getStatus().name().equals("ACTIVE")){
                throw new AppException(ErrorCode.TO_ACCOUNT_NOT_ACTIVE);
            }
            debit(fromAccount,request.getAmount());
            credit(toAccount,request.getAmount());
            CoreTransaction transaction = CoreTransaction.builder()
                    .amount(request.getAmount())
                    .transactionType(request.getType())
                    .timestamp(request.getTimestamp())
                    .status("COMPLETED")
                    .fromAccount(fromAccount)
                    .toAccount(toAccount)
                    .referenceCode(request.getReferenceCode())
                    .description(request.getDescription())
                    .build();
            transactionRepo.save(transaction);
            CommonTransactionDTO transactionDTO = CommonTransactionDTO.builder()
                    .amount(transaction.getAmount())
                    .type(transaction.getTransactionType())
                    .timestamp(transaction.getTimestamp())
                    .status(transaction.getStatus())
                    .fromAccountNumber(transaction.getFromAccount().getAccountNumber())
                    .toAccountNumber(transaction.getToAccount().getAccountNumber())
                    .referenceCode(transaction.getReferenceCode())
                    .build();
            return transactionDTO;
        }

    }

    @Override
    public BigDecimal getBalance(String accountNumber) {
        CoreAccount account = accountRepo.findByAccountNumber(accountNumber);
        if (account == null) {
            throw new AppException(ErrorCode.ACCOUNT_NOT_EXIST);
        }
        if (!account.getStatus().name().equals("ACTIVE")){
            throw new AppException(ErrorCode.ACCOUNT_NOT_ACTIVE);
        }
        return account.getBalance();
    }

    @Override
    @Transactional
    public void reverseTransaction(TransactionRequest request) {
        CoreAccount fromAccount = accountRepo.findByAccountNumber(request.getFromAccountNumber());
        CoreAccount toAccount = accountRepo.findByAccountNumber(request.getToAccountNumber());
        if (fromAccount == null ) {
            throw new AppException(ErrorCode.FROM_ACCOUNT_NOT_EXIST);
        }
        if (toAccount == null) {
            throw new AppException(ErrorCode.TO_ACCOUNT_NOT_EXIST);
        }
        if(!fromAccount.getStatus().name().equals("ACTIVE")){
            throw new AppException(ErrorCode.FROM_ACCOUNT_NOT_ACTIVE);
        }
        if(!toAccount.getStatus().name().equals("ACTIVE")){
            throw new AppException(ErrorCode.TO_ACCOUNT_NOT_ACTIVE);
        }
        debit(toAccount,request.getAmount());
        credit(fromAccount,request.getAmount());
        CoreTransaction reverseTransaction = CoreTransaction.builder()
                .fromAccount(fromAccount)
                .toAccount(toAccount)
                .amount(request.getAmount())
                .transactionType(request.getType())
                .timestamp(LocalDateTime.now())
                .status("COMPLETED")
                .referenceCode(request.getReferenceCode())
                .description(request.getDescription())
                .build();
        transactionRepo.save(reverseTransaction);
    }

    //    Trừ tiền tài khoản nguồn
    private void debit(CoreAccount account, BigDecimal amount) {
        if (account.getBalance().compareTo(amount) < 0) {
            throw new AppException(ErrorCode.INSUFFICIENT_FUNDS);
        }
        account.setBalance(account.getBalance().subtract(amount));
        accountRepo.save(account);
    }

    //  Cộng tiền ở tài khoản đích
    private void credit(CoreAccount account, BigDecimal amount) {
        account.setBalance(account.getBalance().add(amount));
        accountRepo.save(account);
    }
}