package com.example.loan_service.config;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import io.temporal.worker.WorkerFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class TemporalConfig {

    @Value("${temporal.server.host:localhost:7233}")
    private String temporalServerHost;

    @Value("${temporal.enabled:true}")
    private boolean temporalEnabled;

    @Bean
    public WorkflowServiceStubs workflowServiceStubs() {
        if (!temporalEnabled) {
            log.warn("Temporal is disabled - creating mock service stubs");
            return null;
        }
        
        try {
            return WorkflowServiceStubs.newServiceStubs(
                    WorkflowServiceStubsOptions.newBuilder()
                            .setTarget(temporalServerHost)
                            .build()
            );
        } catch (Exception e) {
            log.error("Failed to connect to Temporal server at {}: {}", temporalServerHost, e.getMessage());
            return null;
        }
    }

    @Bean
    public WorkflowClient workflowClient(WorkflowServiceStubs workflowServiceStubs) {
        if (!temporalEnabled || workflowServiceStubs == null) {
            log.warn("Temporal client is disabled - creating mock client");
            return null;
        }
        
        return WorkflowClient.newInstance(
                workflowServiceStubs,
                WorkflowClientOptions.newBuilder()
                        .setNamespace("default")
                        .build()
        );
    }

    @Bean
    public WorkerFactory workerFactory(WorkflowClient workflowClient) {
        if (!temporalEnabled || workflowClient == null) {
            log.warn("Temporal worker factory is disabled");
            return null;
        }
        return WorkerFactory.newInstance(workflowClient);
    }
}
