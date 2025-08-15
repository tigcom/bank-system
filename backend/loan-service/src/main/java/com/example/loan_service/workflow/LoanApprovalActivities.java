package com.example.loan_service.workflow;

import com.example.common_service.dto.request.CommonDisburseRequest;
import com.example.common_service.dto.request.LoanRequestDTO;
import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

@ActivityInterface
public interface LoanApprovalActivities {
    
    @ActivityMethod
    LoanRequestDTO validateLoan(Long loanId);
    
    @ActivityMethod
    String createLoanAccount(LoanRequestDTO loanData);
    
    @ActivityMethod
    String disburseLoan(CommonDisburseRequest disburseReq );
    
    @ActivityMethod
    void approveLoanInDatabase(Long loanId);
    
    @ActivityMethod
    void generateRepaymentSchedule(Long loanId);
    
    @ActivityMethod
    void sendApprovalNotification(Long loanId, String accountNumber);
    
    @ActivityMethod
    void rollbackLoanApproval(Long loanId);
} 