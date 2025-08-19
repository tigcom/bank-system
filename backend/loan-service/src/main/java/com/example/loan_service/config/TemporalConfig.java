package com.example.loan_service.config;

import com.example.loan_service.workflow.LoanApprovalActivitiesImpl;
import com.example.loan_service.workflow.LoanApprovalWorkflowImpl;
import io.temporal.client.WorkflowClient;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TemporalConfig {
    @Bean
    public WorkflowClient workflowClient() {
        WorkflowServiceStubsOptions options = WorkflowServiceStubsOptions.newBuilder()
                .setTarget("localhost:7233")
                .build();
        WorkflowServiceStubs service = WorkflowServiceStubs.newInstance(options);
        return WorkflowClient.newInstance(service);
    }

    @Bean
    public WorkerFactory workerFactory(WorkflowClient client) {
        return WorkerFactory.newInstance(client);
    }
    @Bean
    public Worker LoanWorker(WorkerFactory factory, LoanApprovalActivitiesImpl activitiesImpl) {
        Worker worker = factory.newWorker("LoanApprovalTaskQueue");
        worker.registerWorkflowImplementationTypes(LoanApprovalWorkflowImpl.class);
        worker.registerActivitiesImplementations(activitiesImpl);
        factory.start();
        return worker;
    }
}