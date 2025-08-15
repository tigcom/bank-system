package com.example.loan_service.service;

import com.example.loan_service.workflow.LoanApprovalResult;
import com.example.loan_service.workflow.LoanApprovalWorkflow;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoanWorkflowService {

    private final WorkflowClient workflowClient;

    public LoanApprovalResult startLoanApprovalWorkflow(Long loanId) {
        log.info("Starting loan approval workflow for loanId: {}", loanId);
        
        WorkflowOptions options = WorkflowOptions.newBuilder()
                .setTaskQueue("LoanApprovalTaskQueue")
                .setWorkflowId("loan-approval-" + loanId)
                .setWorkflowExecutionTimeout(Duration.ofMinutes(30))
                .setWorkflowRunTimeout(Duration.ofMinutes(25))
                .build();

        LoanApprovalWorkflow workflow = workflowClient.newWorkflowStub(LoanApprovalWorkflow.class, options);
        
        try {
            LoanApprovalResult result = workflow.approveLoan(loanId);
            log.info("Loan approval workflow completed for loanId: {}, success: {}", loanId, result.isSuccess());
            return result;
        } catch (Exception e) {
            log.error("Loan approval workflow failed for loanId: {}, error: {}", loanId, e.getMessage());
            throw e;
        }
    }

    public LoanApprovalResult startLoanApprovalWorkflowAsync(Long loanId) {
        log.info("Starting async loan approval workflow for loanId: {}", loanId);
        
        WorkflowOptions options = WorkflowOptions.newBuilder()
                .setTaskQueue("LoanApprovalTaskQueue")
                .setWorkflowId("loan-approval-" + loanId)
                .setWorkflowExecutionTimeout(Duration.ofMinutes(30))
                .setWorkflowRunTimeout(Duration.ofMinutes(25))
                .build();

        LoanApprovalWorkflow workflow = workflowClient.newWorkflowStub(LoanApprovalWorkflow.class, options);
        
        // Start workflow asynchronously
        workflow.approveLoan(loanId);
        
        log.info("Async loan approval workflow started for loanId: {}", loanId);
        
        // Return a placeholder result indicating the workflow has started
        return LoanApprovalResult.builder()
                .loanId(loanId)
                .status("STARTED")
                .success(true)
                .build();
    }
} 