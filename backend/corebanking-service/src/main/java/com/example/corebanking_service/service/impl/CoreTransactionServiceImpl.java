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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class CoreTransactionServiceImpl implements CoreTransactionService {

    private final CoreAccountRepo accountRepo;
    private final CoreTransactionRepo transactionRepo;

    private final RestTemplate restTemplate;

    @Value("${mock-napas-api-key}")
    private String NAPAS_API_KEY;

    @Override
    @Transactional
    public CommonTransactionDTO performTransfer(TransactionRequest request) {
        log.info("Bắt đầu xử lý giao dịch. FromAccount={}, ToAccount={}, Amount={}, Type={}, RefCode={}",
                request.getFromAccountNumber(),
                request.getToAccountNumber(),
                request.getAmount(),
                request.getType(),
                request.getReferenceCode());

        CoreAccount fromAccount = accountRepo.findByAccountNumber(request.getFromAccountNumber());
        if (fromAccount == null ) {
            log.error("Tài khoản nguồn không tồn tại. AccountNumber={}", request.getFromAccountNumber());
            throw new AppException(ErrorCode.FROM_ACCOUNT_NOT_EXIST);
        }
        if (!fromAccount.getStatus().name().equals("ACTIVE")) {
            log.error("Tài khoản nguồn không hoạt động. AccountNumber={}, Status={}",
                    fromAccount.getCoreAccountNumber().getNumber(), fromAccount.getStatus());
            throw new AppException(ErrorCode.FROM_ACCOUNT_NOT_ACTIVE);
        }
        if (fromAccount.getBalance().compareTo(request.getAmount()) < 0) {
            log.warn("Không đủ số dư. AccountNumber={}, Balance={}, Required={}",
                    fromAccount.getCoreAccountNumber().getNumber(),
                    fromAccount.getBalance(),
                    request.getAmount());
            throw new AppException(ErrorCode.INSUFFICIENT_FUNDS);
        }

        if (request.getType().equals("EXTERNAL_TRANSFER")) {
            log.info("Thực hiện chuyển khoản liên ngân hàng qua NAPAS. FromAccount={}, ToAccount={}, BankCode={}",
                    fromAccount.getCoreAccountNumber().getNumber(),
                    request.getToAccountNumber(),
                    request.getDestinationBankCode());

            String urlNapasTransfer = "http://localhost:8089/mock-napas/transfer";
            debit(fromAccount, request.getAmount());

            NapasTransferRequest napasTransferRequest = NapasTransferRequest.builder()
                    .toAccountNumber(request.getToAccountNumber())
                    .amount(request.getAmount())
                    .description(request.getDescription())
                    .bankCode(request.getDestinationBankCode())
                    .build();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-API-Key", NAPAS_API_KEY);

            HttpEntity<NapasTransferRequest> entity = new HttpEntity<>(napasTransferRequest, headers);
            try {
                log.debug("Gửi request tới NAPAS: {}", napasTransferRequest);

                ParameterizedTypeReference<ApiResponse<NapasTransferResponse>> responseType =
                        new ParameterizedTypeReference<>() {};

                ResponseEntity<ApiResponse<NapasTransferResponse>> responseEntity =
                        restTemplate.exchange(urlNapasTransfer, HttpMethod.POST, entity, responseType);

                ApiResponse<NapasTransferResponse> apiResponse = responseEntity.getBody();
                log.info("Nhận phản hồi từ NAPAS: {}", apiResponse);

                if (apiResponse.getResult().getStatus().equals("FAILED")) {
                    log.error("Giao dịch thất bại từ NAPAS. Message={}", apiResponse.getResult().getMessage());
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

                log.info("Chuyển khoản liên ngân hàng thành công. TransactionId={}, RefCode={}",
                        transaction.getTransactionId(), transaction.getReferenceCode());

                return CommonTransactionDTO.builder()
                        .amount(transaction.getAmount())
                        .type(transaction.getTransactionType())
                        .timestamp(transaction.getTimestamp())
                        .status(transaction.getStatus())
                        .fromAccountNumber(transaction.getFromAccount().getCoreAccountNumber().getNumber())
                        .toAccountNumber(transaction.getDestinationAccountNumber())
                        .referenceCode(transaction.getReferenceCode())
                        .build();

            } catch (RestClientException e) {
                log.error("Lỗi khi gọi API NAPAS. Message={}", e.getMessage(), e);
                throw new AppException(ErrorCode.NAPAS_SERVER_ERROR);
            }

        } else {
            log.info("Thực hiện chuyển khoản nội bộ. FromAccount={}, ToAccount={}, Amount={}",
                    fromAccount.getCoreAccountNumber().getNumber(),
                    request.getToAccountNumber(),
                    request.getAmount());

            CoreAccount toAccount = accountRepo.findByAccountNumber(request.getToAccountNumber());
            if (toAccount == null) {
                log.error("Tài khoản đích không tồn tại. ToAccount={}", request.getToAccountNumber());
                throw new AppException(ErrorCode.TO_ACCOUNT_NOT_EXIST);
            }
            if (!toAccount.getStatus().name().equals("ACTIVE")) {
                log.error("Tài khoản đích không hoạt động. ToAccount={}, Status={}",
                        toAccount.getCoreAccountNumber().getNumber(), toAccount.getStatus());
                throw new AppException(ErrorCode.TO_ACCOUNT_NOT_ACTIVE);
            }

            debit(fromAccount, request.getAmount());
            credit(toAccount, request.getAmount());

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

            log.info("Chuyển khoản nội bộ thành công. TransactionId={}, RefCode={}",
                    transaction.getTransactionId(), transaction.getReferenceCode());

            return CommonTransactionDTO.builder()
                    .amount(transaction.getAmount())
                    .type(transaction.getTransactionType())
                    .timestamp(transaction.getTimestamp())
                    .status(transaction.getStatus())
                    .fromAccountNumber(transaction.getFromAccount().getCoreAccountNumber().getNumber())
                    .toAccountNumber(transaction.getToAccount().getCoreAccountNumber().getNumber())
                    .referenceCode(transaction.getReferenceCode())
                    .build();
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
        log.info("Bắt đầu trừ tiền. AccountId={}, Số dư hiện tại={}, Số tiền cần trừ={}",
                account.getId(), account.getBalance(), amount);

        if (account.getBalance().compareTo(amount) < 0) {
            log.error("Không đủ số dư. AccountId={}, Số dư={}, Số tiền yêu cầu trừ={}",
                    account.getId(), account.getBalance(), amount);
            throw new AppException(ErrorCode.INSUFFICIENT_FUNDS);
        }

        account.setBalance(account.getBalance().subtract(amount));
        accountRepo.save(account);

        log.info("Trừ tiền thành công. AccountId={}, Số dư mới={}",
                account.getId(), account.getBalance());
    }

    //  Cộng tiền ở tài khoản đích
    private void credit(CoreAccount account, BigDecimal amount) {
        log.info("Bắt đầu cộng tiền. AccountId={}, Số dư hiện tại={}, Số tiền cần cộng={}",
                account.getId(), account.getBalance(), amount);

        account.setBalance(account.getBalance().add(amount));
        accountRepo.save(account);

        log.info("Cộng tiền thành công. AccountId={}, Số dư mới={}",
                account.getId(), account.getBalance());
    }
}