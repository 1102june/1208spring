package com.example.youth.controller;

import com.example.youth.DB.User;
import com.example.youth.dto.ApiResponse;
import com.example.youth.dto.ProfileRequest;
import com.example.youth.dto.PushTokenRequest;
import com.example.youth.service.FcmService;
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

    @Autowired
    private FcmService fcmService;

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

            // 2) 프로필 저장
            userService.saveOrUpdateProfile(uid, profileRequest);

            return ResponseEntity.ok(ApiResponse.success("프로필이 저장되었습니다.", null));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("서버 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    /**
     * FCM 토큰 저장
     * POST /auth/push-token
     */
    @PostMapping("/push-token")
    public ResponseEntity<ApiResponse<String>> savePushToken(@RequestBody PushTokenRequest request) {
        try {
            // 1) Firebase ID Token 검증
            FirebaseToken decodedToken = firebaseAuthService.verifyToken(request.getIdToken());
            String uid = decodedToken.getUid();

            // 2) FCM 토큰 저장
            userService.saveFcmToken(uid, request.getPushToken());

            return ResponseEntity.ok(ApiResponse.success("FCM 토큰이 저장되었습니다.", null));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("FCM 토큰 저장 실패: " + e.getMessage()));
        }
    }

    /**
     * 테스트용: FCM 알림 발송 (특정 사용자)
     * POST /auth/test-notification
     * Body: { "idToken": "...", "title": "테스트 알림", "body": "알림 내용" }
     */
    @PostMapping("/test-notification")
    public ResponseEntity<ApiResponse<String>> sendTestNotification(
            @RequestBody TestNotificationRequest request) {
        try {
            // 1) Firebase ID Token 검증
            FirebaseToken decodedToken = firebaseAuthService.verifyToken(request.getIdToken());
            String uid = decodedToken.getUid();

            // 2) FCM 알림 발송
            boolean success = fcmService.sendNotificationToUser(
                    uid,
                    request.getTitle() != null ? request.getTitle() : "테스트 알림",
                    request.getBody() != null ? request.getBody() : "알림 테스트입니다."
            );

            if (success) {
                return ResponseEntity.ok(ApiResponse.success("알림이 발송되었습니다.", null));
            } else {
                return ResponseEntity.status(500)
                        .body(ApiResponse.error("알림 발송에 실패했습니다. FCM 토큰을 확인해주세요."));
            }
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("알림 발송 실패: " + e.getMessage()));
        }
    }

    /**
     * 테스트용: FCM 토큰으로 직접 알림 발송
     * POST /auth/test-notification-by-token
     * Body: { "fcmToken": "...", "title": "테스트 알림", "body": "알림 내용" }
     */
    @PostMapping("/test-notification-by-token")
    public ResponseEntity<ApiResponse<String>> sendTestNotificationByToken(
            @RequestBody TestNotificationByTokenRequest request) {
        try {
            boolean success = fcmService.sendNotification(
                    request.getFcmToken(),
                    request.getTitle() != null ? request.getTitle() : "테스트 알림",
                    request.getBody() != null ? request.getBody() : "알림 테스트입니다."
            );

            if (success) {
                return ResponseEntity.ok(ApiResponse.success("알림이 발송되었습니다.", null));
            } else {
                return ResponseEntity.status(500)
                        .body(ApiResponse.error("알림 발송에 실패했습니다. FCM 토큰을 확인해주세요."));
            }
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("알림 발송 실패: " + e.getMessage()));
        }
    }

    // 내부 클래스: 테스트 알림 요청 DTO
    private static class TestNotificationRequest {
        private String idToken;
        private String title;
        private String body;

        public String getIdToken() { return idToken; }
        public void setIdToken(String idToken) { this.idToken = idToken; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getBody() { return body; }
        public void setBody(String body) { this.body = body; }
    }

    // 내부 클래스: 토큰으로 직접 알림 발송 요청 DTO
    private static class TestNotificationByTokenRequest {
        private String fcmToken;
        private String title;
        private String body;

        public String getFcmToken() { return fcmToken; }
        public void setFcmToken(String fcmToken) { this.fcmToken = fcmToken; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getBody() { return body; }
        public void setBody(String body) { this.body = body; }
    }
}
