package com.example.youth.service;

import com.example.youth.DB.*;
import com.example.youth.common.ContentType;
import com.example.youth.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * AI 추천 서비스
 * 사용자 행동 데이터를 기반으로 개인화된 추천 생성
 */
@Service
public class AIRecommendationService {

    @Autowired
    private UserActivityRepository userActivityRepository;

    @Autowired
    private BookmarkRepository bookmarkRepository;

    @Autowired
    private InterestCategoryRepository interestCategoryRepository;

    @Autowired
    private PolicyRepository policyRepository;

    @Autowired
    private HousingRepository housingRepository;

    @Autowired
    private AIRecommendationRepository aiRecommendationRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * 사용자에게 AI 추천 생성 및 저장
     * @param userId 사용자 ID
     * @param maxRecommendations 최대 추천 개수
     */
    @Transactional
    public void generateAndSaveRecommendations(String userId, int maxRecommendations) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + userId));

        // 기존 추천 삭제 (선택사항 - 최신 추천만 유지하려면)
        // aiRecommendationRepository.deleteByUser_UserId(userId);

        // 추천 생성
        List<AIRecommendation> recommendations = generateRecommendations(userId, maxRecommendations);

        // 저장
        for (AIRecommendation recommendation : recommendations) {
            recommendation.setUser(user);
            recommendation.setCreatedAt(LocalDateTime.now());
            aiRecommendationRepository.save(recommendation);
        }
    }

    /**
     * 추천 생성 로직
     * 1. 사용자 관심사 기반 추천
     * 2. 사용자 행동 기반 추천 (조회, 북마크)
     * 3. 인기도 기반 추천
     */
    private List<AIRecommendation> generateRecommendations(String userId, int maxRecommendations) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + userId));
        List<AIRecommendation> recommendations = new ArrayList<>();
        Set<String> recommendedContentIds = new HashSet<>();

        // 1. 사용자 관심사 기반 정책 추천
        List<String> interestCategories = getInterestCategories(userId);
        if (!interestCategories.isEmpty()) {
            List<Policy> policiesByInterest = findPoliciesByCategories(interestCategories, maxRecommendations / 3);
            for (Policy policy : policiesByInterest) {
                if (!recommendedContentIds.contains(policy.getPolicyId())) {
                    recommendations.add(AIRecommendation.builder()
                            .contentType(ContentType.policy)
                            .contentId(policy.getPolicyId())
                            .build());
                    recommendedContentIds.add(policy.getPolicyId());
                }
            }
        }

        // 2. 사용자 행동 기반 추천 (최근 조회/북마크한 콘텐츠와 유사한 콘텐츠)
        List<String> viewedContentIds = getMostViewedContentIds(userId, ContentType.policy, 5);
        for (String contentId : viewedContentIds) {
            Policy policy = policyRepository.findById(contentId).orElse(null);
            if (policy != null) {
                // 같은 카테고리 또는 지역의 다른 정책 추천
                List<Policy> similarPolicies = findSimilarPolicies(policy, 2);
                for (Policy similarPolicy : similarPolicies) {
                    if (!recommendedContentIds.contains(similarPolicy.getPolicyId()) 
                            && recommendations.size() < maxRecommendations) {
                        recommendations.add(AIRecommendation.builder()
                                .contentType(ContentType.policy)
                                .contentId(similarPolicy.getPolicyId())
                                .build());
                        recommendedContentIds.add(similarPolicy.getPolicyId());
                    }
                }
            }
        }

        // 3. 인기도 기반 추천 (전체 사용자 중 많이 조회/북마크된 콘텐츠)
        List<String> popularContentIds = getPopularContentIds(ContentType.policy, maxRecommendations / 3);
        for (String contentId : popularContentIds) {
            if (!recommendedContentIds.contains(contentId) && recommendations.size() < maxRecommendations) {
                recommendations.add(AIRecommendation.builder()
                        .contentType(ContentType.policy)
                        .contentId(contentId)
                        .build());
                recommendedContentIds.add(contentId);
            }
        }

        // 4. 사용자 프로필 기반 추천 (나이, 지역 등)
        UserProfile profile = user.getProfile();
        if (profile != null) {
            List<Policy> profileBasedPolicies = findPoliciesByProfile(profile, maxRecommendations / 3);
            for (Policy policy : profileBasedPolicies) {
                if (!recommendedContentIds.contains(policy.getPolicyId()) 
                        && recommendations.size() < maxRecommendations) {
                    recommendations.add(AIRecommendation.builder()
                            .contentType(ContentType.policy)
                            .contentId(policy.getPolicyId())
                            .build());
                    recommendedContentIds.add(policy.getPolicyId());
                }
            }
        }

        return recommendations.stream()
                .limit(maxRecommendations)
                .collect(Collectors.toList());
    }

    /**
     * 사용자 관심사 카테고리 조회
     */
    private List<String> getInterestCategories(String userId) {
        return interestCategoryRepository.findByUser_UserId(userId)
                .stream()
                .map(InterestCategory::getCategory)
                .collect(Collectors.toList());
    }

    /**
     * 카테고리 기반 정책 조회
     */
    private List<Policy> findPoliciesByCategories(List<String> categories, int limit) {
        // 간단한 구현: 카테고리 중 하나라도 포함된 정책 조회
        return policyRepository.findAll().stream()
                .filter(policy -> categories.stream()
                        .anyMatch(cat -> policy.getCategory() != null && policy.getCategory().contains(cat)))
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * 사용자가 가장 많이 조회한 콘텐츠 ID 목록
     */
    private List<String> getMostViewedContentIds(String userId, ContentType contentType, int limit) {
        List<Object[]> results = userActivityRepository.findMostViewedContentIds(userId, contentType);
        return results.stream()
                .limit(limit)
                .map(result -> (String) result[0])
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * 유사한 정책 찾기 (같은 카테고리 또는 지역)
     */
    private List<Policy> findSimilarPolicies(Policy policy, int limit) {
        return policyRepository.findAll().stream()
                .filter(p -> !p.getPolicyId().equals(policy.getPolicyId()))
                .filter(p -> (policy.getCategory() != null && policy.getCategory().equals(p.getCategory())) ||
                            (policy.getRegion() != null && policy.getRegion().equals(p.getRegion())))
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * 인기 콘텐츠 ID 목록 (전체 사용자 중 많이 조회/북마크된 콘텐츠)
     */
    private List<String> getPopularContentIds(ContentType contentType, int limit) {
        // 간단한 구현: 최근 활성 정책 중 랜덤 선택
        // 실제로는 조회/북마크 수를 집계해야 함
        return policyRepository.findAll().stream()
                .limit(limit * 2) // 더 많이 가져와서 랜덤 선택
                .map(Policy::getPolicyId)
                .collect(Collectors.toList());
    }

    /**
     * 사용자 프로필 기반 정책 조회
     */
    private List<Policy> findPoliciesByProfile(UserProfile profile, int limit) {
        List<Policy> policies = new ArrayList<>();
        
        // 나이 기반 필터링
        if (profile.getBirthYear() != null) {
            int age = LocalDateTime.now().getYear() - profile.getBirthYear().getYear();
            policies = policyRepository.findAll().stream()
                    .filter(p -> p.getAgeStart() == null || age >= p.getAgeStart())
                    .filter(p -> p.getAgeEnd() == null || age <= p.getAgeEnd())
                    .collect(Collectors.toList());
        }
        
        // 지역 기반 필터링
        if (profile.getRegion() != null && !policies.isEmpty()) {
            policies = policies.stream()
                    .filter(p -> p.getRegion() == null || p.getRegion().contains(profile.getRegion()))
                    .collect(Collectors.toList());
        }
        
        return policies.stream()
                .limit(limit)
                .collect(Collectors.toList());
    }
}

