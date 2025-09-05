package com.example.common_service.dto.response;


import com.example.common_service.models.KycStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class KycResponse {


    private boolean verified;

    private String message;

    private String details;

    private KycStatus status;
}