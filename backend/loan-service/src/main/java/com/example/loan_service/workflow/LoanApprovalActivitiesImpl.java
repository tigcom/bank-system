    package com.example.loan_service.workflow;

    import com.example.common_service.dto.*;
    import com.example.common_service.constant.LoanType;
    import com.example.common_service.dto.request.CommonDisburseRequest;
    import com.example.common_service.dto.request.LoanRequestDTO;
    import com.example.common_service.services.CommonService;
    import com.example.common_service.services.account.AccountDubboService;
    import com.example.common_service.services.customer.CustomerQueryService;
    import com.example.common_service.services.transactions.CommonTransactionService;
    import com.example.loan_service.entity.Loan;
    import com.example.loan_service.models.LoanStatus;
    import com.example.loan_service.response.ApiResponseWrapper;
    import com.example.loan_service.service.LoanService;
    import com.example.loan_service.service.RepaymentService;
    import lombok.RequiredArgsConstructor;
    import lombok.extern.slf4j.Slf4j;
    import org.apache.dubbo.config.annotation.DubboReference;
    import org.apache.dubbo.rpc.RpcContext;
    import org.springframework.cloud.stream.function.StreamBridge;
    import org.springframework.beans.factory.annotation.Value;
    import org.springframework.security.core.context.SecurityContextHolder;
    import org.springframework.stereotype.Component;
    import org.springframework.stereotype.Service;

    import java.time.LocalDateTime;

    @Slf4j
    @Service
    @RequiredArgsConstructor
    public class LoanApprovalActivitiesImpl implements LoanApprovalActivities {

        private final LoanService loanService;
        private final RepaymentService repaymentService;
        private final StreamBridge streamBridge;

        @Value("${car_account_number}")
        private String carAccountNumber;

        @Value("${home_account_number}")
        private String homeAccountNumber;

        @DubboReference
        private CustomerQueryService customerQueryService;

        @DubboReference
        private AccountDubboService accountDubboService;

        @DubboReference
        private CommonTransactionService commonTransactionService;

        @DubboReference
        private CommonService commonService;

        @Override
        public LoanRequestDTO validateLoan(Long loanId) {
            log.info("Validating loan: {}", loanId);

            Loan loan = loanService.getLoanById(loanId);

            if (!LoanStatus.PENDING.equals(loan.getStatus())) {
                throw new IllegalArgumentException("Loan is not in PENDING status: " + loan.getStatus());
            }

            return LoanRequestDTO.builder()
                    .loanId(loan.getLoanId())
                    .customerId(loan.getCustomerId())
                    .disbursementAccountNumber(loan.getDisbursementAccountNumber())
                    .repaymentAccountNumber(loan.getRepaymentAccountNumber())
                    .amount(loan.getAmount())
                    .interestRate(loan.getInterestRate())
                    .termMonths(loan.getTermMonths())
                    .loanType(loan.getLoanType())
                    .createdAt(loan.getCreatedAt())
                    .approvedAt(LocalDateTime.now())
                    .status(loan.getStatus().toCommonStatus())
                    .build();
        }

        @Override
        public String createLoanAccount(LoanRequestDTO loanData, String username) {
            log.info("Creating loan account for loan: {}", loanData.getLoanId());
            LoanRequestDTO dto = new LoanRequestDTO();
            dto.setLoanId(loanData.getLoanId());
            dto.setDisbursementAccountNumber(loanData.getDisbursementAccountNumber());
            dto.setRepaymentAccountNumber(loanData.getRepaymentAccountNumber());
            dto.setAmount(loanData.getAmount());
            dto.setInterestRate(loanData.getInterestRate());
            dto.setTermMonths(loanData.getTermMonths());
            dto.setCustomerId(loanData.getCustomerId());
            dto.setCreatedAt(LocalDateTime.now());
            dto.setStatus(com.example.common_service.constant.LoanStatus.APPROVED);
            dto.setLoanType(loanData.getLoanType());
            RpcContext.getContext().setAttachment("username", username);
            AccountDTO accountDTO = accountDubboService.createLoanAccount(dto);
            log.info("Loan account created: {}", accountDTO.getAccountNumber());

            return accountDTO.getAccountNumber();
        }

        @Override
        public String disburseLoan(CommonDisburseRequest disburseReq ,String username ) {
            log.info("Disbursing loan to account: {}", disburseReq.getToAccountNumber());
            RpcContext.getContext().setAttachment("username", username);

            CommonTransactionDTO tx = commonTransactionService.loanDisbursement(disburseReq,username);
            log.info("Disbursement transaction: status={}, ref={}", tx.getStatus(), tx.getReferenceCode());

            if (!"COMPLETED".equalsIgnoreCase(tx.getStatus())) {
                throw new IllegalArgumentException("Disbursement failed: " + tx.getFailedReason());
            }

            return tx.getReferenceCode();
        }

        @Override
        public void approveLoanInDatabase(Long loanId,String accountNumber) {
            log.info("Approving loan in database: {}", loanId);

            Loan loan = loanService.getLoanById(loanId);
            loan.setDisbursementAccountNumber(accountNumber);
            loanService.updateLoan(loan);
            loanService.approveLoan(loan);
            log.info("Loan approved in database: {}", loanId);
        }

        @Override
        public void generateRepaymentSchedule(Long loanId) {
            log.info("Generating repayment schedule for loan: {}", loanId);

            Loan loan = loanService.getLoanById(loanId);

            repaymentService.generateRepaymentSchedule(loan);
            accountDubboService.updateOutstandingDebt(loanId,loanService.getTotalOutstandingByLoan(loanId));
            log.info("Repayment schedule generated for loan: {}", loanId);
        }

        @Override
        public void sendApprovalNotification(Long loanId, String accountNumber) {
            log.info("Sending approval notification for loan: {}", loanId);

            try {
                Loan loan = loanService.getLoanById(loanId);

                CustomerResponseDTO customer = customerQueryService.getCustomerById(loan.getCustomerId());

                String subject;
                String body;
                
                if (loan.getLoanType() == LoanType.PERSONAL) {
                    subject = "KÍCH HOẠT KHOẢN VAY TIÊU DÙNG CÁ NHÂN";
                    body = String.format(
                        "Kính chào %s,%n%n" +
                        "Khoản vay tiêu dùng cá nhân ID: %s đã được phê duyệt thành công.%n" +
                        "Số tiền: %s VND đã được giải ngân vào tài khoản vay: %s%n" +
                        "Lãi suất: %s%%/năm, Kỳ hạn: %s tháng%n%n" +
                        "Vui lòng kiểm tra tài khoản và thực hiện thanh toán theo lịch trình đã thỏa thuận.%n%n" +
                        "Trân trọng, Ngân hàng",
                        customer.getFullName(),
                        loan.getLoanId(),
                        loan.getAmount(),
                        accountNumber,
                        loan.getInterestRate(),
                        loan.getTermMonths()
                    );
                } else if (loan.getLoanType() == LoanType.AUTO) {
                    subject = "PHÊ DUYỆT KHOẢN VAY MUA XE";
                    body = String.format(
                        "Kính chào %s,%n%n" +
                        "Khoản vay mua xe ID: %s đã được phê duyệt thành công.%n" +
                        "Số tiền: %s VND đã được giải ngân đến đại lý xe hơi đã đăng ký.%n" +
                        "Lãi suất: %s%%/năm, Kỳ hạn: %s tháng%n%n" +
                        "Vui lòng liên hệ đại lý để hoàn tất thủ tục nhận xe.%n%n" +
                        "Trân trọng, Ngân hàng",
                        customer.getFullName(),
                        loan.getLoanId(),
                        loan.getAmount(),
                        loan.getInterestRate(),
                        loan.getTermMonths()
                    );
                } else if (loan.getLoanType() == LoanType.HOME) {
                    subject = "PHÊ DUYỆT KHOẢN VAY MUA NHÀ";
                    body = String.format(
                        "Kính chào %s,%n%n" +
                        "Khoản vay mua nhà ID: %s đã được phê duyệt thành công.%n" +
                        "Số tiền: %s VND đã được giải ngân đến chủ đầu tư bất động sản đã đăng ký.%n" +
                        "Lãi suất: %s%%/năm, Kỳ hạn: %s tháng%n%n" +
                        "Vui lòng liên hệ chủ đầu tư để hoàn tất thủ tục nhận nhà.%n%n" +
                        "Trân trọng, Ngân hàng",
                        customer.getFullName(),
                        loan.getLoanId(),
                        loan.getAmount(),
                        loan.getInterestRate(),
                        loan.getTermMonths()
                    );
                } else {
                    subject = "PHÊ DUYỆT KHOẢN VAY";
                    body = String.format(
                        "Kính chào %s,%n%n" +
                        "Khoản vay ID: %s đã được phê duyệt thành công.%n" +
                        "Số tiền: %s VND đã được giải ngân đến dịch vụ đã đăng ký.%n%n" +
                        "Trân trọng, Ngân hàng",
                        customer.getFullName(),
                        loan.getLoanId(),
                        loan.getAmount()
                    );
                }

                MailMessageDTO mail = new MailMessageDTO();
                mail.setSubject(subject);
                mail.setRecipient("phanhuynhphuckhang12c8@gmail.com");
                mail.setBody(body);
                mail.setRecipientName(customer.getFullName());

                streamBridge.send("loan-notification-out-0", mail);
                log.info("Approval notification sent for loan: {} with type: {}", loanId, loan.getLoanType());

            } catch (Exception e) {
                log.error("Failed to send approval notification for loan: {}, error: {}", loanId, e.getMessage());
                // Không throw exception vì đây không phải là bước quan trọng
            }
        }

        @Override
        public void rollbackLoanApproval(Long loanId) {
            log.info("Rolling back loan approval: {}", loanId);

            try {
                Loan loan = loanService.getLoanById(loanId);
                if (loan != null && LoanStatus.APPROVED.equals(loan.getStatus())) {
                    loan.setStatus(LoanStatus.PENDING);
                    loanService.updateLoan(loan);

                    // Xóa lịch trả nợ đã tạo
                    repaymentService.deleteRepaymentsByLoanId(loanId);

                    log.info("Loan approval rolled back successfully: {}", loanId);
                }
            } catch (Exception e) {
                log.error("Failed to rollback loan approval: {}, error: {}", loanId, e.getMessage());
            }
        }

        @Override
        public String resolveDisbursementAccount(LoanType loanType) {
            if (loanType == null) {
                throw new IllegalArgumentException("LoanType must not be null");
            }
            switch (loanType) {
                case AUTO:
                    return carAccountNumber;
                case HOME:
                    return homeAccountNumber;
                case PERSONAL:
                default:
                    // PERSONAL will create its own loan account; return null to indicate not applicable
                    return null;
            }
        }
    }