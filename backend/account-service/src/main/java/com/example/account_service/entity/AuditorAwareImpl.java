package com.example.account_service.entity;

import com.example.common_service.dto.CustomerDTO;
import com.example.common_service.services.CommonService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
public class AuditorAwareImpl implements AuditorAware<String> {

    @Autowired
    private CommonService commonService;

    @Override
    public Optional<String> getCurrentAuditor() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();

            // Kiểm tra authentication
            if (auth == null || auth.getName() == null || "anonymousUser".equals(auth.getName())) {
                log.info("No user is authenticated or user is anonymous, returning 'system'");
                return Optional.of("system"); // hoặc "anonymousUser"
            }

            String userId = auth.getName();
            log.info("Getting auditor for userId: {}", userId);

            // Gọi service với try-catch
            CustomerDTO currentCustomer = null;
            try {
                currentCustomer = commonService.getCurrentCustomer(userId);
            } catch (Exception e) {
                log.error("Error getting current customer for audit: {}", e.getMessage());
                // Fallback về userId nếu service lỗi
                return Optional.of(userId);
            }

            // KIỂM TRA NULL trước khi sử dụng
            if (currentCustomer == null) {
                log.warn("Current customer is null for userId: {}, using userId as auditor", userId);
                return Optional.of(userId);
            }

            // Kiểm tra username có null không
            String username = currentCustomer.getUsername();
            if (username == null || username.trim().isEmpty()) {
                log.warn("Username is null/empty for customer, using userId: {}", userId);
                return Optional.of(userId);
            }

            log.info("Current Auditor: {}", username);
            return Optional.of(username);

        } catch (Exception e) {
            log.error("Unexpected error in getCurrentAuditor: {}", e.getMessage(), e);
            // Fallback cuối cùng
            return Optional.of("system");
        }
    }
}