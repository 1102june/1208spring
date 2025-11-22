package com.example.youth.dto;

/**
 * 회원가입 요청 DTO
 * 
 * 닉네임은 제거되었으며, 프로필 설정에서 입력받습니다.
 */
public class SignupRequest {

    private String idToken;

    public String getIdToken() { 
        return idToken; 
    }
    
    public void setIdToken(String idToken) { 
        this.idToken = idToken; 
    }
}
