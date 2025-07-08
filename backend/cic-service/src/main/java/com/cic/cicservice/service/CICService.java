package com.cic.cicservice.service;

import com.example.common_service.dto.request.CICRequest;
import com.cic.cicservice.dto.response.CicResponse;

public interface CICService {
    CicResponse checkCIC(CICRequest cicRequest);
}
