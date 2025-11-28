package com.example.youth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileWithUserResponse {
    // User 정보
    private String userId;
    private String email;
    private Boolean emailVerified;
    private String passwordHash; // 비밀번호 해시 (보안상 실제 해시값 반환, 클라이언트에서 사용하지 않음)
    private String loginType;
    private String osType;
    private String appVersion;
    private String pushToken;
    private String deviceId;
    private String createdAt;
    
    // Profile 정보
    private String nickname;
    private Integer age;
    private String region;
    private String education;
    private String jobStatus;
    private List<String> interests;
}

