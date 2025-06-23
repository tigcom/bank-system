package com.example.transaction_service.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RequestLoggingFilterConfig {
    @Bean
    public FilterRegistrationBean<CustomRequestLoggingFilter> loggingFilter() {
        FilterRegistrationBean<CustomRequestLoggingFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new CustomRequestLoggingFilter());
        registrationBean.addUrlPatterns("/*");
        return registrationBean;
    }
}
