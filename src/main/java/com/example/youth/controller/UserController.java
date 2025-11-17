package com.example.youth.controller;

import com.example.youth.DB.User;
import com.example.youth.dto.ApiResponse;
import com.example.youth.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<String>> registerUser(@RequestBody User user) {
        String result = userService.registerUser(user);
        if (result.equals("회원가입 성공")) {
            return ResponseEntity.ok(ApiResponse.success(result, null));
        } else {
            return ResponseEntity.badRequest().body(ApiResponse.error(result));
        }
    }
}
