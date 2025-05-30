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
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        // Kiểm tra nếu người dùng chưa đăng nhập (authentication là null hoặc không có tên)
        if (auth == null || auth.getName() == null) {
            log.info("No user is authenticated, returning anonymous");
            return Optional.of("anonymousUser");
        }
        String userId = auth.getName();
        CustomerDTO currentCustomer = commonService.getCurrentCustomer(userId);

        log.info("Current Auditor: " + currentCustomer.getUsername());
        return Optional.of(currentCustomer.getUsername());
    }
}