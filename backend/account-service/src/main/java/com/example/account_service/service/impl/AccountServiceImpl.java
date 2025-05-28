package com.example.account_service.service.impl;

import com.example.account_service.dto.request.PaymentCreateDTO;
import com.example.account_service.dto.request.SavingCreateDTO;
import com.example.account_service.dto.response.AccountCreateReponse;
import com.example.account_service.entity.Account;
import com.example.account_service.exception.AppException;
import com.example.account_service.exception.ErrorCode;
import com.example.account_service.mapper.AccountMapper;
import com.example.account_service.repository.AccountRepository;
import com.example.account_service.service.AccountService;
import com.example.common_service.constant.AccountStatus;
import com.example.common_service.constant.AccountType;
import com.example.common_service.constant.CustomerStatus;
import com.example.common_service.dto.CorePaymentAccountDTO;
import com.example.common_service.dto.CustomerDTO;
import com.example.common_service.dto.coreSavingAccountDTO;
import com.example.common_service.dto.response.AccountSummaryDTO;
import com.example.common_service.services.CommonService;
import com.example.common_service.services.CommonServiceCore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Random;

@Service
@Slf4j
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {
    private final AccountRepository accountRepository;

    @DubboReference(timeout = 5000)
    private final CommonService commonService;

    @DubboReference(timeout = 5000)
    private final CommonServiceCore commonServiceCore;

    private final RestTemplate restTemplate;


    @Override
    public AccountCreateReponse createPayment() {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName();
        CustomerDTO currentCustomer = commonService.getCurrentCustomer(userId);
        String token = ((JwtAuthenticationToken) SecurityContextHolder.getContext().getAuthentication()).getToken().getTokenValue();
        log.info("Current Customer : {}", currentCustomer);

        log.info("Create payment request received");

        if (currentCustomer.getStatus() == CustomerStatus.ACTIVE) {
            Account account = Account.builder()
                    .accountType(AccountType.PAYMENT)
                    .cifCode(currentCustomer.getCifCode())
                    .status(AccountStatus.ACTIVE)
                    .build();
            account.setAccountNumber(generateAccountNumber(account));
            log.info("Account : " + account);

            CorePaymentAccountDTO corePaymentAccountDTO = CorePaymentAccountDTO.builder()
                    .cifCode(account.getCifCode())
                    .accountNumber(account.getAccountNumber())
                    .build();
            log.info("corePaymentAccountDTO: {}", corePaymentAccountDTO);
            //dung dubbo luu account payment len core
//            commonServiceCore.createCoreAccountPayment(corePaymentAccountDTO);

            //// dung restTemplate call API save account tren CoreBanking
            String url = "http://localhost:8083/corebanking/create-payment-account";
            restTemplate.postForObject(url ,corePaymentAccountDTO,Void.class);

            accountRepository.save(account);

            return AccountCreateReponse.builder()
                    .accountNumber(account.getAccountNumber())
                    .cifCode(account.getCifCode())
                    .id(account.getId())
                    .accountType(account.getAccountType())
                    .status(account.getStatus())
                    .build();
        }
        throw new AppException(ErrorCode.CUSTOMER_NOTACTIVE);
    }

    @Override
    public AccountCreateReponse createSaving(SavingCreateDTO savingCreateDTO) {
        log.info(" Recive create saving account request");
        /// Get Current Customer
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName();
        log.info("User id "+ userId);
        CustomerDTO currentCustomer = commonService.getCurrentCustomer(userId);
//        boolean isActive = commonService.checkCustomer(savingCreateDTO.getCifCode());
        //Check customer stattus
        if (currentCustomer.getStatus() == CustomerStatus.ACTIVE) {
            ///  Create account
            Account account = Account.builder()
                    .accountType(savingCreateDTO.getAccountType())
                    .cifCode(currentCustomer.getCifCode())
                    .status(AccountStatus.ACTIVE)
                    .build();
            account.setAccountNumber(generateAccountNumber(account));

            ///  got accountnumberSource from DTO

            log.info("Account number Soucre : " + savingCreateDTO.getAccountNumberSource());

            coreSavingAccountDTO coreSavingAccountDTO = com.example.common_service.dto.coreSavingAccountDTO.builder()
                    .cifCode(account.getCifCode())
                    .term(savingCreateDTO.getTerm())
                    .initialDeposit(savingCreateDTO.getInitialDeposit())
                    .accountNumber(account.getAccountNumber())
                    .build();
            log.info("coreSavingAccountDTO: {}", coreSavingAccountDTO);

            /// dung dubbo goi ham save saving account tren core banking
            commonServiceCore.createCoreAccountSaving(coreSavingAccountDTO);

            ///  dung restTemplate call api saving account tren core banking
            //// dung restTemplate call API save account tren CoreBanking
            String url = "http://localhost:8083/corebanking/create-savings-account";
            restTemplate.postForObject(url ,coreSavingAccountDTO,Void.class);
            accountRepository.save(account);

            return AccountCreateReponse.builder()
                    .accountNumber(account.getAccountNumber())
                    .cifCode(account.getCifCode())
                    .id(account.getId())
                    .accountType(account.getAccountType())
                    .status(account.getStatus())
                    .build();
        }
        throw new AppException(ErrorCode.CUSTOMER_NOTACTIVE);
    }

    public List<AccountSummaryDTO> getAllAccountsbyCifCode() {
        // Lấy thông tin người dùng từ context bảo mật
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName();
        log.info("User id: " + userId);

        // Lấy thông tin khách hàng hiện tại
        CustomerDTO currentCustomer = commonService.getCurrentCustomer(userId);

        // Tạo URL gọi tới corebanking
        String url = "http://localhost:8083/corebanking/get-all-account-by-cifcode/" + currentCustomer.getCifCode();

        // Gửi request GET và nhận về danh sách AccountSummaryDTO
        ResponseEntity<List<AccountSummaryDTO>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<AccountSummaryDTO>>() {}
        );

        // Trả về danh sách
        return response.getBody();
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
