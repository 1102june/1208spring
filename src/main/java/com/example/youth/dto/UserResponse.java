package com.example.youth.dto;

import com.example.youth.DB.LoginType;
import com.example.youth.DB.OSType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private String userId;
    private String email;
    private LoginType loginType;
    private OSType osType;
    private String appVersion;
    private String pushToken;
    private String deviceId;
    private LocalDateTime createdAt;
}

