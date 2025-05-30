package com.example.corebanking_service.Controller;

import com.example.common_service.dto.customer.CoreCustomerDTO;
import com.example.common_service.dto.customer.CoreResponse;
import com.example.common_service.services.customer.CoreCustomerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/core/customers")
@RequiredArgsConstructor
@Slf4j
public class CoreCustomerController {

    private final CoreCustomerService coreCustomerService;

    @PostMapping("/sync")
    public ResponseEntity<CoreResponse> syncCoreCustomer(@RequestBody CoreCustomerDTO dto) {
        log.info("Nhận dữ liệu từ customer-service: {}", dto);
        CoreResponse response = coreCustomerService.syncCoreCustomer(dto);
        log.info("Đã đồng bộ: {}", response);
        return ResponseEntity.ok(response);
    }
}
