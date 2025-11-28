package com.example.youth.controller;

import com.example.youth.dto.ApiResponse;
import com.example.youth.dto.HousingResponse;
import com.example.youth.dto.HousingComplexResponse;
import com.example.youth.dto.HousingNoticeResponse;
import com.example.youth.dto.publicdata.LHRentalHouseListResponse;
import com.example.youth.dto.publicdata.LHRentalNoticeResponse;
import com.example.youth.service.HousingService;
import com.example.youth.service.HousingSyncService;
import com.example.youth.service.PolicySyncService;
import com.example.youth.service.PublicDataApiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/api/housing")
public class HousingController {

    @Autowired
    private HousingService housingService;

    @Autowired
    private PublicDataApiService publicDataApiService;

    @Autowired
    private HousingSyncService housingSyncService;

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

    /**
     * housing_complex 데이터 조회 및 DB 저장 (외부 API 호출 → MariaDB 저장)
     * 
     * POST /api/housing/complex/sync - 전체 지역 동기화 (17개 시도 전체)
     * POST /api/housing/complex/sync?brtcCode={brtcCode}&signguCode={signguCode} - 특정 지역만 동기화
     * 
     * 예시:
     * - 전체: POST /api/housing/complex/sync
     * - 특정 지역: POST /api/housing/complex/sync?brtcCode=11&signguCode=110 (서울특별시 종로구)
     */
    @PostMapping("/complex/sync")
    public ResponseEntity<ApiResponse<String>> syncHousingComplex(
            @RequestParam(required = false) String brtcCode,
            @RequestParam(required = false) String signguCode) {
        try {
            // brtcCode와 signguCode가 모두 없으면 전체 지역 동기화
            if ((brtcCode == null || brtcCode.isEmpty()) && (signguCode == null || signguCode.isEmpty())) {
                // 전체 지역 동기화 (17개 시도 전체)
                housingSyncService.syncAllHousingComplex();
                return ResponseEntity.ok(ApiResponse.success("전체 지역 housing_complex 데이터 동기화가 시작되었습니다. (백그라운드에서 실행 중)"));
            } 
            // brtcCode와 signguCode가 모두 있으면 특정 지역만 동기화
            else if (brtcCode != null && !brtcCode.isEmpty() && signguCode != null && !signguCode.isEmpty()) {
                // 특정 지역만 직접 동기화 (공고문 없이 단지정보만 조회)
                housingSyncService.syncHousingComplexOnly(brtcCode, signguCode);
                return ResponseEntity.ok(ApiResponse.success("housing_complex 데이터 동기화가 완료되었습니다. (지역: " + brtcCode + "-" + signguCode + ")"));
            } 
            // 하나만 있으면 공고문 기반 동기화
            else {
                // 공고문 기반 동기화 (공고문에서 지역 정보 추출 후 단지정보 조회)
                housingSyncService.syncLHRentalHouseData(brtcCode, signguCode);
                return ResponseEntity.ok(ApiResponse.success("housing_complex 데이터 동기화가 시작되었습니다. (공고문 기반, 백그라운드에서 실행 중)"));
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("housing_complex 동기화 중 오류 발생: " + e.getMessage()));
        }
    }

    /**
     * housing_notice 데이터 조회 및 DB 저장 (외부 API 호출 → MariaDB 저장)
     * POST /api/housing/notice/sync?brtcCode={brtcCode}
     */
    @PostMapping("/notice/sync")
    public ResponseEntity<ApiResponse<String>> syncHousingNotice(
            @RequestParam(required = false) String brtcCode) {
        try {
            // HousingSyncService를 사용하여 API 호출 후 DB에 저장
            housingSyncService.syncLHRentalHouseData(brtcCode, null);
            return ResponseEntity.ok(ApiResponse.success("housing_notice 데이터 동기화가 시작되었습니다. (백그라운드에서 실행 중)"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("housing_notice 동기화 중 오류 발생: " + e.getMessage()));
        }
    }

    /**
     * 단지정보 목록 조회 (분리된 API)
     * GET /api/housing/complexes?userId={userId}&lat={lat}&lon={lon}&radius={radius}&limit={limit}
     */
    @GetMapping("/complexes")
    public ResponseEntity<ApiResponse<List<HousingComplexResponse>>> getHousingComplexes(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestParam(required = false) String userIdParam,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lon,
            @RequestParam(required = false) Integer radius,
            @RequestParam(required = false) Integer limit) {
        
        String finalUserId = userId != null ? userId : userIdParam;
        
        if (finalUserId == null || finalUserId.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("사용자 ID가 필요합니다."));
        }

        List<HousingComplexResponse> complexes = housingService.getHousingComplexes(
                finalUserId, lat, lon, radius, limit);
        
        return ResponseEntity.ok(ApiResponse.success("단지정보 목록 조회 성공", complexes));
    }

    /**
     * 공고문 목록 조회 (분리된 API)
     * GET /api/housing/notices?userId={userId}&limit={limit}
     */
    @GetMapping("/notices")
    public ResponseEntity<ApiResponse<List<HousingNoticeResponse>>> getHousingNotices(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestParam(required = false) String userIdParam,
            @RequestParam(required = false) Integer limit) {
        
        String finalUserId = userId != null ? userId : userIdParam;
        
        if (finalUserId == null || finalUserId.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("사용자 ID가 필요합니다."));
        }

        List<HousingNoticeResponse> notices = housingService.getHousingNotices(finalUserId, limit);
        
        return ResponseEntity.ok(ApiResponse.success("공고문 목록 조회 성공", notices));
    }
}

