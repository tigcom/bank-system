package com.example.transaction_service.service.impl;

import com.example.common_service.dto.AccountDTO;
import com.example.common_service.dto.TransactionRequest;
import com.example.common_service.services.account.AccountQueryService;
import com.example.common_service.services.transaction.CoreTransactionService;
import com.example.transaction_service.dto.TransactionDTO;
import com.example.transaction_service.dto.request.DepositRequest;
import com.example.transaction_service.dto.request.TransferRequest;
import com.example.transaction_service.dto.request.WithdrawRequest;
import com.example.transaction_service.entity.Transaction;
import com.example.transaction_service.enums.CurrencyType;
import com.example.transaction_service.enums.TransactionStatus;
import com.example.transaction_service.enums.TransactionType;
import com.example.transaction_service.exception.AppException;
import com.example.transaction_service.exception.ErrorCode;
import com.example.transaction_service.mapper.TransactionMapper;
import com.example.transaction_service.repository.TransactionRepository;
import com.example.transaction_service.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@DubboService
public class TransactionServiceImpl implements TransactionService{

    private final TransactionRepository transactionRepository;

    private final TransactionMapper transactionMapper;

    @DubboReference
    private final AccountQueryService accountQueryService;

    @DubboReference
    private final CoreTransactionService coreTransactionService;

    @Value("${masterAccount}")
    private String masterAccount;

    @Override
    @Transactional(noRollbackFor = Exception.class)
    public TransactionDTO transfer(TransferRequest transferRequest) {
        Transaction transaction = new Transaction();
        transaction.setFromAccountNumber(transferRequest.getFromAccountNumber());
        transaction.setToAccountNumber(transferRequest.getToAccountNumber());
        transaction.setAmount(transferRequest.getAmount());
        transaction.setDescription(transferRequest.getDescription());
        transaction.setCurrency(CurrencyType.valueOf(transferRequest.getCurrency()));
        transaction.setType(TransactionType.TRANSFER);

//      Validate Transaction
        validateTransaction(transaction);
//      Khởi tạo transaction
        initTransaction(transaction);

//      Thực thi transaction
        processTransaction(transaction);

        transactionRepository.save(transaction);
        return transactionMapper.toDTO(transaction);
    }

    @Override
    @Transactional(noRollbackFor = Exception.class)
    public TransactionDTO deposit(DepositRequest depositRequest) {
        Transaction transaction = new Transaction();
        transaction.setToAccountNumber(depositRequest.getToAccountNumber());
        transaction.setAmount(depositRequest.getAmount());
        transaction.setDescription(depositRequest.getDescription());
        transaction.setCurrency(CurrencyType.valueOf(depositRequest.getCurrency()));
        transaction.setType(TransactionType.DEPOSIT);

        transaction.setFromAccountNumber(masterAccount);
//      Validate Transaction
        validateTransaction(transaction);

        initTransaction(transaction);

        transactionRepository.save(transaction);
        processTransaction(transaction);

        transactionRepository.save(transaction);
        return transactionMapper.toDTO(transaction);
    }

    @Override
    @Transactional(noRollbackFor = Exception.class)
    public TransactionDTO withdraw(WithdrawRequest withdrawRequest) {
        Transaction transaction = new Transaction();
        transaction.setFromAccountNumber(withdrawRequest.getFromAccountNumber());
        transaction.setAmount(withdrawRequest.getAmount());
        transaction.setDescription(withdrawRequest.getDescription());
        transaction.setCurrency(CurrencyType.valueOf(withdrawRequest.getCurrency()));
        transaction.setType(TransactionType.WITHDRAW);

        transaction.setToAccountNumber(masterAccount);
//      Validate
        validateTransaction(transaction);
//      khởi tạo transaction
        initTransaction(transaction);
        transactionRepository.save(transaction);
//      Thực thi transaction
        processTransaction(transaction);

        transactionRepository.save(transaction);
        return transactionMapper.toDTO(transaction);
    }

    @Override
    public TransactionDTO payBill(TransactionDTO transactionDTO) {
        return null;
    }


    @Override
    public TransactionDTO getTransactionById(String transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new AppException(ErrorCode.TRANSACTION_NOT_EXIST));
        return transactionMapper.toDTO(transaction);
    }

    @Override
    public List<TransactionDTO> getTransactionByAccount(String accountId) {
        return null;
    }

    @Override
    public TransactionDTO getTransactionByTransactionCode(String referenceCode) {
        return null;
    }

    @Override
    public TransactionDTO updateTransactionsStatus(String transactionId, TransactionStatus status) {
        return null;
    }


//    Kiểm tra thông tin Transaction
    private void validateTransaction(Transaction transaction){
        AccountDTO fromAccount = accountQueryService.getAccountByNumber(transaction.getFromAccountNumber());
        AccountDTO toAccount = accountQueryService.getAccountByNumber(transaction.getToAccountNumber());

        if (!fromAccount.getAccountType().equals("PAYMENT")) {
            throw new AppException(ErrorCode.FROM_ACCOUNT_NOT_PAYMENT);
        }

        if (!toAccount.getAccountType().equals("PAYMENT")) {
            throw new AppException(ErrorCode.TO_ACCOUNT_NOT_PAYMENT);
        }

        if (fromAccount==null) {
            throw new AppException(ErrorCode.FROM_ACCOUNT_NOT_EXIST);
        }

        if (toAccount==null) {
            throw new AppException(ErrorCode.TO_ACCOUNT_NOT_EXIST);
        }
//      Lấy customer by account ID

        if (transaction.getFromAccountNumber().equals(transaction.getToAccountNumber())) {
            throw new AppException(ErrorCode.SAME_ACCOUNT_TRANSFER);
        }
        if (transaction.getAmount().compareTo(BigDecimal.valueOf(0.01))<0){
            throw new AppException(ErrorCode.INVALID_AMOUNT);
        }
        if(transaction.getType() == null ){
            throw new AppException(ErrorCode.INVALID_TRANSACTION_TYPE);
        }
//         Nếu là loại giao dịch cần trừ tiền trong tài khoản nguồn, thì kiểm tra số dư
        if (EnumSet.of(TransactionType.TRANSFER, TransactionType.WITHDRAW,
                TransactionType.CORE_BANKING, TransactionType.INTERNAL_TRANSFER).contains(transaction.getType())) {
            BigDecimal balance;
            try {
//                kiểm tra số dư
               balance = accountQueryService.getBalance(transaction.getFromAccountNumber());
            } catch (Exception e) {
                throw new AppException(ErrorCode.CORE_BANKING_UNAVAILABLE);
            }

            if (balance.compareTo(transaction.getAmount()) < 0) {
                throw new AppException(ErrorCode.INSUFFICIENT_FUNDS);
            }
        }

    }

    private void initTransaction(Transaction transaction) {
        transaction.setStatus(TransactionStatus.PENDING);
        transaction.setTimestamp(LocalDateTime.now());
        if (transaction.getCurrency() == null) {
            transaction.setCurrency(CurrencyType.VND);
        }
        if (transaction.getReferenceCode() == null) {
            String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);
            String dateTimeNow = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
            String referenceCode = "TXN-"+ transaction.getFromAccountNumber() + "-"+ dateTimeNow +uniqueSuffix;
            transaction.setReferenceCode(referenceCode);
        }
        transaction.setStatus(TransactionStatus.PENDING);
    }
    private void processTransaction(Transaction transaction) {
        try {
            TransactionRequest request = TransactionRequest.builder()
                    .fromAccountNumber(transaction.getFromAccountNumber())
                    .toAccountNumber(transaction.getToAccountNumber())
                    .amount(transaction.getAmount())
                    .description(transaction.getDescription())
                    .status(transaction.getStatus().name())
                    .timestamp(transaction.getTimestamp())
                    .type(transaction.getType().name())
                    .build();
           coreTransactionService.performTransfer(request);
           transaction.setStatus(TransactionStatus.COMPLETED);
        } catch (Exception ex) {
            transaction.setStatus(TransactionStatus.FAILED);
            throw ex;
        }
    }
}
