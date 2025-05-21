package com.example.loan_service.dto.response;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class NotificationResponseDTO {
    private Long notifId;
    private Long loanId;
    private LocalDateTime sendDate;
    private String type;
    private String message;
}