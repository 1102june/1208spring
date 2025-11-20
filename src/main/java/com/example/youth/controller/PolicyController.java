package com.example.youth.controller;

import com.example.youth.dto.ApiResponse;
import com.example.youth.dto.PolicyResponse;
import com.example.youth.service.PolicyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/policy")
public class PolicyController {

    @Autowired
    private PolicyService policyService;

    /**
     * 활성 정책 목록 조회
     * GET /api/policy/active?userId={userId}
     */
    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<PolicyResponse>>> getActivePolicies(
            @RequestParam(required = false, defaultValue = "test-user") String userId) {
        try {
            List<PolicyResponse> policies = policyService.getActivePolicies(userId);
            return ResponseEntity.ok(ApiResponse.success(policies));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("정책 조회 중 오류 발생: " + e.getMessage()));
        }
    }

    /**
     * 전체 정책 목록 조회 (테스트용)
     * GET /api/policy/all?userId={userId}
     */
    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<PolicyResponse>>> getAllPolicies(
            @RequestParam(required = false, defaultValue = "test-user") String userId) {
        try {
            List<PolicyResponse> policies = policyService.getAllPolicies(userId);
            return ResponseEntity.ok(ApiResponse.success(policies));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("정책 조회 중 오류 발생: " + e.getMessage()));
        }
    }

    /**
     * 정책 상세 조회
     * GET /api/policy/{policyId}?userId={userId}
     */
    @GetMapping("/{policyId}")
    public ResponseEntity<ApiResponse<PolicyResponse>> getPolicyById(
            @PathVariable String policyId,
            @RequestParam(required = false, defaultValue = "test-user") String userId) {
        try {
            PolicyResponse policy = policyService.getPolicyById(policyId, userId);
            return ResponseEntity.ok(ApiResponse.success(policy));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("정책 조회 중 오류 발생: " + e.getMessage()));
        }
    }
}

