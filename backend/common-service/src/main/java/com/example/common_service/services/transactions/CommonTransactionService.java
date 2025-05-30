package com.example.common_service.services.transactions;

import com.example.common_service.dto.request.CommonConfirmTransactionRequest;
import com.example.common_service.dto.request.CommonDepositRequest;
import com.example.common_service.dto.request.CommonDisburseRequest;
import com.example.common_service.dto.request.PayRepaymentRequest;
import com.example.common_service.dto.CommonTransactionDTO;

public interface CommonTransactionService {
    CommonTransactionDTO loanPayment(PayRepaymentRequest paymentRequest);

    CommonTransactionDTO loanDisbursement(CommonDisburseRequest disburseRequest);

    CommonTransactionDTO deposit(CommonDepositRequest depositRequest);

    CommonTransactionDTO confirmTransaction(CommonConfirmTransactionRequest confirmTransactionRequest);

}
