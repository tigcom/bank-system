package com.example.loan_service.service;


import com.example.common_service.dto.customer.CoreResponse;
import com.example.loan_service.dto.request.LoanRequestDTO;
import org.springframework.stereotype.Service;

public interface CoreBankingClient {
    CoreResponse syncLoan(LoanRequestDTO dto);
    void deleteLoan(long id);
}

