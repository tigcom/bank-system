package com.example.transaction_service.config;

import com.example.common_service.dto.CustomerDTO;
import com.example.common_service.services.CommonService;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
@RequiredArgsConstructor
public class JpaAuditingConfig {
    @DubboReference
    private final CommonService commonService;
    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return Optional.empty();
            }

            String userId = authentication.getName();
            CustomerDTO currentCustomer = commonService.getCurrentCustomer(userId);
            return Optional.ofNullable(currentCustomer.getUsername());
        };
    }


}
