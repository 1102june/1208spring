package com.example.youth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Passkey 로그인 요청 생성 응답
 * 클라이언트가 Passkey 로그인을 시작하기 전에 서버에서 받아야 하는 데이터
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasskeyLoginRequestResponse {
    /**
     * 서버에서 생성한 challenge (Base64 URL-safe 인코딩)
     */
    private String challenge;
    
    /**
     * Relying Party ID (도메인)
     */
    private String rpId;
    
    /**
     * 타임아웃 (밀리초)
     */
    private Long timeout;
    
    /**
     * 사용자 검증 요구사항
     * "required", "preferred", "discouraged"
     */
    private String userVerification;
    
    /**
     * 허용할 credential ID 목록
     * 빈 리스트면 모든 등록된 Passkey 허용
     */
    private List<String> allowCredentials;
}

