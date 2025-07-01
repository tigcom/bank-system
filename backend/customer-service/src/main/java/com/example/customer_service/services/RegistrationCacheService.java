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
            log.info("Lưu dữ liệu đăng ký cho email: {}", email);
            redisTemplate.opsForValue().set("registration:" + email, request, ttl);
            log.info("Dữ liệu đăng ký đã được lưu thành công cho email: {}", email);
        } catch (Exception e) {
            log.error("Lỗi khi lưu dữ liệu đăng ký vào Redis cho email: {}. Details: {}", email, e.getMessage(), e);
            throw new RuntimeException("Không thể kết nối với Redis: " + e.getMessage(), e);
        }
    }

    public RegisterCustomerDTO getRegistrationData(String email) {
        try {
            Object data = redisTemplate.opsForValue().get("registration:" + email);
            log.info("Đã lấy dữ liệu đăng ký cho email: {}", email);

            if (data == null) {
                log.warn("Dữ liệu đăng ký không tồn tại hoặc đã hết hạn cho email: {}", email);
                return null;
            }

            // Nếu dữ liệu là LinkedHashMap (do Redis trả về JSON), chuyển đổi thành RegisterCustomerDTO bằng ObjectMapper.
            if (data instanceof LinkedHashMap) {
                return objectMapper.convertValue(data, RegisterCustomerDTO.class);
            }

            if (data instanceof RegisterCustomerDTO) {
                return (RegisterCustomerDTO) data;
            }

            log.error("Định dạng dữ liệu không hợp lệ: {}", data.getClass().getName());
            throw new IllegalStateException("Định dạng dữ liệu không hợp lệ: " + data.getClass().getName());
        } catch (Exception e) {
            log.error("Lỗi khi lấy dữ liệu đăng ký từ Redis cho email: {}. Details: {}", email, e.getMessage(), e);
            throw new RuntimeException("Không thể kết nối với Redis: " + e.getMessage(), e);
        }
    }

    public void updateRegistrationWithKyc(String email, KycRequest kycRequest) {
        try {
            log.info("Cập nhật đăng ký với dữ liệu KYC cho email: {}", email);
            redisTemplate.opsForValue().set("kyc:" + email, kycRequest, ttl);
            log.info("Dữ liệu KYC đã được lưu thành công cho email: {}", email);
        } catch (Exception e) {
            log.error("Lỗi khi lưu dữ liệu KYC vào Redis để gửi email: {}. Details: {}", email, e.getMessage(), e);
            throw new RuntimeException("Không thể kết nối với Redis: " + e.getMessage(), e);
        }
    }

    public KycRequest getKycData(String email) {
        try {
            Object data = redisTemplate.opsForValue().get("kyc:" + email);
            log.info("Đã lấy dữ liệu KYC cho email: {}", email);

            if (data == null) {
                log.warn("Dữ liệu KYC không tồn tại hoặc đã hết hạn cho email: {}", email);
                return null;
            }

            if (data instanceof LinkedHashMap) {
                return objectMapper.convertValue(data, KycRequest.class);
            }

            if (data instanceof KycRequest) {
                return (KycRequest) data;
            }

            log.error("Định dạng dữ liệu KYC không hợp lệ: {}", data.getClass().getName());
            throw new IllegalStateException("Định dạng dữ liệu KYC không hợp lệ: " + data.getClass().getName());
        } catch (Exception e) {
            log.error("Lỗi khi truy xuất dữ liệu KYC từ Redis cho email: {}. Details: {}", email, e.getMessage(), e);
            throw new RuntimeException("Không thể kết nối với Redis: " + e.getMessage(), e);
        }
    }

    public void clearRegistrationData(String email) {
        try {
            redisTemplate.delete("registration:" + email);
            redisTemplate.delete("kyc:" + email);
            log.info("Đã xóa dữ liệu đăng ký và KYC thành công cho email: {}", email);
        } catch (Exception e) {
            log.error("Lỗi khi xóa dữ liệu đăng ký từ Redis cho email: {}. Details: {}", email, e.getMessage(), e);
            throw new RuntimeException("Không thể kết nối với Redis: " + e.getMessage(), e);
        }
    }
}
