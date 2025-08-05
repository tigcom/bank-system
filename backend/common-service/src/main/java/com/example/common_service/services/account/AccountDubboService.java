package com.example.common_service.services.account;

import com.example.common_service.dto.AccountDTO;
import com.example.common_service.dto.CustomerDTO;
import com.example.common_service.dto.request.LoanRequestDTO;
import com.example.common_service.dto.response.AccountPaymentResponse;

import java.util.List;


public interface AccountDubboService {
    AccountDTO createLoanAccount(LoanRequestDTO dto);

    // Thêm method để cập nhật account khi update loan
    void updateAccountFromLoan(LoanRequestDTO dto);
}
