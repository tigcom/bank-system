package com.example.corebanking_service.Controller;

import com.example.common_service.dto.customer.CoreResponse;
import com.example.corebanking_service.dto.request.LoanRequestDTO;
import com.example.corebanking_service.service.CoreLoanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.net.ssl.SSLEngineResult;

@RestController
@RequestMapping("/api/core/loans")
@RequiredArgsConstructor
@Slf4j
public class CoreLoanController {

    private final CoreLoanService coreLoanService;

    @PostMapping("/sync")
    public ResponseEntity<CoreResponse> syncCoreLoan(@RequestBody LoanRequestDTO dto) {
        log.info("Nhận dữ liệu từ customer-service: {}", dto);
        CoreResponse response = coreLoanService.syncCoreLoan(dto);
        log.info("Đã đồng bộ: {}", response);
        return ResponseEntity.ok(response);
    }
    @DeleteMapping("/sync/{id}")
    public ResponseEntity<?> syncCoreLoan(@PathVariable("id") Long id) {
        try {
            System.out.println(id);
            coreLoanService.delteLoan(id);
        }catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok().build();
    }
}
