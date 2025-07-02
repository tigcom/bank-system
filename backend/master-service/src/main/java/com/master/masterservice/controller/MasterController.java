package com.master.masterservice.controller;

import com.master.masterservice.dto.request.CardRegistrationRequest;
import com.master.masterservice.dto.response.ApiResponseWrapper;
import com.master.masterservice.dto.response.MasterCardResponse;
import com.master.masterservice.service.MasterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/master-service")
@RequiredArgsConstructor
@Slf4j
public class MasterController {
    private final MasterService masterService;

    @PostMapping("/registration")
    public ResponseEntity<ApiResponseWrapper<MasterCardResponse>> registerCard(@RequestBody CardRegistrationRequest request) {
        log.info("Received Master card registration request: {}", request);
        MasterCardResponse response = masterService.registerCard(request);
        return ResponseEntity.ok(ApiResponseWrapper.success(response));
    }

    @PostMapping("/test-retry")
    public ResponseEntity<String> testRetry() {
        log.info("Testing retry mechanism...");
        try {
            // Create a test request that will fail
            CardRegistrationRequest testRequest = CardRegistrationRequest.builder()
                    .accountNumber("TEST-RETRY-123")
                    .cifCode("TEST-CIF")
                    .cardType("MASTER")
                    .build();
            
            MasterCardResponse response = masterService.registerCard(testRequest);
            return ResponseEntity.ok("Retry test completed: " + response.getStatus());
        } catch (Exception e) {
            log.error("Retry test failed: {}", e.getMessage());
            return ResponseEntity.ok("Retry test failed with exception: " + e.getClass().getSimpleName());
        }
    }
} 