package com.example.youth.controller;

import com.example.youth.dto.ApiResponse;
import com.example.youth.dto.UserActivityRequest;
import com.example.youth.service.UserActivityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 사용자 행동 데이터 수집 API
 * AI 추천을 위한 사용자 행동 로그 수집
 */
@RestController
@RequestMapping("/api/activity")
public class UserActivityController {

    @Autowired
    private UserActivityService userActivityService;

    /**
     * 사용자 활동 로그 저장
     * POST /api/activity
     * 
     * 예시:
     * {
     *   "activityType": "VIEW",
     *   "contentType": "policy",
     *   "contentId": "policy123",
     *   "searchKeyword": null,
     *   "metadata": null
     * }
     */
    @PostMapping
    public ResponseEntity<ApiResponse<Void>> logActivity(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestBody UserActivityRequest request) {
        
        // 헤더 또는 요청에서 userId 가져오기
        String finalUserId = userId != null ? userId : "anonymous"; // TODO: 실제 인증에서 가져오기
        
        try {
            userActivityService.logActivity(finalUserId, request);
            return ResponseEntity.ok(ApiResponse.success("활동 로그 저장 성공"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("활동 로그 저장 실패: " + e.getMessage()));
        }
    }
}

