package com.example.transaction_service.service.impl;


import com.example.common_service.dto.AccountDTO;
import com.example.common_service.dto.CustomerDTO;
import com.example.common_service.dto.PayRepaymentRequest;
import com.example.common_service.dto.TransactionRequest;
import com.example.common_service.services.account.AccountQueryService;
import com.example.common_service.services.account.CoreQueryService;
import com.example.common_service.services.customer.CustomerQueryService;
import com.example.common_service.services.transaction.CoreTransactionService;
import com.example.transaction_service.dto.TransactionDTO;
import com.example.transaction_service.dto.request.*;
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
import org.apache.dubbo.rpc.RpcException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@DubboService
public class TransactionServiceImpl implements TransactionService{

    private final TransactionRepository transactionRepository;

    private final TransactionMapper transactionMapper;

    @DubboReference
    private final AccountQueryService accountQueryService;

    @DubboReference
    private final CoreQueryService coreQueryService;

    @DubboReference
    private final CoreTransactionService coreTransactionService;

    @DubboReference
    private final CustomerQueryService customerQueryService;

    private final RedisTemplate<String,String> redisTemplate;

    @Value("${masterAccount}")
    private String masterAccount;
    private static final Logger log = LoggerFactory.getLogger(TransactionServiceImpl.class);

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
    //      Gửi OTP
            sendOTP(transaction.getReferenceCode());
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
//       Gửi OTP
        sendOTP(transaction.getReferenceCode());
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
//        Gửi OTP
        sendOTP(transaction.getReferenceCode());

        transactionRepository.save(transaction);
        return transactionMapper.toDTO(transaction);
    }

    @Override
    @Transactional(noRollbackFor = Exception.class)
    public TransactionDTO payBill(PayRepaymentRequest repaymentRequest) {
        Transaction transaction = new Transaction();
        transaction.setFromAccountNumber(repaymentRequest.getFromAccountNumber());
        transaction.setAmount(repaymentRequest.getAmount());
        transaction.setDescription(repaymentRequest.getDescription());
        transaction.setCurrency(CurrencyType.valueOf(repaymentRequest.getCurrency()));
        transaction.setType(TransactionType.PAY_BILL);

        transaction.setToAccountNumber(masterAccount);

        //      Validate
        try{
            validateTransaction(transaction);
        }catch (RpcException rpcEx) {
            transaction.setStatus(TransactionStatus.FAILED);
            log.error("Dubbo provider not available or registry issue: " + rpcEx.getMessage());
            throw rpcEx;
        }
//      khởi tạo transaction
        initTransaction(transaction);
        transactionRepository.save(transaction);

//        Gửi OTP
        sendOTP(transaction.getReferenceCode());

        transactionRepository.save(transaction);
        return transactionMapper.toDTO(transaction);
    }

    @Override
    public TransactionDTO disburse(DisburseRequest disburseRequest) {
        Transaction transaction = new Transaction();
        transaction.setToAccountNumber(disburseRequest.getToAccountNumber());
        transaction.setAmount(disburseRequest.getAmount());
        transaction.setDescription(disburseRequest.getDescription());
        transaction.setCurrency(CurrencyType.valueOf(disburseRequest.getCurrency()));
        transaction.setType(TransactionType.DISBURSEMENT);

        transaction.setFromAccountNumber(masterAccount);
//      Validate Transaction
        validateTransaction(transaction);

        initTransaction(transaction);
//       Gửi OTP
        sendOTP(transaction.getReferenceCode());
        transactionRepository.save(transaction);
        return transactionMapper.toDTO(transaction);
    }

    @Override
    @Transactional
    public TransactionDTO confirmTransaction(ConfirmTransactionRequest request){
//        Lấy OTP từ redis
        String keyOTP = "OTP:" + request.getReferenceCode();
        String storedOtp = redisTemplate.opsForValue().get(keyOTP);

        if (storedOtp == null) {
            throw new AppException(ErrorCode.OTP_EXPIRED);
        }

        if (!storedOtp.equals(request.getOtpCode())) {
            throw new AppException(ErrorCode.INVALID_OTP);
        }
        Transaction txn = transactionRepository.findByReferenceCode(request.getReferenceCode());
        if(txn == null) throw new AppException(ErrorCode.TRANSACTION_NOT_EXIST);
        if (txn.getStatus() != TransactionStatus.PENDING) {
            throw new AppException(ErrorCode.INVALID_TRANSACTION_STATUS);
        }
//        Thực thi giao dịch
        processTransaction(txn);
        transactionRepository.save(txn);

//        Xóa key khỏi redis
        redisTemplate.delete(keyOTP);
        return transactionMapper.toDTO(txn);
    }

    @Override
    public void resendOtp(String referenceCode) {
        Transaction txn = transactionRepository.findByReferenceCode(referenceCode);
        if(txn == null) throw new AppException(ErrorCode.TRANSACTION_NOT_EXIST);
        if (txn.getStatus() != TransactionStatus.PENDING) {
            throw new AppException(ErrorCode.INVALID_TRANSACTION_STATUS);
        }
        String keyOTP = "OTP:" + referenceCode;
        String otp = String.valueOf(100000 + new Random().nextInt(900000));
        redisTemplate.opsForValue().set(keyOTP,otp, Duration.ofSeconds(60));
        System.out.println(otp);
    }

    @Override
    public TransactionDTO getTransactionById(String transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new AppException(ErrorCode.TRANSACTION_NOT_EXIST));
        return transactionMapper.toDTO(transaction);
    }

    @Override
    public List<TransactionDTO> getAccountTransactions(String accountNumber) {
        List<Transaction> transactionList = transactionRepository.getAccountTransactions(accountNumber);
        return transactionList.stream()
                .map(transaction -> transactionMapper.toDTO(transaction)).collect(Collectors.toList());
    }

    @Override
    public TransactionDTO getTransactionByTransactionCode(String referenceCode) {
        Transaction transaction = transactionRepository.findByReferenceCode(referenceCode);
        if(transaction==null) throw new AppException(ErrorCode.TRANSACTION_NOT_EXIST);
        return transactionMapper.toDTO(transaction);
    }

    @Override
    public TransactionDTO updateTransactionsStatus(String transactionId, TransactionStatus status) {
        return null;
    }


//    Kiểm tra thông tin Transaction
    private void validateTransaction(Transaction transaction){
        AccountDTO fromAccount = accountQueryService.getAccountByAccountNumber(transaction.getFromAccountNumber());
        AccountDTO toAccount = accountQueryService.getAccountByAccountNumber(transaction.getToAccountNumber());

        if (fromAccount==null) {
            throw new AppException(ErrorCode.FROM_ACCOUNT_NOT_EXIST);
        }

        if (toAccount==null) {
            throw new AppException(ErrorCode.TO_ACCOUNT_NOT_EXIST);
        }
        if (!fromAccount.getAccountType().equals("PAYMENT")) {
            throw new AppException(ErrorCode.FROM_ACCOUNT_NOT_PAYMENT);
        }

        if (!toAccount.getAccountType().equals("PAYMENT")) {
            throw new AppException(ErrorCode.TO_ACCOUNT_NOT_PAYMENT);
        }

        if(!fromAccount.getStatus().equals("ACTIVE")){
            throw new AppException(ErrorCode.FROM_ACCOUNT_NOT_ACTIVE);
        }
        if(!toAccount.getStatus().equals("ACTIVE")){
            throw new AppException(ErrorCode.TO_ACCOUNT_NOT_ACTIVE);
        }
        CustomerDTO fromCustomer = customerQueryService.getCustomerByCifCode(fromAccount.getCifCode());
        CustomerDTO toCustomer = customerQueryService.getCustomerByCifCode(toAccount.getCifCode());

        if (fromCustomer==null) {
            throw new AppException(ErrorCode.CUSTOMER_NOT_EXIST);
        }

        if (toCustomer==null) {
            throw new AppException(ErrorCode.CORE_BANKING_UNAVAILABLE);
        }

        if(fromCustomer.getStatus().equals("ACTIVE")){
            throw new AppException(ErrorCode.FROM_CUSTOMER_NOT_ACTIVE);
        }
        if(toCustomer.getStatus().equals("ACTIVE")){
            throw new AppException(ErrorCode.TO_CUSTOMER_NOT_ACTIVE);
        }

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
        if (EnumSet.of(TransactionType.TRANSFER, TransactionType.WITHDRAW,TransactionType.PAY_BILL,
                TransactionType.CORE_BANKING, TransactionType.INTERNAL_TRANSFER).contains(transaction.getType())) {
            BigDecimal balance;
            try {
//                kiểm tra số dư
               balance = coreQueryService.getBalance(transaction.getFromAccountNumber());
            }
            catch (Exception e) {
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

    private void sendOTP(String referenceCode){
        String keyOTP = "OTP:"+referenceCode;
        String otp = String.valueOf(100000 + new Random().nextInt(900000));
        redisTemplate.opsForValue().set(keyOTP,otp, Duration.ofSeconds(60));
        System.out.println(otp);
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
