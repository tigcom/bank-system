package com.example.account_service.consumer;

import com.example.account_service.dto.kafkaMessage.CardRegistrationMessage;
import com.example.account_service.dto.response.MasterCardResponse;
import com.example.account_service.dto.response.VisaCardResponse;
import com.example.account_service.entity.Account;
import com.example.account_service.entity.CreditAccount;
import com.example.account_service.entity.CreditCardType;
import com.example.account_service.entity.CreditRequest;
import com.example.account_service.exception.AppException;
import com.example.account_service.exception.ErrorCode;
import com.example.account_service.repository.CreditAccountRepository;
import com.example.account_service.repository.CreditCardTypeRepository;
import com.example.account_service.repository.CreditRequestRepository;
import com.example.account_service.service.CardRegistrationService;
import com.example.common_service.constant.AccountStatus;
import com.example.common_service.constant.CreditRequestStatus;
import com.example.common_service.dto.CoreAccountRequest;
import com.example.common_service.dto.CoreAccountUpdateStatusRequest;
import com.example.common_service.dto.CreditNotificationDTO;
import com.example.common_service.dto.CustomerDTO;
import com.example.common_service.services.CommonService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;


@Component
@RequiredArgsConstructor
@Slf4j
public class CardRegistrationConsumer {
    private final CreditRequestRepository creditRequestRepository;
    @Value("${core-banking.base-url:http://localhost:8083/corebanking}")
        private String coreBankingBaseUrl;
       private final CreditAccountRepository creditAccountRepository;
       private final RestTemplate restTemplate;
       private final CardRegistrationService cardRegistrationService;
    private final StreamBridge streamBridge;
    @DubboReference(timeout = 5000)
    private CommonService commonService;
    private final CreditCardTypeRepository creditCardTypeRepository;

    @KafkaListener(topics = "card-registration-topic", groupId = "card-registration-consumer",
                   containerFactory = "cardRegistrationKafkaListenerContainerFactory")
    public void handleCardRegistration(CardRegistrationMessage message) {
         log.info("Received Card Registration message: {}", message);
         
         try {
             // Gọi API tổ chức dựa trên loại thẻ với Resilience4j
             if ("VISA".equals(message.getCardType())) {
                 handleVisaRegistrationWithResilience4j(message);
             } else if ("MASTER".equals(message.getCardType())) {
                 // TODO: Implement MasterCard registration with Resilience4j
                 handleMasterRegistrationWithResilience4j(message);
                 log.info("MasterCard registration not implemented yet");
} else {
        log.warn("Unknown card type: {}", message.getCardType());
        updateCreditAccountStatus(message.getAccountNumber(), AccountStatus.REGISTRATION_FAILED);

        }
        } catch (Exception e) {
        log.error("Error processing card registration message: {}", e.getMessage(), e);
        updateCreditAccountStatus(message.getAccountNumber(), AccountStatus.REGISTRATION_ERROR);

        }
        }

private void handleVisaRegistrationWithResilience4j(CardRegistrationMessage message) {
        log.info("Processing VISA card registration with Resilience4j for account: {}", message.getAccountNumber());
        
        try {
            // Sử dụng CardRegistrationService với Resilience4j
            VisaCardResponse response = cardRegistrationService.registerVisaCard(message);
            
            if (response != null) {
                switch (response.getStatus()) {
                    case "SUCCESS":
                        log.info("VISA registration successful for account: {}. Message: {}", 
                               message.getAccountNumber(), response.getMessage());
                        CreditAccount account= updateCreditAccountStatus(message.getAccountNumber(), AccountStatus.ACTIVE);
                        //thay vi update thi toi luc nay mới lưu tren core
                        createCoreBankingCreditAccount(message.getAccountNumber());
                        updateCardDetails(message.getAccountNumber(), response);
                        sendApprovalNotification(account);
                        break;
                        
                    case "FAILED":
                        log.warn("VISA registration failed for account: {}. Error Code: {}, Reason: {}", 
                               message.getAccountNumber(), response.getErrorCode(), response.getMessage());
                        updateCreditAccountStatus(message.getAccountNumber(), AccountStatus.REGISTRATION_FAILED);
                        updateCreditRequestStatus(message.getIdRequest(), CreditRequestStatus.FAILED,response.getMessage(),message.getAccountNumber());
                        break;
                        
                    case "ERROR":
                        log.error("VISA registration error for account: {}. Error Code: {}, Error: {}", 
                                message.getAccountNumber(), response.getErrorCode(), response.getMessage());
                        updateCreditAccountStatus(message.getAccountNumber(), AccountStatus.REGISTRATION_ERROR);
                        updateCreditRequestStatus(message.getIdRequest(), CreditRequestStatus.ERROR,response.getMessage(),message.getAccountNumber());
                        break;
                    default:
                        log.warn("Unknown VISA response status for account: {}. Status: {}", 
                               message.getAccountNumber(), response.getStatus());
                        updateCreditAccountStatus(message.getAccountNumber(), AccountStatus.REGISTRATION_FAILED);
                        updateCreditRequestStatus(message.getIdRequest(), CreditRequestStatus.FAILED,response.getMessage(),message.getAccountNumber());
                }
            } else {
                log.error("Received null response from VISA registration service for account: {}", message.getAccountNumber());
                updateCreditAccountStatus(message.getAccountNumber(), AccountStatus.REGISTRATION_ERROR);
                updateCreditRequestStatus(message.getIdRequest(), CreditRequestStatus.ERROR,"Received null response from VISA registration service",message.getAccountNumber());

            }
        } catch (Exception e) {
            log.error("Unexpected error in VISA registration with Resilience4j for account: {}. Error: {}", 
                     message.getAccountNumber(), e.getMessage(), e);
            updateCreditAccountStatus(message.getAccountNumber(), AccountStatus.REGISTRATION_ERROR);
            updateCreditRequestStatus(message.getIdRequest(), CreditRequestStatus.ERROR,e.getMessage(),message.getAccountNumber());
        }
    }

    private void handleMasterRegistrationWithResilience4j(CardRegistrationMessage message) {
        log.info("Processing MASTER card registration with Resilience4j for account: {}", message.getAccountNumber());

        try {
            // Sử dụng CardRegistrationService với Resilience4j
            MasterCardResponse response = cardRegistrationService.registerMasterCard(message);

            if (response != null) {
                switch (response.getStatus()) {
                    case "SUCCESS":
                        log.info("Master registration successful for account: {}. Message: {}",
                                message.getAccountNumber(), response.getMessage());
                        CreditAccount account= updateCreditAccountStatus(message.getAccountNumber(), AccountStatus.ACTIVE);
                        createCoreBankingCreditAccount(message.getAccountNumber());
                        updateCardDetails(message.getAccountNumber(), response);
                        sendApprovalNotification(account);
                        break;

                    case "FAILED":
                        log.warn("Master registration failed for account: {}. Error Code: {}, Reason: {}",
                                message.getAccountNumber(), response.getErrorCode(), response.getMessage());
                        updateCreditAccountStatus(message.getAccountNumber(), AccountStatus.REGISTRATION_FAILED);
                        updateCreditRequestStatus(message.getIdRequest(), CreditRequestStatus.FAILED,response.getMessage(),message.getAccountNumber());
                        break;

                    case "ERROR":
                        log.error("Master registration error for account: {}. Error Code: {}, Error: {}",
                                message.getAccountNumber(), response.getErrorCode(), response.getMessage());
                        updateCreditAccountStatus(message.getAccountNumber(), AccountStatus.REGISTRATION_ERROR);
                        updateCreditRequestStatus(message.getIdRequest(), CreditRequestStatus.ERROR,response.getMessage(),message.getAccountNumber());
                        break;
                    default:
                        log.warn("Unknown Master response status for account: {}. Status: {}",
                                message.getAccountNumber(), response.getStatus());
                        updateCreditAccountStatus(message.getAccountNumber(), AccountStatus.REGISTRATION_FAILED);
                        updateCreditRequestStatus(message.getIdRequest(), CreditRequestStatus.FAILED,response.getMessage(),message.getAccountNumber());
                }
            } else {
                log.error("Received null response from Master registration service for account: {}", message.getAccountNumber());
                updateCreditAccountStatus(message.getAccountNumber(), AccountStatus.REGISTRATION_ERROR);
                updateCreditRequestStatus(message.getIdRequest(), CreditRequestStatus.ERROR,"Received null response from Master registration service",message.getAccountNumber());
            }
        } catch (Exception e) {
            log.error("Unexpected error in Master registration with Resilience4j for account: {}. Error: {}",
                    message.getAccountNumber(), e.getMessage(), e);
            updateCreditAccountStatus(message.getAccountNumber(), AccountStatus.REGISTRATION_ERROR);
            updateCreditRequestStatus(message.getIdRequest(), CreditRequestStatus.ERROR,e.getMessage(),message.getAccountNumber());
        }
    }
    private void updateCreditRequestStatus(String id, CreditRequestStatus status,String reson, String accountNumber) {
        CreditRequest creditRequestStatus = creditRequestRepository.findById(id).orElseThrow(() ->
                 new AppException(ErrorCode.CREDIT_REQUEST_NOTEXISTED));
        creditRequestStatus.setStatus(status);
        creditRequestStatus.setReason(reson);
        creditRequestStatus.setAccountNumber(accountNumber);
        creditRequestRepository.save(creditRequestStatus);
    }
    private CreditAccount updateCreditAccountStatus(String accountNumber, AccountStatus status) {
        try {
            CreditAccount account = creditAccountRepository.findByAccountNumber(accountNumber);
            if (account != null) {
                account.setStatus(status);
                creditAccountRepository.save(account);
                log.info("Updated credit account status to {} for account: {}", status, accountNumber);
            } else {
                log.warn("Credit account not found for account number: {}", accountNumber);
            }
            return account;
        } catch (Exception e) {
            log.error("Error updating credit account status for account: {}. Error: {}", 
                     accountNumber, e.getMessage(), e);
            throw new AppException(ErrorCode.UNCATERROR_ERROR);
        }
    }
    private void createCoreBankingCreditAccount(String accountNumber) {
        // Create simple account structure for Core Banking (only balance and status)
        CreditAccount account = creditAccountRepository.findByAccountNumber(accountNumber);
        if (account == null) {
            throw new AppException(ErrorCode.ACCOUNT_NOT_FOUND);
        }
        String url = coreBankingBaseUrl + "/save-account";
        try {
            CoreAccountRequest coreAccount = CoreAccountRequest.builder()
                    .accountNumber(account.getAccountNumber())
                    .cifCode(account.getCifCode())
                    .balance(BigDecimal.ZERO)
                    .accountType(account.getAccountType())
                    .status(AccountStatus.ACTIVE)
                    .build();
            log.info("corePaymentAccountDTO: {}", coreAccount);

            // Call API save account trên CoreBanking
            restTemplate.postForObject(url ,coreAccount,Void.class);
        } catch (Exception e) {
            log.error("Failed to create account in core banking system", e);
            throw new AppException(ErrorCode.CORE_BANKING_SERVICE_ERROR);
        }
    }
    
    private void updateCardDetails(String accountNumber, VisaCardResponse response) {
        try {
            CreditAccount account = creditAccountRepository.findByAccountNumber(accountNumber);
            if (account != null) {
                // Cập nhật thông tin thẻ từ response
                if (response.getCardNumber() != null) {
                    account.setCardNumber(response.getCardNumber());
                }
                if (response.getExpiryDate() != null) {
                    account.setExpiryDate(response.getExpiryDate());
                }
                if (response.getCardToken() != null) {
                    account.setCardToken(response.getCardToken());
                }
                if (response.getCardHolderName() != null) {
                    account.setCardHolderName(response.getCardHolderName());
                }
                creditAccountRepository.save(account);
                log.info("Updated card details for account: {}", accountNumber);
            } else {
                log.warn("Credit account not found for updating card details: {}", accountNumber);
            }
        } catch (Exception e) {
            log.error("Error updating card details for account: {}. Error: {}", 
                     accountNumber, e.getMessage(), e);
        }
    }
    private void updateCardDetails(String accountNumber, MasterCardResponse response) {
        try {
            CreditAccount account = creditAccountRepository.findByAccountNumber(accountNumber);
            if (account != null) {
                // Cập nhật thông tin thẻ từ response
                if (response.getCardNumber() != null) {
                    account.setCardNumber(response.getCardNumber());
                }
                if (response.getExpiryDate() != null) {
                    account.setExpiryDate(response.getExpiryDate());
                }
                if (response.getCardToken() != null) {
                    account.setCardToken(response.getCardToken());
                }
                if (response.getCardHolderName() != null) {
                    account.setCardHolderName(response.getCardHolderName());
                }
                creditAccountRepository.save(account);
                log.info("Updated card details for account: {}", accountNumber);
            } else {
                log.warn("Credit account not found for updating card details: {}", accountNumber);
            }
        } catch (Exception e) {
            log.error("Error updating card details for account: {}. Error: {}",
                    accountNumber, e.getMessage(), e);
        }
    }
    private void sendApprovalNotification(CreditAccount account) {
        try {
            CustomerDTO customer = commonService.getCustomerByCifCode(account.getCifCode());

            CreditNotificationDTO notification = CreditNotificationDTO.builder()
                    .customerName(customer.getFullName())
                    .customerEmail(customer.getEmail())
                    .accountNumber(account.getAccountNumber())
                    .templateType("approval")
                    .subject("🎉 Chúc mừng! Yêu cầu thẻ tín dụng được phê duyệt - Ngân hàng ABC")
                    .build();

            streamBridge.send("send-credit-notification", notification);
            log.info("Approval notification sent to: {}", customer.getEmail());
        } catch (Exception e) {
            log.error("Failed to send approval notification", e);
        }
    }
}
