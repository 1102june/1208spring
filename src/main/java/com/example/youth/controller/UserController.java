package com.example.youth.controller;

import com.example.youth.DB.User;
import com.example.youth.dto.ApiResponse;
import com.example.youth.dto.ProfileRequest;
import com.example.youth.dto.PushTokenRequest;
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

            // 2) 사용자 찾기 또는 생성 (Google 로그인 사용자)
            User user = userService.getUserByUid(uid);
            if (user == null) {
                // UID로 찾지 못했을 때, 이메일로도 확인
                User existingUserByEmail = userService.getUserByEmail(email);
                if (existingUserByEmail != null) {
                    // 같은 이메일로 이미 가입된 사용자가 있으면 기존 사용자 사용
                    user = existingUserByEmail;
                    // UID가 다르면 업데이트 (하지만 UID는 PK이므로 변경 불가)
                    // 따라서 기존 사용자를 그대로 사용
                } else {
                    // 이메일로도 없으면 새로 생성
                    User newUser = User.builder()
                            .userId(uid)
                            .email(email)
                            .passwordHash("") // Google login users don't have a password
                            .emailVerified(false)
                            .loginType(com.example.youth.DB.LoginType.google)
                            .osType(com.example.youth.DB.OSType.android)
                            .appVersion(profileRequest.getAppVersion())
                            .deviceId(profileRequest.getDeviceId())
                            .createdAt(java.time.LocalDateTime.now())
                            .build();
                    userService.registerUser(newUser);
                    // 사용자 생성 후 다시 조회하여 영속성 컨텍스트에 로드
                    user = userService.getUserByUid(uid);
                    if (user == null) {
                        return ResponseEntity.status(500)
                                .body(ApiResponse.error("사용자 생성 후 조회 실패"));
                    }
                }
            }
            
            // 기존 사용자 정보 업데이트 (appVersion, deviceId)
            if (profileRequest.getAppVersion() != null) {
                user.setAppVersion(profileRequest.getAppVersion());
            }
            if (profileRequest.getDeviceId() != null) {
                user.setDeviceId(profileRequest.getDeviceId());
            }
            userService.updateUser(user);
            
            // 프로필 저장 시 실제 사용자의 UID 사용
            String actualUid = user.getUserId();

            // 3) 프로필 저장 (실제 사용자의 UID 사용)
            userService.saveOrUpdateProfile(actualUid, profileRequest);

            return ResponseEntity.ok(ApiResponse.success("프로필이 저장되었습니다.", null));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("서버 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    /**
     * FCM Push Token 저장/업데이트
     * POST /auth/push-token
     */
    @PostMapping("/push-token")
    public ResponseEntity<ApiResponse<String>> savePushToken(@RequestBody PushTokenRequest pushTokenRequest) {
        try {
            // 1) Firebase ID Token 검증
            FirebaseToken decodedToken = firebaseAuthService.verifyToken(pushTokenRequest.getIdToken());
            String uid = decodedToken.getUid();

            // 2) 사용자 조회
            User user = userService.getUserByUid(uid);
            if (user == null) {
                return ResponseEntity.status(404)
                        .body(ApiResponse.error("사용자를 찾을 수 없습니다."));
            }

            // 3) Push Token 업데이트
            user.setPushToken(pushTokenRequest.getPushToken());
            userService.updateUser(user);

            return ResponseEntity.ok(ApiResponse.success("Push Token이 저장되었습니다.", null));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("서버 오류가 발생했습니다: " + e.getMessage()));
        }
    }
}
