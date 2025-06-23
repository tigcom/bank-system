package com.example.common_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MailMessageDTO implements Serializable {
    private String recipient;
    private String recipientName;
    private String subject;
    private String body;
}
