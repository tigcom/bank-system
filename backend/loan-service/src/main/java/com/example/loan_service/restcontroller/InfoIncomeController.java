package com.example.loan_service.restcontroller;

import com.example.loan_service.dto.request.InfoIncomeRequestDto;
import com.example.loan_service.dto.response.InfoIncomeResponseDto;
import com.example.loan_service.entity.InfoIncome;
import com.example.loan_service.mapper.InfoIncomeMapper;
import com.example.loan_service.response.ApiResponseWrapper;
import com.example.loan_service.service.InfoIncomeService;
import com.example.loan_service.service.LoanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/info-income")
@RequiredArgsConstructor
@Slf4j
public class InfoIncomeController {

    private final InfoIncomeService infoIncomeService;
    private final InfoIncomeMapper infoIncomeMapper;
    private final LoanService loanService;
    @PostMapping
    public ResponseEntity<ApiResponseWrapper<InfoIncomeResponseDto>> createInfoIncome(@RequestBody InfoIncomeRequestDto dto) {
        log.info("CREATE_INFO_INCOME_START {}",dto.toString());
        ApiResponseWrapper<InfoIncomeResponseDto> response = new ApiResponseWrapper<>();
        try {
            InfoIncome entity = infoIncomeMapper.toEntity(dto);
            entity.setLoan(loanService.getLoanById(dto.getLoanId()).orElse(null));
            InfoIncome saved = infoIncomeService.createInfoIncome(entity);
            response.setData(infoIncomeMapper.toDto(saved));
            response.setStatus(HttpStatus.OK.value());
            response.setMessage("InfoIncome created successfully");
            log.info("CREATE_INFO_INCOME_SUCCESS - infoId: {}", saved.getInfoId());
        } catch (Exception e) {
            log.error("CREATE_INFO_INCOME_ERROR - error: {}", e.getMessage(), e);
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.setMessage("Failed to create InfoIncome: " + e.getMessage());
        }
        return new ResponseEntity<>(response, HttpStatus.valueOf(response.getStatus()));
    }

    @PutMapping("/{infoId}")
    public ResponseEntity<ApiResponseWrapper<InfoIncomeResponseDto>> updateInfoIncome(@PathVariable Long infoId, @RequestBody InfoIncomeRequestDto dto) {
        log.info("UPDATE_INFO_INCOME_START - infoId: {}", infoId);
        ApiResponseWrapper<InfoIncomeResponseDto> response = new ApiResponseWrapper<>();
        try {
            InfoIncome entity = infoIncomeMapper.toEntity(dto);
            entity.setLoan(loanService.getLoanById(dto.getLoanId()).orElse(null));
            entity.setInfoId(infoId);
            InfoIncome updated = infoIncomeService.updateInfoIncome(entity);
            response.setData(infoIncomeMapper.toDto(updated));
            response.setStatus(HttpStatus.OK.value());
            response.setMessage("InfoIncome updated successfully");
            log.info("UPDATE_INFO_INCOME_SUCCESS - infoId: {}", infoId);
        } catch (Exception e) {
            log.error("UPDATE_INFO_INCOME_ERROR - infoId: {}, error: {}", infoId, e.getMessage(), e);
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.setMessage("Failed to update InfoIncome: " + e.getMessage());
        }
        return new ResponseEntity<>(response, HttpStatus.valueOf(response.getStatus()));
    }

    @GetMapping("/loan/{loanId}")
    public ResponseEntity<ApiResponseWrapper<List<InfoIncomeResponseDto>>> getByLoanId(@PathVariable Long loanId) {
        log.info("GET_INFO_INCOME_BY_LOAN_START - loanId: {}", loanId);
        ApiResponseWrapper<List<InfoIncomeResponseDto>> response = new ApiResponseWrapper<>();
        try {
            List<InfoIncome> list = infoIncomeService.getByLoanId(loanId);
            List<InfoIncomeResponseDto> dtos = list.stream()
                    .map(infoIncomeMapper::toDto)
                    .collect(Collectors.toList());
            response.setData(dtos);
            response.setStatus(HttpStatus.OK.value());
            response.setMessage("InfoIncome list retrieved successfully");
            log.info("GET_INFO_INCOME_BY_LOAN_SUCCESS - loanId: {}, total: {}", loanId, dtos.size());
        } catch (Exception e) {
            log.error("GET_INFO_INCOME_BY_LOAN_ERROR - loanId: {}, error: {}", loanId, e.getMessage(), e);
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.setMessage("Failed to retrieve InfoIncome list: " + e.getMessage());
        }
        return new ResponseEntity<>(response, HttpStatus.valueOf(response.getStatus()));
    }
}
