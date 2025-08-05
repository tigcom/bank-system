package com.example.loan_service.service;


import com.example.common_service.dto.CoreAccountRequest;
import com.example.common_service.dto.customer.CoreResponse;
import com.example.loan_service.dto.request.LoanRequestDTO;
import com.example.loan_service.dto.response.LoanResponseDTO;
import org.springframework.stereotype.Service;

public interface CoreBankingClient {
    CoreResponse updateAccount(CoreAccountRequest request);
    void deleteLoan(long id);
}

