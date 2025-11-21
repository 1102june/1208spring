package com.example.youth.controller;

import com.example.youth.DB.User;
import com.example.youth.dto.ApiResponse;
import com.example.youth.dto.ProfileRequest;
import com.example.youth.dto.SignupRequest;
import com.example.youth.service.FirebaseAuthService;
import com.example.youth.service.UserService;
import com.google.firebase.auth.FirebaseToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private FirebaseAuthService firebaseAuthService;

    @PostMapping("/register")
    public String registerUser(@RequestBody User user) {
        return userService.registerUser(user);
    }

    /**
     * 회원가입 (Android 앱용)
     * POST /auth/signup
     * 요청 형식: {"idToken": "...", "nickname": "..."}
     */
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<String>> signup(@RequestBody SignupRequest signupRequest) {
        try {
            // 1) Firebase ID Token 검증
            FirebaseToken decodedToken = firebaseAuthService.verifyToken(signupRequest.getIdToken());
            String uid = decodedToken.getUid();
            String email = decodedToken.getEmail();

            // 2) 이미 존재하는 사용자인지 확인
            User existingUser = userService.getUserByUid(uid);
            if (existingUser != null) {
                return ResponseEntity.ok(ApiResponse.success("이미 가입된 사용자입니다.", null));
            }

            // 3) 새 사용자 생성 (Google 로그인 시 password_hash는 빈 문자열로 설정)
            User newUser = User.builder()
                    .userId(uid)
                    .email(email)
                    .emailVerified(false) // OTP 인증 전까지는 false
                    .passwordHash("") // Google 로그인 시 password 없음, NOT NULL 제약을 위해 빈 문자열 사용
                    .loginType(com.example.youth.DB.LoginType.google) // Google 로그인
                    .osType(com.example.youth.DB.OSType.android)
                    .appVersion(signupRequest.getAppVersion())
                    .deviceId(signupRequest.getDeviceId())
                    .createdAt(java.time.LocalDateTime.now())
                    .build();

            userService.registerUser(newUser);

            return ResponseEntity.ok(ApiResponse.success("회원가입이 완료되었습니다.", null));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("회원가입 실패: " + e.getMessage()));
        }
    }

    /**
     * 프로필 저장/업데이트
     * POST /auth/profile
     */
    @PostMapping("/profile")
    public ResponseEntity<ApiResponse<String>> saveProfile(@RequestBody ProfileRequest profileRequest) {
        try {
            // 1) Firebase ID Token 검증
            FirebaseToken decodedToken = firebaseAuthService.verifyToken(profileRequest.getIdToken());
            String uid = decodedToken.getUid();
            String email = decodedToken.getEmail();

            // 2) 사용자가 없으면 자동으로 생성 (Google 로그인 등에서 회원가입이 누락된 경우)
            User user = userService.getUserByUid(uid);
            if (user == null) {
                // 새 사용자 생성 (Google 로그인 시 password_hash는 빈 문자열로 설정)
                User newUser = User.builder()
                        .userId(uid)
                        .email(email)
                        .emailVerified(false) // OTP 인증 전까지는 false
                        .passwordHash("") // Google 로그인 시 password 없음, NOT NULL 제약을 위해 빈 문자열 사용
                        .loginType(com.example.youth.DB.LoginType.google) // Google 로그인
                        .osType(com.example.youth.DB.OSType.android)
                        .appVersion(profileRequest.getAppVersion())
                        .deviceId(profileRequest.getDeviceId())
                        .createdAt(java.time.LocalDateTime.now())
                        .build();
                userService.registerUser(newUser);
            } else {
                // 기존 사용자의 appVersion과 deviceId 업데이트
                if (profileRequest.getAppVersion() != null) {
                    user.setAppVersion(profileRequest.getAppVersion());
                }
                if (profileRequest.getDeviceId() != null) {
                    user.setDeviceId(profileRequest.getDeviceId());
                }
                userService.updateUser(user);
            }

            // 3) 프로필 저장
            userService.saveOrUpdateProfile(uid, profileRequest);

            return ResponseEntity.ok(ApiResponse.success("프로필이 저장되었습니다.", null));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("서버 오류가 발생했습니다: " + e.getMessage()));
        }
    }
}
