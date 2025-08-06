package com.example.common_service.services.account;

import com.example.common_service.dto.AccountDTO;
import com.example.common_service.dto.CustomerDTO;
import com.example.common_service.dto.request.LoanRequestDTO;
import com.example.common_service.dto.response.AccountPaymentResponse;

import java.math.BigDecimal;
import java.util.List;


public interface AccountDubboService {
    AccountDTO createLoanAccount(LoanRequestDTO dto);

    // Thêm method để cập nhật account khi update loan
    void updateAccountFromLoan(LoanRequestDTO dto);
    /**
     * Chỉ cập nhật dư nợ cho loan account
     */
    void updateOutstandingDebt(Long loanId, BigDecimal outstandingDebt);
}
