package com.example.corebanking_service.Config;

<<<<<<< HEAD
import lombok.RequiredArgsConstructor;
=======
<<<<<<< HEAD
import org.springframework.beans.factory.annotation.Value;
=======
import lombok.RequiredArgsConstructor;
>>>>>>> 15e536bc976093c4e921fde702250bbcb4776b94
>>>>>>> main
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
<<<<<<< HEAD

=======
>>>>>>> main
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;


@Configuration
@RequiredArgsConstructor
@EnableWebSecurity
public class SecurityConfig {

<<<<<<< HEAD
=======
<<<<<<< HEAD
    @Value("${core-banking.api.key}")
    private String apiKey;
=======
>>>>>>> main
    private final ApiKeyFilter apiKeyFilter;

>>>>>>> 15e536bc976093c4e921fde702250bbcb4776b94
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
<<<<<<< HEAD
=======
<<<<<<< HEAD
                .addFilterBefore(new ApiKeyFilter(apiKey), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }


=======
>>>>>>> main
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .addFilterBefore(apiKeyFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

<<<<<<< HEAD
=======
>>>>>>> 15e536bc976093c4e921fde702250bbcb4776b94
>>>>>>> main

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}