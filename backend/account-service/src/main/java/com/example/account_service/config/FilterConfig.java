package com.example.account_service.config;

import com.example.account_service.filter.CustomRequestLoggingFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered; // Import này để dùng HIGHEST_PRECEDENCE

@Configuration
public class FilterConfig {

    @Bean
    public FilterRegistrationBean<CustomRequestLoggingFilter> loggingFilter() {
        FilterRegistrationBean<CustomRequestLoggingFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new CustomRequestLoggingFilter());
        registrationBean.addUrlPatterns("/*"); // hoặc chỉ định URL cụ thể
        return registrationBean;
    }
}