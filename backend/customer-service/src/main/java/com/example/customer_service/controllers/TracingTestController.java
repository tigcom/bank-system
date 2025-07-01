package com.example.customer_service.controllers;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/tracing")
@Slf4j
public class TracingTestController {

    @GetMapping("/test")
    public Map<String, Object> testTracing() {
        log.info("=== TRACING DEBUG TEST ===");
        
        // Get all MDC keys
        Map<String, String> mdcPropertyMap = MDC.getCopyOfContextMap();
        
        log.info("MDC Keys and Values:");
        if (mdcPropertyMap != null && !mdcPropertyMap.isEmpty()) {
            for (Map.Entry<String, String> entry : mdcPropertyMap.entrySet()) {
                log.info("  {} = {}", entry.getKey(), entry.getValue());
            }
        } else {
            log.warn("No MDC context found!");
        }
        
        // Test different possible key names
        String[] possibleTraceKeys = {
            "traceId", "trace_id", "traceid", "trace.id",
            "spanId", "span_id", "spanid", "span.id",
            "X-Trace-Id", "X-Span-Id"
        };
        
        log.info("Testing possible trace key names:");
        for (String key : possibleTraceKeys) {
            String value = MDC.get(key);
            if (value != null) {
                log.info("  FOUND: {} = {}", key, value);
            }
        }
        
        return Map.of(
            "status", "success", 
            "mdcKeys", mdcPropertyMap != null ? mdcPropertyMap.keySet() : Set.of(),
            "mdcValues", mdcPropertyMap != null ? mdcPropertyMap : Map.of(),
            "message", "Check logs for detailed tracing information"
        );
    }
    
    @GetMapping("/generate-trace")
    public Map<String, String> generateTrace() {
        log.info("Generating trace for testing - Step 1");
        
        // Simulate some processing
        processStep1();
        processStep2();
        
        log.info("Trace generation completed");
        
        return Map.of(
            "result", "trace generated", 
            "timestamp", String.valueOf(System.currentTimeMillis()),
            "service", "customer-service"
        );
    }
    
    private void processStep1() {
        log.info("Processing step 1...");
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        log.info("Step 1 completed");
    }
    
    private void processStep2() {
        log.info("Processing step 2...");
        try {
            Thread.sleep(150);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        log.info("Step 2 completed");
    }
} 