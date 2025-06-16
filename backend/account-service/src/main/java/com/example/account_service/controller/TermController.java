package com.example.account_service.controller;

import com.example.account_service.service.TermService;
import com.example.common_service.dto.response.CoreTermDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/terms")
@Tag(name = "Terms Management", description = "APIs for managing term configurations")
public class TermController {
    
    private final TermService termService;
    
    @GetMapping("/active")
    @Operation(summary = "Get all active terms", description = "Retrieve all active term configurations")
    public ResponseEntity<List<CoreTermDTO>> getAllActiveTerms() {
        List<CoreTermDTO> terms = termService.getAllActiveTerms();
        return ResponseEntity.ok(terms);
    }
}