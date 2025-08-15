package com.example.loan_service.worker;

import com.example.loan_service.workflow.LoanApprovalWorkflowImpl;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class LoanApprovalWorker {

    private final WorkerFactory workerFactory;

    @Value("${temporal.workers.default.task-queue:LoanApprovalTaskQueue}")
    private String taskQueue;

    @Value("${temporal.enabled:true}")
    private boolean temporalEnabled;

    private Worker worker;

    @PostConstruct
    public void startWorker() {
        if (!temporalEnabled || workerFactory == null) {
            log.warn("Temporal worker is disabled - skipping worker startup");
            return;
        }
        
        try {
            log.info("Starting Temporal worker for task queue: {}", taskQueue);
            
            worker = workerFactory.newWorker(taskQueue);
            worker.registerWorkflowImplementationTypes(LoanApprovalWorkflowImpl.class);
            
            workerFactory.start();
            log.info("Temporal worker started successfully for task queue: {}", taskQueue);
        } catch (Exception e) {
            log.error("Failed to start Temporal worker: {}", e.getMessage());
            log.warn("Continuing without Temporal worker - workflows will not be processed");
        }
    }

    @PreDestroy
    public void shutdownWorker() {
        log.info("Shutting down Temporal worker");
        if (workerFactory != null) {
            try {
                workerFactory.shutdown();
            } catch (Exception e) {
                log.error("Error shutting down Temporal worker: {}", e.getMessage());
            }
        }
    }
}
