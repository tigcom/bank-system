package com.example.corebanking_service.service;

import com.example.common_service.dto.customer.CoreCustomerDTO;
import com.example.common_service.dto.customer.CoreResponse;
import com.example.corebanking_service.dto.request.LoanRequestDTO;

public interface CoreLoanService {
    CoreResponse syncCoreLoan (LoanRequestDTO loan);
}
