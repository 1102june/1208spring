package com.example.youth.controller;

import com.example.youth.dto.ApiResponse;
import com.example.youth.service.DatabaseCleanupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 데이터베이스 정리 컨트롤러
 * 주의: 프로덕션 환경에서는 이 컨트롤러를 비활성화하거나 보안을 강화해야 합니다!
 */
@RestController
@RequestMapping("/api/admin/database")
public class DatabaseCleanupController {

    @Autowired
    private DatabaseCleanupService databaseCleanupService;

    /**
     * Policy, HousingNotice, HousingComplex를 제외한 모든 데이터 삭제
     * 주의: 이 API는 모든 사용자 데이터를 삭제합니다!
     * 
     * DELETE /api/admin/database/cleanup
     */
    @DeleteMapping("/cleanup")
    public ResponseEntity<ApiResponse<String>> cleanupDatabase() {
        try {
            databaseCleanupService.deleteAllDataExceptPolicyAndHousing();
            return ResponseEntity.ok(
                ApiResponse.success("데이터 삭제 완료", "Policy, HousingNotice, HousingComplex를 제외한 모든 데이터가 삭제되었습니다.")
            );
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("데이터 삭제 실패: " + e.getMessage()));
        }
    }

    /**
     * 테이블별 데이터 개수 확인
     * GET /api/admin/database/counts
     */
    @GetMapping("/counts")
    public ResponseEntity<ApiResponse<String>> getTableCounts() {
        try {
            databaseCleanupService.printTableCounts();
            return ResponseEntity.ok(
                ApiResponse.success("테이블 개수 확인 완료", "콘솔 로그를 확인하세요.")
            );
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("테이블 개수 확인 실패: " + e.getMessage()));
        }
    }
}

