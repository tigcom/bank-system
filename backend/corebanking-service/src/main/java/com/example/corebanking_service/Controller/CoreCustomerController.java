package com.example.corebanking_service.Controller;

import com.example.common_service.dto.customer.CoreCustomerDTO;
import com.example.common_service.dto.customer.CoreResponse;
import com.example.corebanking_service.service.CoreCustomerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/core/customers")
@RequiredArgsConstructor
@Slf4j
public class CoreCustomerController {

    private final CoreCustomerService coreCustomerService;

    @PostMapping("/sync")
    public ResponseEntity<CoreResponse> syncCoreCustomer(@RequestBody CoreCustomerDTO dto) {
        String requestId = UUID.randomUUID().toString();
        log.info("SYNC_CORE_CUSTOMER_REQUEST - RequestId: {}, CifCode: {}, Status: {}", requestId, dto.getCifCode(), dto.getStatus());
        CoreResponse response = coreCustomerService.syncCoreCustomer(dto);
        log.info("SYNC_CORE_CUSTOMER_RESPONSE - RequestId: {}, CifCode: {}, Success: {}, Message: {}", requestId, dto.getCifCode(), response.isSuccess(), response.getMessage());
        return ResponseEntity.ok(response);
    }
}
