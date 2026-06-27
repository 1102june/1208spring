package com.example.youth.controller;

import com.example.youth.dto.ApiResponse;
import com.example.youth.service.PolicySyncService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/policy")
public class PolicySyncController {

    @Autowired
    private PolicySyncService policySyncService;

    /**
     * region backfill (기존 DB에 sync 없이 region 채우기)
     */
    @PostMapping("/backfill-regions")
    public ResponseEntity<ApiResponse<Integer>> backfillPolicyRegions() {
        try {
            int updatedCount = policySyncService.backfillPolicyRegions();
            return ResponseEntity.ok(ApiResponse.success(
                    "policy.region backfill 완료: " + updatedCount + "건 갱신", updatedCount));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("region backfill 중 오류: " + e.getMessage()));
        }
    }

    /**
     * 청년정책 데이터 동기화 (수동 실행)
     */
    @PostMapping("/sync")
    public ResponseEntity<ApiResponse<String>> syncPolicyData() {
        
        try {
            policySyncService.syncYouthPolicyData();
            return ResponseEntity.ok(ApiResponse.success("청년정책 데이터 동기화가 시작되었습니다."));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("동기화 중 오류 발생: " + e.getMessage()));
        }
    }

    /**
     * 잘못 저장된 null 값이 있는 정책 데이터 삭제
     */
    @PostMapping("/cleanup")
    public ResponseEntity<ApiResponse<Integer>> cleanupInvalidPolicies() {
        try {
            int deletedCount = policySyncService.deleteInvalidPolicies();
            return ResponseEntity.ok(ApiResponse.success("잘못 저장된 정책 " + deletedCount + "건이 삭제되었습니다.", deletedCount));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("삭제 중 오류 발생: " + e.getMessage()));
        }
    }
}

