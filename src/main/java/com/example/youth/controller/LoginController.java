package com.example.youth.controller;

import com.example.youth.service.FirebaseAuthService;
import com.example.youth.service.UserService;
import com.example.youth.dto.LoginRequest;
import com.example.youth.DB.User;
import com.google.firebase.auth.FirebaseToken;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class LoginController {

    private final FirebaseAuthService firebaseAuthService;
    private final UserService userService;

    public LoginController(FirebaseAuthService firebaseAuthService, UserService userService) {
        this.firebaseAuthService = firebaseAuthService;
        this.userService = userService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        try {
            // 1) Firebase ID Token 검증
            FirebaseToken decodedToken = firebaseAuthService.verifyToken(loginRequest.getIdToken());
            String uid = decodedToken.getUid();

            // 2) DB에서 사용자 조회
            User user = userService.getUserByUid(uid);
            if (user == null) {
                return ResponseEntity.status(404).body("USER_NOT_FOUND");
            }

            // 3) 🔥 OTP 이메일 인증 여부 체크
            if (!user.isEmailVerified()) {
                return ResponseEntity.status(403).body("EMAIL_NOT_VERIFIED");
            }

            // 4) 로그인 성공
            return ResponseEntity.ok("LOGIN_SUCCESS");

        } catch (Exception e) {
            return ResponseEntity.status(500).body("LOGIN_ERROR: " + e.getMessage());
        }
    }
}
