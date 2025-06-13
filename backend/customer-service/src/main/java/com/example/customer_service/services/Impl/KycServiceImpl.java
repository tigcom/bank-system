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
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class KycServiceImpl implements KycService {

    private final CustomerRepository customerRepository;

    private final MessageSource messageSource;

    private final KycProfileRepository kycProfileRepository;

    @Override
    public KycResponse verifyIdentity(String identityNumber, String fullName) {
        if (identityNumber == null || fullName == null) {
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
        return response;
    }

    @Override
    public KycResponse getKycStatus(String userId) {
        Customer customer = customerRepository.findCustomerByUserId(userId);
        if (customer == null) {
            throw new EntityNotFoundException("Không tìm thấy người dùng");
        }

        Optional<KycProfile> kycProfileOpt = kycProfileRepository.findByCustomer(customer);

        if (kycProfileOpt.isEmpty()) {
            return new KycResponse(false, "Người dùng chưa có thông tin KYC", null, null);
        }

        KycProfile kycProfile = kycProfileOpt.get();
        KycStatus status = kycProfile.getStatus();
        boolean isVerified = KycStatus.VERIFIED.equals(status);

        String message = switch (status) {
            case VERIFIED -> "Tài khoản đã được xác minh KYC";
            case PENDING -> "Thông tin KYC đang chờ xác minh";
            case REJECTED -> "Thông tin KYC đã bị từ chối";
            default -> "Trạng thái KYC không xác định";
        };

        return new KycResponse(isVerified, message, null, status);
    }


    @Override
    @Transactional
    public void saveKycInfo(Long customerId, KycResponse kycResponse, String identityNumber, String fullName,
                            LocalDate dateOfBirth, String gender) {
        if (customerId == null || identityNumber == null || fullName == null ||
                dateOfBirth == null || gender == null) {
            throw new IllegalArgumentException("Dữ liệu KYC không đầy đủ");
        }

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy khách hàng"));

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
    }

    private String getMessage(String key, Object... args) {
        return messageSource.getMessage(key, args, LocaleContextHolder.getLocale());
    }
}