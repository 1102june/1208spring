package com.example.youth.controller;

import com.example.youth.dto.ApiResponse;
import com.example.youth.dto.UserProfileResponse;
import com.example.youth.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    @Autowired
    private UserService userService;

    /**
     * 사용자 프로필 정보 조회
     * GET /api/profile
     * Header: X-User-Id
     */
    @GetMapping
    public ResponseEntity<ApiResponse<UserProfileResponse>> getProfile(
            @RequestHeader("X-User-Id") String userId) {
        try {
            UserProfileResponse profile = userService.getUserProfile(userId);
            return ResponseEntity.ok(ApiResponse.success("프로필 조회 성공", profile));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("프로필 조회 중 오류 발생: " + e.getMessage()));
        }
    }
}


