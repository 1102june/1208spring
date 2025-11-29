package com.example.youth.controller;

import com.example.youth.DB.LoginType;
import com.example.youth.DB.OSType;
import com.example.youth.DB.User;
import com.example.youth.DB.UserProfile;
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
     * 회원가입 (이메일/비밀번호)
     * POST /auth/signup
     * idToken과 password를 받아서 User 생성 (password는 BCrypt로 해시화)
     */
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<String>> signup(@RequestBody com.example.youth.dto.SignupRequest signupRequest) {
        try {
            // 1) Firebase ID Token 검증
            FirebaseToken decodedToken = firebaseAuthService.verifyToken(signupRequest.getIdToken());
            String uid = decodedToken.getUid();
            String email = decodedToken.getEmail();

            // 2) 비밀번호 해시화 (BCrypt)
            String passwordHash = "";
            if (signupRequest.getPassword() != null && !signupRequest.getPassword().isEmpty()) {
                passwordHash = userService.hashPassword(signupRequest.getPassword());
            }

            // 3) 사용자 확인 및 생성
            User user = userService.getUserByUid(uid);
            
            if (user == null) {
                // 새로운 사용자 생성
                User newUser = User.builder()
                        .userId(uid)
                        .email(email != null ? email : "")
                        .emailVerified(false) // 이메일 인증은 OTP로 처리
                        .passwordHash(passwordHash)
                        .loginType(LoginType.local) // 이메일/비밀번호 로그인은 local 사용
                        .osType(OSType.android)
                        .createdAt(java.time.LocalDateTime.now())
                        .build();
                userService.registerUser(newUser);
            } else {
                // 기존 사용자가 있으면 passwordHash 업데이트
                if (!passwordHash.isEmpty()) {
                    user.setPasswordHash(passwordHash);
                    userService.updateUser(user);
                }
            }

            return ResponseEntity.ok(ApiResponse.success("회원가입이 완료되었습니다.", null));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("회원가입 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    /**
     * 프로필 조회
     * GET /auth/profile
     * GET /api/profile (안드로이드 호환)
     */
    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<com.example.youth.DB.UserProfile>> getProfile(
            @RequestHeader(value = "Authorization", required = false) String idToken,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        try {
            String uid = null;
            
            // X-User-Id 헤더가 있으면 직접 사용
            if (userId != null && !userId.isEmpty()) {
                uid = userId;
            } 
            // Authorization 헤더가 있으면 Firebase Token 검증
            else if (idToken != null && !idToken.isEmpty()) {
                String token = idToken.replace("Bearer ", "");
                FirebaseToken decodedToken = firebaseAuthService.verifyToken(token);
                uid = decodedToken.getUid();
            } else {
                return ResponseEntity.status(400)
                        .body(ApiResponse.error("Authorization 헤더 또는 X-User-Id 헤더가 필요합니다."));
            }

            // UID로 사용자 확인
            User user = userService.getUserByUid(uid);
            
            if (user == null) {
                return ResponseEntity.status(404)
                        .body(ApiResponse.error("사용자를 찾을 수 없습니다."));
            }

            // 프로필 조회
            com.example.youth.DB.UserProfile profile = userService.getProfileByUserId(user.getUserId());
            
            if (profile == null) {
                return ResponseEntity.status(404)
                        .body(ApiResponse.error("프로필을 찾을 수 없습니다."));
            }

            return ResponseEntity.ok(ApiResponse.success("프로필 조회 성공", profile));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("서버 오류가 발생했습니다: " + e.getMessage()));
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

            // 2) 사용자 확인 및 생성/업데이트
            User user = userService.getUserByUid(uid);
            
            if (user == null) {
                // UID로 사용자를 찾을 수 없으면
                // 이메일로 기존 사용자 확인
                User existingUserByEmail = userService.getUserByEmail(email);
                
                if (existingUserByEmail != null) {
                    // 기존 사용자가 있으면 userId를 변경하지 않고 그대로 사용
                    // (Hibernate는 엔티티의 ID 변경을 허용하지 않음)
                    // 기존 User의 userId를 사용하여 프로필 저장
                    user = existingUserByEmail;
                    // 기타 정보만 업데이트 (userId는 변경하지 않음)
                    if (!user.isEmailVerified()) {
                        user.setEmailVerified(true);
                    }
                    if (user.getLoginType() != LoginType.google) {
                        user.setLoginType(LoginType.google);
                    }
                    if (user.getOsType() != OSType.android) {
                        user.setOsType(OSType.android);
                    }
                    userService.updateUser(user);
                } else {
                    // 완전히 새로운 사용자면 생성
                    User newUser = User.builder()
                            .userId(uid)
                            .email(email != null ? email : "")
                            .emailVerified(true) // Google 로그인은 이메일 인증 완료
                            .passwordHash("") // Google 로그인은 비밀번호 없음 (빈 문자열)
                            .loginType(LoginType.google)
                            .osType(OSType.android)
                            .createdAt(java.time.LocalDateTime.now())
                            .build();
                    userService.registerUser(newUser);
                    user = newUser;
                }
            }

            // 3) 프로필 저장 (기존 프로필이 있으면 업데이트, 없으면 생성)
            userService.saveOrUpdateProfile(user.getUserId(), profileRequest);

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
