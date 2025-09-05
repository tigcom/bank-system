package com.example.account_service.service;

import com.example.account_service.dto.request.PaymentConfirmOtpDTO;
import com.example.account_service.dto.request.PaymentCreateDTO;
import com.example.account_service.dto.response.AccountCreateReponse;
import com.example.account_service.dto.response.CicResponse;
import com.example.account_service.dto.response.CreditRequestReponse;
import com.example.account_service.dto.response.PaymentRequestResponse;
import com.example.common_service.dto.AccountDTO;
import com.example.common_service.dto.CreditCardDTO;
import com.example.common_service.dto.CustomerDTO;
import com.example.common_service.dto.request.LoanRequestDTO;
import com.example.common_service.dto.response.AccountPaymentResponse;
import com.example.common_service.dto.response.AccountSummaryDTO;
import com.example.common_service.dto.response.CreditAccountResponse;
import com.example.common_service.dto.response.SavingAccountResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

public interface AccountService {

    PaymentRequestResponse createPaymentRequest(String cifcode);

    AccountCreateReponse confirmOtpAndCreatePayment(PaymentConfirmOtpDTO paymentConfirmOtpDTO);

    void resendPaymentOtp(String tempRequestKey);

    List<AccountSummaryDTO> getAllAccountsbyCifCode();

    List<AccountPaymentResponse> getAllPaymentAccountsbyCifCode();

    List<AccountPaymentResponse> getAllPaymentAccountsbyCifCode2();

    AccountPaymentResponse getAccountPaymentbyID(String id);

    List<SavingAccountResponse> getAllSavingAccountbyCifCode();

    List<CreditAccountResponse> getAllCreditAccountbyCifCode();
    List<CreditAccountResponse> getAllCreditAccountNonbyCifCode();
    List<CreditCardDTO> getAllCreditCard();

    CicResponse checkCIC(String idNumber);
    PaymentRequestResponse createPaymentInit(PaymentCreateDTO paymentRequest);
    CustomerDTO getCustomerByAccountNumber(String accountNumber);


    List<CreditRequestReponse> getAllCreditRequestPending();


    AccountCreateReponse createPaymentAccountForCustomer(String cifCode);

    BigDecimal getBalanceFromCorebanking(String accountNumber);

    Page<CreditRequestReponse> getAllCreditRequestPendingPaginated(Pageable pageable);

    AccountDTO createLoanAccount (LoanRequestDTO dto) ;

    /**
     * Cập nhật dư nợ tài khoản vay dựa trên thông tin khoản vay
     */
    void updateAccountFromLoan(LoanRequestDTO dto);

    List<com.example.common_service.dto.response.LoanAccountResponse> getAllLoanAccountsByCifCode();

    com.example.common_service.dto.response.LoanAccountResponse getLoanAccountByNumber(String accountNumber);

    void updateLoanOutstandingDebt(String accountNumber, java.math.BigDecimal newOutstandingDebt);

    void closeLoanAccount(String accountNumber);
}