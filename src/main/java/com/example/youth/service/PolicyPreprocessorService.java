package com.example.youth.service;

import com.example.youth.DB.Policy;
import com.example.youth.repository.PolicyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 주간 policy sync 직후 정책 단위 전처리 (사용자 무관, 1회만 실행).
 */
@Service
public class PolicyPreprocessorService {

    @Autowired
    private PolicyRepository policyRepository;

    @Autowired
    private PolicyScoringService policyScoringService;

    @Autowired
    private PolicyRegionService policyRegionService;

    @Transactional
    public int preprocessAllPolicies() {
        List<Policy> policies = policyRepository.findAll();
        LocalDateTime now = LocalDateTime.now();
        int count = 0;
        for (Policy policy : policies) {
            policy.setHasApplicationLink(policyScoringService.hasApplicationLink(policy));
            if (policy.getRegion() == null || policy.getRegion().isBlank()) {
                policy.setRegion(policyRegionService.inferRegionFromPolicy(policy));
            }
            policy.setPreprocessedAt(now);
            policyRepository.save(policy);
            count++;
        }
        System.out.println("PolicyPreprocessorService: 전처리 완료 " + count + "건");
        return count;
    }

    @Transactional
    public void preprocessPolicy(Policy policy) {
        policy.setHasApplicationLink(policyScoringService.hasApplicationLink(policy));
        if (policy.getRegion() == null || policy.getRegion().isBlank()) {
            policy.setRegion(policyRegionService.inferRegionFromPolicy(policy));
        }
        policy.setPreprocessedAt(LocalDateTime.now());
        policyRepository.save(policy);
    }
}
