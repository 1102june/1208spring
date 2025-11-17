package com.example.youth.controller;

import com.example.youth.dto.ApiResponse;
import com.example.youth.dto.LoginRequest;
import com.example.youth.dto.UserResponse;
import com.example.youth.service.FirebaseAuthService;
import com.example.youth.service.UserService;
import com.example.youth.DB.User;
import com.google.firebase.auth.FirebaseToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class LoginController {

    @Autowired
    private FirebaseAuthService firebaseAuthService;

    @Autowired
    private UserService userService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<UserResponse>> login(@RequestBody LoginRequest loginRequest) {
        // Firebase ID Token 검증
        FirebaseToken decodedToken = firebaseAuthService.verifyToken(loginRequest.getIdToken());
        String uid = decodedToken.getUid();

        // MariaDB에서 사용자 조회
        User user = userService.getUserByUid(uid);
        if (user != null) {
            // User 엔티티를 UserResponse DTO로 변환 (순환 참조 방지)
            UserResponse userResponse = UserResponse.builder()
                    .userId(user.getUserId())
                    .email(user.getEmail())
                    .loginType(user.getLoginType())
                    .osType(user.getOsType())
                    .appVersion(user.getAppVersion())
                    .pushToken(user.getPushToken())
                    .deviceId(user.getDeviceId())
                    .createdAt(user.getCreatedAt())
                    .build();
            
            return ResponseEntity.ok(ApiResponse.success("로그인 성공", userResponse));
        } else {
            return ResponseEntity.badRequest().body(ApiResponse.error("사용자 정보가 없습니다. 회원가입이 필요합니다."));
        }
    }
}
