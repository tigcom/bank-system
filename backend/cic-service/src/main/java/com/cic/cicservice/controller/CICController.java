package com.cic.cicservice.controller;

import com.example.common_service.dto.request.CICRequest;
import com.cic.cicservice.dto.response.ApiResponseWrapper;
import com.cic.cicservice.dto.response.CicResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.cic.cicservice.service.CICService;

@RestController
@RequestMapping("/api/cic-servive")
@RequiredArgsConstructor
public class CICController {
    private final CICService cicService;
    @PostMapping("/check-cic")
    public ApiResponseWrapper<CicResponse> checkCIC(@RequestBody CICRequest cicRequest) {
        return ApiResponseWrapper.<CicResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Check CIC successfully ")
                .data(cicService.checkCIC(cicRequest))
                .build();
    }
}
