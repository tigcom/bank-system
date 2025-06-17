package com.example.customer_service.services;

import com.example.customer_service.dtos.KycRequest;
import com.example.customer_service.dtos.RegisterCustomerDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.LinkedHashMap;

@RequiredArgsConstructor
@Slf4j
@Service
public class RegistrationCacheService {
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final Duration ttl = Duration.ofMinutes(30);

    public void saveRegistrationData(String email, RegisterCustomerDTO request) {
        try {
            log.info("Saving registration data for email: {}", email);
            redisTemplate.opsForValue().set("registration:" + email, request, ttl);
            log.info("Registration data saved successfully for email: {}", email);
        } catch (Exception e) {
            log.error("Error saving registration data to Redis for email: {}. Details: {}", email, e.getMessage(), e);
            throw new RuntimeException("Unable to connect to Redis: " + e.getMessage(), e);
        }
    }

    public RegisterCustomerDTO getRegistrationData(String email) {
        try {
            Object data = redisTemplate.opsForValue().get("registration:" + email);
            log.info("Retrieved registration data for email: {}", email);

            if (data == null) {
                log.warn("Registration data does not exist or has expired for email: {}", email);
                return null;
            }

            if (data instanceof LinkedHashMap) {
                return objectMapper.convertValue(data, RegisterCustomerDTO.class);
            }

            if (data instanceof RegisterCustomerDTO) {
                return (RegisterCustomerDTO) data;
            }

            log.error("Invalid data format: {}", data.getClass().getName());
            throw new IllegalStateException("Invalid data format: " + data.getClass().getName());
        } catch (Exception e) {
            log.error("Error retrieving registration data from Redis for email: {}. Details: {}", email, e.getMessage(), e);
            throw new RuntimeException("Unable to connect to Redis: " + e.getMessage(), e);
        }
    }

    public void updateRegistrationWithKyc(String email, KycRequest kycRequest) {
        try {
            log.info("Updating registration with KYC data for email: {}", email);
            redisTemplate.opsForValue().set("kyc:" + email, kycRequest, ttl);
            log.info("KYC data saved successfully for email: {}", email);
        } catch (Exception e) {
            log.error("Error saving KYC data to Redis for email: {}. Details: {}", email, e.getMessage(), e);
            throw new RuntimeException("Unable to connect to Redis: " + e.getMessage(), e);
        }
    }

    public KycRequest getKycData(String email) {
        try {
            Object data = redisTemplate.opsForValue().get("kyc:" + email);
            log.info("Retrieved KYC data for email: {}", email);

            if (data == null) {
                log.warn("KYC data does not exist or has expired for email: {}", email);
                return null;
            }

            if (data instanceof LinkedHashMap) {
                return objectMapper.convertValue(data, KycRequest.class);
            }

            if (data instanceof KycRequest) {
                return (KycRequest) data;
            }

            log.error("Invalid KYC data format: {}", data.getClass().getName());
            throw new IllegalStateException("Invalid KYC data format: " + data.getClass().getName());
        } catch (Exception e) {
            log.error("Error retrieving KYC data from Redis for email: {}. Details: {}", email, e.getMessage(), e);
            throw new RuntimeException("Unable to connect to Redis: " + e.getMessage(), e);
        }
    }

    public void clearRegistrationData(String email) {
        try {
            redisTemplate.delete("registration:" + email);
            redisTemplate.delete("kyc:" + email);
            log.info("Registration and KYC data cleared successfully for email: {}", email);
        } catch (Exception e) {
            log.error("Error clearing registration data from Redis for email: {}. Details: {}", email, e.getMessage(), e);
            throw new RuntimeException("Unable to connect to Redis: " + e.getMessage(), e);
        }
    }
}
