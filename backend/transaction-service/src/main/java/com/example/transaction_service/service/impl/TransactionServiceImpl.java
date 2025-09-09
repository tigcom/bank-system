package com.example.transaction_service.service.impl;


import com.example.common_service.dto.*;
import com.example.common_service.dto.request.CreateAccountSavingRequest;
import com.example.common_service.dto.request.PayInterestRequest;
import com.example.common_service.dto.request.TransactionRequest;
import com.example.common_service.dto.request.WithdrawAccountSavingRequest;
import com.example.common_service.dto.response.AccountPaymentResponse;
import com.example.common_service.dto.response.CustomerResponse;
import com.example.common_service.services.account.AccountQueryService;
import com.example.common_service.services.customer.CustomerQueryService;

import com.example.transaction_service.client.ProviderClient;
import com.example.transaction_service.dto.TransactionDTO;
import com.example.transaction_service.dto.request.*;
import com.example.transaction_service.dto.response.*;
import com.example.transaction_service.entity.Transaction;
import com.example.transaction_service.enums.*;
import com.example.transaction_service.exception.AppException;
import com.example.transaction_service.exception.ErrorCode;
import com.example.transaction_service.filter.TransactionSpecification;
import com.example.transaction_service.gateways.ProviderGateway;
import com.example.transaction_service.mapper.TransactionMapper;
import com.example.transaction_service.repository.TransactionRepository;
import com.example.transaction_service.service.TransactionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.rpc.RpcContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
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

    @DubboReference(timeout = 5000)
    private final AccountQueryService accountQueryService;

    @DubboReference(timeout = 5000)
    private final CustomerQueryService customerQueryService;

    private final Map<String, ProviderGateway> providerGateways; // Spring sẽ tự inject tất cả các Bean ProviderGateway vào Map này với key là tên bean
    private final RedisTemplate<String,String> redisTemplate;

    private final StreamBridge streamBridge;

    private final ProviderClient providerClient;
    @Value("${core-banking.api.url}")
    private String URL_CORE_BANK;

    @Value("${mock-napas-api}")
    private String URL_NAPAS;

    @Value("${masterAccount}")
    private String masterAccount;
    private static final Logger log = LoggerFactory.getLogger(TransactionServiceImpl.class);

    @Autowired
    @Qualifier("coreBankRestTemplate")
    private RestTemplate coreBankRestTemplate;

    @Autowired
    @Qualifier("mockServerRestTemplate")
    private RestTemplate mockServerRestTemplate;
    @Override
    @Transactional
    @CacheEvict(value = "latestRecipients", key = "#transferRequest.fromAccountNumber")
    public TransactionDTO transfer(TransferRequest transferRequest) {
        CustomerResponse currentCustomer = customerQueryService.getCurrentCustomer();
        log.info("[customerId:{}][cifCode:{}][TRANSFER] From: {} | To: {} | Amount: {} | Currency: {} | Desc: {}", currentCustomer.getUserId(), currentCustomer.getCifCode(), transferRequest.getFromAccountNumber(), transferRequest.getToAccountNumber(), transferRequest.getAmount(), transferRequest.getCurrency(), transferRequest.getDescription());
        Transaction transaction = new Transaction();
        transaction.setFromAccountNumber(transferRequest.getFromAccountNumber());
        transaction.setToAccountNumber(transferRequest.getToAccountNumber());
        transaction.setAmount(transferRequest.getAmount());
        transaction.setDescription(transferRequest.getDescription());
        transaction.setCurrency(CurrencyType.valueOf(transferRequest.getCurrency()));
        transaction.setType(TransactionType.TRANSFER);
        try {
            log.info("[TRANSFER] Validate transaction...");
            validateTransaction(transaction);
            log.info("[TRANSFER] Validate thành công");
            // Khởi tạo transaction
            initTransaction(transaction);
            log.info("[customerId:{}][cifCode:{}][TRANSFER] Init transaction thành công | ReferenceCode: {}", currentCustomer.getUserId(), currentCustomer.getCifCode(), transaction.getReferenceCode());
            // Gửi OTP
            sendOTP(transaction.getReferenceCode(), transaction.getFromAccountNumber());
            log.info("[customerId:{}][cifCode:{}][TRANSFER] Đã gửi OTP cho account: {}", currentCustomer.getUserId(), currentCustomer.getCifCode(), transaction.getFromAccountNumber());
            transactionRepository.save(transaction);
            log.info("[customerId:{}][cifCode:{}][TRANSFER] Lưu transaction thành công | ReferenceCode: {}", currentCustomer.getUserId(), currentCustomer.getCifCode(), transaction.getReferenceCode());
            log.info("[customerId:{}][cifCode:{}][TRANSFER] Giao dịch chuyển tiền khởi tạo thành công | ReferenceCode: {}", currentCustomer.getUserId(), currentCustomer.getCifCode(), transaction.getReferenceCode());
            return transactionMapper.toDTO(transaction);
        } catch (Exception ex) {
            log.error("[customerId:{}][cifCode:{}][TRANSFER] Lỗi khi thực hiện giao dịch chuyển tiền | ReferenceCode: {} | From: {} | To: {} | Amount: {} | Lý do: {}",
                    currentCustomer.getUserId(), currentCustomer.getCifCode(), transaction.getReferenceCode(), transferRequest.getFromAccountNumber(), transferRequest.getToAccountNumber(), transferRequest.getAmount(), ex.getMessage(), ex);
            throw ex;
        }
    }

    @Override
    @Transactional
    public TransactionDTO deposit(DepositRequest depositRequest) {
        CustomerResponse currentCustomer = customerQueryService.getCurrentCustomer();
        log.info("[customerId:{}][cifCode:{}][DEPOSIT] To: {} | Amount: {} | Currency: {} | Desc: {}", currentCustomer.getUserId(), currentCustomer.getCifCode(), depositRequest.getToAccountNumber(), depositRequest.getAmount(), depositRequest.getCurrency(), depositRequest.getDescription());
        Transaction transaction = new Transaction();
        transaction.setToAccountNumber(depositRequest.getToAccountNumber());
        transaction.setAmount(depositRequest.getAmount());
        transaction.setDescription(depositRequest.getDescription());
        transaction.setCurrency(CurrencyType.valueOf(depositRequest.getCurrency()));
        transaction.setType(TransactionType.DEPOSIT);
        transaction.setFromAccountNumber(masterAccount);
        try {
            log.info("[DEPOSIT] Validate transaction...");
            validateTransaction(transaction);
            log.info("[DEPOSIT] Validate thành công");
            initTransaction(transaction);
            log.info("[customerId:{}][cifCode:{}][DEPOSIT] Init transaction thành công | ReferenceCode: {}", currentCustomer.getUserId(), currentCustomer.getCifCode(), transaction.getReferenceCode());
            sendOTP(transaction.getReferenceCode(), transaction.getToAccountNumber());
            log.info("[customerId:{}][cifCode:{}][DEPOSIT] Đã gửi OTP cho account: {}", currentCustomer.getUserId(), currentCustomer.getCifCode(), transaction.getToAccountNumber());
            transactionRepository.save(transaction);
            log.info("[customerId:{}][cifCode:{}][DEPOSIT] Lưu transaction thành công | ReferenceCode: {}", currentCustomer.getUserId(), currentCustomer.getCifCode(), transaction.getReferenceCode());
            log.info("[customerId:{}][cifCode:{}][DEPOSIT] Giao dịch nạp tiền khởi tạo thành công | ReferenceCode: {}", currentCustomer.getUserId(), currentCustomer.getCifCode(), transaction.getReferenceCode());
            return transactionMapper.toDTO(transaction);
        } catch (Exception ex) {
            log.error("[customerId:{}][cifCode:{}][DEPOSIT] Lỗi khi thực hiện giao dịch nạp tiền | ReferenceCode: {} | To: {} | Amount: {} | Lý do: {}",
                    currentCustomer.getUserId(), currentCustomer.getCifCode(), transaction.getReferenceCode(), depositRequest.getToAccountNumber(), depositRequest.getAmount(), ex.getMessage(), ex);
            throw ex;
        }
    }

    @Override
    @Transactional
    public TransactionDTO withdraw(WithdrawRequest withdrawRequest) {
        CustomerResponse currentCustomer = customerQueryService.getCurrentCustomer();
        log.info("[customerId:{}][cifCode:{}][WITHDRAW] From: {} | Amount: {} | Currency: {} | Desc: {}", currentCustomer.getUserId(), currentCustomer.getCifCode(), withdrawRequest.getFromAccountNumber(), withdrawRequest.getAmount(), withdrawRequest.getCurrency(), withdrawRequest.getDescription());
        Transaction transaction = new Transaction();
        transaction.setFromAccountNumber(withdrawRequest.getFromAccountNumber());
        transaction.setAmount(withdrawRequest.getAmount());
        transaction.setDescription(withdrawRequest.getDescription());
        transaction.setCurrency(CurrencyType.valueOf(withdrawRequest.getCurrency()));
        transaction.setType(TransactionType.WITHDRAW);
        transaction.setToAccountNumber(masterAccount);
        try {
            log.info("[WITHDRAW] Validate transaction...");
            validateTransaction(transaction);
            log.info("[WITHDRAW] Validate thành công");
            initTransaction(transaction);
            log.info("[customerId:{}][cifCode:{}][WITHDRAW] Init transaction thành công | ReferenceCode: {}", currentCustomer.getUserId(), currentCustomer.getCifCode(), transaction.getReferenceCode());
            sendOTP(transaction.getReferenceCode(), transaction.getFromAccountNumber());
            log.info("[customerId:{}][cifCode:{}][WITHDRAW] Đã gửi OTP cho account: {}", currentCustomer.getUserId(), currentCustomer.getCifCode(), transaction.getFromAccountNumber());
            transactionRepository.save(transaction);
            log.info("[customerId:{}][cifCode:{}][WITHDRAW] Lưu transaction thành công | ReferenceCode: {}", currentCustomer.getUserId(), currentCustomer.getCifCode(), transaction.getReferenceCode());
            log.info("[customerId:{}][cifCode:{}][WITHDRAW] Giao dịch rút tiền khởi tạo thành công | ReferenceCode: {}", currentCustomer.getUserId(), currentCustomer.getCifCode(), transaction.getReferenceCode());
            return transactionMapper.toDTO(transaction);
        } catch (Exception ex) {
            log.error("[customerId:{}][cifCode:{}][WITHDRAW] Lỗi khi thực hiện giao dịch rút tiền | ReferenceCode: {} | From: {} | Amount: {} | Lý do: {}",
                    currentCustomer.getUserId(), currentCustomer.getCifCode(), transaction.getReferenceCode(), withdrawRequest.getFromAccountNumber(), withdrawRequest.getAmount(), ex.getMessage(), ex);
            throw ex;
        }
    }

    @Override
    @Transactional
    public TransactionDTO payBillLoan(LoanPaymentRequest repaymentRequest) {
        CustomerResponse currentCustomer = customerQueryService.getCurrentCustomer();
        log.info("[customerId:{}][cifCode:{}][LOAN_PAYMENT] From: {} | Amount: {} | Currency: {} | Desc: {}", currentCustomer.getUserId(), currentCustomer.getCifCode(), repaymentRequest.getFromAccountNumber(), repaymentRequest.getAmount(), repaymentRequest.getCurrency(), repaymentRequest.getDescription());
        Transaction transaction = new Transaction();
        transaction.setFromAccountNumber(repaymentRequest.getFromAccountNumber());
        transaction.setAmount(repaymentRequest.getAmount());
        transaction.setDescription(repaymentRequest.getDescription());
        transaction.setCurrency(CurrencyType.valueOf(repaymentRequest.getCurrency()));
        transaction.setType(TransactionType.LOAN_PAYMENT);
        if (repaymentRequest.getToAccountNumber() != null){
            transaction.setToAccountNumber(repaymentRequest.getToAccountNumber());
        }else{
            transaction.setToAccountNumber(masterAccount);
        }

        try {
            log.info("[LOAN_PAYMENT] Validate transaction...");
            validateTransaction(transaction);
            log.info("[LOAN_PAYMENT] Validate thành công");
            initTransaction(transaction);
            log.info("[customerId:{}][cifCode:{}][LOAN_PAYMENT] Init transaction thành công | ReferenceCode: {}", currentCustomer.getUserId(), currentCustomer.getCifCode(), transaction.getReferenceCode());
            transactionRepository.save(transaction);
            sendOTP(transaction.getReferenceCode(), transaction.getFromAccountNumber());
            log.info("[LOAN_PAYMENT] Đã gửi OTP cho account: {}", transaction.getFromAccountNumber());
            transactionRepository.save(transaction);
            log.info("[LOAN_PAYMENT] Lưu transaction thành công | ReferenceCode: {}", transaction.getReferenceCode());
            log.info("[LOAN_PAYMENT] Giao dịch thanh toán khoản vay khởi tạo thành công | ReferenceCode: {}", transaction.getReferenceCode());
            return transactionMapper.toDTO(transaction);
        } catch (Exception ex) {
            log.error("[customerId:{}][cifCode:{}][LOAN_PAYMENT] Lỗi khi thực hiện giao dịch thanh toán khoản vay | ReferenceCode: {} | From: {} | Amount: {} | Lý do: {}",
                    currentCustomer.getUserId(), currentCustomer.getCifCode(), transaction.getReferenceCode(), repaymentRequest.getFromAccountNumber(), repaymentRequest.getAmount(), ex.getMessage(), ex);
            throw ex;
        }
    }

    @Override
    public BillDetailsResponse checkBill(BillCheckRequest request) {
        ProviderGateway gateway = providerGateways.get(request.getBillType());
        if (gateway == null) {
            throw new AppException(ErrorCode.UNSUPPORTED_OPERATION);
        }
        BillDetailsResponse response = gateway.checkBill(request.getCustomerCode(),request.getProvider());

        return response;
    }

    @Override
    public TransactionDTO payBill(BillPaymentRequest request) {
        CustomerResponse currentCustomer = customerQueryService.getCurrentCustomer();
        log.info("[customerId:{}][cifCode:{}][PAY_BILL] From: {} | BillType: {} | CustomerCode: {} | Provider: {} | Desc: {}", currentCustomer.getUserId(), currentCustomer.getCifCode(), request.getFromAccountNumber(), request.getBillType(), request.getCustomerCode(), request.getProvider(), request.getDescription());
        BillDetailsResponse response = checkBill(BillCheckRequest.builder()
                .billType(request.getBillType())
                .customerCode(request.getCustomerCode())
                .provider(request.getProvider())
                .build());
        if(response==null) throw new AppException(ErrorCode.BILL_NOT_FOUND);
        Transaction transaction = new Transaction();
        transaction.setFromAccountNumber(request.getFromAccountNumber());
        transaction.setAmount(response.getAmount());
        transaction.setDescription(request.getDescription());
        transaction.setCurrency(CurrencyType.valueOf(request.getCurrency()));
        transaction.setBillType(request.getBillType());
        transaction.setBillCustomerCode(request.getCustomerCode());
        transaction.setBillProviderCode(request.getProvider());
        transaction.setBillId(response.getBillId());
        transaction.setType(TransactionType.PAY_BILL);
        ProviderGateway gateway = providerGateways.get(transaction.getBillType());
        transaction.setToAccountNumber(gateway.getAccountNumberProvider());
        try {

            log.info("[PAY_BILL] Validate transaction...");
            validateTransaction(transaction);
            log.info("[PAY_BILL] Validate thành công");
            initTransaction(transaction);
            log.info("[customerId:{}][cifCode:{}][PAY_BILL] Init transaction thành công | ReferenceCode: {}", currentCustomer.getUserId(), currentCustomer.getCifCode(), transaction.getReferenceCode());
            transactionRepository.save(transaction);
            sendOTP(transaction.getReferenceCode(), transaction.getFromAccountNumber());
            log.info("[customerId:{}][cifCode:{}][PAY_BILL] Đã gửi OTP cho account: {}", currentCustomer.getUserId(), currentCustomer.getCifCode(), transaction.getFromAccountNumber());
            transactionRepository.save(transaction);
            log.info("[customerId:{}][cifCode:{}][PAY_BILL] Lưu transaction thành công | ReferenceCode: {}", currentCustomer.getUserId(), currentCustomer.getCifCode(), transaction.getReferenceCode());
            log.info("[customerId:{}][cifCode:{}][PAY_BILL] Giao dịch thanh toán hóa đơn khởi tạo thành công | ReferenceCode: {}", currentCustomer.getUserId(), currentCustomer.getCifCode(), transaction.getReferenceCode());
            return transactionMapper.toDTO(transaction);
        } catch (Exception ex) {
            log.error("[customerId:{}][cifCode:{}][PAY_BILL] Lỗi khi thực hiện giao dịch thanh toán hóa đơn  | ReferenceCode: {} | From: {} | BillType: {} | Lý do: {}",
                    currentCustomer.getUserId(), currentCustomer.getCifCode(), transaction.getReferenceCode(), request.getFromAccountNumber(), request.getBillType(), ex.getMessage(), ex);
            throw ex;
        }
    }

    @Override
    public Map<String, List<ProviderDTO>> getGroupedProviders() {
        Map<String, List<ProviderDTO>> providers = providerClient.getProviders();
        if (providers.isEmpty()) {
            log.warn("Service: Không nhận được dữ liệu nhà cung cấp từ ProviderClient.");
        }
        return providers;
    }

    @Override
    @Transactional
    public TransactionDTO disburse(DisburseRequest disburseRequest,String username) {
        CustomerResponseDTO currentCustomer = customerQueryService.getCustomerByUserId(username);
        RpcContext.getContext().setAttachment("username", username);

        log.info("[customerId:{}][cifCode:{}][DISBURSE] To: {} | Amount: {} | Currency: {} | Desc: {}", currentCustomer.getUserId(), currentCustomer.getCifCode(), disburseRequest.getToAccountNumber(), disburseRequest.getAmount(), disburseRequest.getCurrency(), disburseRequest.getDescription());
        Transaction transaction = new Transaction();
        transaction.setToAccountNumber(disburseRequest.getToAccountNumber());
        transaction.setAmount(disburseRequest.getAmount());
        transaction.setDescription(disburseRequest.getDescription());
        transaction.setCurrency(CurrencyType.valueOf(disburseRequest.getCurrency()));
        transaction.setType(TransactionType.DISBURSEMENT);
        transaction.setFromAccountNumber(masterAccount);
        try {
            log.info("[DISBURSE] Validate transaction...");
            validateTransaction(transaction);
            log.info("[DISBURSE] Validate thành công");
            initTransaction(transaction);
            log.info("[customerId:{}][cifCode:{}][DISBURSE] Init transaction thành công | ReferenceCode: {}", currentCustomer.getUserId(), currentCustomer.getCifCode(), transaction.getReferenceCode());
            processTransaction(transaction);
            log.info("[customerId:{}][cifCode:{}][DISBURSE] Process transaction thành công | ReferenceCode: {}", currentCustomer.getUserId(), currentCustomer.getCifCode(), transaction.getReferenceCode());
            transactionRepository.save(transaction);
            log.info("[customerId:{}][cifCode:{}][DISBURSE] Lưu transaction thành công | ReferenceCode: {}", currentCustomer.getUserId(), currentCustomer.getCifCode(), transaction.getReferenceCode());
            log.info("[customerId:{}][cifCode:{}][DISBURSE] Giao dịch giải ngân thành công | ReferenceCode: {}", currentCustomer.getUserId(), currentCustomer.getCifCode(), transaction.getReferenceCode());
            return transactionMapper.toDTO(transaction);
        } catch (Exception ex) {
            log.error("[customerId:{}][cifCode:{}][DISBURSE] Lỗi khi thực hiện giao dịch giải ngân | ReferenceCode: {} | To: {} | Amount: {} | Lý do: {}",
                    currentCustomer.getUserId(), currentCustomer.getCifCode(), transaction.getReferenceCode(), disburseRequest.getToAccountNumber(), disburseRequest.getAmount(), ex.getMessage(), ex);
            throw ex;
        }
    }
    @Override
    @Transactional
    public TransactionDTO disburse(DisburseRequest disburseRequest) {
        CustomerResponse currentCustomer = customerQueryService.getCurrentCustomer();
        log.info("[customerId:{}][cifCode:{}][DISBURSE] To: {} | Amount: {} | Currency: {} | Desc: {}", currentCustomer.getUserId(), currentCustomer.getCifCode(), disburseRequest.getToAccountNumber(), disburseRequest.getAmount(), disburseRequest.getCurrency(), disburseRequest.getDescription());
        Transaction transaction = new Transaction();
        transaction.setToAccountNumber(disburseRequest.getToAccountNumber());
        transaction.setAmount(disburseRequest.getAmount());
        transaction.setDescription(disburseRequest.getDescription());
        transaction.setCurrency(CurrencyType.valueOf(disburseRequest.getCurrency()));
        transaction.setType(TransactionType.DISBURSEMENT);
        transaction.setFromAccountNumber(masterAccount);
        try {
            log.info("[DISBURSE] Validate transaction...");
            validateTransaction(transaction);
            log.info("[DISBURSE] Validate thành công");
            initTransaction(transaction);
            log.info("[customerId:{}][cifCode:{}][DISBURSE] Init transaction thành công | ReferenceCode: {}", currentCustomer.getUserId(), currentCustomer.getCifCode(), transaction.getReferenceCode());
            processTransaction(transaction);
            log.info("[customerId:{}][cifCode:{}][DISBURSE] Process transaction thành công | ReferenceCode: {}", currentCustomer.getUserId(), currentCustomer.getCifCode(), transaction.getReferenceCode());
            transactionRepository.save(transaction);
            log.info("[customerId:{}][cifCode:{}][DISBURSE] Lưu transaction thành công | ReferenceCode: {}", currentCustomer.getUserId(), currentCustomer.getCifCode(), transaction.getReferenceCode());
            log.info("[customerId:{}][cifCode:{}][DISBURSE] Giao dịch giải ngân thành công | ReferenceCode: {}", currentCustomer.getUserId(), currentCustomer.getCifCode(), transaction.getReferenceCode());
            return transactionMapper.toDTO(transaction);
        } catch (Exception ex) {
            log.error("[customerId:{}][cifCode:{}][DISBURSE] Lỗi khi thực hiện giao dịch giải ngân | ReferenceCode: {} | To: {} | Amount: {} | Lý do: {}",
                    currentCustomer.getUserId(), currentCustomer.getCifCode(), transaction.getReferenceCode(), disburseRequest.getToAccountNumber(), disburseRequest.getAmount(), ex.getMessage(), ex);
            throw ex;
        }
    }

    @Override
    public TransactionDTO createAccountSaving(CreateAccountSavingRequest accountSavingRequest) {
        CustomerResponse currentCustomer = customerQueryService.getCurrentCustomer();
        log.info("[customerId:{}][cifCode:{}][CREATE_SAVING] From: {} | Amount: {} | Currency: {} | Desc: {}", currentCustomer.getUserId(), currentCustomer.getCifCode(), accountSavingRequest.getFromAccountNumber(), accountSavingRequest.getAmount(), accountSavingRequest.getCurrency(), accountSavingRequest.getDescription());
        Transaction transaction = new Transaction();
        transaction.setFromAccountNumber(accountSavingRequest.getFromAccountNumber());
        transaction.setAmount(accountSavingRequest.getAmount());
        transaction.setDescription(accountSavingRequest.getDescription());
        transaction.setCurrency(CurrencyType.valueOf(accountSavingRequest.getCurrency()));
        transaction.setType(TransactionType.CREATE_ACCOUNT_SAVING);
        transaction.setToAccountNumber(masterAccount);
        try {
            log.info("[CREATE_SAVING] Validate transaction...");
            validateTransaction(transaction);
            log.info("[CREATE_SAVING] Validate thành công");
            initTransaction(transaction);
            log.info("[customerId:{}][cifCode:{}][CREATE_SAVING] Init transaction thành công | ReferenceCode: {}", currentCustomer.getUserId(), currentCustomer.getCifCode(), transaction.getReferenceCode());
            processTransaction(transaction);
            log.info("[customerId:{}][cifCode:{}][CREATE_SAVING] Process transaction thành công | ReferenceCode: {}", currentCustomer.getUserId(), currentCustomer.getCifCode(), transaction.getReferenceCode());
            transactionRepository.save(transaction);
            log.info("[customerId:{}][cifCode:{}][CREATE_SAVING] Lưu transaction thành công | ReferenceCode: {}", currentCustomer.getUserId(), currentCustomer.getCifCode(), transaction.getReferenceCode());
            log.info("[customerId:{}][cifCode:{}][CREATE_SAVING] Giao dịch mở sổ tiết kiệm thành công | ReferenceCode: {}", currentCustomer.getUserId(), currentCustomer.getCifCode(), transaction.getReferenceCode());
            return transactionMapper.toDTO(transaction);
        } catch (Exception ex) {
            log.error("[customerId:{}][cifCode:{}][CREATE_SAVING] Lỗi khi thực hiện giao dịch mở sổ tiết kiệm | ReferenceCode: {} | From: {} | Amount: {} | Lý do: {}",
                    currentCustomer.getUserId(), currentCustomer.getCifCode(), transaction.getReferenceCode(), accountSavingRequest.getFromAccountNumber(), accountSavingRequest.getAmount(), ex.getMessage(), ex);
            throw ex;
        }
    }

    @Override
    @Transactional
    public TransactionDTO confirmTransaction(ConfirmTransactionRequest request){
        CustomerResponse currentCustomer = customerQueryService.getCurrentCustomer();
        log.info("[customerId:{}][cifCode:{}][CONFIRM_TXN] ReferenceCode: {} | OTP: {}", currentCustomer.getUserId(), currentCustomer.getCifCode(), request.getReferenceCode(), request.getOtpCode());
        String keyOTP = "OTP:" + request.getReferenceCode();
        String storedOtp = redisTemplate.opsForValue().get(keyOTP);
        String keyFailCount = "OTP_FAIL_COUNT:" + request.getReferenceCode();
        try {
            if (storedOtp == null) {
                log.warn("[customerId:{}][cifCode:{}][CONFIRM_TXN] OTP hết hạn | ReferenceCode: {}", currentCustomer.getUserId(), currentCustomer.getCifCode(), request.getReferenceCode());
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
                    log.error("[customerId:{}][cifCode:{}][CONFIRM_TXN] Giao dịch thất bại do nhập sai OTP quá 3 lần | ReferenceCode: {}", currentCustomer.getUserId(), currentCustomer.getCifCode(), request.getReferenceCode());
                    return transactionMapper.toDTO(txn);
                }
                log.warn("[customerId:{}][cifCode:{}][CONFIRM_TXN] Sai OTP | ReferenceCode: {} | Lần sai: {}", currentCustomer.getUserId(), currentCustomer.getCifCode(), request.getReferenceCode(), failCount);
                throw new AppException(ErrorCode.INVALID_OTP);
            }
            Transaction txn = transactionRepository.findByReferenceCode(request.getReferenceCode());
            if(txn == null) throw new AppException(ErrorCode.TRANSACTION_NOT_EXIST);
            if (txn.getStatus() != TransactionStatus.PENDING) {
                log.warn("[customerId:{}][cifCode:{}][CONFIRM_TXN] Trạng thái giao dịch không hợp lệ | ReferenceCode: {} | Status: {}", currentCustomer.getUserId(), currentCustomer.getCifCode(), request.getReferenceCode(), txn.getStatus());
                throw new AppException(ErrorCode.INVALID_TRANSACTION_STATUS);
            }
            processTransaction(txn);
            transactionRepository.save(txn);
            log.info("[customerId:{}][cifCode:{}][CONFIRM_TXN] Giao dịch xác nhận thành công | ReferenceCode: {}", currentCustomer.getUserId(), currentCustomer.getCifCode(), request.getReferenceCode());
            AccountDTO fromAccount = accountQueryService.getAccountByAccountNumber(txn.getFromAccountNumber());
            AccountDTO toAccount = accountQueryService.getAccountByAccountNumber(txn.getToAccountNumber());

            CustomerDTO fromCustomer = customerQueryService.getCustomerByCifCode(fromAccount.getCifCode());

            if (EnumSet.of(TransactionType.TRANSFER, TransactionType.WITHDRAW,
                    TransactionType.PAY_BILL).contains(txn.getType())) {
                CustomerDTO toCustomer = customerQueryService.getCustomerByCifCode(toAccount.getCifCode());
                MailTransactionDTO mailTransactionDTO = MailTransactionDTO.builder()
                        .name(fromCustomer.getFullName())
                        .recipientMail(fromCustomer.getEmail())
                        .amount(txn.getAmount())
                        .referenceCode(txn.getReferenceCode())
                        .toAccountNumber(txn.getToAccountNumber())
                        .toCustomerName(toCustomer.getFullName())
                        .timestamp(txn.getTimestamp())
                        .description(txn.getDescription())
                        .subject("Thông báo giao dịch")
                        .build();

                streamBridge.send("mail-transaction-out-0", mailTransactionDTO);
            }else if (EnumSet.of(TransactionType.DEPOSIT).contains(txn.getType())) {
                CustomerDTO toCustomer = customerQueryService.getCustomerByCifCode(toAccount.getCifCode());
                MailTransactionDTO mailTransactionDTO = MailTransactionDTO.builder()
                        .name(toCustomer.getFullName())
                        .recipientMail(toCustomer.getEmail())
                        .amount(txn.getAmount())
                        .referenceCode(txn.getReferenceCode())
                        .timestamp(txn.getTimestamp())
                        .description(txn.getDescription())
                        .subject("Thông báo giao dịch")
                        .build();
                streamBridge.send("mail-transaction-out-0", mailTransactionDTO);
            }
            redisTemplate.delete(keyFailCount);
            redisTemplate.delete(keyOTP);
            return transactionMapper.toDTO(txn);
        } catch (Exception ex) {
            log.error("[customerId:{}][cifCode:{}][CONFIRM_TXN] Lỗi xác nhận giao dịch | ReferenceCode: {} | Lý do: {}", currentCustomer.getUserId(), currentCustomer.getCifCode(), request.getReferenceCode(), ex.getMessage(), ex);
            throw ex;
        }
    }

    @Override
    public TransactionDTO withdrawAccountSaving(WithdrawAccountSavingRequest depositAccountSavingRequest) {
        CustomerResponse currentCustomer = customerQueryService.getCurrentCustomer();
        log.info("[customerId:{}][cifCode:{}][WITHDRAW_SAVING] To: {} | Amount: {} | Currency: {} | Desc: {}", currentCustomer.getUserId(), currentCustomer.getCifCode(), depositAccountSavingRequest.getToAccountNumber(), depositAccountSavingRequest.getAmount(), depositAccountSavingRequest.getCurrency(), depositAccountSavingRequest.getDescription());
        Transaction transaction = new Transaction();
        transaction.setToAccountNumber(depositAccountSavingRequest.getToAccountNumber());
        transaction.setAmount(depositAccountSavingRequest.getAmount());
        transaction.setDescription(depositAccountSavingRequest.getDescription());
        transaction.setCurrency(CurrencyType.valueOf(depositAccountSavingRequest.getCurrency()));
        transaction.setType(TransactionType.WITHDRAW_ACCOUNT_SAVING);
        transaction.setFromAccountNumber(masterAccount);
        try {
            log.info("[WITHDRAW_SAVING] Validate transaction...");
            validateTransaction(transaction);
            log.info("[WITHDRAW_SAVING] Validate thành công");
            initTransaction(transaction);
            log.info("[customerId:{}][cifCode:{}][WITHDRAW_SAVING] Init transaction thành công | ReferenceCode: {}", currentCustomer.getUserId(), currentCustomer.getCifCode(), transaction.getReferenceCode());
            processTransaction(transaction);
            log.info("[customerId:{}][cifCode:{}][WITHDRAW_SAVING] Process transaction thành công | ReferenceCode: {}", currentCustomer.getUserId(), currentCustomer.getCifCode(), transaction.getReferenceCode());
            transactionRepository.save(transaction);
            log.info("[customerId:{}][cifCode:{}][WITHDRAW_SAVING] Lưu transaction thành công | ReferenceCode: {}", currentCustomer.getUserId(), currentCustomer.getCifCode(), transaction.getReferenceCode());
            log.info("[customerId:{}][cifCode:{}][WITHDRAW_SAVING] Giao dịch rút tiết kiệm thành công | ReferenceCode: {}", currentCustomer.getUserId(), currentCustomer.getCifCode(), transaction.getReferenceCode());
            return transactionMapper.toDTO(transaction);
        } catch (Exception ex) {
            log.error("[customerId:{}][cifCode:{}][WITHDRAW_SAVING] Lỗi khi thực hiện giao dịch rút tiết kiệm | ReferenceCode: {} | To: {} | Amount: {} | Lý do: {}",
                    currentCustomer.getUserId(), currentCustomer.getCifCode(), transaction.getReferenceCode(), depositAccountSavingRequest.getToAccountNumber(), depositAccountSavingRequest.getAmount(), ex.getMessage(), ex);
            throw ex;
        }
    }

    @Override
    @Transactional
    public TransactionDTO transferToExternalBank(ExternalTransferRequest externalTransferRequest) {
        CustomerResponse currentCustomer = customerQueryService.getCurrentCustomer();
        log.info("[customerId:{}][cifCode:{}][EXTERNAL_TRANSFER] From: {} | To: {} | Amount: {} | BankCode: {} | Desc: {}", currentCustomer.getUserId(), currentCustomer.getCifCode(), externalTransferRequest.getFromAccountNumber(), externalTransferRequest.getToAccountNumber(), externalTransferRequest.getAmount(), externalTransferRequest.getDestinationBankCode(), externalTransferRequest.getDescription());
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
        try {
            log.info("[EXTERNAL_TRANSFER] Validate thông tin giao dịch...");
            AccountDTO fromAccount = accountQueryService.getAccountByAccountNumber(transaction.getFromAccountNumber());
            if(!accountQueryService.existsAccountByAccountNumberAndCifCode(
                    fromAccount.getAccountNumber(),currentCustomer.getCifCode())){
                throw new AppException(ErrorCode.INVALID_ACCOUNT);
            }
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
                String url = URL_CORE_BANK+"/get-balance/{accountNumber}";
                ParameterizedTypeReference<ApiResponse<BigDecimal>> responseType =
                        new ParameterizedTypeReference<ApiResponse<BigDecimal>>() {};
                ResponseEntity<ApiResponse<BigDecimal>> response = coreBankRestTemplate.exchange(
                        url,
                        HttpMethod.GET,
                        null,
                        responseType,
                        transaction.getFromAccountNumber()
                );
                balance = response.getBody().getResult();
                log.info("[EXTERNAL_TRANSFER] Fetched balance: {} for account {}", balance, transaction.getFromAccountNumber());
            }
            catch (Exception e) {
                throw new AppException(ErrorCode.CORE_BANKING_UNAVAILABLE);
            }
            if (balance.compareTo(transaction.getAmount()) < 0) {
                throw new AppException(ErrorCode.INSUFFICIENT_FUNDS);
            }
            NapasInquiryResponse napasInquiryResponse = inquiryDestinationAccount(NapasInquiryRequest.builder()
                    .accountNumber(transaction.getToAccountNumber())
                    .bankCode(transaction.getDestinationBankCode())
                    .build());
            if(!napasInquiryResponse.getAccountStatus().equals("ACTIVE"))
                throw new AppException(ErrorCode.DESTINATION_ACCOUNT_NOT_ACTIVE);
            initTransaction(transaction);
            log.info("[customerId:{}][cifCode:{}][EXTERNAL_TRANSFER] Init transaction thành công | ReferenceCode: {}", currentCustomer.getUserId(), currentCustomer.getCifCode(), transaction.getReferenceCode());
            sendOTP(transaction.getReferenceCode(),transaction.getFromAccountNumber());
            log.info("[customerId:{}][cifCode:{}][EXTERNAL_TRANSFER] Đã gửi OTP cho account: {}", currentCustomer.getUserId(), currentCustomer.getCifCode(), transaction.getFromAccountNumber());
            transactionRepository.save(transaction);
            log.info("[customerId:{}][cifCode:{}][EXTERNAL_TRANSFER] Lưu transaction thành công | ReferenceCode: {}", currentCustomer.getUserId(), currentCustomer.getCifCode(), transaction.getReferenceCode());
            log.info("[customerId:{}][cifCode:{}][EXTERNAL_TRANSFER] Giao dịch chuyển tiền liên ngân hàng khởi tạo thành công | ReferenceCode: {}", currentCustomer.getUserId(), currentCustomer.getCifCode(), transaction.getReferenceCode());
            return transactionMapper.toDTO(transaction);
        } catch (Exception ex) {
            log.error("[customerId:{}][cifCode:{}][EXTERNAL_TRANSFER] Lỗi khi thực hiện giao dịch chuyển tiền liên ngân hàng | ReferenceCode: {} | From: {} | To: {} | Amount: {} | Lý do: {}",
                    currentCustomer.getUserId(), currentCustomer.getCifCode(), transaction.getReferenceCode(), externalTransferRequest.getFromAccountNumber(), externalTransferRequest.getToAccountNumber(), externalTransferRequest.getAmount(), ex.getMessage(), ex);
            throw ex;
        }
    }

    @Override
    public void resendOtp(ResendOtpRequest resendOtpRequest) {
        CustomerResponse currentCustomer = customerQueryService.getCurrentCustomer();
        log.info("[customerId:{}][cifCode:{}][RESEND_OTP] ReferenceCode: {} | Account: {}", currentCustomer.getUserId(), currentCustomer.getCifCode(), resendOtpRequest.getReferenceCode(), resendOtpRequest.getAccountNumberRecipient());
        try {
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
            log.info("[customerId:{}][cifCode:{}][RESEND_OTP] Đã gửi lại OTP cho account: {}", currentCustomer.getUserId(), currentCustomer.getCifCode(), resendOtpRequest.getAccountNumberRecipient());

        } catch (Exception ex) {
            log.error("[customerId:{}][cifCode:{}][RESEND_OTP] Lỗi khi gửi lại OTP | ReferenceCode: {} | Lý do: {}", currentCustomer.getUserId(), currentCustomer.getCifCode(), resendOtpRequest.getReferenceCode(), ex.getMessage(), ex);
            throw ex;
        }
    }

    @Override
    public TransactionDTO getTransactionById(String transactionId) {
        log.info("[GET_TXN_BY_ID] Lấy thông tin giao dịch theo ID | TransactionId: {}", transactionId);
        try {
            Transaction transaction = transactionRepository.findById(transactionId)
                    .orElseThrow(() -> new AppException(ErrorCode.TRANSACTION_NOT_EXIST));
            return transactionMapper.toDTO(transaction);
        } catch (Exception ex) {
            log.error("[GET_TXN_BY_ID] Lỗi khi lấy thông tin giao dịch | TransactionId: {} | Lý do: {}", transactionId, ex.getMessage(), ex);
            throw ex;
        }
    }

    @Override
    public List<TransactionDTO> getAccountTransactions(String accountNumber) {
        log.info("[GET_TXN_BY_ACC] Lấy danh sách giao dịch theo account | Account: {}", accountNumber);
        try {
            List<Transaction> transactionList = transactionRepository.getAccountTransactions(accountNumber);
            return transactionList.stream()
                    .map(transaction -> transactionMapper.toDTO(transaction)).collect(Collectors.toList());
        } catch (Exception ex) {
            log.error("[GET_TXN_BY_ACC] Lỗi khi lấy danh sách giao dịch | Account: {} | Lý do: {}", accountNumber, ex.getMessage(), ex);
            throw ex;
        }
    }

    @Override
    @Cacheable(value = "TransactionDetail", key = "#referenceCode")
    public TransactionDTO getTransactionByTransactionCode(String referenceCode) {
        log.info("[GET_TXN_BY_CODE] Lấy thông tin giao dịch theo mã tham chiếu | ReferenceCode: {}", referenceCode);
        try {
            Transaction transaction = transactionRepository.findByReferenceCode(referenceCode);
            if(transaction==null) throw new AppException(ErrorCode.TRANSACTION_NOT_EXIST);
            return transactionMapper.toDTO(transaction);
        } catch (Exception ex) {
            log.error("[GET_TXN_BY_CODE] Lỗi khi lấy thông tin giao dịch | ReferenceCode: {} | Lý do: {}", referenceCode, ex.getMessage(), ex);
            throw ex;
        }
    }

    @Override
    @Cacheable(value = "latestRecipients", key = "#fromAccountNumber")
    public List<InforTransactionLatestResponse> getListToAccountNumberLatest(String fromAccountNumber) {
        log.info("[GET_LATEST_TO_ACC] Lấy danh sách account nhận gần nhất | FromAccount: {}", fromAccountNumber);
        try {
            AccountDTO fromAccount = accountQueryService.getAccountByAccountNumber(fromAccountNumber);
            if (fromAccount==null) {
                throw new AppException(ErrorCode.FROM_ACCOUNT_NOT_EXIST);
            }
            List<String> accountNumberList = transactionRepository.getListToAccountNumberLatest(fromAccountNumber);
            List<InforTransactionLatestResponse> listRs = accountNumberList.stream()
                    .map(accountNumber -> InforTransactionLatestResponse.builder()
                            .accountNumber(accountNumber)
                            .customerName(accountQueryService.getCustomerByAccountNumber(accountNumber).getFullName())
                            .build())
                    .collect(Collectors.toList());
            return listRs;
        } catch (Exception ex) {
            log.error("[GET_LATEST_TO_ACC] Lỗi khi lấy danh sách account nhận gần nhất | FromAccount: {} | Lý do: {}", fromAccountNumber, ex.getMessage(), ex);
            throw ex;
        }
    }

    @Override
    public Page<Transaction> filterTransaction(TransactionFilterRequest request) {
        log.info("[FILTER_TXN] Lọc giao dịch | Request: {}", request);
        try {
            Sort sort = Sort.by("timestamp").descending();
            if(request.getSortBy()!=null && !request.getSortBy().isEmpty()){
                Sort.Direction direction = "asc".equalsIgnoreCase(request.getSortDirection()) ? Sort.Direction.ASC
                        : Sort.Direction.DESC;
                sort = Sort.by(direction,request.getSortBy());
            }
            Pageable pageable = PageRequest.of(request.getPage()-1 , request.getSize(),sort);
            return transactionRepository.findAll(TransactionSpecification.filter(request),pageable);
        } catch (Exception ex) {
            log.error("[FILTER_TXN] Lỗi khi lọc giao dịch | Lý do: {}", ex.getMessage(), ex);
            throw ex;
        }
    }

    @Override
    @Cacheable("filterMetadata")
    public FilterMetadataResponse getFilterMetadata() {
        log.info("[FILTER_METADATA] Lấy metadata filter giao dịch");
        try {
            List<FilterOptionDTO> types = Arrays.stream(TransactionType.values())
                    .map(type->  FilterOptionDTO.builder()
                            .label(type.getDisplayName())
                            .value(type.name())
                            .build())
                    .collect(Collectors.toList());
            List<FilterOptionDTO> statuses = Arrays.stream(TransactionStatus.values())
                    .map(s->  FilterOptionDTO.builder()
                            .label(s.getDisplayName())
                            .value(s.name())
                            .build())
                    .collect(Collectors.toList());
            List<FilterOptionDTO> currencies = Arrays.stream(CurrencyType.values())
                    .map(currencyType->  FilterOptionDTO.builder()
                            .label(currencyType.getDisplayName())
                            .value(currencyType.name())
                            .build())
                    .collect(Collectors.toList());
            return FilterMetadataResponse.builder()
                    .transactionTypes(types)
                    .statuses(statuses)
                    .currencies(currencies)
                    .build();
        } catch (Exception ex) {
            log.error("[FILTER_METADATA] Lỗi khi lấy metadata filter giao dịch | Lý do: {}", ex.getMessage(), ex);
            throw ex;
        }
    }

    @Override
    public NapasInquiryResponse checkDestinationAccount(NapasInquiryRequest request) {
        CustomerResponse currentCustomer = customerQueryService.getCurrentCustomer();
        log.info("[CHECK_DEST_ACC] Kiểm tra tài khoản đích NAPAS | CustomerId: {} | CifCode: {} | Account: {} | BankCode: {}", currentCustomer.getUserId(), currentCustomer.getCifCode(), request.getAccountNumber(), request.getBankCode());
        try {
            return inquiryDestinationAccount(request);
        } catch (Exception ex) {
            log.error("[customerId:{}][cifCode:{}][CHECK_DEST_ACC] Lỗi khi kiểm tra tài khoản đích NAPAS | Account: {} | Lý do: {}", currentCustomer.getUserId(), currentCustomer.getCifCode(), request.getAccountNumber(), ex.getMessage(), ex);
            throw ex;
        }
    }

    @Override
    public Page<TransactionDTO> getAccountTransactions(String accountNumber, Pageable pageable) {
        log.info("[GET_TXN_BY_ACC_PAGE] Lấy danh sách giao dịch theo account (phân trang) | Account: {}", accountNumber);
        try {
            return transactionRepository.findByAccountNumber(accountNumber, pageable)
                    .map(transactionMapper::toDTO);
        } catch (Exception ex) {
            log.error("[GET_TXN_BY_ACC_PAGE] Lỗi khi lấy danh sách giao dịch (phân trang) | Account: {} | Lý do: {}", accountNumber, ex.getMessage(), ex);
            throw ex;
        }
    }

    @Override
    public TransactionDTO payInterest(PayInterestRequest request) {
        CustomerResponse currentCustomer = customerQueryService.getCurrentCustomer();
        log.info("[customerId:{}][cifCode:{}][PAY_INTEREST] To: {} | Amount: {} | Currency: {} | Desc: {}", currentCustomer.getUserId(), currentCustomer.getCifCode(), request.getToAccountNumber(), request.getAmount(), request.getCurrency(), request.getDescription());
        Transaction transaction = new Transaction();
        transaction.setToAccountNumber(request.getToAccountNumber());
        transaction.setAmount(request.getAmount());
        transaction.setDescription(request.getDescription());
        transaction.setCurrency(CurrencyType.valueOf(request.getCurrency()));
        transaction.setType(TransactionType.PAY_INTEREST);
        transaction.setFromAccountNumber(masterAccount);
        try {
            log.info("[PAY_INTEREST] Validate transaction...");
            validateTransaction(transaction);
            log.info("[PAY_INTEREST] Validate thành công");
            initTransaction(transaction);
            log.info("[PAY_INTEREST] Init transaction thành công | ReferenceCode: {}", transaction.getReferenceCode());
            processTransaction(transaction);
            log.info("[PAY_INTEREST] Process transaction thành công | ReferenceCode: {}", transaction.getReferenceCode());
            transactionRepository.save(transaction);
            log.info("[PAY_INTEREST] Lưu transaction thành công | ReferenceCode: {}", transaction.getReferenceCode());
            log.info("[PAY_INTEREST] Giao dịch trả lãi tiết kiệm thành công | ReferenceCode: {}", transaction.getReferenceCode());
            return transactionMapper.toDTO(transaction);
        } catch (Exception ex) {
            log.error("[customerId:{}][cifCode:{}][PAY_INTEREST] Lỗi khi thực hiện giao dịch trả lãi tiết kiệm | ReferenceCode: {} | To: {} | Amount: {} | Lý do: {}",
                    currentCustomer.getUserId(), currentCustomer.getCifCode(), transaction.getReferenceCode(), request.getToAccountNumber(), request.getAmount(), ex.getMessage(), ex);
            throw ex;
        }
    }

    @Override
    public List<AccountPaymentResponse> getAllAccountPaymentForCurrentCustomer() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication != null ? authentication.getName() : null;
        RpcContext.getClientAttachment().setAttachment("username", username);
        return accountQueryService.getAllAccountPaymentForCurrentCustomer();
    }

    @Override
    public CustomerDTO getCustomerByAccountNumber(String accountNumber) {
        return accountQueryService.getCustomerByAccountNumber(accountNumber);
    }

    @Override
    public CustomerResponse getCurrentCustomer() {
        return customerQueryService.getCurrentCustomer();
    }

    @Override
    public TransactionStatsResponse getTransactionStats(LocalDateTime startDate,
                                                        LocalDateTime endDate,Pageable pageable) {
        log.info("[GET_TXN_STATS] Bắt đầu lấy thống kê giao dịch | startDate: {} | endDate: {} | pageable: {}", startDate, endDate, pageable);
        try {
            if (startDate == null) startDate = LocalDateTime.now().minusDays(30);
            if (endDate == null) endDate = LocalDateTime.now();
            log.info("[GET_TXN_STATS] Thời gian thống kê | startDate: {} | endDate: {}", startDate, endDate);

            // Tổng giao dịch và tổng tiền
            log.info("[GET_TXN_STATS] Đang tính tổng giao dịch và tổng tiền...");
            long totalTransactions = transactionRepository.countByCreatedAtBetween(startDate, endDate);
            BigDecimal totalAmount = transactionRepository.sumAmountByCreatedAtBetween(startDate, endDate);
            log.info("[GET_TXN_STATS] Tổng giao dịch: {} | Tổng tiền: {}", totalTransactions, totalAmount);

            // Đếm theo trạng thái
            log.info("[GET_TXN_STATS] Đang thống kê theo trạng thái...");
            long successCount = transactionRepository.countByStatusAndCreatedAtBetween(TransactionStatus.COMPLETED, startDate, endDate);
            long failedCount = transactionRepository.countByStatusAndCreatedAtBetween(TransactionStatus.FAILED, startDate, endDate);
            long pendingCount = transactionRepository.countByStatusAndCreatedAtBetween(TransactionStatus.PENDING, startDate, endDate);
            log.info("[GET_TXN_STATS] Thống kê trạng thái | Success: {} | Failed: {} | Pending: {}", successCount, failedCount, pendingCount);

            // Top khách hàng
            log.info("[GET_TXN_STATS] Đang lấy top khách hàng...");
            List<Object[]> topAccountsRaw = transactionRepository.findTopAccounts(startDate, endDate, pageable);
            Map<String, TransactionStatsResponse.TopCustomerStats> topCustomersMap = topAccountsRaw.stream()
                    .filter(row -> {
                        String accountNumber = (String) row[0];
                        return !accountNumber.equals(masterAccount);
                    })
                    .map(row -> {
                        String accountNumber = (String) row[0];
                        long count = ((Number) row[1]).longValue();
                        BigDecimal total = (BigDecimal) row[2];
                        CustomerDTO customerDTO = accountQueryService.getCustomerByAccountNumber(accountNumber);
                        return new AbstractMap.SimpleEntry<>(customerDTO.getCifCode(), new Object[] {
                                customerDTO.getFullName(), count, total
                        });
                    })
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            entry -> TransactionStatsResponse.TopCustomerStats.builder()
                                    .cifCode(entry.getKey())
                                    .name((String) entry.getValue()[0])
                                    .transactionCount((Long) entry.getValue()[1])
                                    .totalAmount((BigDecimal) entry.getValue()[2])
                                    .build(),
                            (existing, incoming) -> TransactionStatsResponse.TopCustomerStats.builder()
                                    .cifCode(existing.getCifCode())
                                    .name(existing.getName()) // giữ nguyên tên đầu tiên
                                    .transactionCount(existing.getTransactionCount() + incoming.getTransactionCount())
                                    .totalAmount(existing.getTotalAmount().add(incoming.getTotalAmount()))
                                    .build()
                    ));

            List<TransactionStatsResponse.TopCustomerStats> topCustomers = new ArrayList<>(topCustomersMap.values());
            log.info("[GET_TXN_STATS] Top khách hàng: {} khách hàng", topCustomers.size());

            // Thống kê theo loại giao dịch
            log.info("[GET_TXN_STATS] Đang thống kê theo loại giao dịch...");
            List<Object[]> typeSummaryRaw = transactionRepository.groupByTypeAndSum(startDate, endDate);
            List<TransactionStatsResponse.TransactionTypeSummary> typeSummary = typeSummaryRaw.stream().map(row -> {
                TransactionStatsResponse.TransactionTypeSummary summary =
                        TransactionStatsResponse.TransactionTypeSummary.builder()
                                .transactionType(row[0].toString())
                                .count(((Number) row[1]).longValue())
                                .totalAmount((BigDecimal) row[2])
                                .build();
                return summary;
            }).toList();
            log.info("[GET_TXN_STATS] Thống kê theo loại giao dịch: {} loại", typeSummary.size());

            // Giao dịch gần nhất
            log.info("[GET_TXN_STATS] Đang lấy giao dịch gần nhất...");
            List<Transaction> latestTransactions = transactionRepository.findTop5ByCreatedAtBetweenOrderByCreatedAtDesc(startDate, endDate);
            List<TransactionStatsResponse.RecentTransaction> recentTransactions = latestTransactions.stream().map(tx -> {
                TransactionStatsResponse.RecentTransaction recent = TransactionStatsResponse.RecentTransaction.builder()
                        .transactionId(tx.getReferenceCode())
                        .fromAccount(tx.getFromAccountNumber())
                        .toAccount(tx.getToAccountNumber())
                        .amount(tx.getAmount())
                        .type(tx.getType().getDisplayName())
                        .status(tx.getStatus().name())
                        .createdAt(tx.getCreatedAt())
                        .build();
                return recent;
            }).toList();
            log.info("[GET_TXN_STATS] Giao dịch gần nhất: {} giao dịch", recentTransactions.size());

            TransactionStatsResponse response = new TransactionStatsResponse();
            response.setTotalTransactions(totalTransactions);
            response.setTotalAmount(totalAmount);
            response.setSuccessCount(successCount);
            response.setFailedCount(failedCount);
            response.setPendingCount(pendingCount);
            response.setTransactionTypeSummary(typeSummary);
            response.setTopCustomers(topCustomers);
            response.setLatestTransactions(recentTransactions);

            log.info("[GET_TXN_STATS] Hoàn thành lấy thống kê giao dịch | Tổng giao dịch: {} | Tổng tiền: {}", totalTransactions, totalAmount);
            return response;
        } catch (Exception ex) {
            log.error("[GET_TXN_STATS] Lỗi khi lấy thống kê giao dịch | startDate: {} | endDate: {} | Lý do: {}", startDate, endDate, ex.getMessage(), ex);
            throw ex;
        }
    }

    @Override
    @Transactional
    public TransactionDTO loanRecovery(DisburseRequest recoveryRequest) {
        CustomerResponse currentCustomer = customerQueryService.getCurrentCustomer();
        log.info("[customerId:{}][cifCode:{}][LOAN_RECOVERY] From: {} | To: {} | Amount: {} | Currency: {} | Desc: {}", 
            currentCustomer.getUserId(), currentCustomer.getCifCode(), 
             recoveryRequest.getToAccountNumber(), masterAccount,
            recoveryRequest.getAmount(), recoveryRequest.getCurrency(), recoveryRequest.getDescription());
        
        Transaction transaction = new Transaction();
        transaction.setFromAccountNumber(recoveryRequest.getToAccountNumber());
        transaction.setToAccountNumber(masterAccount);
        transaction.setAmount(recoveryRequest.getAmount());
        transaction.setDescription(recoveryRequest.getDescription());
        transaction.setCurrency(CurrencyType.valueOf(recoveryRequest.getCurrency()));
        transaction.setType(TransactionType.CORE_BANKING);
        
        try {
            log.info("[LOAN_RECOVERY] Validate transaction...");
            validateTransaction(transaction);
            log.info("[LOAN_RECOVERY] Validate thành công");
            initTransaction(transaction);
            log.info("[customerId:{}][cifCode:{}][LOAN_RECOVERY] Init transaction thành công | ReferenceCode: {}", 
                currentCustomer.getUserId(), currentCustomer.getCifCode(), transaction.getReferenceCode());
            processTransaction(transaction);
            log.info("[customerId:{}][cifCode:{}][LOAN_RECOVERY] Process transaction thành công | ReferenceCode: {}", 
                currentCustomer.getUserId(), currentCustomer.getCifCode(), transaction.getReferenceCode());
            transactionRepository.save(transaction);
            log.info("[customerId:{}][cifCode:{}][LOAN_RECOVERY] Lưu transaction thành công | ReferenceCode: {}", 
                currentCustomer.getUserId(), currentCustomer.getCifCode(), transaction.getReferenceCode());
            log.info("[customerId:{}][cifCode:{}][LOAN_RECOVERY] Giao dịch thu hồi khoản vay thành công | ReferenceCode: {}", 
                currentCustomer.getUserId(), currentCustomer.getCifCode(), transaction.getReferenceCode());
            return transactionMapper.toDTO(transaction);
        } catch (Exception ex) {
            log.error("[customerId:{}][cifCode:{}][LOAN_RECOVERY] Lỗi khi thực hiện giao dịch thu hồi khoản vay | ReferenceCode: {} | From: {} | To: {} | Amount: {} | Lý do: {}",
                    currentCustomer.getUserId(), currentCustomer.getCifCode(), transaction.getReferenceCode(), 
                    recoveryRequest.getToAccountNumber(), masterAccount,
                    recoveryRequest.getAmount(), ex.getMessage(), ex);
            throw ex;
        }
    }

    @Override
    @Transactional
    public TransactionDTO autoDeductRepayment(AutoDeductRequest autoDeductRequest) {
        log.info("[AUTO_DEDUCT] Bắt đầu xử lý tự động trừ tiền | LoanId: {} | RepaymentId: {} | From: {} | To: {} | Amount: {} | Currency: {} | Desc: {}",
                autoDeductRequest.getLoanId(), autoDeductRequest.getRepaymentId(),
                autoDeductRequest.getFromAccountNumber(), autoDeductRequest.getToAccountNumber(),
                autoDeductRequest.getAmount(), autoDeductRequest.getCurrency(),
                autoDeductRequest.getDescription());

        Transaction transaction = new Transaction();
        transaction.setFromAccountNumber(autoDeductRequest.getFromAccountNumber());
        transaction.setToAccountNumber(masterAccount);
        transaction.setAmount(autoDeductRequest.getAmount());
        transaction.setDescription(autoDeductRequest.getDescription());
        transaction.setCurrency(CurrencyType.valueOf(autoDeductRequest.getCurrency()));
        transaction.setType(TransactionType.LOAN_PAYMENT);

        try {
            log.info("[AUTO_DEDUCT] Validate transaction...");
            validateTransaction(transaction);
            log.info("[AUTO_DEDUCT] Validate thành công");

            initTransaction(transaction);
            log.info("[AUTO_DEDUCT] Init transaction thành công | ReferenceCode: {}", transaction.getReferenceCode());

            processTransaction(transaction);
            log.info("[AUTO_DEDUCT] Process transaction thành công | ReferenceCode: {}", transaction.getReferenceCode());

            transactionRepository.save(transaction);
            log.info("[AUTO_DEDUCT] Lưu transaction thành công | ReferenceCode: {}", transaction.getReferenceCode());

            log.info("[AUTO_DEDUCT] Giao dịch tự động trừ tiền định kỳ thành công | ReferenceCode: {}", transaction.getReferenceCode());

            return transactionMapper.toDTO(transaction);
        } catch (Exception ex) {
            log.error("[AUTO_DEDUCT] Lỗi khi thực hiện giao dịch tự động trừ tiền định kỳ | LoanId: {} | RepaymentId: {} | From: {} | To: {} | Amount: {} | ReferenceCode: {} | Lý do: {}",
                    autoDeductRequest.getLoanId(), autoDeductRequest.getRepaymentId(),
                    autoDeductRequest.getFromAccountNumber(), autoDeductRequest.getToAccountNumber(),
                    autoDeductRequest.getAmount(), transaction.getReferenceCode(), ex.getMessage(), ex);
            throw ex;
        }
    }



    //    Kiểm tra thông tin Transaction
    private void validateTransaction(Transaction transaction){
        log.info("context: {}",RpcContext.getServerAttachment().getAttachment("username"));
        CustomerResponseDTO currentCustomer = null;
        if (RpcContext.getContext() != null) {
            String username = RpcContext.getServerAttachment().getAttachment("username");
             currentCustomer = customerQueryService.getCustomerByUserId(username);

        }else {
            CustomerResponse currentCustomer1 = customerQueryService.getCurrentCustomer();
            currentCustomer.setUserId(currentCustomer1.getUserId());
            currentCustomer.setCifCode(currentCustomer1.getCifCode());
        }

        log.info("[customerId:{}][cifCode:{}][VALIDATE] fromAccount: {} | toAccount: {} | amount: {} | type: {}", currentCustomer.getUserId(), currentCustomer.getCifCode(), transaction.getFromAccountNumber(), transaction.getToAccountNumber(), transaction.getAmount(), transaction.getType());
        AccountDTO fromAccount = accountQueryService.getAccountByAccountNumber(transaction.getFromAccountNumber());
        AccountDTO toAccount = accountQueryService.getAccountByAccountNumber(transaction.getToAccountNumber());

        if (fromAccount==null) {
            log.error("[customerId:{}][cifCode:{}][VALIDATE] Lỗi: FROM_ACCOUNT_NOT_EXIST | fromAccount: {} | toAccount: {} | amount: {} | type: {}", currentCustomer.getUserId(), currentCustomer.getCifCode(), transaction.getFromAccountNumber(), transaction.getToAccountNumber(), transaction.getAmount(), transaction.getType());
            throw new AppException(ErrorCode.FROM_ACCOUNT_NOT_EXIST);
        }

        if (toAccount==null) {
            log.error("[customerId:{}][cifCode:{}][VALIDATE] Lỗi: TO_ACCOUNT_NOT_EXIST | fromAccount: {} | toAccount: {} | amount: {} | type: {}", currentCustomer.getUserId(), currentCustomer.getCifCode(), transaction.getFromAccountNumber(), transaction.getToAccountNumber(), transaction.getAmount(), transaction.getType());
            throw new AppException(ErrorCode.TO_ACCOUNT_NOT_EXIST);
        }

        CustomerDTO fromCustomer = customerQueryService.getCustomerByCifCode(fromAccount.getCifCode());
        CustomerDTO toCustomer = customerQueryService.getCustomerByCifCode(toAccount.getCifCode());
        if (fromCustomer==null) {
            log.error("[customerId:{}][cifCode:{}][VALIDATE] Lỗi: CUSTOMER_NOT_EXIST | fromAccount: {} | toAccount: {} | amount: {} | type: {}", currentCustomer.getUserId(), currentCustomer.getCifCode(), transaction.getFromAccountNumber(), transaction.getToAccountNumber(), transaction.getAmount(), transaction.getType());
            throw new AppException(ErrorCode.CUSTOMER_NOT_EXIST);
        }

        if (toCustomer==null) {
            log.error("[customerId:{}][cifCode:{}][VALIDATE] Lỗi: CORE_BANKING_UNAVAILABLE | fromAccount: {} | toAccount: {} | amount: {} | type: {}", currentCustomer.getUserId(), currentCustomer.getCifCode(), transaction.getFromAccountNumber(), transaction.getToAccountNumber(), transaction.getAmount(), transaction.getType());
            throw new AppException(ErrorCode.CORE_BANKING_UNAVAILABLE);
        }

        if(!fromCustomer.getStatus().name().equals("ACTIVE")){
            log.error("[customerId:{}][cifCode:{}][VALIDATE] Lỗi: FROM_CUSTOMER_NOT_ACTIVE | fromAccount: {} | toAccount: {} | amount: {} | type: {}", currentCustomer.getUserId(), currentCustomer.getCifCode(), transaction.getFromAccountNumber(), transaction.getToAccountNumber(), transaction.getAmount(), transaction.getType());
            throw new AppException(ErrorCode.FROM_CUSTOMER_NOT_ACTIVE);
        }
        if(!toCustomer.getStatus().name().equals("ACTIVE")){
            log.error("[customerId:{}][cifCode:{}][VALIDATE] Lỗi: TO_CUSTOMER_NOT_ACTIVE | fromAccount: {} | toAccount: {} | amount: {} | type: {}", currentCustomer.getUserId(), currentCustomer.getCifCode(), transaction.getFromAccountNumber(), transaction.getToAccountNumber(), transaction.getAmount(), transaction.getType());
            throw new AppException(ErrorCode.TO_CUSTOMER_NOT_ACTIVE);
        }

        if (EnumSet.of(TransactionType.TRANSFER, TransactionType.WITHDRAW,TransactionType.LOAN_PAYMENT,
                TransactionType.PAY_BILL).contains(transaction.getType())) {
            if(!accountQueryService.existsAccountByAccountNumberAndCifCode( fromAccount.getAccountNumber(),currentCustomer.getCifCode())){
                throw new AppException(ErrorCode.INVALID_ACCOUNT);
            }
        }


        if (EnumSet.of(TransactionType.TRANSFER, TransactionType.WITHDRAW,TransactionType.PAY_BILL,
                TransactionType.DISBURSEMENT,
                TransactionType.CORE_BANKING).contains(transaction.getType())) {

            Set<String> allowedTypes = Set.of("PAYMENT", "MASTER","LOAN");
            if (!allowedTypes.contains(fromAccount.getAccountType())) {
                throw new AppException(ErrorCode.FROM_ACCOUNT_NOT_PAYMENT);
            }

            if (!allowedTypes.contains(toAccount.getAccountType())) {
                throw new AppException(ErrorCode.TO_ACCOUNT_NOT_PAYMENT);
            }
        }

        if(!fromAccount.getStatus().equals("ACTIVE")){
            throw new AppException(ErrorCode.FROM_ACCOUNT_NOT_ACTIVE);
        }
        if(!toAccount.getStatus().equals("ACTIVE")){
            throw new AppException(ErrorCode.TO_ACCOUNT_NOT_ACTIVE);
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
        if (EnumSet.of(TransactionType.LOAN_PAYMENT,TransactionType.TRANSFER, TransactionType.WITHDRAW,TransactionType.PAY_BILL,
                TransactionType.CORE_BANKING).contains(transaction.getType())) {
            BigDecimal balance;
            try {
//                kiểm tra số dư
                String url = URL_CORE_BANK+"/get-balance/{accountNumber}";
                ParameterizedTypeReference<ApiResponse<BigDecimal>> responseType =
                        new ParameterizedTypeReference<ApiResponse<BigDecimal>>() {};
                ResponseEntity<ApiResponse<BigDecimal>> response = coreBankRestTemplate.exchange(
                        url,
                        HttpMethod.GET,
                        null,
                        responseType,
                        transaction.getFromAccountNumber()
                );
                balance = response.getBody().getResult();
            }
            catch (Exception e) {
                log.info("lỗi :{}",e.getMessage());
                throw new AppException(ErrorCode.CORE_BANKING_UNAVAILABLE);
            }

            if (balance.compareTo(transaction.getAmount()) < 0) {
                throw new AppException(ErrorCode.INSUFFICIENT_FUNDS);
            }
        }

    }
    private NapasInquiryResponse inquiryDestinationAccount(NapasInquiryRequest request){
        log.info("[INQUIRY_DEST_ACC] Kiểm tra tài khoản đích NAPAS | Account: {} | BankCode: {}", request.getAccountNumber(), request.getBankCode());
        String urlNapasInquiry = URL_NAPAS+"/inquiry";
        HttpEntity<NapasInquiryRequest> entity = new HttpEntity<>(request);
        try {
            ParameterizedTypeReference<ApiResponse<NapasInquiryResponse>> responseType =
                    new ParameterizedTypeReference<>() {};

            // Dùng exchange để gọi API
            ResponseEntity<ApiResponse<NapasInquiryResponse>> responseEntity =
                    mockServerRestTemplate.exchange(urlNapasInquiry, HttpMethod.POST, entity, responseType);

            ApiResponse<NapasInquiryResponse> apiResponse = responseEntity.getBody();
            if(apiResponse.getCode()==404){
                throw new AppException(ErrorCode.DESTINATION_ACCOUNT_NOT_EXIT);
            }
            return apiResponse.getResult();
        }  catch (RestClientException e) {
            log.error("[INQUIRY_DEST_ACC] Lỗi server khi gọi API NAPAS | Account: {} | BankCode: {} | Lý do: {}", request.getAccountNumber(), request.getBankCode(), e.getMessage());
            throw new AppException(ErrorCode.NAPAS_SERVER_ERROR);
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
        System.out.println("Mã OTP: "+ otp);
        MailMessageDTO mailMessage = MailMessageDTO.builder()
                .subject("Xác nhận OTP ")
                .body(otp)
                .recipient(fromCustomer.getEmail())
                .recipientName(fromCustomer.getFullName())
                .build();
        streamBridge.send("mail-out-0", mailMessage);
        log.info("[SEND_OTP] Đã gửi OTP cho account: {}", accountNumberRecipient);
    }
    private void processTransaction(Transaction transaction) {
        CustomerResponseDTO currentCustomer = null;
        if (RpcContext.getContext() != null) {
            String username = RpcContext.getServerAttachment().getAttachment("username");
            currentCustomer = customerQueryService.getCustomerByUserId(username);

        }else {
            CustomerResponse currentCustomer1 = customerQueryService.getCurrentCustomer();
            currentCustomer.setUserId(currentCustomer1.getUserId());
            currentCustomer.setCifCode(currentCustomer1.getCifCode());
        }
        log.info("[customerId:{}][cifCode:{}][PROCESS_TXN] Bắt đầu xử lý transaction | ReferenceCode: {} | Type: {} | Amount: {}", currentCustomer.getUserId(), currentCustomer.getCifCode(), transaction.getReferenceCode(), transaction.getType(), transaction.getAmount());
        try {
            TransactionRequest request = TransactionRequest.builder()
                    .fromAccountNumber(transaction.getFromAccountNumber())
                    .toAccountNumber(transaction.getToAccountNumber())
                    .amount(transaction.getAmount())
                    .description(transaction.getDescription())
                    .status(transaction.getStatus().name())
                    .timestamp(transaction.getTimestamp())
                    .type(transaction.getType().name())
                    .referenceCode(transaction.getReferenceCode())
                    .destinationBankCode(transaction.getDestinationBankCode())
                    .currency(transaction.getCurrency().name())
                    .build();
            log.info("[customerId:{}][cifCode:{}][PROCESS_TXN] Đang gửi request đến core banking | ReferenceCode: {}", currentCustomer.getUserId(), currentCustomer.getCifCode(), transaction.getReferenceCode());
            String url = URL_CORE_BANK+"/perform-transaction";
            HttpEntity<TransactionRequest> httpEntity = new HttpEntity<>(request);
            ParameterizedTypeReference<ApiResponse<CommonTransactionDTO>> responseType =
                    new ParameterizedTypeReference<ApiResponse<CommonTransactionDTO>>() {};
            ResponseEntity<ApiResponse<CommonTransactionDTO>> responseEntity = coreBankRestTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    httpEntity,
                    responseType
            );
            log.info("[customerId:{}][cifCode:{}][PROCESS_TXN] Đã nhận được response từ core banking | ReferenceCode: {}", currentCustomer.getUserId(), currentCustomer.getCifCode(), transaction.getReferenceCode());
            ApiResponse<CommonTransactionDTO> apiResponse = responseEntity.getBody();
            if(apiResponse.getCode()==200){
                transaction.setStatus(TransactionStatus.COMPLETED);
                log.info("[customerId:{}][cifCode:{}][PROCESS_TXN] Transaction thành công | ReferenceCode: {}", currentCustomer.getUserId(), currentCustomer.getCifCode(), transaction.getReferenceCode());
                if(transaction.getType()==TransactionType.PAY_BILL){
                    log.info("[customerId:{}][cifCode:{}][PROCESS_TXN] Đang xử lý giao dịch thanh toán hóa đơn | ReferenceCode: {}", currentCustomer.getUserId(), currentCustomer.getCifCode(), transaction.getReferenceCode());
                    ProviderGateway gateway = providerGateways.get(transaction.getBillType());
                    ProviderPaymentRequest providerPaymentRequest = ProviderPaymentRequest.builder()
                            .customerCode(transaction.getBillCustomerCode())
                            .billId(transaction.getBillId())
                            .paymentTimestamp(transaction.getTimestamp())
                            .bankTransactionReference(transaction.getReferenceCode())
                            .amount(transaction.getAmount())
                            .provider(transaction.getBillProviderCode())
                            .build();
                    log.info("[customerId:{}][cifCode:{}][PROCESS_TXN] Đang gửi request thanh toán hóa đơn đến gateway | ReferenceCode: {}", currentCustomer.getUserId(), currentCustomer.getCifCode(), transaction.getReferenceCode());
                    ProviderPaymentResponse response = gateway.payBill(providerPaymentRequest);
                    log.info("[customerId:{}][cifCode:{}][PROCESS_TXN] Đã nhận được response thanh toán hóa đơn từ gateway | ReferenceCode: {}", currentCustomer.getUserId(), currentCustomer.getCifCode(), transaction.getReferenceCode());
                    transaction.setProviderTransactionId(response.getProviderTransactionId());
                }
            }else {
                transaction.setStatus(TransactionStatus.FAILED);
                transaction.setFailedReason(apiResponse.getMessage());
                log.error("[customerId:{}][cifCode:{}][PROCESS_TXN] Transaction thất bại | ReferenceCode: {} | Lý do: {}", currentCustomer.getUserId(), currentCustomer.getCifCode(), transaction.getReferenceCode(), apiResponse.getMessage());
            }
        } catch (HttpClientErrorException e) {
            String errorBody = e.getResponseBodyAsString();
            String failedReason = "Lỗi không xác định";
            if (errorBody != null && !errorBody.isEmpty()) {
                try {
                    ObjectMapper objectMapper = new ObjectMapper();
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
            log.error("[customerId:{}][cifCode:{}][PROCESS_TXN] Transaction failed (HttpClientErrorException) | ReferenceCode: {} | Lý do: {}", currentCustomer.getUserId(), currentCustomer.getCifCode(), transaction.getReferenceCode(), failedReason, e);
        } catch (Exception ex) {
            transaction.setStatus(TransactionStatus.FAILED);
            transaction.setFailedReason(ex.getMessage());
            if(transaction.getType()==TransactionType.PAY_BILL){
                TransactionRequest reverseRequest = TransactionRequest.builder()
                        .fromAccountNumber(transaction.getToAccountNumber())
                        .toAccountNumber(transaction.getFromAccountNumber())
                        .amount(transaction.getAmount())
                        .type(TransactionType.REFUND.name())
                        .timestamp(LocalDateTime.now())
                        .description("Hoàn trả tiền thanh toán hóa đơn")
                        .referenceCode(transaction.getReferenceCode())
                        .build();
                log.info("[customerId:{}][cifCode:{}][PROCESS_TXN] Đang gửi request hoàn tiền đến core banking | ReferenceCode: {}", currentCustomer.getUserId(), currentCustomer.getCifCode(), transaction.getReferenceCode());
                String url = URL_CORE_BANK+"/perform-transaction";
                HttpEntity<TransactionRequest> httpEntity = new HttpEntity<>(reverseRequest);
                ParameterizedTypeReference<ApiResponse<CommonTransactionDTO>> responseType =
                        new ParameterizedTypeReference<ApiResponse<CommonTransactionDTO>>() {};
                ResponseEntity<ApiResponse<CommonTransactionDTO>> responseEntity = coreBankRestTemplate.exchange(
                        url,
                        HttpMethod.POST,
                        httpEntity,
                        responseType
                );
                ApiResponse<CommonTransactionDTO> refundResponse = responseEntity.getBody();
                if (refundResponse == null || refundResponse.getCode() != 200) {
                    log.error("[customerId:{}][cifCode:{}][PROCESS_TXN] Hoàn tiền thất bại cho giao dịch {} - response lỗi: {}", currentCustomer.getUserId(), currentCustomer.getCifCode(), transaction.getReferenceCode(), refundResponse);
                } else {
                    log.info("[customerId:{}][cifCode:{}][PROCESS_TXN] Đã hoàn tiền thành công cho giao dịch {}", currentCustomer.getUserId(), currentCustomer.getCifCode(), transaction.getReferenceCode());
                }
            }
            log.error("[customerId:{}][cifCode:{}][PROCESS_TXN] Unexpected error | ReferenceCode: {} | Lý do: {}", currentCustomer.getUserId(), currentCustomer.getCifCode(), transaction.getReferenceCode(), ex.getMessage(), ex);
        }
    }


}