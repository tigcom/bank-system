package com.example.corebanking_service.service.impl;

import com.example.common_service.dto.customer.CoreCustomerDTO;
import com.example.common_service.dto.customer.CoreResponse;
import com.example.corebanking_service.dto.request.LoanRequestDTO;
import com.example.corebanking_service.entity.CoreLoan;
import com.example.corebanking_service.repository.CoreAccountRepo;
import com.example.corebanking_service.repository.CoreLoanRepo;
import com.example.corebanking_service.service.CoreLoanService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CoreLoanServiceImpl implements CoreLoanService {
    private final CoreLoanRepo coreLoanRepo;
    private final CoreAccountRepo coreAccountRepo;

    @Override
    public CoreResponse syncCoreLoan(LoanRequestDTO dto) {
        try{
            CoreLoan loan = new CoreLoan();
            loan.setLoanId(dto.getLoanId());
            loan.setCoreAccount(coreAccountRepo.findByAccountNumber(dto.getAccountNumber()));
            loan.setLoanAmount(dto.getAmount());
            loan.setInterestRate(dto.getInterestRate());
            loan.setTermMonths(dto.getTermMonths());
            loan.setStartDate(dto.getStartDate());
            loan.setStatus(dto.getStatus());
            coreLoanRepo.save(loan);

        }catch (Exception e){
            return new CoreResponse(false, e.getMessage());
        }
        return new CoreResponse(true,"Succes sync");
    }
}
