package com.example.customer_service.services;

import com.example.common_service.dto.customer.CoreCustomerDTO;
import com.example.common_service.dto.customer.CoreResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CoreBankingClient {

    private static final Logger log = LoggerFactory.getLogger(CoreBankingClient.class);

    private final RestTemplate restTemplate;

    public CoreResponse syncCustomer(CoreCustomerDTO dto) {
        String requestId = UUID.randomUUID().toString();
        log.info("SYNC_CUSTOMER_TO_CORE - RequestId: {}, CifCode: {}", requestId, dto.getCifCode());
        
        try {
            ResponseEntity<CoreResponse> response = restTemplate.postForEntity(
                    "http://localhost:8083/corebanking/api/core/customers/sync",
                    dto,
                    CoreResponse.class
            );
            
            CoreResponse coreResponse = response.getBody();
            if (coreResponse != null && coreResponse.isSuccess()) {
                log.info("SYNC_CUSTOMER_TO_CORE_SUCCESS - RequestId: {}, CifCode: {}", requestId, dto.getCifCode());
            } else {
                log.warn("SYNC_CUSTOMER_TO_CORE_FAILED - RequestId: {}, CifCode: {}, Message: {}", 
                        requestId, dto.getCifCode(), coreResponse != null ? coreResponse.getMessage() : "No response");
            }
            return coreResponse;
        } catch (Exception e) {
            log.error("SYNC_CUSTOMER_TO_CORE_ERROR - RequestId: {}, CifCode: {}, Error: {}", 
                    requestId, dto.getCifCode(), e.getMessage(), e);
            throw e;
        }
    }
}

