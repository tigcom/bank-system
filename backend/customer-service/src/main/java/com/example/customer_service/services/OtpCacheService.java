package com.example.customer_service.services;

import com.example.customer_service.dtos.RegisterCustomerDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.UUID;

@RequiredArgsConstructor
@Getter
@Service
public class OtpCacheService {
    
    private static final Logger log = LoggerFactory.getLogger(OtpCacheService.class);
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final PasswordEncoder passwordEncoder;
    private final Duration ttl = Duration.ofMinutes(5);

    public void saveOtp(String email, String otp, RegisterCustomerDTO request) {
        String requestId = UUID.randomUUID().toString();
        log.info("SAVE_OTP - RequestId: {}, Email: {}", requestId, email);
        
        try {
            String hashedOtp = passwordEncoder.encode(otp);
            redisTemplate.opsForValue().set("otp:" + email, hashedOtp, ttl);
            redisTemplate.opsForValue().set("register:" + email, request, ttl);
            log.info("SAVE_OTP_SUCCESS - RequestId: {}, Email: {}", requestId, email);
        } catch (Exception e) {
            log.error("SAVE_OTP_FAILED - RequestId: {}, Email: {}, Error: {}", requestId, email, e.getMessage(), e);
            throw new RuntimeException("Unable to connect to Redis: " + e.getMessage(), e);
        }
    }

    public boolean isValidOtp(String email, String otp) {
        String requestId = UUID.randomUUID().toString();
        log.info("VALIDATE_OTP - RequestId: {}, Email: {}", requestId, email);
        
        try {
            String hashedOtp = (String) redisTemplate.opsForValue().get("otp:" + email);
            boolean isValid = hashedOtp != null && passwordEncoder.matches(otp, hashedOtp);
            log.info("VALIDATE_OTP_RESULT - RequestId: {}, Email: {}, IsValid: {}", requestId, email, isValid);
            return isValid;
        } catch (Exception e) {
            log.error("VALIDATE_OTP_FAILED - RequestId: {}, Email: {}, Error: {}", requestId, email, e.getMessage(), e);
            throw new RuntimeException("Unable to connect to Redis: " + e.getMessage(), e);
        }
    }

    public void clearOtp(String email) {
        String requestId = UUID.randomUUID().toString();
        log.info("CLEAR_OTP - RequestId: {}, Email: {}", requestId, email);
        
        try {
            redisTemplate.delete("otp:" + email);
            redisTemplate.delete("register:" + email);
            log.info("CLEAR_OTP_SUCCESS - RequestId: {}, Email: {}", requestId, email);
        } catch (Exception e) {
            log.error("CLEAR_OTP_FAILED - RequestId: {}, Email: {}, Error: {}", requestId, email, e.getMessage(), e);
            throw new RuntimeException("Unable to connect to Redis: " + e.getMessage(), e);
        }
    }
}

