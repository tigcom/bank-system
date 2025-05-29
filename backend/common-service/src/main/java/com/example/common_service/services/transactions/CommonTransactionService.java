package com.example.common_service.services.transactions;

import com.example.common_service.dto.request.*;
import com.example.common_service.dto.CommonTransactionDTO;

public interface CommonTransactionService {
    CommonTransactionDTO loanPayment(PayRepaymentRequest paymentRequest);

    CommonTransactionDTO loanDisbursement(CommonDisburseRequest disburseRequest);

    CommonTransactionDTO deposit(CommonDepositRequest depositRequest);

    CommonTransactionDTO createAccountSaving(CreateAccountSavingRequest accountSavingRequest);

    CommonTransactionDTO confirmTransaction(CommonConfirmTransactionRequest confirmTransactionRequest);

}
