package com.example.youth.service;

import com.example.youth.DB.Policy;
import com.example.youth.DB.ActiveStatus;
import com.example.youth.common.ContentType;
import com.example.youth.dto.PolicyResponse;
import com.example.youth.dto.UserProfileResponse;
import com.example.youth.repository.BookmarkRepository;
import com.example.youth.repository.PolicyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class PolicyService {

    @Autowired
    private PolicyRepository policyRepository;

    @Autowired
    private BookmarkRepository bookmarkRepository;

    @Autowired
    private UserService userService;

    // 정책을 PolicyResponse로 변환 (북마크 여부 포함)
    private PolicyResponse convertToResponse(Policy policy, String userId) {
        boolean isBookmarked = bookmarkRepository
                .findByUser_UserIdAndContentTypeAndContentId(userId, ContentType.policy, policy.getPolicyId())
                .map(bookmark -> bookmark.getIsActive() == ActiveStatus.Y)
                .orElse(false);

        return PolicyResponse.builder()
                .policyId(policy.getPolicyId())
                .title(policy.getTitle())
                .summary(policy.getSummary())
                .category(policy.getCategory())
                .region(policy.getRegion())
                .ageStart(policy.getAgeStart())
                .ageEnd(policy.getAgeEnd())
                .eligibility(policy.getEligibility())
                .applicationStart(policy.getApplicationStart())
                .applicationEnd(policy.getApplicationEnd())
                .link1(policy.getLink1())
                .link2(policy.getLink2())
                .isBookmarked(isBookmarked)
                .build();
    }

    // ID로 정책 조회
    public PolicyResponse getPolicyById(String policyId, String userId) {
        Policy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new RuntimeException("정책을 찾을 수 없습니다: " + policyId));
        return convertToResponse(policy, userId);
    }

    // 활성 정책 목록 조회
    public List<PolicyResponse> getActivePolicies(String userId) {
        Date currentDate = new Date();
        List<Policy> policies = policyRepository.findActivePolicies(currentDate);
        return policies.stream()
                .map(policy -> convertToResponse(policy, userId))
                .collect(Collectors.toList());
    }

    // 카테고리별 활성 정책 목록 조회
    public List<PolicyResponse> getActivePoliciesByCategory(String category, String userId) {
        Date currentDate = new Date();
        List<Policy> policies = policyRepository.findActivePoliciesByCategory(currentDate, category);
        return policies.stream()
                .map(policy -> convertToResponse(policy, userId))
                .collect(Collectors.toList());
    }

    // 나이에 맞는 정책 조회
    public List<PolicyResponse> getPoliciesByAge(Integer age, String userId) {
        List<Policy> policies = policyRepository.findPoliciesByAge(age);
        return policies.stream()
                .map(policy -> convertToResponse(policy, userId))
                .collect(Collectors.toList());
    }

    // 전체 정책 목록 조회 (테스트용)
    public List<PolicyResponse> getAllPolicies(String userId) {
        List<Policy> policies = policyRepository.findAll();
        return policies.stream()
                .map(policy -> convertToResponse(policy, userId))
                .collect(Collectors.toList());
    }

    /**
     * 사용자 프로필 기반 맞춤 정책 추천 목록 조회
     * - 나이 / 지역 / 관심 카테고리를 최대한 활용하고
     * - 해당 정보가 없는 정책은 "제한 없음"으로 간주하여 완전히 제외하지 않음
     * - 점수 기반 정렬 후 상위 N개만 반환
     */
    public List<PolicyResponse> getPersonalizedPolicies(String userId, String category, Integer limit) {
        // 1. 사용자 프로필 조회 (없으면 기존 활성 정책 반환)
        UserProfileResponse profile = null;
        try {
            profile = userService.getUserProfile(userId);
        } catch (Exception e) {
            // 프로필이 없으면 기존 활성 정책 목록을 그대로 반환
            return (category != null && !category.isEmpty())
                    ? getActivePoliciesByCategory(category, userId)
                    : getActivePolicies(userId);
        }

        // 프로필이 null이면 활성 정책 반환
        if (profile == null) {
            return (category != null && !category.isEmpty())
                    ? getActivePoliciesByCategory(category, userId)
                    : getActivePolicies(userId);
        }

        Integer age = profile.getAge();
        String userRegion = profile.getRegion();
        List<String> interests = profile.getInterests();

        // 2. 활성 정책(선택 카테고리 포함) 조회
        Date currentDate = new Date();
        List<Policy> activePolicies;
        if (category != null && !category.isEmpty()) {
            activePolicies = policyRepository.findActivePoliciesByCategory(currentDate, category);
        } else {
            activePolicies = policyRepository.findActivePolicies(currentDate);
        }
        
        // activePolicies가 null이면 빈 리스트 반환
        if (activePolicies == null) {
            activePolicies = new java.util.ArrayList<>();
        }

        // 3. 점수 계산 + 필터링
        LocalDate today = LocalDate.now();
        int maxResults = (limit != null && limit > 0) ? limit : 30;

        return activePolicies.stream()
                // 신청 기간이 현재 날짜 기준으로 남아있는 정책만 사용
                // applicationEnd가 null이면 기간 제한 없는 정책으로 간주하여 포함
                .filter(policy -> {
                    if (policy.getApplicationEnd() == null) {
                        // applicationEnd가 null이면 기간 제한 없는 정책으로 간주
                        return true;
                    }
                    LocalDate endDate = policy.getApplicationEnd().toInstant()
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate();
                    return !endDate.isBefore(today);
                })
                .map(policy -> {
                    // 기본 점수: 모든 정책에 기본 점수 부여 (다양한 정책 노출)
                    double score = 10.0;

                    // 3-1. 나이 조건: 범위가 명시되어 있으면, 벗어나는 정책은 점수만 낮게 반영
                    Integer ageStart = policy.getAgeStart();
                    Integer ageEnd = policy.getAgeEnd();
                    if (age != null && ageStart != null && ageEnd != null) {
                        if (age >= ageStart && age <= ageEnd) {
                            score += 30.0;
                        } else {
                            // 범위 밖인 경우 약간의 패널티 (완전히 제외하지 않음)
                            score -= 5.0;
                        }
                    } else if (age != null) {
                        // ageStart/ageEnd가 null이면 "모든 연령 가능"으로 보고 보너스 점수
                        score += 15.0;
                    }

                    // 3-2. 지역 점수: 정책 지역이 사용자 지역을 포함하면 가산점
                    String policyRegion = policy.getRegion();
                    if (userRegion != null && policyRegion != null) {
                        if (policyRegion.contains(userRegion) || userRegion.contains(policyRegion)) {
                            score += 20.0;
                        } else if (policyRegion.equals("전국") || policyRegion.equals("전체")) {
                            // 전국 정책은 중간 점수
                            score += 10.0;
                        }
                    } else if (policyRegion != null && (policyRegion.equals("전국") || policyRegion.equals("전체"))) {
                        // 지역 정보가 없지만 전국 정책인 경우
                        score += 10.0;
                    }

                    // 3-3. 관심 카테고리 점수
                    String policyCategory = policy.getCategory();
                    if (interests != null && policyCategory != null &&
                            interests.stream().anyMatch(interest -> Objects.equals(interest, policyCategory))) {
                        score += 20.0;
                    }

                    // 3-4. 마감 임박 가중치 (미래일 경우에만)
                    if (policy.getApplicationEnd() != null) {
                        LocalDate endDate = policy.getApplicationEnd().toInstant()
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate();
                        long daysUntilEnd = ChronoUnit.DAYS.between(today, endDate);
                        if (daysUntilEnd >= 0) {
                            // 마감이 가까울수록 조금 더 높은 점수 (최대 +10, 최소 0)
                            double deadlineScore = Math.max(0.0, 10.0 - Math.min(daysUntilEnd, 10));
                            score += deadlineScore;
                        }
                    }

                    // 최소 점수 보장 (음수 방지)
                    score = Math.max(0.0, score);

                    return new ScoredPolicy(policy, score);
                })
                // 점수가 0이라도, 사용자에게 노출될 수 있도록 그대로 둠
                .sorted(Comparator
                        .comparingDouble(ScoredPolicy::getScore).reversed()
                        .thenComparing(sp -> {
                            Date end = sp.policy.getApplicationEnd();
                            return end != null
                                    ? end.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
                                    : LocalDate.MAX;
                        }))
                .limit(maxResults)
                .map(scored -> convertToResponse(scored.policy, userId))
                .collect(Collectors.toList());
    }

    /**
     * 내부 사용용 점수 래퍼 클래스
     */
    private static class ScoredPolicy {
        private final Policy policy;
        private final double score;

        private ScoredPolicy(Policy policy, double score) {
            this.policy = policy;
            this.score = score;
        }

        public double getScore() {
            return score;
        }
    }
}

