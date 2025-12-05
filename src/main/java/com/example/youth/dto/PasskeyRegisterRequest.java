package com.example.youth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Passkey 등록 요청
 * 클라이언트에서 서버로 전송하는 Passkey credential
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PasskeyRegisterRequest {
    /**
     * Passkey credential JSON 문자열
     * PublicKeyCredential의 registrationResponseJson
     */
    private String credential;
    
    /**
     * 사용자 이메일 (검증용)
     */
    private String email;
}

