package com.example.youth.controller;

import com.example.youth.DB.InterestCategory;
import com.example.youth.dto.ApiResponse;
import com.example.youth.repository.InterestCategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 관심사 카테고리 API 컨트롤러
 */
@RestController
@RequestMapping("/api/interests")
public class InterestCategoryController {

    @Autowired
    private InterestCategoryRepository interestCategoryRepository;

    /**
     * 사용자 관심사 목록 조회
     * GET /api/interests?userId={userId}
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<String>>> getInterests(
            @RequestParam(required = false) String userId,
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId) {
        try {
            // userId는 쿼리 파라미터 또는 헤더에서 가져오기
            String finalUserId = userId != null ? userId : headerUserId;
            
            if (finalUserId == null || finalUserId.isEmpty()) {
                return ResponseEntity.status(400)
                        .body(ApiResponse.error("사용자 ID가 필요합니다."));
            }

            // 관심사 목록 조회
            List<InterestCategory> interests = interestCategoryRepository.findByUser_UserId(finalUserId);
            
            // 카테고리 문자열 리스트로 변환
            List<String> categories = interests.stream()
                    .map(InterestCategory::getCategory)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(ApiResponse.success("관심사 목록 조회 성공", categories));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("서버 오류가 발생했습니다: " + e.getMessage()));
        }
    }
}

