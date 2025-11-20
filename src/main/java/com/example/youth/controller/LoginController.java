package com.example.youth.controller;

import com.example.youth.service.FirebaseAuthService;
import com.example.youth.service.UserService;
import com.example.youth.dto.LoginRequest;
import com.example.youth.db.User;
import com.google.firebase.auth.FirebaseToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class LoginController {

    @Autowired
    private FirebaseAuthService firebaseAuthService;

    @Autowired
    private UserService userService;

    @PostMapping("/login")
    public String login(@RequestBody LoginRequest loginRequest) {
        try {
            // Firebase ID Token 검증
            FirebaseToken decodedToken = firebaseAuthService.verifyToken(loginRequest.getIdToken());
            String uid = decodedToken.getUid();

            // MariaDB에서 사용자 조회
            User user = userService.getUserByUid(uid);
            if (user != null) {
                return "로그인 성공";
            } else {
                return "사용자 정보 없음";
            }
        } catch (Exception e) {
            // 그 외의 예외 처리
            return "로그인 처리 중 오류 발생: " + e.getMessage();
        }
    }
}
