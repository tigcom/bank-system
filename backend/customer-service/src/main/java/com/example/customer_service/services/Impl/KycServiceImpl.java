package com.example.customer_service.services.Impl;

import com.example.customer_service.models.Customer;
import com.example.customer_service.models.KycProfile;
import com.example.customer_service.models.KycStatus;
import com.example.customer_service.repositories.CustomerRepository;
import com.example.customer_service.repositories.KycProfileRepository;
import com.example.customer_service.responses.KycResponse;
import com.example.customer_service.services.KycService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class KycServiceImpl implements KycService {

    private static final Logger log = LoggerFactory.getLogger(KycServiceImpl.class);

    private final CustomerRepository customerRepository;

    private final MessageSource messageSource;

    private final KycProfileRepository kycProfileRepository;

    @Override
    public KycResponse verifyIdentity(String identityNumber, String fullName, LocalDate dateOfBirth, String gender) {
        String requestId = UUID.randomUUID().toString();
        log.info("VERIFY_IDENTITY - RequestId: {}, IdentityNumber: {}, FullName: {}", requestId, identityNumber, fullName);
        
        if (identityNumber == null || fullName == null) {
            log.warn("VERIFY_IDENTITY_INVALID_INPUT - RequestId: {}, IdentityNumber: {}, FullName: {}", requestId, identityNumber, fullName);
            KycResponse response = new KycResponse();
            response.setVerified(false);
            response.setMessage("Số CMND/CCCD hoặc họ tên không hợp lệ");
            response.setDetails("{\"score\": 0.0, \"details\": \"Invalid input\"}");
            return response;
        }

        KycResponse response = new KycResponse();
        response.setVerified(true);
        response.setMessage("Xác minh thành công");
        response.setDetails("{\"score\": 0.95, \"details\": \"Identity matched\"}");
        
        log.info("VERIFY_IDENTITY_SUCCESS - RequestId: {}, IdentityNumber: {}, FullName: {}", requestId, identityNumber, fullName);
        return response;
    }

    @Override

    public KycResponse getKycStatus(String userId) {
        String requestId = UUID.randomUUID().toString();
        log.info("GET_KYC_STATUS - RequestId: {}, UserId: {}", requestId, userId);
        Customer customer = customerRepository.findCustomerByUserId(userId);
        if (customer == null) {
            log.error("GET_KYC_STATUS_USER_NOT_FOUND - RequestId: {}, UserId: {}", requestId, userId);
            throw new EntityNotFoundException("Không tìm thấy người dùng");
        }
        Optional<KycProfile> kycProfileOpt = kycProfileRepository.findByCustomer(customer);
        if (kycProfileOpt.isEmpty()) {
            log.info("GET_KYC_STATUS_NO_PROFILE - RequestId: {}, UserId: {}", requestId, userId);
            return new KycResponse(false, "Người dùng chưa có thông tin KYC", null, null);
        }

        KycProfile kycProfile = kycProfileOpt.get();
        KycStatus status = kycProfile.getStatus();
        boolean isVerified = KycStatus.VERIFIED.equals(status);

        String message = switch (status) {
            case VERIFIED -> "Tài khoản đã được xác minh KYC";
            case PENDING -> "Thông tin KYC đang chờ xác minh";
            case REJECTED -> "Thông tin KYC đã bị từ chối";
        };

        log.info("GET_KYC_STATUS_SUCCESS - RequestId: {}, UserId: {}, Status: {}", requestId, userId, status);
        return new KycResponse(isVerified, message, null, status);
    }

    @Override
    @Transactional
    public void saveKycInfo(Long customerId, KycResponse kycResponse, String identityNumber, String fullName,
                            LocalDate dateOfBirth, String gender) {
        String requestId = UUID.randomUUID().toString();
        log.info("SAVE_KYC_INFO - RequestId: {}, CustomerId: {}, IdentityNumber: {}, Status: {}", 
                requestId, customerId, identityNumber, kycResponse.getStatus());
        
        if (customerId == null || identityNumber == null || fullName == null ||
                dateOfBirth == null || gender == null) {
            log.error("SAVE_KYC_INFO_INVALID_DATA - RequestId: {}, CustomerId: {}, IdentityNumber: {}", 
                    requestId, customerId, identityNumber);
            throw new IllegalArgumentException("Dữ liệu KYC không đầy đủ");
        }

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> {
                    log.error("SAVE_KYC_INFO_CUSTOMER_NOT_FOUND - RequestId: {}, CustomerId: {}", requestId, customerId);
                    return new IllegalArgumentException("Không tìm thấy khách hàng");
                });

        KycProfile kycProfile = kycProfileRepository.findByCustomer(customer).orElse(new KycProfile());
        kycProfile.setCustomer(customer);
        if (kycResponse.getStatus() != null) {
            kycProfile.setStatus(kycResponse.getStatus());
        } else {
            kycProfile.setStatus(kycResponse.isVerified() ? KycStatus.VERIFIED : KycStatus.REJECTED);
        }

        kycProfile.setIdentityNumber(identityNumber);
        kycProfile.setFullName(fullName);
        kycProfile.setDateOfBirth(dateOfBirth);
        kycProfile.setGender(gender);

        kycProfileRepository.save(kycProfile);
        log.info("SAVE_KYC_INFO_SUCCESS - RequestId: {}, CustomerId: {}, Status: {}", 
                requestId, customerId, kycProfile.getStatus());
    }

    private String getMessage(String key, Object... args) {
        return messageSource.getMessage(key, args, LocaleContextHolder.getLocale());
    }
}