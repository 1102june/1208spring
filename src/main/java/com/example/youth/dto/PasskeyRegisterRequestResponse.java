package com.example.youth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Passkey 등록 요청 생성 응답
 * 클라이언트가 Passkey 등록을 시작하기 전에 서버에서 받아야 하는 데이터
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasskeyRegisterRequestResponse {
    /**
     * 서버에서 생성한 challenge (Base64 URL-safe 인코딩)
     */
    private String challenge;
    
    /**
     * Relying Party ID (도메인)
     */
    private String rpId;
    
    /**
     * Relying Party 이름
     */
    private String rpName;
    
    /**
     * 사용자 ID (Base64 URL-safe 인코딩된 이메일)
     */
    private String userId;
    
    /**
     * 사용자 이름 (이메일)
     */
    private String userName;
    
    /**
     * 사용자 표시 이름
     */
    private String userDisplayName;
    
    /**
     * 타임아웃 (밀리초)
     */
    private Long timeout;
    
    /**
     * 사용자 검증 요구사항
     */
    private String userVerification;
}

