package com.example.transaction_service.service.impl;


import com.example.common_service.dto.*;
import com.example.common_service.dto.request.CreateAccountSavingRequest;
import com.example.common_service.dto.request.PayInterestRequest;
import com.example.common_service.dto.request.TransactionRequest;
import com.example.common_service.dto.request.WithdrawAccountSavingRequest;
import com.example.common_service.services.CommonService;
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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
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
import org.springframework.http.*;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
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
    private final JwtDecoder jwtDecoder;
    private final JwtAuthenticationConverter authConverter;
    private final TransactionMapper transactionMapper;

    @DubboReference
    private final AccountQueryService accountQueryService;


    @DubboReference
    private final CustomerQueryService customerQueryService;

    @DubboReference
    private final CommonService commonService;

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
        log.info("[TRANSFER] Bắt đầu giao dịch chuyển tiền | From: {} | To: {} | Amount: {} | Currency: {} | Desc: {}",
                transferRequest.getFromAccountNumber(), transferRequest.getToAccountNumber(), transferRequest.getAmount(), transferRequest.getCurrency(), transferRequest.getDescription());
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
            log.info("[TRANSFER] Init transaction thành công | ReferenceCode: {}", transaction.getReferenceCode());
            // Gửi OTP
            sendOTP(transaction.getReferenceCode(), transaction.getFromAccountNumber());
            log.info("[TRANSFER] Đã gửi OTP cho account: {}", transaction.getFromAccountNumber());
            transactionRepository.save(transaction);
            log.info("[TRANSFER] Lưu transaction thành công | ReferenceCode: {}", transaction.getReferenceCode());
            log.info("[TRANSFER] Giao dịch chuyển tiền khởi tạo thành công | ReferenceCode: {}", transaction.getReferenceCode());
            return transactionMapper.toDTO(transaction);
        } catch (Exception ex) {
            log.error("[TRANSFER] Lỗi khi thực hiện giao dịch chuyển tiền | From: {} | To: {} | Amount: {} | Lý do: {}",
                    transferRequest.getFromAccountNumber(), transferRequest.getToAccountNumber(), transferRequest.getAmount(), ex.getMessage(), ex);
            throw ex;
        }
    }

    @Override
    @Transactional
    public TransactionDTO deposit(DepositRequest depositRequest) {
        log.info("[DEPOSIT] Bắt đầu giao dịch nạp tiền | To: {} | Amount: {} | Currency: {} | Desc: {}",
                depositRequest.getToAccountNumber(), depositRequest.getAmount(), depositRequest.getCurrency(), depositRequest.getDescription());
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
            log.info("[DEPOSIT] Init transaction thành công | ReferenceCode: {}", transaction.getReferenceCode());
            sendOTP(transaction.getReferenceCode(), transaction.getToAccountNumber());
            log.info("[DEPOSIT] Đã gửi OTP cho account: {}", transaction.getToAccountNumber());
            transactionRepository.save(transaction);
            log.info("[DEPOSIT] Lưu transaction thành công | ReferenceCode: {}", transaction.getReferenceCode());
            log.info("[DEPOSIT] Giao dịch nạp tiền khởi tạo thành công | ReferenceCode: {}", transaction.getReferenceCode());
            return transactionMapper.toDTO(transaction);
        } catch (Exception ex) {
            log.error("[DEPOSIT] Lỗi khi thực hiện giao dịch nạp tiền | To: {} | Amount: {} | Lý do: {}",
                    depositRequest.getToAccountNumber(), depositRequest.getAmount(), ex.getMessage(), ex);
            throw ex;
        }
    }

    @Override
    @Transactional
    public TransactionDTO withdraw(WithdrawRequest withdrawRequest) {
        log.info("[WITHDRAW] Bắt đầu giao dịch rút tiền | From: {} | Amount: {} | Currency: {} | Desc: {}",
                withdrawRequest.getFromAccountNumber(), withdrawRequest.getAmount(), withdrawRequest.getCurrency(), withdrawRequest.getDescription());
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
            log.info("[WITHDRAW] Init transaction thành công | ReferenceCode: {}", transaction.getReferenceCode());
            sendOTP(transaction.getReferenceCode(), transaction.getFromAccountNumber());
            log.info("[WITHDRAW] Đã gửi OTP cho account: {}", transaction.getFromAccountNumber());
            transactionRepository.save(transaction);
            log.info("[WITHDRAW] Lưu transaction thành công | ReferenceCode: {}", transaction.getReferenceCode());
            log.info("[WITHDRAW] Giao dịch rút tiền khởi tạo thành công | ReferenceCode: {}", transaction.getReferenceCode());
            return transactionMapper.toDTO(transaction);
        } catch (Exception ex) {
            log.error("[WITHDRAW] Lỗi khi thực hiện giao dịch rút tiền | From: {} | Amount: {} | Lý do: {}",
                    withdrawRequest.getFromAccountNumber(), withdrawRequest.getAmount(), ex.getMessage(), ex);
            throw ex;
        }
    }

    @Override
    @Transactional
    public TransactionDTO payBillLoan(LoanPaymentRequest repaymentRequest) {
        log.info("[LOAN_PAYMENT] Bắt đầu giao dịch thanh toán khoản vay | From: {} | Amount: {} | Currency: {} | Desc: {}",
                repaymentRequest.getFromAccountNumber(), repaymentRequest.getAmount(), repaymentRequest.getCurrency(), repaymentRequest.getDescription());
        Transaction transaction = new Transaction();
        transaction.setFromAccountNumber(repaymentRequest.getFromAccountNumber());
        transaction.setAmount(repaymentRequest.getAmount());
        transaction.setDescription(repaymentRequest.getDescription());
        transaction.setCurrency(CurrencyType.valueOf(repaymentRequest.getCurrency()));
        transaction.setType(TransactionType.LOAN_PAYMENT);
        transaction.setToAccountNumber(masterAccount);
        try {
            log.info("[LOAN_PAYMENT] Validate transaction...");
            validateTransaction(transaction);
            log.info("[LOAN_PAYMENT] Validate thành công");
            initTransaction(transaction);
            log.info("[LOAN_PAYMENT] Init transaction thành công | ReferenceCode: {}", transaction.getReferenceCode());
            transactionRepository.save(transaction);
            sendOTP(transaction.getReferenceCode(), transaction.getFromAccountNumber());
            log.info("[LOAN_PAYMENT] Đã gửi OTP cho account: {}", transaction.getFromAccountNumber());
            transactionRepository.save(transaction);
            log.info("[LOAN_PAYMENT] Lưu transaction thành công | ReferenceCode: {}", transaction.getReferenceCode());
            log.info("[LOAN_PAYMENT] Giao dịch thanh toán khoản vay khởi tạo thành công | ReferenceCode: {}", transaction.getReferenceCode());
            return transactionMapper.toDTO(transaction);
        } catch (Exception ex) {
            log.error("[LOAN_PAYMENT] Lỗi khi thực hiện giao dịch thanh toán khoản vay | From: {} | Amount: {} | Lý do: {}",
                    repaymentRequest.getFromAccountNumber(), repaymentRequest.getAmount(), ex.getMessage(), ex);
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
        log.info("[PAY_BILL] Bắt đầu giao dịch thanh toán hóa đơn | From: {} | BillType: {} | CustomerCode: {} | Provider: {} | Desc: {}",
                request.getFromAccountNumber(), request.getBillType(), request.getCustomerCode(), request.getProvider(), request.getDescription());
        try {
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
            transaction.setToAccountNumber(masterAccount);
            log.info("[PAY_BILL] Validate transaction...");
            validateTransaction(transaction);
            log.info("[PAY_BILL] Validate thành công");
            initTransaction(transaction);
            log.info("[PAY_BILL] Init transaction thành công | ReferenceCode: {}", transaction.getReferenceCode());
            transactionRepository.save(transaction);
            sendOTP(transaction.getReferenceCode(), transaction.getFromAccountNumber());
            log.info("[PAY_BILL] Đã gửi OTP cho account: {}", transaction.getFromAccountNumber());
            transactionRepository.save(transaction);
            log.info("[PAY_BILL] Lưu transaction thành công | ReferenceCode: {}", transaction.getReferenceCode());
            log.info("[PAY_BILL] Giao dịch thanh toán hóa đơn khởi tạo thành công | ReferenceCode: {}", transaction.getReferenceCode());
            return transactionMapper.toDTO(transaction);
        } catch (Exception ex) {
            log.error("[PAY_BILL] Lỗi khi thực hiện giao dịch thanh toán hóa đơn | From: {} | BillType: {} | Lý do: {}",
                    request.getFromAccountNumber(), request.getBillType(), ex.getMessage(), ex);
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
    public TransactionDTO disburse(DisburseRequest disburseRequest) {
        log.info("[DISBURSE] Bắt đầu giao dịch giải ngân | To: {} | Amount: {} | Currency: {} | Desc: {}",
                disburseRequest.getToAccountNumber(), disburseRequest.getAmount(), disburseRequest.getCurrency(), disburseRequest.getDescription());
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
            log.info("[DISBURSE] Init transaction thành công | ReferenceCode: {}", transaction.getReferenceCode());
            processTransaction(transaction);
            log.info("[DISBURSE] Process transaction thành công | ReferenceCode: {}", transaction.getReferenceCode());
            transactionRepository.save(transaction);
            log.info("[DISBURSE] Lưu transaction thành công | ReferenceCode: {}", transaction.getReferenceCode());
            log.info("[DISBURSE] Giao dịch giải ngân thành công | ReferenceCode: {}", transaction.getReferenceCode());
            return transactionMapper.toDTO(transaction);
        } catch (Exception ex) {
            log.error("[DISBURSE] Lỗi khi thực hiện giao dịch giải ngân | To: {} | Amount: {} | Lý do: {}",
                    disburseRequest.getToAccountNumber(), disburseRequest.getAmount(), ex.getMessage(), ex);
            throw ex;
        }
    }

    @Override
    public TransactionDTO createAccountSaving(CreateAccountSavingRequest accountSavingRequest) {
        log.info("[CREATE_SAVING] Bắt đầu giao dịch mở sổ tiết kiệm | From: {} | Amount: {} | Currency: {} | Desc: {}",
                accountSavingRequest.getFromAccountNumber(), accountSavingRequest.getAmount(), accountSavingRequest.getCurrency(), accountSavingRequest.getDescription());
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
            log.info("[CREATE_SAVING] Init transaction thành công | ReferenceCode: {}", transaction.getReferenceCode());
            processTransaction(transaction);
            log.info("[CREATE_SAVING] Process transaction thành công | ReferenceCode: {}", transaction.getReferenceCode());
            transactionRepository.save(transaction);
            log.info("[CREATE_SAVING] Lưu transaction thành công | ReferenceCode: {}", transaction.getReferenceCode());
            log.info("[CREATE_SAVING] Giao dịch mở sổ tiết kiệm thành công | ReferenceCode: {}", transaction.getReferenceCode());
            return transactionMapper.toDTO(transaction);
        } catch (Exception ex) {
            log.error("[CREATE_SAVING] Lỗi khi thực hiện giao dịch mở sổ tiết kiệm | From: {} | Amount: {} | Lý do: {}",
                    accountSavingRequest.getFromAccountNumber(), accountSavingRequest.getAmount(), ex.getMessage(), ex);
            throw ex;
        }
    }

    @Override
    @Transactional
    public TransactionDTO confirmTransaction(ConfirmTransactionRequest request){
        log.info("[CONFIRM_TXN] Xác nhận giao dịch | ReferenceCode: {} | OTP: {}", request.getReferenceCode(), request.getOtpCode());
        String keyOTP = "OTP:" + request.getReferenceCode();
        String storedOtp = redisTemplate.opsForValue().get(keyOTP);
        String keyFailCount = "OTP_FAIL_COUNT:" + request.getReferenceCode();
        try {
            if (storedOtp == null) {
                log.warn("[CONFIRM_TXN] OTP hết hạn | ReferenceCode: {}", request.getReferenceCode());
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
                    log.error("[CONFIRM_TXN] Giao dịch thất bại do nhập sai OTP quá 3 lần | ReferenceCode: {}", request.getReferenceCode());
                    return transactionMapper.toDTO(txn);
                }
                log.warn("[CONFIRM_TXN] Sai OTP | ReferenceCode: {} | Lần sai: {}", request.getReferenceCode(), failCount);
                throw new AppException(ErrorCode.INVALID_OTP);
            }
            Transaction txn = transactionRepository.findByReferenceCode(request.getReferenceCode());
            if(txn == null) throw new AppException(ErrorCode.TRANSACTION_NOT_EXIST);
            if (txn.getStatus() != TransactionStatus.PENDING) {
                log.warn("[CONFIRM_TXN] Trạng thái giao dịch không hợp lệ | ReferenceCode: {} | Status: {}", request.getReferenceCode(), txn.getStatus());
                throw new AppException(ErrorCode.INVALID_TRANSACTION_STATUS);
            }
            processTransaction(txn);
            transactionRepository.save(txn);
            log.info("[CONFIRM_TXN] Giao dịch xác nhận thành công | ReferenceCode: {}", request.getReferenceCode());
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
                System.out.println("Gửi mail tới: "+fromCustomer.getFullName()+ fromCustomer.getEmail());
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
                System.out.println("Gửi mail tới: "+toCustomer.getFullName());
                streamBridge.send("mail-transaction-out-0", mailTransactionDTO);
            }
            redisTemplate.delete(keyFailCount);
            redisTemplate.delete(keyOTP);
            return transactionMapper.toDTO(txn);
        } catch (Exception ex) {
            log.error("[CONFIRM_TXN] Lỗi xác nhận giao dịch | ReferenceCode: {} | Lý do: {}", request.getReferenceCode(), ex.getMessage(), ex);
            throw ex;
        }
    }

    @Override
    public TransactionDTO withdrawAccountSaving(WithdrawAccountSavingRequest depositAccountSavingRequest) {
        log.info("[WITHDRAW_SAVING] Bắt đầu giao dịch rút tiết kiệm | To: {} | Amount: {} | Currency: {} | Desc: {}",
                depositAccountSavingRequest.getToAccountNumber(), depositAccountSavingRequest.getAmount(), depositAccountSavingRequest.getCurrency(), depositAccountSavingRequest.getDescription());
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
            log.info("[WITHDRAW_SAVING] Init transaction thành công | ReferenceCode: {}", transaction.getReferenceCode());
            processTransaction(transaction);
            log.info("[WITHDRAW_SAVING] Process transaction thành công | ReferenceCode: {}", transaction.getReferenceCode());
            transactionRepository.save(transaction);
            log.info("[WITHDRAW_SAVING] Lưu transaction thành công | ReferenceCode: {}", transaction.getReferenceCode());
            log.info("[WITHDRAW_SAVING] Giao dịch rút tiết kiệm thành công | ReferenceCode: {}", transaction.getReferenceCode());
            return transactionMapper.toDTO(transaction);
        } catch (Exception ex) {
            log.error("[WITHDRAW_SAVING] Lỗi khi thực hiện giao dịch rút tiết kiệm | To: {} | Amount: {} | Lý do: {}",
                    depositAccountSavingRequest.getToAccountNumber(), depositAccountSavingRequest.getAmount(), ex.getMessage(), ex);
            throw ex;
        }
    }

    @Override
    @Transactional
    public TransactionDTO transferToExternalBank(ExternalTransferRequest externalTransferRequest) {
        log.info("[EXTERNAL_TRANSFER] Bắt đầu giao dịch chuyển tiền liên ngân hàng | From: {} | To: {} | Amount: {} | BankCode: {} | Desc: {}",
                externalTransferRequest.getFromAccountNumber(), externalTransferRequest.getToAccountNumber(), externalTransferRequest.getAmount(), externalTransferRequest.getDestinationBankCode(), externalTransferRequest.getDescription());
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
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String userId = authentication.getName();
            CustomerDTO currentCustomer = commonService.getCurrentCustomer(userId);
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
            log.info("[EXTERNAL_TRANSFER] Init transaction thành công | ReferenceCode: {}", transaction.getReferenceCode());
            sendOTP(transaction.getReferenceCode(),transaction.getFromAccountNumber());
            log.info("[EXTERNAL_TRANSFER] Đã gửi OTP cho account: {}", transaction.getFromAccountNumber());
            transactionRepository.save(transaction);
            log.info("[EXTERNAL_TRANSFER] Lưu transaction thành công | ReferenceCode: {}", transaction.getReferenceCode());
            log.info("[EXTERNAL_TRANSFER] Giao dịch chuyển tiền liên ngân hàng khởi tạo thành công | ReferenceCode: {}", transaction.getReferenceCode());
            return transactionMapper.toDTO(transaction);
        } catch (Exception ex) {
            log.error("[EXTERNAL_TRANSFER] Lỗi khi thực hiện giao dịch chuyển tiền liên ngân hàng | From: {} | To: {} | Amount: {} | Lý do: {}",
                    externalTransferRequest.getFromAccountNumber(), externalTransferRequest.getToAccountNumber(), externalTransferRequest.getAmount(), ex.getMessage(), ex);
            throw ex;
        }
    }

    @Override
    public void resendOtp(ResendOtpRequest resendOtpRequest) {
        log.info("[RESEND_OTP] Gửi lại OTP | ReferenceCode: {} | Account: {}", resendOtpRequest.getReferenceCode(), resendOtpRequest.getAccountNumberRecipient());
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
                    .recipient("phanhuynhphuckhang12c8@gmail.com")
                    .recipientName(fromCustomer.getFullName())
                    .build();
            streamBridge.send("mail-out-0", mailMessage);
            log.info("[RESEND_OTP] Đã gửi lại OTP cho account: {}", resendOtpRequest.getAccountNumberRecipient());
            System.out.println(otp);
        } catch (Exception ex) {
            log.error("[RESEND_OTP] Lỗi khi gửi lại OTP | ReferenceCode: {} | Lý do: {}", resendOtpRequest.getReferenceCode(), ex.getMessage(), ex);
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
        log.info("[CHECK_DEST_ACC] Kiểm tra tài khoản đích NAPAS | Account: {} | BankCode: {}", request.getAccountNumber(), request.getBankCode());
        try {
            return inquiryDestinationAccount(request);
        } catch (Exception ex) {
            log.error("[CHECK_DEST_ACC] Lỗi khi kiểm tra tài khoản đích NAPAS | Account: {} | Lý do: {}", request.getAccountNumber(), ex.getMessage(), ex);
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
        log.info("[PAY_INTEREST] Bắt đầu giao dịch trả lãi tiết kiệm | To: {} | Amount: {} | Currency: {} | Desc: {}",
                request.getToAccountNumber(), request.getAmount(), request.getCurrency(), request.getDescription());
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
            log.error("[PAY_INTEREST] Lỗi khi thực hiện giao dịch trả lãi tiết kiệm | To: {} | Amount: {} | Lý do: {}",
                    request.getToAccountNumber(), request.getAmount(), ex.getMessage(), ex);
            throw ex;
        }
    }

    //    Kiểm tra thông tin Transaction
    private void validateTransaction(Transaction transaction){
        String tokenValue = "";
        try{
            String authJson = RpcContext.getContext().getObjectAttachment("security_authentication_context").toString();
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(authJson);
            tokenValue = root.path("token").path("tokenValue").asText();
        }catch (JsonProcessingException e){
            System.out.println(e);
        }if (tokenValue == null) {
            throw new SecurityException("Missing JWT token");
        }
        Jwt jwt = jwtDecoder.decode(tokenValue);
        AbstractAuthenticationToken tokenAuth = authConverter.convert(jwt);
        SecurityContextHolder.getContext().setAuthentication(tokenAuth);
        if (!(tokenAuth instanceof JwtAuthenticationToken)) {
            throw new SecurityException("Expected JwtAuthenticationToken but got "+ tokenAuth.getClass().getName());
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName();
        CustomerDTO currentCustomer = commonService.getCurrentCustomer(userId);
        log.info("CurrentCustomer: {}",currentCustomer);
        AccountDTO fromAccount = accountQueryService.getAccountByAccountNumber(transaction.getFromAccountNumber());
        AccountDTO toAccount = accountQueryService.getAccountByAccountNumber(transaction.getToAccountNumber());


        if (fromAccount==null) {
            throw new AppException(ErrorCode.FROM_ACCOUNT_NOT_EXIST);
        }

        if (toAccount==null) {
            throw new AppException(ErrorCode.TO_ACCOUNT_NOT_EXIST);
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

        if (EnumSet.of(TransactionType.TRANSFER, TransactionType.WITHDRAW,
                TransactionType.PAY_BILL).contains(transaction.getType())) {
            if(!accountQueryService.existsAccountByAccountNumberAndCifCode(
                    fromAccount.getAccountNumber(),currentCustomer.getCifCode())){
                throw new AppException(ErrorCode.INVALID_ACCOUNT);
            }
        }else if (EnumSet.of(TransactionType.DEPOSIT).contains(transaction.getType())) {
            if(!accountQueryService.existsAccountByAccountNumberAndCifCode(
                    toAccount.getAccountNumber(),currentCustomer.getCifCode())){
                throw new AppException(ErrorCode.INVALID_ACCOUNT);
            }
        }


        if (EnumSet.of(TransactionType.TRANSFER, TransactionType.WITHDRAW,TransactionType.PAY_BILL,
                TransactionType.DISBURSEMENT,
                TransactionType.CORE_BANKING).contains(transaction.getType())) {

            Set<String> allowedTypes = Set.of("PAYMENT", "MASTER");
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
        if (EnumSet.of(TransactionType.TRANSFER, TransactionType.WITHDRAW,TransactionType.PAY_BILL,
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
                throw new AppException(ErrorCode.CORE_BANKING_UNAVAILABLE);
            }

            if (balance.compareTo(transaction.getAmount()) < 0) {
                throw new AppException(ErrorCode.INSUFFICIENT_FUNDS);
            }
        }

    }
    private NapasInquiryResponse inquiryDestinationAccount(NapasInquiryRequest request){
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
            log.error("Lỗi server khi gọi API NAPAS {}", e.getMessage());
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
        MailMessageDTO mailMessage = MailMessageDTO.builder()
                .subject("Xác nhận OTP ")
                .body(otp)
                .recipient("phanhuynhphuckhang12c8@gmail.com")
                .recipientName(fromCustomer.getFullName())
                .build();
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
                    .referenceCode(transaction.getReferenceCode())
                    .destinationBankCode(transaction.getDestinationBankCode())
                    .build();
            String url = URL_CORE_BANK+"/perform-transaction";

            HttpEntity<TransactionRequest> httpEntity = new HttpEntity<>(request);
//          Định nghĩa kiểu dữ liệu trả về
            ParameterizedTypeReference<ApiResponse<CommonTransactionDTO>> responseType =
                    new ParameterizedTypeReference<ApiResponse<CommonTransactionDTO>>() {};

//          Gửi POST request
            ResponseEntity<ApiResponse<CommonTransactionDTO>> responseEntity = coreBankRestTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    httpEntity,
                    responseType
            );
            ApiResponse<CommonTransactionDTO> apiResponse = responseEntity.getBody();
            if(apiResponse.getCode()==200){
                transaction.setStatus(TransactionStatus.COMPLETED);
                if(transaction.getType()==TransactionType.PAY_BILL){
                    System.out.println(transaction.getType());
                    ProviderGateway gateway = providerGateways.get(transaction.getBillType());
                    ProviderPaymentRequest providerPaymentRequest = ProviderPaymentRequest.builder()
                            .customerCode(transaction.getBillCustomerCode())
                            .billId(transaction.getBillId())
                            .paymentTimestamp(transaction.getTimestamp())
                            .bankTransactionReference(transaction.getReferenceCode())
                            .amount(transaction.getAmount())
                            .provider(transaction.getBillProviderCode())
                            .build();
                    ProviderPaymentResponse response = gateway.payBill(providerPaymentRequest);
                    transaction.setProviderTransactionId(response.getProviderTransactionId());

                }

            }else {
                transaction.setStatus(TransactionStatus.FAILED);
                transaction.setFailedReason(apiResponse.getMessage());
            }
        } catch (HttpClientErrorException e) {
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
            transaction.setFailedReason(ex.getMessage());
            if(transaction.getType()==TransactionType.PAY_BILL){
                TransactionRequest reverseRequest = TransactionRequest.builder()
                        .fromAccountNumber(transaction.getFromAccountNumber())
                        .toAccountNumber(transaction.getToAccountNumber())
                        .amount(transaction.getAmount())
                        .type(TransactionType.REFUND.name())
                        .timestamp(LocalDateTime.now())
                        .description("Hoàn trả tiền thanh toán hóa đơn")
                        .referenceCode(transaction.getReferenceCode())
                        .build();
                String url = URL_CORE_BANK+"/reverse-transaction";
                HttpEntity<TransactionRequest> httpEntity = new HttpEntity<>(reverseRequest);
//          Định nghĩa kiểu dữ liệu trả về
                ParameterizedTypeReference<ApiResponse<CommonTransactionDTO>> responseType =
                        new ParameterizedTypeReference<ApiResponse<CommonTransactionDTO>>() {};

//          Gửi POST request
                ResponseEntity<ApiResponse<CommonTransactionDTO>> responseEntity = coreBankRestTemplate.exchange(
                        url,
                        HttpMethod.POST,
                        httpEntity,
                        responseType
                );
                ApiResponse<CommonTransactionDTO> refundResponse = responseEntity.getBody();
                if (refundResponse == null || refundResponse.getCode() != 200) {
                    log.error("Hoàn tiền thất bại cho giao dịch {} - response lỗi: {}", transaction.getReferenceCode(), refundResponse);
                } else {
                    log.info("Đã hoàn tiền thành công cho giao dịch {}", transaction.getReferenceCode());
                }
            }
            log.error("Unexpected error:", ex);
        }

    }


}