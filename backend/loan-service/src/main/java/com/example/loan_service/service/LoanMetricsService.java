package com.example.loan_service.service;

import io.micrometer.core.instrument.*;
import org.springframework.stereotype.Service;



@Service
public class LoanMetricsService {
    
    // Counters - Đếm số lượng các sự kiện
    private final Counter loanApplicationsCounter;
    private final Counter loanApprovalsCounter;
    private final Counter loanRejectionsCounter;
    private final Counter loanDisbursementsCounter;
    private final Counter loanRepaymentsCounter;
    private final Counter cicChecksCounter;
    private final Counter coreBankingCallsCounter;
    
    // Timers - Đo thời gian thực thi
    private final Timer loanApplicationProcessingTimer;
    private final Timer cicCheckTimer;
    private final Timer coreBankingCallTimer;
    private final Timer loanDisbursementTimer;
    private final Timer loanRepaymentTimer;


    public LoanMetricsService(MeterRegistry registry) {
        // Khởi tạo Counters
        this.loanApplicationsCounter = Counter.builder("loan.applications.total")
                .description("Total number of loan applications")
                .register(registry);
                
        this.loanApprovalsCounter = Counter.builder("loan.approvals.total")
                .description("Total number of approved loans")
                .register(registry);
                
        this.loanRejectionsCounter = Counter.builder("loan.rejections.total")
                .description("Total number of rejected loans")
                .register(registry);
                
        this.loanDisbursementsCounter = Counter.builder("loan.disbursements.total")
                .description("Total number of loan disbursements")
                .register(registry);
                
        this.loanRepaymentsCounter = Counter.builder("loan.repayments.total")
                .description("Total number of loan repayments")
                .register(registry);
                
        this.cicChecksCounter = Counter.builder("cic.checks.total")
                .description("Total number of CIC checks performed")
                .register(registry);
                
        this.coreBankingCallsCounter = Counter.builder("corebanking.calls.total")
                .description("Total number of core banking API calls")
                .register(registry);

        this.loanApplicationProcessingTimer = Timer.builder("loan.application.processing.time")
                .description("Time taken to process loan applications")
                .register(registry);
                
        this.cicCheckTimer = Timer.builder("cic.check.time")
                .description("Time taken for CIC checks")
                .register(registry);
                
        this.coreBankingCallTimer = Timer.builder("corebanking.call.time")
                .description("Time taken for core banking API calls")
                .register(registry);
                

                
        this.loanDisbursementTimer = Timer.builder("loan.disbursement.time")
                .description("Time taken for loan disbursement")
                .register(registry);
                
        this.loanRepaymentTimer = Timer.builder("loan.repayment.time")
                .description("Time taken for loan repayment processing")
                .register(registry);

    }

    public void incrementLoanApplications() {
        loanApplicationsCounter.increment();
    }
    public void incrementLoanApprovals() {
        loanApprovalsCounter.increment();
    }
    public void incrementLoanRejections() {
        loanRejectionsCounter.increment();
    }
    public void incrementLoanDisbursements() {
        loanDisbursementsCounter.increment();
    }
    public void incrementCicChecks() {
        cicChecksCounter.increment();
    }
    public void incrementCoreBankingCalls() {
        coreBankingCallsCounter.increment();
    }

    public Timer.Sample startLoanApplicationProcessing() {
        return Timer.start();
    }
    public void stopLoanApplicationProcessing(Timer.Sample sample) {
        sample.stop(loanApplicationProcessingTimer);
    }
    public Timer.Sample startCicCheck() {
        return Timer.start();
    }
    public void stopCicCheck(Timer.Sample sample) {
        sample.stop(cicCheckTimer);
    }
    public Timer.Sample startCoreBankingCall() {
        return Timer.start();
    }
    public void stopCoreBankingCall(Timer.Sample sample) {
        sample.stop(coreBankingCallTimer);
    }
    public Timer.Sample startLoanDisbursement() {
        return Timer.start();
    }
    public void stopLoanDisbursement(Timer.Sample sample) {
        sample.stop(loanDisbursementTimer);
    }

} 