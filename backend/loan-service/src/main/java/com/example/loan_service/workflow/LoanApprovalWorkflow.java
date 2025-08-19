package com.example.loan_service.workflow;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface LoanApprovalWorkflow {
    
    @WorkflowMethod
    LoanApprovalResult approveLoan(Long loanId, String username);
} 