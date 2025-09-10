package com.master.masterservice.service;

import com.master.masterservice.dto.request.CardRegistrationRequest;
import com.master.masterservice.dto.response.MasterCardResponse;

public interface MasterService {
    MasterCardResponse registerCard(CardRegistrationRequest request);
} 