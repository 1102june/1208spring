package com.example.youth.controller;

import com.example.youth.dto.SignupRequest;
import com.example.youth.db.User;
import com.example.youth.service.FirebaseAuthService;
import com.example.youth.service.UserService;
import com.google.firebase.auth.FirebaseToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.example.youth.db.LoginType;


@RestController
@RequestMapping("/auth")
public class UserController {

    @Autowired
    private FirebaseAuthService firebaseAuthService;

    @Autowired
    private UserService userService;

    @PostMapping("/signup")
    public String signup(@RequestBody SignupRequest request) {

        try {
            FirebaseToken token = firebaseAuthService.verifyToken(request.getIdToken());

            String uid = token.getUid();
            String email = token.getEmail(); // Firebase에서 가져옴

            if (email == null) {
                return "Firebase에서 이메일 정보를 가져올 수 없습니다.";
            }

            // 이미 존재하면 저장 X
            User exist = userService.getUserByUid(uid);
            if (exist != null) {
                return "이미 가입된 사용자입니다.";
            }

            // User 엔티티 생성
            User user = User.builder()
                    .userId(uid)
                    .email(email)
                    .passwordHash("firebase") // 실제 비밀번호는 Firebase가 관리함
                    .loginType(LoginType.google)
                    .build();

            userService.registerUser(user);  // ⭐ DB 저장

            return "DB 저장 완료";

        } catch (Exception e) {
            return "회원가입 실패: " + e.getMessage();
        }
    }
}
