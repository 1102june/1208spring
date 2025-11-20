package com.example.youth.controller;

import com.example.youth.dto.ApiResponse;
import com.example.youth.repository.HousingRepository;
import com.example.youth.service.HousingSyncService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/housing")
public class HousingSyncController {

    @Autowired
    private HousingSyncService housingSyncService;
    
    @Autowired
    private HousingRepository housingRepository;

    /**
     * 임대주택 데이터 동기화 (수동 실행)
     * @param brtcCode 광역시도 코드 (옵션, 전체 조회 시 생략)
     * @param signguCode 시군구 코드 (옵션)
     */
    @PostMapping("/sync")
    public ResponseEntity<ApiResponse<String>> syncHousingData(
            @RequestParam(required = false) String brtcCode,
            @RequestParam(required = false) String signguCode) {
        
        try {
            housingSyncService.syncLHRentalHouseData(brtcCode, signguCode);
            return ResponseEntity.ok(ApiResponse.success("임대주택 데이터 동기화가 시작되었습니다."));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("동기화 중 오류 발생: " + e.getMessage()));
        }
    }
    
    /**
     * 동기화 상태 확인 (테스트용)
     * GET /api/admin/housing/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Object>> getSyncStats() {
        try {
            long totalCount = housingRepository.count();
            
            // 각 필드별 카운트 (쿼리 메서드 사용)
            long withAddress = totalCount > 0 ? housingRepository.countWithAddress() : 0;
            long withSupplyArea = totalCount > 0 ? housingRepository.countWithSupplyArea() : 0;
            long withApplicationStart = totalCount > 0 ? housingRepository.countWithApplicationStart() : 0;
            long withApplicationEnd = totalCount > 0 ? housingRepository.countWithApplicationEnd() : 0;
            
            java.util.Map<String, Object> stats = new java.util.HashMap<>();
            stats.put("totalCount", totalCount);
            stats.put("withAddress", withAddress);
            stats.put("withSupplyArea", withSupplyArea);
            stats.put("withApplicationStart", withApplicationStart);
            stats.put("withApplicationEnd", withApplicationEnd);
            stats.put("matchingRate", totalCount > 0 ? 
                    String.format("%.2f%%", (withAddress * 100.0 / totalCount)) : "0%");
            
            return ResponseEntity.ok(ApiResponse.success("동기화 상태 조회 성공", stats));
        } catch (Exception e) {
            e.printStackTrace();
            // 에러 발생 시 기본 정보만 반환
            try {
                long totalCount = housingRepository.count();
                java.util.Map<String, Object> stats = new java.util.HashMap<>();
                stats.put("totalCount", totalCount);
                stats.put("error", e.getMessage());
                return ResponseEntity.ok(ApiResponse.success("동기화 상태 조회 (부분 성공)", stats));
            } catch (Exception e2) {
                return ResponseEntity.internalServerError()
                        .body(ApiResponse.error("상태 조회 중 오류 발생: " + e.getMessage()));
            }
        }
    }
}

