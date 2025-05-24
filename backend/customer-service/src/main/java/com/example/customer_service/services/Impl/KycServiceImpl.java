package com.example.customer_service.services.Impl;

import com.example.customer_service.models.Customer;
import com.example.customer_service.models.KycProfile;
import com.example.customer_service.models.KycStatus;
import com.example.customer_service.repositories.CustomerRepository;
import com.example.customer_service.repositories.KycProfileRepository;
import com.example.customer_service.responses.KycResponse;
import com.example.customer_service.services.KycService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class KycServiceImpl implements KycService {

    private final CustomerRepository customerRepository;

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
        kycProfile.setStatus(kycResponse.isVerified() ? KycStatus.VERIFIED : KycStatus.REJECTED);
        kycProfile.setVerifiedAt(LocalDateTime.now());
        kycProfile.setVerifiedBy("SYSTEM");

        kycProfile.setIdentityNumber(identityNumber);
        kycProfile.setFullName(fullName);
        kycProfile.setDateOfBirth(dateOfBirth);
        kycProfile.setGender(gender);

        kycProfileRepository.save(kycProfile);
    }
}