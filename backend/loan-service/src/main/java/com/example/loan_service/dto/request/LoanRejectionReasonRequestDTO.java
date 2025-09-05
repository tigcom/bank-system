package com.example.loan_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanRejectionReasonRequestDTO {
    @NotBlank(message = "{loanRejection.reason.notBlank}")
    private String reason;

    @NotNull(message = "{loanRejection.loanId.notNull}")
    private Long loan_id;
}
