package com.example.youth.controller;

import com.example.youth.dto.ApiResponse;
import com.example.youth.service.AIRecommendationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * AI 추천 API
 */
@RestController
@RequestMapping("/api/ai/recommendations")
public class AIRecommendationController {

    @Autowired
    private AIRecommendationService aiRecommendationService;

    /**
     * AI 추천 생성 및 저장
     * POST /api/ai/recommendations/generate
     * 
     * 사용자 행동 데이터를 기반으로 개인화된 추천을 생성하고 저장합니다.
     */
    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<Void>> generateRecommendations(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestParam(defaultValue = "10") int maxRecommendations) {
        
        if (userId == null || userId.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("사용자 ID가 필요합니다."));
        }

        try {
            aiRecommendationService.generateAndSaveRecommendations(userId, maxRecommendations);
            return ResponseEntity.ok(ApiResponse.success("AI 추천 생성 완료"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("AI 추천 생성 실패: " + e.getMessage()));
        }
    }
}

