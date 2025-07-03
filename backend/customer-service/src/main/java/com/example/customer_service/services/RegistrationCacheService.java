package com.example.customer_service.services;

import com.example.customer_service.dtos.KycRequest;
import com.example.customer_service.dtos.RegisterCustomerDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class RegistrationCacheService {
    
    private static final Logger log = LoggerFactory.getLogger(RegistrationCacheService.class);
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final Duration ttl = Duration.ofMinutes(30);

    public void saveRegistrationData(String email, RegisterCustomerDTO request) {
        String requestId = UUID.randomUUID().toString();
        log.info("SAVE_REGISTRATION_DATA - RequestId: {}, Email: {}", requestId, email);
        
        try {
            redisTemplate.opsForValue().set("registration:" + email, request, ttl);
            log.info("SAVE_REGISTRATION_DATA_SUCCESS - RequestId: {}, Email: {}", requestId, email);
        } catch (Exception e) {
            log.error("SAVE_REGISTRATION_DATA_FAILED - RequestId: {}, Email: {}, Error: {}", requestId, email, e.getMessage(), e);
            throw new RuntimeException("Unable to connect to Redis: " + e.getMessage(), e);
        }
    }

    public RegisterCustomerDTO getRegistrationData(String email) {
        String requestId = UUID.randomUUID().toString();
        log.info("GET_REGISTRATION_DATA - RequestId: {}, Email: {}", requestId, email);
        
        try {
            Object data = redisTemplate.opsForValue().get("registration:" + email);

            if (data == null) {
                log.warn("GET_REGISTRATION_DATA_NOT_FOUND - RequestId: {}, Email: {}", requestId, email);
                return null;
            }

            // Nếu dữ liệu là LinkedHashMap (do Redis trả về JSON), chuyển đổi thành RegisterCustomerDTO bằng ObjectMapper.
            if (data instanceof LinkedHashMap) {
                RegisterCustomerDTO result = objectMapper.convertValue(data, RegisterCustomerDTO.class);
                log.info("GET_REGISTRATION_DATA_SUCCESS - RequestId: {}, Email: {}", requestId, email);
                return result;
            }

            if (data instanceof RegisterCustomerDTO) {
                log.info("GET_REGISTRATION_DATA_SUCCESS - RequestId: {}, Email: {}", requestId, email);
                return (RegisterCustomerDTO) data;
            }

            log.error("GET_REGISTRATION_DATA_INVALID_FORMAT - RequestId: {}, Email: {}, Format: {}", 
                    requestId, email, data.getClass().getName());
            throw new IllegalStateException("Invalid data format: " + data.getClass().getName());
        } catch (Exception e) {
            log.error("GET_REGISTRATION_DATA_FAILED - RequestId: {}, Email: {}, Error: {}", requestId, email, e.getMessage(), e);
            throw new RuntimeException("Unable to connect to Redis: " + e.getMessage(), e);
        }
    }

    public void updateRegistrationWithKyc(String email, KycRequest kycRequest) {
        String requestId = UUID.randomUUID().toString();
        log.info("UPDATE_REGISTRATION_WITH_KYC - RequestId: {}, Email: {}, IdentityNumber: {}", 
                requestId, email, kycRequest.getIdentityNumber());
        
        try {
            redisTemplate.opsForValue().set("kyc:" + email, kycRequest, ttl);
            log.info("UPDATE_REGISTRATION_WITH_KYC_SUCCESS - RequestId: {}, Email: {}", requestId, email);
        } catch (Exception e) {
            log.error("UPDATE_REGISTRATION_WITH_KYC_FAILED - RequestId: {}, Email: {}, Error: {}", requestId, email, e.getMessage(), e);
            throw new RuntimeException("Unable to connect to Redis: " + e.getMessage(), e);
        }
    }

    public KycRequest getKycData(String email) {
        String requestId = UUID.randomUUID().toString();
        log.info("GET_KYC_DATA - RequestId: {}, Email: {}", requestId, email);
        
        try {
            Object data = redisTemplate.opsForValue().get("kyc:" + email);

            if (data == null) {
                log.warn("GET_KYC_DATA_NOT_FOUND - RequestId: {}, Email: {}", requestId, email);
                return null;
            }

            if (data instanceof LinkedHashMap) {
                KycRequest result = objectMapper.convertValue(data, KycRequest.class);
                log.info("GET_KYC_DATA_SUCCESS - RequestId: {}, Email: {}", requestId, email);
                return result;
            }

            if (data instanceof KycRequest) {
                log.info("GET_KYC_DATA_SUCCESS - RequestId: {}, Email: {}", requestId, email);
                return (KycRequest) data;
            }

            log.error("GET_KYC_DATA_INVALID_FORMAT - RequestId: {}, Email: {}, Format: {}", 
                    requestId, email, data.getClass().getName());
            throw new IllegalStateException("Invalid KYC data format: " + data.getClass().getName());
        } catch (Exception e) {
            log.error("GET_KYC_DATA_FAILED - RequestId: {}, Email: {}, Error: {}", requestId, email, e.getMessage(), e);
            throw new RuntimeException("Unable to connect to Redis: " + e.getMessage(), e);
        }
    }

    public void clearRegistrationData(String email) {
        String requestId = UUID.randomUUID().toString();
        log.info("CLEAR_REGISTRATION_DATA - RequestId: {}, Email: {}", requestId, email);
        
        try {
            redisTemplate.delete("registration:" + email);
            redisTemplate.delete("kyc:" + email);
            log.info("CLEAR_REGISTRATION_DATA_SUCCESS - RequestId: {}, Email: {}", requestId, email);
        } catch (Exception e) {
            log.error("CLEAR_REGISTRATION_DATA_FAILED - RequestId: {}, Email: {}, Error: {}", requestId, email, e.getMessage(), e);
            throw new RuntimeException("Unable to connect to Redis: " + e.getMessage(), e);
        }
    }
}
