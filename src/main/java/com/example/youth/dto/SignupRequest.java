package com.example.youth.dto;

import lombok.Data;

/**
 * 회원가입 요청 DTO
 * 
 * 닉네임은 제거되었으며, 프로필 설정에서 입력받습니다.
 */
@Data
public class SignupRequest {
    private String idToken;
    private String password; // 비밀번호 (서버에서 BCrypt로 해시화)
}
