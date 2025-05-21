package com.example.customer_service.services.Impl;

import com.example.customer_service.responses.KycResponse;
import com.example.customer_service.services.KycService;
import org.springframework.stereotype.Service;

@Service
public class KycServiceImpl implements KycService {
//    @Override
//    public KycResponse verifyIdentity(String identityNumber, String fullName) {
//        KycResponse response = new KycResponse();
//        response.setVerified(true);
//        response.setMessage("Xác minh thành công");
//        response.setDetails("{\"score\": 0.95, \"details\": \"Identity matched\"}");
//        return response;
//    }
    @Override
    public KycResponse verifyIdentity(String identityNumber, String fullName) {
        KycResponse response = new KycResponse();
        response.setVerified(false);
        response.setMessage("Xác minh thất bại");
        response.setDetails("{\"score\": 0.1, \"details\": \"Identity not matched\"}");
        return response;
    }
}