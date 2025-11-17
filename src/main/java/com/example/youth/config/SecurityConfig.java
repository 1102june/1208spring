package com.example.youth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // REST API이므로 CSRF 비활성화
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // JWT 사용 시 STATELESS
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/auth/**").permitAll() // 인증 관련 엔드포인트는 모두 허용
                .requestMatchers("/api/main/**").permitAll() // 메인 페이지 (추후 인증 추가 시 authenticated()로 변경)
                .requestMatchers("/api/user/**").permitAll() // 개발 단계에서는 허용 (추후 인증 추가 시 authenticated()로 변경)
                .requestMatchers("/api/housing/**").permitAll() // 임대주택 API (추후 인증 추가 시 authenticated()로 변경)
                .requestMatchers("/api/admin/**").permitAll() // 관리자 API (추후 인증 추가 시 ADMIN 권한 필요)
                .anyRequest().permitAll() // 개발 단계에서는 나머지 허용 (필요시 변경)
            );

        return http.build();
    }
}

