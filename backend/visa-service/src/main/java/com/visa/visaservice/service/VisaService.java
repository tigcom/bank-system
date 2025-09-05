package com.visa.visaservice.service;

import com.visa.visaservice.dto.request.CardRegistrationRequest;
import com.visa.visaservice.dto.response.VisaCardResponse;

public interface VisaService {
    VisaCardResponse registerCard(CardRegistrationRequest request);
}