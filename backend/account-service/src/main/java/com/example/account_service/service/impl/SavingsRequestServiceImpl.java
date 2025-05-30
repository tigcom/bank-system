package com.example.account_service.service.impl;

import com.example.account_service.dto.request.ConfirmRequestDTO;
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
import com.example.common_service.dto.*;
import com.example.common_service.dto.request.CreateAccountSavingRequest;
import com.example.common_service.dto.response.ApiResponse;
import com.example.common_service.dto.response.CoreTermDTO;
import com.example.common_service.services.CommonService;
import com.example.common_service.services.CommonServiceCore;
import com.example.common_service.services.transactions.CommonTransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Random;
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

    @DubboReference(timeout = 5000)
    private final CommonTransactionService commonTransactionService;

    private final CreditRequestRepository creditRequestRepository;

    private final AccountNumberUtils    accountNumberUtils;
    private final RestTemplate restTemplate;
    private final SavingsRequestRepository savingsRequestRepository;
    private final StreamBridge streamBridge;
    private final RedisTemplate<Object, Object> redisTemplate;


    @Override
    public SavingsRequestResponse CreateSavingRequest(SavingRequestCreateDTO savingRequestCreateDTO) {

        String urlGetBaLance = "http://localhost:8083/corebanking/api/core-bank/get-balance/" + savingRequestCreateDTO.getAccountNumberSource();
        ResponseEntity<ApiResponse<BigDecimal>> response = restTemplate.exchange(
                urlGetBaLance,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ApiResponse<BigDecimal>>() {}
        );
        BigDecimal balance = response.getBody().getResult();
        log.info("balance of src account payment : {}", balance);
        if (balance.compareTo(savingRequestCreateDTO.getInitialDeposit()) < 0) {
            throw new AppException(ErrorCode.BALANCE_NOT_ENOUGH);
        }

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

        String keyOTP = "OTP:"+savingsRequestId;
        String  OTP = createOTP(savingsRequestId);
        log.info("OTP created: {}", OTP);
        String storedOtp = (String) redisTemplate.opsForValue().get(keyOTP);
        log.info("OTP on redis : {}", OTP);
        MailMessageDTO  mailMessageDTO= MailMessageDTO.builder()
                .recipientName(customerDTO.getFullName())
                .recipient(customerDTO.getEmail())
                .body(OTP)
                .subject("Xác thực OTP")
                .build();
        streamBridge.send("mail-out-0", mailMessageDTO);

    }

    @Override
    public SavingsRequestResponse confirmOTPandSave(ConfirmRequestDTO confirmRequestDTO) {
        String keyOTP = "OTP:"+confirmRequestDTO.getSavingRequestID();
        String storedOtp = (String) redisTemplate.opsForValue().get(keyOTP);
        log.info("OTP on redis : {}", storedOtp);
        SavingsRequest savingsRequest = savingsRequestRepository.findById(confirmRequestDTO.getSavingRequestID()).orElseThrow(
                ()->  new AppException(ErrorCode.SAVING_REQUEST_NOTEXISTED)
        );
        if (!savingsRequest.getStatus().equals(SavingsRequestStatus.PENDING)) {
            throw new AppException(ErrorCode.UNCATERROR_ERROR);
        }
        if (storedOtp == null) {
            throw new AppException(ErrorCode.OTP_EXPIRED);
        }
        if (!storedOtp.equals(confirmRequestDTO.getOtpCode())) {
            String keyFailCount = "OTP_FAIL_COUNT:" + confirmRequestDTO.getSavingRequestID();
            String failStr = (String) redisTemplate.opsForValue().get(keyFailCount);
            int failCount = (failStr == null) ? 0 : Integer.parseInt(failStr);

            failCount++;
            redisTemplate.opsForValue().set(keyFailCount, String.valueOf(failCount),Duration.ofSeconds(120));
            if (failCount > 3) {
            savingsRequest.setStatus(SavingsRequestStatus.REJECTED);
            savingsRequestRepository.save(savingsRequest);
            log.error("Giao dịch thất bại vì nhập sai OTP quá 3 lần ");
            throw  new AppException(ErrorCode.OTP_WRONG_MANY);
            }
            throw new AppException(ErrorCode.INVALID_OTP);
        }
        ///  ma otp hop le luu vao account db va core db
        /// chuyen tien tu account paymen src to master

        /// chuyen thanh cong thi save account

         CommonTransactionDTO commonTransactionDTO = commonTransactionService.createAccountSaving(CreateAccountSavingRequest.builder()
                         .fromAccountNumber(savingsRequest.getAccountNumberSource())
                         .amount(savingsRequest.getInitialDeposit())
                         .currency("VND")
                         .description("Gửi tiền tiết kiệm")
                        .build());
         /// check trang thai giao dịch
        if(!commonTransactionDTO.getStatus().equals("COMPLETED")){
            throw new AppException(ErrorCode.UNCATERROR_ERROR);
        }

        Account account = Account.builder()
                .accountType(AccountType.SAVING)
                .cifCode(savingsRequest.getCifCode())
                .status(AccountStatus.ACTIVE)
                .build();
        account.setAccountNumber(generateAccountNumber(account));
        coreSavingAccountDTO coreSavingAccountDTO = com.example.common_service.dto.coreSavingAccountDTO.builder()
                .cifCode(account.getCifCode())
                .term(savingsRequest.getTerm())
                .initialDeposit(savingsRequest.getInitialDeposit())
                .accountNumber(account.getAccountNumber())
                .build();
        log.info("coreSavingAccountDTO: {}", coreSavingAccountDTO);
        //// dung restTemplate call API save account tren CoreBanking
        String url = "http://localhost:8083/corebanking/create-savings-account";
        restTemplate.postForObject(url ,coreSavingAccountDTO,Void.class);

        accountRepository.save(account);
        log.info("Mã otp hợp lệ . ");

        savingsRequest.setStatus(SavingsRequestStatus.APPROVED);
        savingsRequestRepository.save(savingsRequest);


        return SavingsRequestResponse.builder()
                .id(savingsRequest.getId())
                .cifCode(savingsRequest.getCifCode())
                .term(savingsRequest.getTerm())
                .initialDeposit(savingsRequest.getInitialDeposit())
                .status(SavingsRequestStatus.APPROVED)
                .accountNumberSource(savingsRequest.getAccountNumberSource())
                .interestRate(savingsRequest.getInterestRate())
                .build();

    }

    @Override
    public List<CoreTermDTO> getAllTerm() {
        String url = "http://localhost:8083/corebanking/get-all-term-isactive";
        ResponseEntity<List<CoreTermDTO>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<CoreTermDTO>>() {}
        );
        return response.getBody();
    }

    public String createOTP(String savingsRequestId) {
        String keyOTP = "OTP:"+savingsRequestId;
        String otp = String.valueOf(100000 + new Random().nextInt(900000));
        redisTemplate.opsForValue().set(keyOTP,otp, Duration.ofSeconds(3600));
        return otp;
    }
    public String generateAccountNumber(Account dto) {
        String cif = dto.getCifCode();
        int typeCode;
        if (dto.getAccountType().name().equals("PAYMENT")) {
            typeCode = 0;
        } else if (dto.getAccountType().name().equals("CREDIT")) {
            typeCode = 1;
        } else {
            typeCode = 2;
        }
        String randomPart = String.format("%03d", new Random().nextInt(1000));
        return cif + typeCode + randomPart;

    }
}

