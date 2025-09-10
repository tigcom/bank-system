package com.example.loan_service.workflow;

import com.example.common_service.constant.LoanType;
import com.example.common_service.dto.request.CommonDisburseRequest;
import com.example.common_service.dto.request.LoanRequestDTO;
import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.workflow.Workflow;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.rpc.RpcContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Duration;

@Slf4j
public class LoanApprovalWorkflowImpl implements LoanApprovalWorkflow {

    private final LoanApprovalActivities activities = Workflow.newActivityStub(
        LoanApprovalActivities.class,
        ActivityOptions.newBuilder()
            .setStartToCloseTimeout(Duration.ofMinutes(5))
            .setRetryOptions(RetryOptions.newBuilder().setInitialInterval(Duration.ofSeconds(1)).build()).build()
    );

    @Override
    public LoanApprovalResult approveLoan(Long loanId, String username) {
        log.info("Starting loan approval workflow for loanId: {}", loanId);
        RpcContext.getClientAttachment().setAttachment("username", username);
        log.info("USERNAME - : {}", username);
        try {
            // Bước 1: Validate loan
            log.info("Step 1: Validating loan {}", loanId);
            LoanRequestDTO loanData = activities.validateLoan(loanId);
            
            String accountNumber;
            String transactionRef = null;
            if (loanData.getLoanType() == LoanType.PERSONAL) {
                // Bước 2: Tạo tài khoản vay
                log.info("Step 2: Creating loan account for loan {}", loanId);
                accountNumber = activities.createLoanAccount(loanData, username);
                
                // Bước 3: Thực hiện giải ngân đến tài khoản vay vừa tạo
                log.info("Step 3: Disbursing loan {} to created loan account", loanId);
                CommonDisburseRequest disburseReq = new CommonDisburseRequest();
                disburseReq.setToAccountNumber(accountNumber);
                disburseReq.setAmount(loanData.getAmount());
                disburseReq.setCurrency("VND");
                transactionRef = activities.disburseLoan(disburseReq, username);
                // Bước 4: Approve loan trong database
                log.info("Step 4: Approving loan in database {}", loanId);
                activities.approveLoanInDatabase(loanId,accountNumber);
            } else {
                // AUTO / MORTGAGE: không tạo loan account. Giải ngân trực tiếp theo Vault
                log.info("Step 2: Resolving disbursement account via Vault for loan {}", loanId);
                String targetAccount = activities.resolveDisbursementAccount(loanData.getLoanType());
                if (targetAccount == null || targetAccount.isEmpty()) {
                    throw new IllegalStateException("No target disbursement account configured for loan type: " + loanData.getLoanType());
                }
                // Thực hiện giải ngân trực tiếp
                log.info("Step 3: Disbursing loan {} directly to {}", loanId, targetAccount);
                CommonDisburseRequest disburseReq = new CommonDisburseRequest();
                disburseReq.setToAccountNumber(targetAccount);
                disburseReq.setAmount(loanData.getAmount());
                disburseReq.setCurrency("VND");
                transactionRef = activities.disburseLoan(disburseReq, username);
                // Cập nhật approve trong DB nhưng không có loan account => lưu số tài khoản giải ngân là targetAccount
                log.info("Step 4: Approving loan in database {} (no loan account created)", loanId);
                activities.approveLoanInDatabase(loanId, targetAccount);
                accountNumber = targetAccount;
            }
            // Bước 5: Tạo lịch trả nợ
            log.info("Step 5: Generating repayment schedule for loan {}", loanId);
            activities.generateRepaymentSchedule(loanId);
            // Bước 6: Gửi thông báo
            log.info("Step 6: Sending approval notification for loan {}", loanId);
            activities.sendApprovalNotification(loanId, accountNumber);
            
            log.info("Loan approval workflow completed successfully for loanId: {}", loanId);
            
            return LoanApprovalResult.builder()
                .loanId(loanId)
                .status("APPROVED")
                .disbursementAccountNumber(accountNumber)
                .amount(loanData.getAmount())
                .transactionReference(transactionRef)
                .approvedAt(java.time.LocalDateTime.now())
                .success(true)
                .build();
                
        } catch (Exception e) {
            log.error("Loan approval workflow failed for loanId: {}, error: {}", loanId, e.getMessage());

            try {
                activities.rollbackLoanApproval(loanId);
            } catch (Exception rollbackEx) {
                log.error("Rollback failed for loanId: {}, error: {}", loanId, rollbackEx.getMessage());
            }
            
            return LoanApprovalResult.builder()
                .loanId(loanId)
                .status("FAILED")
                .errorMessage(e.getMessage())
                .success(false)
                .build();
        }
    }
} 