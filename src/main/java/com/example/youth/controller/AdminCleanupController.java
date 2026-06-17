package com.example.youth.controller;

import com.example.youth.dto.ApiResponse;
import com.example.youth.repository.HousingNoticeRepository;
import com.example.youth.repository.PolicyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.sql.Date;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * 마감된 데이터 정리용 관리자 엔드포인트.
 * 로컬/배포 DB 양쪽에서 동일하게 호출하여 데이터를 일치시키기 위함.
 */
@RestController
@RequestMapping("/api/admin/cleanup")
public class AdminCleanupController {

    @Autowired
    private PolicyRepository policyRepository;

    @Autowired
    private HousingNoticeRepository housingNoticeRepository;

    /**
     * 마감일(applicationEnd)이 지정일 이전인(이미 마감된) 정책/주택공고 삭제.
     * applicationEnd가 null인 상시 데이터는 보존한다.
     *
     * DELETE /api/admin/cleanup/expired              (기본값: 2026-06-01 이전 마감 삭제)
     * DELETE /api/admin/cleanup/expired?before=2026-06-01
     */
    @DeleteMapping("/expired")
    @Transactional
    public ResponseEntity<ApiResponse<Object>> deleteExpired(
            @RequestParam(required = false, defaultValue = "2026-06-01") String before) {
        try {
            Date beforeDate = Date.valueOf(LocalDate.parse(before));

            int deletedPolicies = policyRepository.deleteExpiredBefore(beforeDate);
            int deletedNotices = housingNoticeRepository.deleteExpiredBefore(beforeDate);

            Map<String, Object> result = new HashMap<>();
            result.put("before", before);
            result.put("deletedPolicies", deletedPolicies);
            result.put("deletedHousingNotices", deletedNotices);
            result.put("remainingPolicies", policyRepository.count());
            result.put("remainingHousingNotices", housingNoticeRepository.count());

            return ResponseEntity.ok(
                    ApiResponse.success(before + " 이전 마감 데이터 삭제 완료", result));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("마감 데이터 삭제 중 오류 발생: " + e.getMessage()));
        }
    }

    /**
     * 정책/주택공고 데이터를 전부 삭제한다. (로컬-배포 DB를 동일 상태로 초기화 후 재동기화하기 위함)
     * 실수 방지를 위해 confirm=true 가 반드시 필요하다.
     *
     * DELETE /api/admin/cleanup/all?target=both&confirm=true   (기본값: both)
     * target: policies | notices | both
     */
    @DeleteMapping("/all")
    @Transactional
    public ResponseEntity<ApiResponse<Object>> deleteAll(
            @RequestParam(required = false, defaultValue = "both") String target,
            @RequestParam(required = false, defaultValue = "false") boolean confirm) {
        if (!confirm) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("전체 삭제는 confirm=true 파라미터가 필요합니다."));
        }
        try {
            Map<String, Object> result = new HashMap<>();
            boolean doPolicies = "both".equalsIgnoreCase(target) || "policies".equalsIgnoreCase(target);
            boolean doNotices = "both".equalsIgnoreCase(target) || "notices".equalsIgnoreCase(target);

            if (doPolicies) {
                long deleted = policyRepository.count();
                policyRepository.deleteAllInBatch();
                result.put("deletedPolicies", deleted);
            }
            if (doNotices) {
                long deleted = housingNoticeRepository.count();
                housingNoticeRepository.deleteAllInBatch();
                result.put("deletedHousingNotices", deleted);
            }

            result.put("target", target);
            result.put("remainingPolicies", policyRepository.count());
            result.put("remainingHousingNotices", housingNoticeRepository.count());

            return ResponseEntity.ok(ApiResponse.success("전체 삭제 완료 (target=" + target + ")", result));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("전체 삭제 중 오류 발생: " + e.getMessage()));
        }
    }
}
