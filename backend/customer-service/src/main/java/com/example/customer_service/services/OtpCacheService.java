package com.example.customer_service.services;

import com.example.customer_service.dtos.RegisterCustomerDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.LinkedHashMap;

@RequiredArgsConstructor
@Slf4j
@Getter
@Service
public class OtpCacheService {
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final PasswordEncoder passwordEncoder;
    private final Duration ttl = Duration.ofMinutes(5);


    public void saveOtp(String email, String otp, RegisterCustomerDTO request) {
        try {
            log.info("Bawst đầu lưu OTP cho email: {}", email);
            String hashedOtp = passwordEncoder.encode(otp);
            redisTemplate.opsForValue().set("otp:" + email, hashedOtp, ttl);
            redisTemplate.opsForValue().set("register:" + email, request, ttl);
            log.info("Lưu OTP thành công cho email: {}", email);
        } catch (Exception e) {
            log.error("Lỗi khi lưu OTP vào Redis cho email: {}. Chi tiết: {}", email, e.getMessage(), e);
            throw new RuntimeException("Unable to connect to Redis: " + e.getMessage(), e);
        }
    }

    public boolean isValidOtp(String email, String otp) {
        try {
            log.info("Kiểm tra OTP cho email: {}", email);
            String hashedOtp = (String) redisTemplate.opsForValue().get("otp:" + email);
            boolean isValid = hashedOtp != null && passwordEncoder.matches(otp, hashedOtp);
            log.info("Kiểm tra OTP cho email: {}, hợp lệ: {}", email, isValid);
            return isValid;
        } catch (Exception e) {
            log.error("Lỗi khi kiểm tra OTP từ Redis cho email: {}. Chi tiết: {}", email, e.getMessage(), e);
            throw new RuntimeException("Unable to connect to Redis: " + e.getMessage(), e);
        }
    }

    public RegisterCustomerDTO getRegisterData(String email) {
        try {
            Object data = redisTemplate.opsForValue().get("register:" + email);
            log.info("Dữ liệu từ Redis cho email {}: {}", email, data);

            if (data == null) {
                log.warn("Dữ liệu đăng ký không tồn tại hoặc đã hết hạn cho email: {}", email);
                throw new IllegalArgumentException("Dữ liệu đăng ký không tồn tại hoặc đã hết hạn");
            }

            if (data instanceof LinkedHashMap) {
                log.warn("Dữ liệu là LinkedHashMap, convert sang RegisterCustomerDTO");
                return objectMapper.convertValue(data, RegisterCustomerDTO.class);
            }

            if (data instanceof RegisterCustomerDTO) {
                return (RegisterCustomerDTO) data;
            }

            log.error("Dữ liệu không đúng định dạng: {}", data.getClass().getName());
            throw new IllegalStateException("Dữ liệu không đúng định dạng: " + data.getClass().getName());
        } catch (Exception e) {
            log.error("Lỗi khi lấy dữ liệu đăng ký từ Redis cho email: {}. Chi tiết: {}", email, e.getMessage(), e);
            throw new RuntimeException("Unable to connect to Redis: " + e.getMessage(), e);
        }
    }


    public void clearOtp(String email) {
        try {
            redisTemplate.delete("otp:" + email);
            redisTemplate.delete("register:" + email);
            log.info("Xóa OTP thành công cho email: {}", email);
        } catch (Exception e) {
            log.error("Lỗi khi xóa OTP từ Redis cho email: {}. Chi tiết: {}", email, e.getMessage(), e);
            throw new RuntimeException("Unable to connect to Redis: " + e.getMessage(), e);
        }
    }
}

