package com.example.youth.service;

import com.example.youth.DB.Policy;
import com.example.youth.DB.UserPolicyRecommendation;
import com.example.youth.dto.UserProfileResponse;
import com.example.youth.repository.PolicyRepository;
import com.example.youth.repository.UserPolicyRecommendationRepository;
import com.example.youth.repository.UserProfileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * 사용자별 Top-K 추천 캐시 저장·조회·갱신.
 */
@Service
public class UserPolicyRecommendationService {

    @Autowired
    private UserPolicyRecommendationRepository recommendationRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private UserProfileLoader userProfileLoader;

    @Autowired
    private PolicyRepository policyRepository;

    @Autowired
    private PolicyScoringService policyScoringService;

    @Value("${app.policy.cache-top-k:35}")
    private int cacheTopK;

    @Value("${app.policy.show-all:false}")
    private boolean showAllPolicies;

    /**
     * 캐시된 Top-K policyId 목록 (순위 순). 없으면 빈 리스트.
     */
    @Transactional(readOnly = true)
    public List<UserPolicyRecommendation> getCachedRecommendations(String userId) {
        return recommendationRepository.findByUserIdOrderByRankOrderAsc(userId);
    }

    /**
     * 해당 사용자 Top-K 재계산 후 DB 저장 (프로필 변경·캐시 miss 시).
     */
    @Transactional
    public int recomputeForUser(String userId) {
        UserProfileResponse profile = userProfileLoader.loadProfile(userId);
        if (profile == null) {
            recommendationRepository.deleteByUserId(userId);
            return 0;
        }

        List<Policy> policies = loadPoliciesForScoring(null);
        List<PolicyScoringService.ScoredPolicy> ranked = policyScoringService.rankPolicies(
                policies, profile, null, cacheTopK);

        recommendationRepository.deleteByUserId(userId);
        LocalDateTime now = LocalDateTime.now();
        int rank = 1;
        for (PolicyScoringService.ScoredPolicy sp : ranked) {
            recommendationRepository.save(UserPolicyRecommendation.builder()
                    .userId(userId)
                    .policyId(sp.policy().getPolicyId())
                    .score(sp.score())
                    .rankOrder(rank++)
                    .computedAt(now)
                    .build());
        }
        System.out.println("UserPolicyRecommendationService: userId=" + userId + " Top-" + ranked.size() + " 저장");
        return ranked.size();
    }

    /**
     * 주간 sync 후 전체 사용자 배치 재계산.
     */
    public void recomputeAllUsers() {
        List<String> userIds = userProfileRepository.findDistinctUserIdsWithProfile();
        System.out.println("UserPolicyRecommendationService: 배치 Top-K 시작, 사용자 " + userIds.size() + "명");
        int success = 0;
        for (String userId : userIds) {
            try {
                recomputeForUser(userId);
                success++;
            } catch (Exception e) {
                System.err.println("Top-K 재계산 실패 userId=" + userId + ": " + e.getMessage());
            }
        }
        System.out.println("UserPolicyRecommendationService: 배치 완료 " + success + "/" + userIds.size());
    }

    /**
     * 프로필 저장 후 비동기에 가깝게 단일 사용자만 갱신.
     */
    public void refreshForUserAsync(String userId) {
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            try {
                recomputeForUser(userId);
            } catch (Exception e) {
                System.err.println("비동기 Top-K 갱신 실패 userId=" + userId + ": " + e.getMessage());
            }
        });
    }

    @Transactional(readOnly = true)
    public Optional<Policy> findPolicyById(String policyId) {
        return policyRepository.findById(policyId);
    }

    private List<Policy> loadPoliciesForScoring(String category) {
        Date currentDate = new Date();
        List<Policy> policies;
        if (showAllPolicies) {
            policies = category != null && !category.isEmpty()
                    ? policyRepository.findByCategory(category)
                    : policyRepository.findAll();
        } else if (category != null && !category.isEmpty()) {
            policies = policyRepository.findActivePoliciesByCategory(currentDate, category);
        } else {
            policies = policyRepository.findActivePolicies(currentDate);
        }
        return policies != null ? new ArrayList<>(policies) : new ArrayList<>();
    }
}
