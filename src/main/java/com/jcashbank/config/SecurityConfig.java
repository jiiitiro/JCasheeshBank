package com.jcashbank.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security is used only for CSRF protection and security headers.
 * Login, logout and session checks are still handled by the app's own controllers
 * (AuthController + SessionUserProvider), so every route is permitted here.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                // CSRF stays enabled (default); Thymeleaf th:action forms get the hidden _csrf field automatically
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                // Let AuthController handle POST /logout instead of Spring Security's LogoutFilter
                .logout(AbstractHttpConfigurer::disable);

        return http.build();
    }
}
