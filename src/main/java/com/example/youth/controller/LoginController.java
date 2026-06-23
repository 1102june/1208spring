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
            FirebaseToken decodedToken = firebaseAuthService.verifyToken(loginRequest.getIdToken());
            String uid = decodedToken.getUid();

            User user = userService.getUserByUid(uid);
            if (user == null) {
                return ResponseEntity.status(404).body("USER_NOT_FOUND");
            }

            return ResponseEntity.ok("LOGIN_SUCCESS");

        } catch (Exception e) {
            return ResponseEntity.status(500).body("LOGIN_ERROR: " + e.getMessage());
        }
    }
}
