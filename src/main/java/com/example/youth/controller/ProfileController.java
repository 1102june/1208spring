package com.example.youth.controller;

import com.example.youth.DB.User;
import com.example.youth.dto.ApiResponse;
import com.example.youth.dto.ProfileWithUserResponse;
import com.example.youth.dto.UserProfileResponse;
import com.example.youth.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 프로필 API 컨트롤러 (안드로이드 호환)
 * /api/profile 엔드포인트 제공
 */
@RestController
@RequestMapping("/api")
public class ProfileController {

    @Autowired
    private UserService userService;

    /**
     * 프로필 조회 (안드로이드 호환 - UserProfileResponse 반환)
     * GET /api/profile
     * X-User-Id 헤더로 사용자 ID 전달
     */
    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getProfile(
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        try {
            if (userId == null || userId.isEmpty()) {
                return ResponseEntity.status(400)
                        .body(ApiResponse.error("X-User-Id 헤더가 필요합니다."));
            }

            // 프로필 조회 (DTO로 변환)
            UserProfileResponse profile = userService.getUserProfile(userId);
            
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
     * 프로필 조회 (User 정보 포함 - 전체 정보)
     * GET /api/profile/full
     * X-User-Id 헤더로 사용자 ID 전달
     */
    @GetMapping("/profile/full")
    public ResponseEntity<ApiResponse<ProfileWithUserResponse>> getProfileFull(
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        try {
            if (userId == null || userId.isEmpty()) {
                return ResponseEntity.status(400)
                        .body(ApiResponse.error("X-User-Id 헤더가 필요합니다."));
            }

            // User 정보 조회
            User user = userService.getUserByUid(userId);
            if (user == null) {
                return ResponseEntity.status(404)
                        .body(ApiResponse.error("사용자를 찾을 수 없습니다."));
            }

            // 프로필 조회 (DTO로 변환)
            UserProfileResponse profile = userService.getUserProfile(userId);
            
            if (profile == null) {
                return ResponseEntity.status(404)
                        .body(ApiResponse.error("프로필을 찾을 수 없습니다."));
            }

            // User 정보와 Profile 정보를 합쳐서 반환
            ProfileWithUserResponse response = ProfileWithUserResponse.builder()
                    // User 정보
                    .userId(user.getUserId())
                    .email(user.getEmail())
                    .emailVerified(user.isEmailVerified())
                    .passwordHash(user.getPasswordHash() != null ? user.getPasswordHash() : "")
                    .loginType(user.getLoginType() != null ? user.getLoginType().name() : null)
                    .osType(user.getOsType() != null ? user.getOsType().name() : null)
                    .appVersion(user.getAppVersion())
                    .pushToken(user.getPushToken())
                    .deviceId(user.getDeviceId())
                    .createdAt(user.getCreatedAt() != null ? user.getCreatedAt().toString() : null)
                    // Profile 정보
                    .nickname(profile.getNickname())
                    .age(profile.getAge())
                    .region(profile.getRegion())
                    .education(profile.getEducation())
                    .jobStatus(profile.getJobStatus())
                    .interests(profile.getInterests())
                    .build();

            return ResponseEntity.ok(ApiResponse.success("프로필 조회 성공", response));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("서버 오류가 발생했습니다: " + e.getMessage()));
        }
    }
}

