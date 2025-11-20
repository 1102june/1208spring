package com.example.youth.controller;

import com.example.youth.dto.ApiResponse;
import com.example.youth.dto.HousingResponse;
import com.example.youth.service.HousingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/housing")
public class HousingController {

    @Autowired
    private HousingService housingService;

    /**
     * 임대주택 추천 목록 조회 (사용자 위치 기반)
     * GET /api/housing/recommended?userId={userId}&lat={lat}&lon={lon}&radius={radius}&limit={limit}
     */
    @GetMapping("/recommended")
    public ResponseEntity<ApiResponse<List<HousingResponse>>> getRecommendedHousing(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestParam(required = false) String userIdParam,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lon,
            @RequestParam(required = false) Integer radius,
            @RequestParam(required = false) Integer limit) {
        
        // 헤더 또는 파라미터에서 userId 가져오기
        String finalUserId = userId != null ? userId : userIdParam;
        
        if (finalUserId == null || finalUserId.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("사용자 ID가 필요합니다."));
        }

        List<HousingResponse> housingList = housingService.getRecommendedHousing(
                finalUserId, lat, lon, radius, limit);
        
        return ResponseEntity.ok(ApiResponse.success("임대주택 추천 목록 조회 성공", housingList));
    }

    /**
     * 임대주택 상세 조회
     * GET /api/housing/{housingId}?userId={userId}&lat={lat}&lon={lon}
     */
    @GetMapping("/{housingId}")
    public ResponseEntity<ApiResponse<HousingResponse>> getHousingDetail(
            @PathVariable String housingId,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestParam(required = false) String userIdParam,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lon) {
        
        String finalUserId = userId != null ? userId : userIdParam;
        
        if (finalUserId == null || finalUserId.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("사용자 ID가 필요합니다."));
        }

        HousingResponse housing = housingService.getHousingDetail(housingId, finalUserId, lat, lon);
        return ResponseEntity.ok(ApiResponse.success("임대주택 상세 조회 성공", housing));
    }

    /**
     * 활성 임대주택 목록 조회 (전체)
     * GET /api/housing/active?userId={userId}
     */
    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<HousingResponse>>> getActiveHousing(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestParam(required = false) String userIdParam) {
        
        String finalUserId = userId != null ? userId : userIdParam;
        
        if (finalUserId == null || finalUserId.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("사용자 ID가 필요합니다."));
        }

        List<HousingResponse> housingList = housingService.getActiveHousing(finalUserId);
        return ResponseEntity.ok(ApiResponse.success("활성 임대주택 목록 조회 성공", housingList));
    }
}

