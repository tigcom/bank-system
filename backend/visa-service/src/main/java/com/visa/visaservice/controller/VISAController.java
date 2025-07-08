package com.visa.visaservice.controller;

import com.visa.visaservice.dto.request.CardRegistrationRequest;
import com.visa.visaservice.dto.response.ApiResponseWrapper;
import com.visa.visaservice.dto.response.VisaCardResponse;
import com.visa.visaservice.service.VisaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/visa-service")
@RequiredArgsConstructor
@Slf4j
public class VISAController {
    private final VisaService visaService;

    @PostMapping("/registration")
    public ResponseEntity<ApiResponseWrapper<VisaCardResponse>> registerCard(@RequestBody CardRegistrationRequest request) {
        log.info("Received VISA card registration request: {}", request);
        VisaCardResponse response = visaService.registerCard(request);
        return ResponseEntity.ok(ApiResponseWrapper.<VisaCardResponse>builder()
                .data(response)
                .build());
    }
}