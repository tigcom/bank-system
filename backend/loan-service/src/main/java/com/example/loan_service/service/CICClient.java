package com.example.loan_service.service;


import com.example.common_service.dto.customer.CoreResponse;
import com.example.common_service.dto.request.CICRequest;
import com.example.loan_service.dto.response.CicResponse;
import com.example.loan_service.dto.response.LoanResponseDTO;

public interface CICClient {
    CicResponse checkCIC(CICRequest cicRequest);
}

