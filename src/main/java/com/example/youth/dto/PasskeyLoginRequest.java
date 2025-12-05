package com.example.youth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Passkey 로그인 요청
 * 클라이언트에서 서버로 전송하는 Passkey credential
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PasskeyLoginRequest {
    /**
     * Passkey credential JSON 문자열
     * PublicKeyCredential의 authenticationResponseJson
     */
    private String credential;
}

