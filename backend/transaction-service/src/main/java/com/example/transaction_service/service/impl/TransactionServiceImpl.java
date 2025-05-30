package com.example.transaction_service.service.impl;


import com.example.common_service.dto.AccountDTO;
import com.example.common_service.dto.CommonTransactionDTO;
import com.example.common_service.dto.CustomerDTO;
import com.example.common_service.dto.MailMessageDTO;
import com.example.common_service.dto.request.CreateAccountSavingRequest;
import com.example.common_service.dto.request.TransactionRequest;
import com.example.common_service.services.account.AccountQueryService;
import com.example.common_service.services.customer.CustomerQueryService;
import com.example.transaction_service.dto.TransactionDTO;
import com.example.transaction_service.dto.request.*;
import com.example.transaction_service.dto.response.ApiResponse;
import com.example.transaction_service.entity.Transaction;
import com.example.transaction_service.enums.*;
import com.example.transaction_service.exception.AppException;
import com.example.transaction_service.exception.ErrorCode;
import com.example.transaction_service.mapper.TransactionMapper;
import com.example.transaction_service.repository.TransactionRepository;
import com.example.transaction_service.service.TransactionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.rpc.RpcException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService{

    private final TransactionRepository transactionRepository;

    private final TransactionMapper transactionMapper;

    @DubboReference
    private final AccountQueryService accountQueryService;


    @DubboReference
    private final CustomerQueryService customerQueryService;

    private final RedisTemplate<String,String> redisTemplate;

    private final StreamBridge streamBridge;

    private String URL_CORE_BANK = "http://localhost:8083/corebanking/api/core-bank";

    @Value("${masterAccount}")
    private String masterAccount;
    private static final Logger log = LoggerFactory.getLogger(TransactionServiceImpl.class);

    @Autowired
    private RestTemplate restTemplate;
    @Override
    @Transactional
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
        sendOTP(transaction.getReferenceCode(),transaction.getFromAccountNumber());
        transactionRepository.save(transaction);
        return transactionMapper.toDTO(transaction);
    }

    @Override
    @Transactional
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
        sendOTP(transaction.getReferenceCode(),transaction.getToAccountNumber());
        transactionRepository.save(transaction);
        return transactionMapper.toDTO(transaction);
    }

    @Override
    @Transactional
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
        sendOTP(transaction.getReferenceCode(),transaction.getFromAccountNumber());

        transactionRepository.save(transaction);
        return transactionMapper.toDTO(transaction);
    }

    @Override
    @Transactional
    public TransactionDTO payBill(PaymentRequest repaymentRequest) {
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
        sendOTP(transaction.getReferenceCode(),transaction.getFromAccountNumber());

        transactionRepository.save(transaction);
        return transactionMapper.toDTO(transaction);
    }

    @Override
    @Transactional
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
//      Thực thi giao dịch
        processTransaction(transaction);
        transactionRepository.save(transaction);
        return transactionMapper.toDTO(transaction);
    }

    @Override
    public TransactionDTO createAccountSaving(CreateAccountSavingRequest accountSavingRequest) {
        Transaction transaction = new Transaction();
        transaction.setFromAccountNumber(accountSavingRequest.getFromAccountNumber());
        transaction.setAmount(accountSavingRequest.getAmount());
        transaction.setDescription(accountSavingRequest.getDescription());
        transaction.setCurrency(CurrencyType.valueOf(accountSavingRequest.getCurrency()));
        transaction.setType(TransactionType.CREATE_ACCOUNT_SAVING);

        transaction.setToAccountNumber(masterAccount);
//      Validate
        validateTransaction(transaction);
//      khởi tạo transaction
        initTransaction(transaction);
//      Thực thi transaction
        processTransaction(transaction);

        transactionRepository.save(transaction);
        return transactionMapper.toDTO(transaction);
    }

    @Override
    @Transactional
    public TransactionDTO confirmTransaction(ConfirmTransactionRequest request){
//        Lấy OTP từ redis
        String keyOTP = "OTP:" + request.getReferenceCode();
        String storedOtp = redisTemplate.opsForValue().get(keyOTP);

        String keyFailCount = "OTP_FAIL_COUNT:" + request.getReferenceCode();
        if (storedOtp == null) {
            throw new AppException(ErrorCode.OTP_EXPIRED);
        }

        if (!storedOtp.equals(request.getOtpCode())) {
            String failStr = redisTemplate.opsForValue().get(keyFailCount);
            int failCount = (failStr == null) ? 0 : Integer.parseInt(failStr);

            failCount++;
            redisTemplate.opsForValue().set(keyFailCount, String.valueOf(failCount));
            if (failCount > 3) {
                Transaction txn = transactionRepository.findByReferenceCode(request.getReferenceCode());
                if (txn != null) {
                    txn.setStatus(TransactionStatus.FAILED);
                    txn.setFailedReason(ErrorCode.OTP_FAILED_TOO_MANY_TIMES.getMessage());
                    transactionRepository.save(txn);
                }
                log.error("Giao dịch thất bại: {}",ErrorCode.OTP_FAILED_TOO_MANY_TIMES);
                return transactionMapper.toDTO(txn);

            }
            throw new AppException(ErrorCode.INVALID_OTP);
        }


        Transaction txn = transactionRepository.findByReferenceCode(request.getReferenceCode());
        if(txn == null) throw new AppException(ErrorCode.TRANSACTION_NOT_EXIST);
        if (txn.getStatus() != TransactionStatus.PENDING) {
            throw new AppException(ErrorCode.INVALID_TRANSACTION_STATUS);
        }
//      Thực thi giao dịch
        processTransaction(txn);
        transactionRepository.save(txn);
//        Xóa key khỏi redis
        redisTemplate.delete(keyFailCount);
        redisTemplate.delete(keyOTP);
        return transactionMapper.toDTO(txn);
    }

    @Override
    @Transactional
    public TransactionDTO transferToExternalBank(ExternalTransferRequest externalTransferRequest) {
        Transaction transaction = new Transaction();
        transaction.setFromAccountNumber(externalTransferRequest.getFromAccountNumber());
        transaction.setToAccountNumber(externalTransferRequest.getToAccountNumber());
        transaction.setAmount(externalTransferRequest.getAmount());
        transaction.setDescription(externalTransferRequest.getDescription());
        transaction.setCurrency(CurrencyType.valueOf(externalTransferRequest.getCurrency()));
        transaction.setType(TransactionType.EXTERNAL_TRANSFER);
        transaction.setBankType(BankType.EXTERNAL);
        String bankCode = externalTransferRequest.getDestinationBankCode();
        if(bankCode.equals(BankCode.KIENLONGBANK.getCode()))
            throw new AppException(ErrorCode.INVALID_BANK_CODE);
        transaction.setDestinationBankCode(bankCode);
        try {
            String bankName = BankCode.fromCode(bankCode).getBankName();
            transaction.setDestinationBankName(bankName);
        } catch (AppException e) {
            throw new AppException(ErrorCode.BANK_CODE_VALID);
        }
//        Validate thông tin giao dịch
        AccountDTO fromAccount = accountQueryService.getAccountByAccountNumber(transaction.getFromAccountNumber());
        if (fromAccount==null) {
            throw new AppException(ErrorCode.FROM_ACCOUNT_NOT_EXIST);
        }
        if (!fromAccount.getAccountType().equals("PAYMENT")) {
            throw new AppException(ErrorCode.FROM_ACCOUNT_NOT_PAYMENT);
        }
        CustomerDTO fromCustomer = customerQueryService.getCustomerByCifCode(fromAccount.getCifCode());
        if (fromCustomer==null) {
            throw new AppException(ErrorCode.CUSTOMER_NOT_EXIST);
        }
        if(!fromCustomer.getStatus().name().equals("ACTIVE")){
            throw new AppException(ErrorCode.FROM_CUSTOMER_NOT_ACTIVE);
        }
        BigDecimal balance;
            try {
//                kiểm tra số dư
                String url = URL_CORE_BANK+"/get-balance/{accountNumber}";
                ParameterizedTypeReference<ApiResponse<BigDecimal>> responseType =
                        new ParameterizedTypeReference<ApiResponse<BigDecimal>>() {};
                ResponseEntity<ApiResponse<BigDecimal>> response = restTemplate.exchange(
                        url,
                        HttpMethod.GET,
                        null,
                        responseType,
                        transaction.getFromAccountNumber()
                );
                balance = response.getBody().getResult();
            }
            catch (Exception e) {
                throw new AppException(ErrorCode.CORE_BANKING_UNAVAILABLE);
            }

            if (balance.compareTo(transaction.getAmount()) < 0) {
                throw new AppException(ErrorCode.INSUFFICIENT_FUNDS);
            }

        initTransaction(transaction);
//        Gửi OTP
        sendOTP(transaction.getReferenceCode(),transaction.getFromAccountNumber());

        transactionRepository.save(transaction);
        return transactionMapper.toDTO(transaction);
    }

    @Override
    public void resendOtp(ResendOtpRequest resendOtpRequest) {
        Transaction txn = transactionRepository.findByReferenceCode(resendOtpRequest.getReferenceCode());
        if(txn == null) throw new AppException(ErrorCode.TRANSACTION_NOT_EXIST);
        if (txn.getStatus() != TransactionStatus.PENDING) {
            throw new AppException(ErrorCode.INVALID_TRANSACTION_STATUS);
        }
        String keyOTP = "OTP:" + resendOtpRequest.getReferenceCode();
        String otp = String.valueOf(100000 + new Random().nextInt(900000));
        redisTemplate.opsForValue().set(keyOTP,otp, Duration.ofSeconds(60));
        AccountDTO fromAccount = accountQueryService.getAccountByAccountNumber(resendOtpRequest.getAccountNumberRecipient());
        CustomerDTO fromCustomer = customerQueryService.getCustomerByCifCode(fromAccount.getCifCode());
        MailMessageDTO mailMessage = MailMessageDTO.builder()
                .subject("Xác nhận OTP ")
                .body(otp)
                .recipient("levandai2692003@gmail.com")
                .recipientName(fromCustomer.getFullName())
                .build();
        streamBridge.send("mail-out-0", mailMessage);
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
        if (EnumSet.of(TransactionType.TRANSFER, TransactionType.WITHDRAW,TransactionType.PAY_BILL,
                TransactionType.DISBURSEMENT,
                TransactionType.CORE_BANKING, TransactionType.INTERNAL_TRANSFER).contains(transaction.getType())) {
            if (!fromAccount.getAccountType().equals("PAYMENT")) {
                throw new AppException(ErrorCode.FROM_ACCOUNT_NOT_PAYMENT);
            }

            if (!toAccount.getAccountType().equals("PAYMENT")) {
                throw new AppException(ErrorCode.TO_ACCOUNT_NOT_PAYMENT);
            }
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

        if(!fromCustomer.getStatus().name().equals("ACTIVE")){
            throw new AppException(ErrorCode.FROM_CUSTOMER_NOT_ACTIVE);
        }
        if(!toCustomer.getStatus().name().equals("ACTIVE")){
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
               String url = URL_CORE_BANK+"/get-balance/{accountNumber}";
               ParameterizedTypeReference<ApiResponse<BigDecimal>> responseType =
                        new ParameterizedTypeReference<ApiResponse<BigDecimal>>() {};
               ResponseEntity<ApiResponse<BigDecimal>> response = restTemplate.exchange(
                        url,
                        HttpMethod.GET,
                        null,
                        responseType,
                        transaction.getFromAccountNumber()
                );
               balance = response.getBody().getResult();
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

    private void sendOTP(String referenceCode,String accountNumberRecipient){
        String keyOTP = "OTP:"+referenceCode;
        String otp = String.valueOf(100000 + new Random().nextInt(900000));
        redisTemplate.opsForValue().set(keyOTP,otp, Duration.ofSeconds(60));
        AccountDTO fromAccount = accountQueryService.getAccountByAccountNumber(accountNumberRecipient);
        CustomerDTO fromCustomer = customerQueryService.getCustomerByCifCode(fromAccount.getCifCode());
        MailMessageDTO mailMessage = MailMessageDTO.builder()
                .subject("Xác nhận OTP ")
                .body(otp)
                .recipient("levandai2692003@gmail.com")
                .recipientName(fromCustomer.getFullName())
                .build();
        System.out.println("OTP:"+otp);
        streamBridge.send("mail-out-0", mailMessage);
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
            String url = URL_CORE_BANK+"/perform-transaction";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<TransactionRequest> httpEntity = new HttpEntity<>(request, headers);
//          Định nghĩa kiểu dữ liệu trả về
            ParameterizedTypeReference<ApiResponse<CommonTransactionDTO>> responseType =
                    new ParameterizedTypeReference<ApiResponse<CommonTransactionDTO>>() {};

//          Gửi POST request
            ResponseEntity<ApiResponse<CommonTransactionDTO>> responseEntity = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    httpEntity,
                    responseType
            );
            ApiResponse<CommonTransactionDTO> apiResponse = responseEntity.getBody();
            if(apiResponse.getCode()==200){
                transaction.setStatus(TransactionStatus.COMPLETED);
            }else {
                transaction.setStatus(TransactionStatus.FAILED);
                transaction.setFailedReason(apiResponse.getMessage());
            }
        } catch (HttpClientErrorException e) {
            // Lấy body lỗi trả về dạng JSON
            String errorBody = e.getResponseBodyAsString();

            String failedReason = "Lỗi không xác định";

            if (errorBody != null && !errorBody.isEmpty()) {
                try {
                    ObjectMapper objectMapper = new ObjectMapper();
                    // Map hoặc class tương ứng với response lỗi
                    Map<String, Object> errorMap = objectMapper.readValue(errorBody, Map.class);
                    if (errorMap.containsKey("message")) {
                        failedReason = (String) errorMap.get("message");
                    }
                } catch (Exception jsonEx) {
                    failedReason = errorBody;
                }
            } else {
                failedReason = e.getStatusText();
            }

            transaction.setStatus(TransactionStatus.FAILED);
            transaction.setFailedReason(failedReason);

            log.error("Transaction failed: {}", failedReason, e);

        } catch (Exception ex) {
            transaction.setStatus(TransactionStatus.FAILED);
            transaction.setFailedReason("Lỗi hệ thống");
            log.error("Unexpected error:", ex);
        }

    }
}
