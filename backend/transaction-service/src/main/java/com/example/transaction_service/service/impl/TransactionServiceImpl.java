package com.example.transaction_service.service.impl;

import com.example.transaction_service.dto.TransactionDTO;
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
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;

    private final TransactionMapper transactionMapper;

    @Override
    @Transactional(noRollbackFor = Exception.class)
    public TransactionDTO createTransaction(TransactionDTO transactionDTO) {
        Transaction transaction = transactionMapper.toEntity(transactionDTO);

//      validateTransaction(transaction);

//      Khởi tạo transaction
        initTransaction(transaction);

//      Lưu trạng thái transaction là PENDING
        transactionRepository.save(transaction);

//      Thực thi transaction
        processTransaction(transaction);

        transactionRepository.save(transaction);
        return transactionMapper.toDTO(transaction);
    }


    @Override
    public TransactionDTO getTransactionById(String transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new AppException(ErrorCode.TRANSACTION_NOT_EXIST));
        return null;
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
//    private void validateTransaction(Transaction transaction){
//        if (!accountService.existsByAccountNumber(transaction.getFromAccountNumber())) {
//            throw new AppException(ErrorCode.FROM_ACCOUNT_NOT_EXIST);
//        }
//
//        if (!accountService.existsByAccountNumber(transaction.getToAccountNumber())) {
//                throw new AppException(ErrorCode.TO_ACCOUNT_NOT_EXIST);
//        }
//
//        if (transaction.getFromAccountNumber().equals(transaction.getToAccountNumber())) {
//            throw new AppException(ErrorCode.SAME_ACCOUNT_TRANSFER);
//        }
//        if (transaction.getAmount().compareTo(BigDecimal.valueOf(0.01))<0){
//            throw new AppException(ErrorCode.INVALID_AMOUNT);
//        }
//        if(transaction.getType() == null ){
//            throw new AppException(ErrorCode.INVALID_TRANSACTION_TYPE);
//        }
//
//
//        // Nếu là loại giao dịch cần trừ tiền trong tài khoản nguồn, thì kiểm tra số dư
//
//        if (EnumSet.of(TransactionType.TRANSFER, TransactionType.WITHDRAW,
//                TransactionType.CORE_BANKING, TransactionType.INTERNAL_TRANSFER).contains(transaction.getType())) {
//            BigDecimal balance;
//            try {
//                kiểm tra số dư
//               balance = coreBankingClient.getBalance(transaction.getFromAccountNumber());
//            } catch (Exception e) {
//                throw new AppException(ErrorCode.CORE_BANKING_UNAVAILABLE);
//            }
//
//            if (balance.compareTo(transaction.getAmount()) < 0) {
//                throw new AppException(ErrorCode.INSUFFICIENT_FUNDS);
//            }
//        }
//
//    }

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
    }
    private void processTransaction(Transaction transaction) {

        try {
            // Gọi core-banking để trừ/cộng tiền
//           coreBankingClient.performTransaction(transaction);
            transaction.setStatus(TransactionStatus.COMPLETED);
        } catch (Exception ex) {
            transaction.setStatus(TransactionStatus.FAILED);
            throw ex;
        }
    }
}
