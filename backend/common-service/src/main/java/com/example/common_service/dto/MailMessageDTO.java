package com.example.common_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MailMessageDTO {
    private String recipient;
    private String recipientName;
    private String subject;
    private String body;
}
