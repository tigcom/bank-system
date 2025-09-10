package com.example.loan_service.handler;

import com.example.loan_service.models.LoanStatus;
import com.example.common_service.constant.LoanType;
import com.example.common_service.dto.*;
import com.example.common_service.constant.CustomerStatus;
import com.example.common_service.dto.request.*;
import com.example.common_service.dto.response.AccountPaymentResponse;
import com.example.common_service.services.CommonService;
import com.example.common_service.services.account.AccountDubboService;
import com.example.common_service.services.account.AccountQueryService;
import com.example.common_service.services.customer.CustomerCommonService;
import com.example.common_service.services.customer.CustomerQueryService;
import com.example.common_service.services.transactions.CommonTransactionService;
import com.example.loan_service.dto.request.LoanRejectionReasonRequestDTO;
import com.example.common_service.dto.request.LoanRequestDTO;
import com.example.loan_service.dto.response.CicResponse;
import com.example.loan_service.entity.Loan;
import com.example.loan_service.entity.LoanRejectionReason;
import com.example.loan_service.entity.Repayment;
import com.example.loan_service.handler.LoanHandler;
import com.example.loan_service.mapper.CoreAccountMapper;
import com.example.loan_service.mapper.LoanMapper;
import com.example.loan_service.mapper.RepaymentMapper;
import com.example.loan_service.models.RepaymentStatus;
import com.example.loan_service.response.ApiResponseWrapper;
import com.example.loan_service.service.*;
import com.example.loan_service.workflow.LoanApprovalResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.rpc.RpcContext;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.security.concurrent.DelegatingSecurityContextExecutor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.cache.annotation.Cacheable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Slf4j
@Component
@RequiredArgsConstructor
public class LoanHandler {
    private final StreamBridge streamBridge;
    private final LoanService loanService;
    private final LoanMapper loanMapper;
    private final CoreBankingClient coreBankingClient;
    private final RepaymentService repaymentService;
    private final CICClient cicClient;
    private final LoanRejectionReasonService loanRejectionReasonService;
    private final LoanWorkflowService loanWorkflowService;
    @DubboReference private final CustomerQueryService customerQueryService;
    @DubboReference private final AccountQueryService accountQueryService;
    @DubboReference private final CommonTransactionService commonTransactionService;
    @DubboReference private final CommonService commonService;
    @DubboReference private final AccountDubboService accountDubboService;
    @DubboReference private final CustomerCommonService customerCommonService;

    private String getCurrentUserIdSafe() {
        try {
            return Optional.ofNullable(customerQueryService.getCurrentCustomer())
                .map(com.example.common_service.dto.response.CustomerResponse::getUserId)
                .orElse("UNKNOWN");
        } catch (Exception ex) {
            log.warn("GET_CURRENT_USER_ID_FAILED - {}", ex.getMessage());
            return "UNKNOWN";
        }
    }

    public Loan approveLoan(Long loanId) {
        log.info("APPROVE_LOAN_HANDLER_START - userId: {}, loanId: {}", getCurrentUserIdSafe(), loanId);
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication != null ? authentication.getName() : null;
            RpcContext.getClientAttachment().setAttachment("username", username);
            log.info("USERNAME - : {}", username);
            LoanApprovalResult result = loanWorkflowService.startLoanApprovalWorkflow(loanId, username);
            
            if (!result.isSuccess()) {
                throw new IllegalArgumentException("Loan approval failed: " + result.getErrorMessage());
            }
            Loan loan = loanService.getLoanById(loanId);
            log.info("APPROVE_LOAN_HANDLER_SUCCESS - loanId: {}, accountNumber: {}", loanId, result.getDisbursementAccountNumber());
            return loan;
            
        } catch (IllegalArgumentException e) {
            log.error("APPROVE_LOAN_HANDLER_INVALID - loanId: {}, error: {}", loanId, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("APPROVE_LOAN_HANDLER_ERROR - loanId: {}, error: {}", loanId, e.getMessage(), e);
            throw e;
        }
    }

    public LoanApprovalResult approveLoanAsync(Long loanId) {
        log.info("APPROVE_LOAN_ASYNC_HANDLER_START - userId: {}, loanId: {}", getCurrentUserIdSafe(), loanId);
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication != null ? authentication.getName() : null;
            // Sử dụng Temporal Workflow để approve loan bất đồng bộ
            LoanApprovalResult result = loanWorkflowService.startLoanApprovalWorkflowAsync(loanId, username);
            
            log.info("APPROVE_LOAN_ASYNC_HANDLER_SUCCESS - loanId: {}, status: {}", loanId, result.getStatus());
            return result;
            
        } catch (Exception e) {
            log.error("APPROVE_LOAN_ASYNC_HANDLER_ERROR - loanId: {}, error: {}", loanId, e.getMessage(), e);
            throw e;
        }
    }
    public Loan createLoan(LoanRequestDTO dto) {
        log.info("CREATE_LOAN_HANDLER_START - userId: {}, request: {}", getCurrentUserIdSafe(), dto);
        try {
            Long customerId = getCustomerId();
            log.debug("CUSTOMER_ID_FETCHED - {}", customerId);
            CustomerResponseDTO customer = customerQueryService.getCustomerById(customerId);
            log.info("CUSTOMER_INFO - idNumber={}, status={}", customer.getIdentityNumber(), customer.getStatus());
            AccountDTO repaymentAccount = accountQueryService.getAccountByAccountNumber(dto.getRepaymentAccountNumber());
            log.info("ACCOUNT_INFO - repaymentAccount={}, status={}", dto.getRepaymentAccountNumber(), repaymentAccount.getStatus());
            if (!CustomerStatus.ACTIVE.equals(customer.getStatus())) {
                throw new IllegalArgumentException("Hồ sơ khách hàng không hợp lệ");
            }
            if (Period.between(customer.getDateOfBirth(), LocalDate.now()).getYears() <= 18) {
                throw new IllegalArgumentException("Người dùng chưa đủ tuổi");
            }
            if (!"ACTIVE".equalsIgnoreCase(repaymentAccount.getStatus())) {
                throw new IllegalArgumentException("Tài khoản thanh toán không hợp lệ");
            }
            CICRequest cicRequest = new CICRequest();
            cicRequest.setIdNumber(customer.getIdentityNumber());
            cicRequest.setName(customer.getFullName());
            log.info("CALLING_CIC - CICRequest: {}", cicRequest);
            CicResponse cicResponse = cicClient.checkCIC(cicRequest);
            log.info("CIC_RESPONSE - status={}, creditScore={}, overdue={}, debtGroup={}, errorCode={}, message={}",
                    cicResponse.getStatus(),
                    cicResponse.getCreditScore(),
                    cicResponse.getOverdue(),
                    cicResponse.getDebtGroup(),
                    cicResponse.getErrorCode(),
                    cicResponse.getMessage()
            );
            if (!"success".equalsIgnoreCase(cicResponse.getStatus())) {
                throw new IllegalArgumentException("Không thể truy vấn CIC");
            }
            int score = cicResponse.getCreditScore();
            boolean overdue = cicResponse.getOverdue();
            int group = cicResponse.getDebtGroup();
            if (overdue) throw new IllegalArgumentException("Khách hàng đang có nợ quá hạn theo CIC");
            if (group >= 2) throw new IllegalArgumentException("Khách hàng thuộc nhóm nợ xấu (nhóm " + group + ")");
            if (score < 700 || group == 1)
                log.warn("CIC warning: Khách hàng có điểm tín dụng trung bình hoặc nhóm nợ cần chú ý");
            log.info("CIC PASS - Khách hàng đủ điều kiện tín dụng");
            Loan l = loanMapper.toEntity(dto);
            l.setCustomerId(customerId);
            l.setCreatedAt(LocalDateTime.now());
            Loan r = loanService.createLoan(l);
            log.info("CREATE_LOAN_HANDLER_SUCCESS - loanId: {}", l.getLoanId());
            return r;
        } catch (IllegalArgumentException e) {
            log.error("CREATE_LOAN_HANDLER_INVALID - dto={}, error: {}", dto, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("CREATE_LOAN_HANDLER_ERROR - dto={}, error: {}", dto, e.getMessage(), e);
            throw new RuntimeException("Xảy ra lỗi khi xử lý khoản vay", e);
        }
    }
    public RuntimeException createLoanFallback(LoanRequestDTO dto, Throwable t) {
        log.warn("CREATE_LOAN_HANDLER_FALLBACK - dto: {}, error: {}", dto, t.getMessage());
        return new RuntimeException("Loan creation service is temporarily unavailable", t);
    }

    public Loan updateLoan(LoanRequestDTO dto) {
        log.info("UPDATE_LOAN_HANDLER_START - userId: {}, request: {}", getCurrentUserIdSafe(), dto);
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        RpcContext.getClientAttachment().setAttachment("username", username);
        try {
            Loan loan = loanMapper.toEntity(dto);
            Loan updatedLoan = loanService.updateLoan(loan);
            log.info("UPDATE_LOAN_HANDLER_SUCCESS - loanId: {}", updatedLoan.getLoanId());
            return updatedLoan;
        } catch (Exception e) {
            log.error("UPDATE_LOAN_HANDLER_ERROR - error: {}", e.getMessage(), e);
            throw e;
        }
    }
    public Loan getLoanById(Long loanId) {
        log.info("GET_LOAN_BY_ID_HANDLER_START - userId: {}, loanId: {}", getCurrentUserIdSafe(), loanId);
        try {
            Loan loan = loanService.getLoanById(loanId);
            log.info("GET_LOAN_BY_ID_HANDLER_SUCCESS - found: {}", loan.getLoanId());
            return loan;
        } catch (Exception e) {
            log.error("GET_LOAN_BY_ID_HANDLER_ERROR - loanId: {}, error: {}", loanId, e.getMessage(), e);
            throw e;
        }
    }
    public List<Loan> getLoansByCustomerId() {
        log.info("GET_LOANS_BY_CUSTOMER_HANDLER_START - userId: {}", getCurrentUserIdSafe());
        try {
            Long customerId = getCustomerId();
            List<Loan> list = loanService.getLoansByCustomerId(customerId);
            log.info("GET_LOANS_BY_CUSTOMER_HANDLER_SUCCESS - count: {}", list.size());
            return list;
        } catch (Exception e) {
            log.error("GET_LOANS_BY_CUSTOMER_HANDLER_ERROR - error: {}", e.getMessage(), e);
            throw e;
        }
    }
    public Loan closedLoan(Long loanId) {
        log.info("CLOSE_LOAN_HANDLER_START - userId: {}, loanId: {}", getCurrentUserIdSafe(), loanId);
        try {
            Loan loan = loanService.closedLoan(loanId);
            com.example.common_service.dto.CoreAccountRequest coreAccountRequest = com.example.loan_service.mapper.CoreAccountMapper.INSTANCE.fromLoan(loan);
            coreBankingClient.updateAccount(coreAccountRequest);
            log.info("CLOSE_LOAN_HANDLER_SUCCESS - loanId: {}", loanId);
            return loan;
        } catch (Exception e) {
            log.error("CLOSE_LOAN_HANDLER_ERROR - loanId: {}, error: {}", loanId, e.getMessage(), e);
            throw e;
        }
    }
    public Loan rejectedLoan(Long loanId, LoanRejectionReasonRequestDTO req) {
        log.info("REJECT_LOAN_HANDLER_START - userId: {}, loanId: {}, reason: {}", getCurrentUserIdSafe(), loanId, req.getReason());
        try {
            Loan loan = loanService.rejectedLoan(loanId);
            LoanRejectionReason reason = new LoanRejectionReason();
            reason.setReason(req.getReason());
            reason.setLoan(loan);
            loanRejectionReasonService.save(reason);
            log.info("REJECT_LOAN_HANDLER_SUCCESS - loanId: {}", loanId);
            return loan;
        } catch (Exception e) {
            log.error("REJECT_LOAN_HANDLER_ERROR - loanId: {}, error: {}", loanId, e.getMessage(), e);
            throw e;
        }
    }
    public List<Loan> findall() {
        log.info("FIND_ALL_LOANS_HANDLER_START - userId: {}", getCurrentUserIdSafe());
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            JwtAuthenticationToken jwt = (JwtAuthenticationToken) auth;
            List<Loan> list = loanService.findAllLoan();
            log.info("FIND_ALL_LOANS_HANDLER_SUCCESS - count: {}", list.size());
            return list;
        } catch (Exception e) {
            log.error("FIND_ALL_LOANS_HANDLER_ERROR - error: {}", e.getMessage(), e);
            throw e;
        }
    }
    public List<Loan> findPendingLoans() {
        log.info("FIND_ALL_LOANS_HANDLER_START - userId: {}", getCurrentUserIdSafe());
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            JwtAuthenticationToken jwt = (JwtAuthenticationToken) auth;
            List<Loan> list = loanService.getPendingLoans();
            log.info("FIND_ALL_LOANS_HANDLER_SUCCESS - count: {}", list.size());
            return list;
        } catch (Exception e) {
            log.error("FIND_ALL_LOANS_HANDLER_ERROR - error: {}", e.getMessage(), e);
            throw e;
        }
    }
    public void deleteLoan(Long loanId) {
        log.info("DELETE_LOAN_HANDLER_START - userId: {}, loanId: {}", getCurrentUserIdSafe(), loanId);
        try {
            loanService.deleteLoan(loanId);
            coreBankingClient.deleteLoan(loanId);
            log.info("DELETE_LOAN_HANDLER_SUCCESS - loanId: {}", loanId);
        } catch (Exception e) {
            log.error("DELETE_LOAN_HANDLER_ERROR - loanId: {}, error: {}", loanId, e.getMessage(), e);
            throw e;
        }
    }
    public List<Repayment> getRepaymentsByLoanId(Long loanId) {
        log.info("GET_REPAYMENTS_BY_LOAN_HANDLER_START - userId: {}, loanId: {}", getCurrentUserIdSafe(), loanId);
        try {
            List<Repayment> list = repaymentService.getRepaymentsByLoanId(loanId);
            log.info("GET_REPAYMENTS_BY_LOAN_HANDLER_SUCCESS - loanId: {}, count: {}", loanId, list.size());
            return list;
        } catch (Exception e) {
            log.error("GET_REPAYMENTS_BY_LOAN_HANDLER_ERROR - loanId: {}, error: {}", loanId, e.getMessage(), e);
            throw e;
        }
    }
    public String makeRepayment(Long repaymentId, BigDecimal amount, String accountNumber) {
        log.info("MAKE_REPAYMENT_HANDLER_START - userId: {}, repaymentId: {}, amount: {}, account: {}", getCurrentUserIdSafe(), repaymentId, amount, accountNumber);
        try {
            PayRepaymentRequest pay = new PayRepaymentRequest();
            pay.setAmount(amount);
            pay.setCurrency("VND");
            pay.setDescription("OTP request");
            pay.setFromAccountNumber(accountNumber);

            CommonTransactionDTO tx = commonTransactionService.loanPayment(pay);
            log.info("MAKE_REPAYMENT_TRANSACTION - status: {}, ref: {}", tx.getStatus(), tx.getReferenceCode());
            if (!"PENDING".equalsIgnoreCase(tx.getStatus())) {
                throw new IllegalArgumentException(tx.getFailedReason());
            }
            log.info("MAKE_REPAYMENT_HANDLER_SUCCESS - referenceCode: {}", tx.getReferenceCode());
            return tx.getReferenceCode() ;
        } catch (IllegalArgumentException e) {
            log.error("MAKE_REPAYMENT_HANDLER_INVALID - {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("MAKE_REPAYMENT_HANDLER_ERROR - error: {}", e.getMessage(), e);
            throw e;
        }
    }
    public Repayment confirmRepayment(Long repaymentId, BigDecimal amount, String otpCode, String referenceCode) {
        log.info("CONFIRM_REPAYMENT_HANDLER_START - userId: {}, repaymentId: {}, referenceCode: {}", getCurrentUserIdSafe(), repaymentId, referenceCode);
        try {
            CommonConfirmTransactionRequest confirm = new CommonConfirmTransactionRequest();
            confirm.setOtpCode(otpCode);
            confirm.setReferenceCode(referenceCode);

            CommonTransactionDTO tx = commonTransactionService.confirmTransaction(confirm);
            log.info("CONFIRM_REPAYMENT_TRANSACTION - status: {}", tx.getStatus());
            if (!"COMPLETED".equalsIgnoreCase(tx.getStatus())) {
                throw new IllegalArgumentException(tx.getFailedReason());
            }

            Repayment r = repaymentService.makeRepayment(repaymentId, amount);
            // Gọi updateAccountFromLoan với paidAmount
            try {
                LoanRequestDTO updateDto = new LoanRequestDTO();
                updateDto.setLoanId(r.getLoan().getLoanId());
                updateDto.setAmount(r.getLoan().getAmount());
                updateDto.setPaidAmount(amount);
                com.example.common_service.constant.LoanStatus mappedStatus = com.example.common_service.constant.LoanStatus.PENDING;
                if (repaymentService.checkLastMonthRepayment(r)) {
                    log.info("CLOSE_LOAN_AFTER_AUTO_DEDUCT - loanId: {}", r.getLoan().getLoanId());
                    loanService.closedLoan(r.getLoan().getLoanId());
                    sendLoanClosedNotification(r.getLoan(), "Hoàn thành trả nợ");
                }
                updateDto.setStatus(mappedStatus);
                updateDto.setDisbursementAccountNumber(r.getLoan().getDisbursementAccountNumber());
                updateDto.setRepaymentAccountNumber(r.getLoan().getRepaymentAccountNumber());
                updateDto.setInterestRate(r.getLoan().getInterestRate());
                updateDto.setTermMonths(r.getLoan().getTermMonths());
                accountDubboService.updateAccountFromLoan(updateDto);
                // Gửi thông báo thanh toán thành công
                sendSuccessfulPaymentNotification(r.getLoan(), r, amount);
                log.info("MAKE_REPAYMENT_SUCCESS - repaymentId: {}, status: {}", repaymentId, r.getStatus());
            } catch (Exception ex) {
                log.warn("UPDATE_ACCOUNT_FROM_LOAN_AFTER_REPAYMENT_FAILED: {}", ex.getMessage());
            }
            log.info("CONFIRM_REPAYMENT_HANDLER_SUCCESS - repaymentId: {}", repaymentId);
            return r;
        } catch (IllegalArgumentException e) {
            log.error("CONFIRM_REPAYMENT_HANDLER_INVALID - {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("CONFIRM_REPAYMENT_HANDLER_ERROR - error: {}", e.getMessage(), e);
            throw e;
        }
    }
    private void sendSuccessfulPaymentNotification(Loan loan, Repayment repayment, BigDecimal amount) {
        try {
            CustomerResponseDTO cust = customerQueryService.getCustomerById(loan.getCustomerId());
            String body = String.format(
                    "Kính chào %s,%n%n" +
                            "Khoản vay ID: %s đã được thanh toán thành công.%n" +
                            "Số tiền: %s VND.%n" +
                            "Kỳ thanh toán: %s.%n%n" +
                            "Cảm ơn bạn đã sử dụng dịch vụ của chúng tôi.%n%n" +
                            "Trân trọng, Ngân hàng",
                    cust.getFullName(),
                    loan.getLoanId(),
                    amount,
                    repayment.getDueDate().format(DateTimeFormatter.ofPattern("MM/yyyy"))
            );
            MailMessageDTO mail = MailMessageDTO.builder()
                    .subject("THÔNG BÁO THANH TOÁN THÀNH CÔNG")
                    .recipient(cust.getEmail())
                    .recipientName(cust.getFullName())
                    .body(body)
                    .build();
            streamBridge.send("loan-notification-out-0", mail);
            log.info("SUCCESSFUL_PAYMENT_NOTIFICATION_SENT - loanId: {}", loan.getLoanId());
        } catch (Exception e) {
            log.error("SEND_SUCCESSFUL_PAYMENT_NOTIFICATION_ERROR - loanId: {}, error: {}", loan.getLoanId(), e.getMessage());
        }
    }
    private void sendLoanClosedNotification(Loan loan, String reason) {
        try {
            CustomerResponseDTO cust = customerQueryService.getCustomerById(loan.getCustomerId());
            String body = String.format(
                    "Kính chào %s,%n%n" +
                            "Khoản vay ID: %s đã được đóng.%n" +
                            "Lý do: %s.%n%n" +
                            "Vui lòng liên hệ ngân hàng để biết thêm chi tiết.%n%n" +
                            "Trân trọng, Ngân hàng",
                    cust.getFullName(),
                    loan.getLoanId(),
                    reason
            );
            MailMessageDTO mail = MailMessageDTO.builder()
                    .subject("THÔNG BÁO ĐÓNG KHOẢN VAY")
                    .recipient(cust.getEmail())
                    .recipientName(cust.getFullName())
                    .body(body)
                    .build();
            streamBridge.send("loan-notification-out-0", mail);
            log.info("LOAN_CLOSED_NOTIFICATION_SENT - loanId: {}", loan.getLoanId());
        } catch (Exception e) {
            log.error("SEND_LOAN_CLOSED_NOTIFICATION_ERROR - loanId: {}, error: {}", loan.getLoanId(), e.getMessage());
        }
    }
    public List<Repayment> getHistory() {
        log.info("GET_HISTORY_HANDLER_START - userId: {}", getCurrentUserIdSafe());
        try {
            Long customerId = getCustomerId();
            List<Repayment> history = repaymentService.getHistoryRepayment(customerId);
            log.info("GET_HISTORY_HANDLER_SUCCESS - count: {}", history.size());
            return history;
        } catch (Exception e) {
            log.error("GET_HISTORY_HANDLER_ERROR - error: {}", e.getMessage(), e);
            throw e;
        }
    }
    public Repayment getCurrentRepayment() {
        log.info("GET_CURRENT_REPAYMENT_HANDLER_START - userId: {}", getCurrentUserIdSafe());
        try {
            Long customerId = getCustomerId();
            Repayment r = repaymentService.getCurrentRepayment(customerId);
            if (r != null ) log.info("GET_CURRENT_REPAYMENT_HANDLER_SUCCESS - repaymentId: {}", r.getRepaymentId());
            else log.info("GET_CURRENT_REPAYMENT_HANDLER_NOT_FIND_CURRENT_REPAYMENT ");
            return r;
        } catch (Exception e) {
            log.error("GET_CURRENT_REPAYMENT_HANDLER_ERROR - error: {}", e.getMessage(), e);
            throw e;
        }
    }
    public Optional<Repayment> getRepaymentById(Long repaymentId) {
        log.info("GET_REPAYMENT_BY_ID_HANDLER_START - userId: {}, repaymentId: {}", getCurrentUserIdSafe(), repaymentId);
        try {
            Optional<Repayment> r = repaymentService.getRepaymentById(repaymentId);
            log.info("GET_REPAYMENT_BY_ID_HANDLER_SUCCESS - found: {}", r.isPresent());
            return r;
        } catch (Exception e) {
            log.error("GET_REPAYMENT_BY_ID_HANDLER_ERROR - error: {}", e.getMessage(), e);
            throw e;
        }
    }
    public Repayment unpaidRepayment(Long repaymentId) {
        log.info("UNPAID_REPAYMENT_HANDLER_START - userId: {}, repaymentId: {}", getCurrentUserIdSafe(), repaymentId);
        try {
            Repayment r = repaymentService.updateRepaymentStatus(repaymentId, RepaymentStatus.UNPAID);
            log.info("UNPAID_REPAYMENT_HANDLER_SUCCESS - repaymentId: {}", repaymentId);
            return r;
        } catch (Exception e) {
            log.error("UNPAID_REPAYMENT_HANDLER_ERROR - error: {}", e.getMessage(), e);
            throw e;
        }
    }
    public Repayment lateRepayment(Long repaymentId) {
        log.info("LATE_REPAYMENT_HANDLER_START - userId: {}, repaymentId: {}", getCurrentUserIdSafe(), repaymentId);
        try {
            Repayment r = repaymentService.updateRepaymentStatus(repaymentId, RepaymentStatus.LATE);
            log.info("LATE_REPAYMENT_HANDLER_SUCCESS - repaymentId: {}", repaymentId);
            return r;
        } catch (Exception e) {
            log.error("LATE_REPAYMENT_HANDLER_ERROR - error: {}", e.getMessage(), e);
            throw e;
        }
    }
    public BigDecimal getTotalBorrowed() {
        log.info("GET_TOTAL_BORROWED_HANDLER_START - userId: {}", getCurrentUserIdSafe());
        try {
            Long customerId = getCustomerId();
            BigDecimal total = loanService.getTotalBorrowed(customerId);
            log.info("GET_TOTAL_BORROWED_HANDLER_SUCCESS - total: {}", total);
            return total;
        } catch (Exception e) {
            log.error("GET_TOTAL_BORROWED_HANDLER_ERROR - error: {}", e.getMessage(), e);
            throw e;
        }
    }
    public BigDecimal getTotalOutstanding() {
        log.info("GET_TOTAL_OUTSTANDING_HANDLER_START - userId: {}", getCurrentUserIdSafe());
        try {
            Long customerId = getCustomerId();
            BigDecimal total = loanService.getTotalOutstanding(customerId);
            log.info("GET_TOTAL_OUTSTANDING_HANDLER_SUCCESS - total: {}", total);
            return total;
        } catch (Exception e) {
            log.error("GET_TOTAL_OUTSTANDING_HANDLER_ERROR - error: {}", e.getMessage(), e);
            throw e;
        }
    }
    public Long getCustomerId() {
        log.info("GET_CUSTOMER_ID_HANDLER_START - userId: {}", getCurrentUserIdSafe());
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            JwtAuthenticationToken jwt = (JwtAuthenticationToken) auth;
            String user = jwt.getName();
            Long customerId = commonService.getCurrentCustomer(user).getCustomerId();
            log.info("GET_CUSTOMER_ID_HANDLER_SUCCESS - customerId: {}", customerId);
            return customerId;
        } catch (Exception e) {
            log.error("GET_CUSTOMER_ID_HANDLER_ERROR - error: {}", e.getMessage(), e);
            throw e;
        }
    }
    public void deleteRepaymentsByLoanId(Long loanId) {
        log.info("DELETE_REPAYMENTS_BY_LOAN_HANDLER_START - userId: {}, loanId: {}", getCurrentUserIdSafe(), loanId);
        try {
            repaymentService.deleteRepaymentsByLoanId(loanId);
            log.info("DELETE_REPAYMENTS_BY_LOAN_HANDLER_SUCCESS - loanId: {}", loanId);
        } catch (Exception e) {
            log.error("DELETE_REPAYMENTS_BY_LOAN_HANDLER_ERROR - loanId: {}, error: {}", loanId, e.getMessage(), e);
            throw e;
        }
    }
    public CustomerResponseDTO getCustomerDetailById(Long id) {
        try {
            log.debug("CUSTOMER_ID_FETCHED - {}", id);
            CustomerResponseDTO customer = customerQueryService.getCustomerById(id);
            log.info("CUSTOMER_INFO - status: {}", customer.getStatus());
            return customer;
        } catch (Exception e) {
            log.error("CUSTOMER_ID_FETCHED - error: {}", e.getMessage(), e);
            throw e;
        }
    }


    public List<AccountPaymentResponse> getAllPaymentAccountsbyUserId(String id) {
        try {
            log.debug("GET_ALL_PAYMENT_ACCOUNTS_BY_USER_START - userId: {}", id);
            List<AccountPaymentResponse> accounts = customerCommonService.getAllPaymentAccountsbyUserId(id);
            log.info("GET_ALL_PAYMENT_ACCOUNTS_BY_USER_SUCCESS - userId: {}, totalAccounts: {}", id, accounts.size());
            return accounts;
        } catch (Exception e) {
            log.error("GET_ALL_PAYMENT_ACCOUNTS_BY_USER_ERROR - userId: {}, error: {}", id, e.getMessage(), e);
            throw e;
        }
    }
    public List<AccountPaymentResponse> getAllPaymentAccountsbyCurrentUser() {
        try {
            log.debug("GET_ALL_PAYMENT_ACCOUNTS_BY_CURRENT_USER");
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            JwtAuthenticationToken jwt = (JwtAuthenticationToken) auth;
            String id = jwt.getName();
            log.info("GET_USER_ID_FROM_AUTHEN - userId: {}", id);
            List<AccountPaymentResponse> accounts = customerCommonService.getAllPaymentAccountsbyUserId(id);
            log.info("GET_ALL_PAYMENT_ACCOUNTS_BY_USER_SUCCESS - userId: {}, totalAccounts: {}", id, accounts.size());
            return accounts;
        } catch (Exception e) {
            log.error("GET_ALL_PAYMENT_ACCOUNTS_BY_USER_ERROR - userId: {}, error: {}", null, e.getMessage(), e);
            throw e;
        }
    }


    public java.math.BigDecimal getTotalDisbursedSystem() {
        return loanService.getTotalDisbursedSystem();
    }
    public java.math.BigDecimal getTotalCollectedSystem() {
        return repaymentService.getTotalCollectedSystem();
    }
    public java.math.BigDecimal getTotalProfitSystem() {
        return repaymentService.getTotalProfitSystem();
    }
    public Map<String, Long> getRepaymentStats() {
        return repaymentService.getRepaymentStats();
    }
    public java.math.BigDecimal getTotalRecoveredSystem() {
        return loanService.getTotalRecoveredSystem();
    }
    
    public List<Loan> getPendingLoans() {
        log.info("GET_PENDING_LOANS_HANDLER_START");
        try {
            List<Loan> pendingLoans = loanService.getPendingLoans();
            log.info("GET_PENDING_LOANS_HANDLER_SUCCESS - count: {}", pendingLoans.size());
            return pendingLoans;
        } catch (Exception e) {
            log.error("GET_PENDING_LOANS_HANDLER_ERROR - error: {}", e.getMessage(), e);
            throw e;
        }
    }
}
