package com.example.youth.controller;

import com.example.youth.DB.User;
import com.example.youth.dto.ApiResponse;
import com.example.youth.dto.UserResponse;
import com.example.youth.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 사용자 정보 API 컨트롤러
 */
@RestController
@RequestMapping("/api/user")
public class UserInfoController {

    @Autowired
    private UserService userService;

    /**
     * 사용자 정보 조회
     * GET /api/user/info
     * X-User-Id 헤더로 사용자 ID 전달
     */
    @GetMapping("/info")
    public ResponseEntity<ApiResponse<UserResponse>> getUserInfo(
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        try {
            if (userId == null || userId.isEmpty()) {
                return ResponseEntity.status(400)
                        .body(ApiResponse.error("X-User-Id 헤더가 필요합니다."));
            }

            // 사용자 조회
            User user = userService.getUserByUid(userId);
            
            if (user == null) {
                return ResponseEntity.status(404)
                        .body(ApiResponse.error("사용자를 찾을 수 없습니다."));
            }

            // UserResponse로 변환
            UserResponse userResponse = UserResponse.builder()
                    .userId(user.getUserId())
                    .email(user.getEmail())
                    .emailVerified(user.isEmailVerified())
                    .loginType(user.getLoginType() != null ? user.getLoginType().name() : null)
                    .osType(user.getOsType() != null ? user.getOsType().name() : null)
                    .appVersion(user.getAppVersion())
                    .pushToken(user.getPushToken())
                    .deviceId(user.getDeviceId())
                    .createdAt(user.getCreatedAt() != null ? user.getCreatedAt().toString() : null)
                    .build();

            return ResponseEntity.ok(ApiResponse.success("사용자 정보 조회 성공", userResponse));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("서버 오류가 발생했습니다: " + e.getMessage()));
        }
    }
}

