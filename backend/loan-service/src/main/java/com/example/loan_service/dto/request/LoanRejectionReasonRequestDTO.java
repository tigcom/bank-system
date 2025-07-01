package com.example.loan_service.dto.request;

import com.example.loan_service.entity.Loan;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanRejectionReasonRequestDTO {
    private String reason;
    private Long loan_id;
}