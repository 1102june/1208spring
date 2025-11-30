package com.example.youth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource())) // CORS 설정 추가
            .csrf(csrf -> csrf.disable()) // REST API이므로 CSRF 비활성화
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // JWT 사용 시 STATELESS
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/auth/**").permitAll() // 인증 관련 엔드포인트는 모두 허용
                .requestMatchers("/api/main/**").permitAll() // 메인 페이지 (추후 인증 추가 시 authenticated()로 변경)
                .requestMatchers("/api/user/**").permitAll() // 사용자 정보 API (추후 인증 추가 시 authenticated()로 변경)
                .requestMatchers("/api/profile/**").permitAll() // 프로필 API (추후 인증 추가 시 authenticated()로 변경)
                .requestMatchers("/api/interests/**").permitAll() // 관심사 API (추후 인증 추가 시 authenticated()로 변경)
                .requestMatchers("/api/housing/**").permitAll() // 임대주택 API (추후 인증 추가 시 authenticated()로 변경)
                .requestMatchers("/api/policy/**").permitAll() // 청년정책 API (추후 인증 추가 시 authenticated()로 변경)
                .requestMatchers("/api/chat/**").permitAll() // 챗봇 API (추후 인증 추가 시 authenticated()로 변경)
                .requestMatchers("/api/activity/**").permitAll() // 사용자 활동 로그 API (추후 인증 추가 시 authenticated()로 변경)
                .requestMatchers("/api/ai/**").permitAll() // AI 추천 API (추후 인증 추가 시 authenticated()로 변경)
                .requestMatchers("/api/admin/**").permitAll() // 관리자 API (추후 인증 추가 시 ADMIN 권한 필요)
                .anyRequest().permitAll() // 개발 단계에서는 나머지 허용 (필요시 변경)
            );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // 허용할 Origin 설정 (개발/배포 환경 모두 포함)
        configuration.setAllowedOrigins(Arrays.asList(
            "http://localhost:8080",
            "http://127.0.0.1:8080"
            // 배포 시 추가: "http://서버IP:8080", "https://your-domain.com"
        ));
        
        // 개발 환경에서 모든 Origin 허용하려면 주석 해제 (보안 주의)
        // configuration.setAllowedOriginPatterns(Arrays.asList("*"));
        
        // 허용할 HTTP 메서드
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        
        // 허용할 헤더
        configuration.setAllowedHeaders(Arrays.asList("*"));
        
        // 인증 정보 허용 (쿠키, Authorization 헤더 등)
        configuration.setAllowCredentials(true);
        
        // Preflight 요청 캐시 시간 (초)
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}

