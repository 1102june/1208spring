package com.example.youth.service;

import com.example.youth.DB.Policy;
import com.example.youth.dto.UserProfileResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 정책 추천 가중치 공식 (코드 단일 소스).
 * DB에는 Top-K 결과만 저장하고, 점수 계산은 항상 이 서비스를 사용한다.
 *
 * @see PolicyRegionService 지역 매칭·감점
 */
@Service
public class PolicyScoringService {

    /** 기본 점수 */
    public static final double BASE_SCORE = 10.0;

    /** 연령 완전 일치 (ageStart~ageEnd 안) */
    public static final double AGE_MATCH_BONUS = 30.0;
    /** 연령 범위 밖 */
    public static final double AGE_MISMATCH_PENALTY = 5.0;
    /** 연령 범위 미기재 시 (사용자 나이만 있을 때) */
    public static final double AGE_UNKNOWN_BONUS = 15.0;

    /** policy.region 필드가 사용자 지역과 일치 */
    public static final double POLICY_REGION_MATCH_BONUS = 20.0;
    /** policy.region = 전국/전체 */
    public static final double NATIONWIDE_REGION_BONUS = 10.0;

    /** title에 사용자 지역 키워드 포함 */
    public static final double TITLE_REGION_MATCH_BONUS = 40.0;
    /** summary에 사용자 지역 키워드 포함 */
    public static final double SUMMARY_REGION_MATCH_BONUS = 40.0;

    /** eligibility에 사용자 지역명 포함 */
    public static final double ELIGIBILITY_REGION_MATCH_BONUS = 10.0;

    /** 관심 카테고리 일치 */
    public static final double INTEREST_CATEGORY_BONUS = 20.0;

    /** 마감 임박 보너스 최대 (0~10일) */
    public static final double DEADLINE_BONUS_MAX = 10.0;

    /** 신청 링크(link1·link2) 없음 */
    public static final double NO_APPLICATION_LINK_PENALTY = 20.0;

    public static final int DEFAULT_TOP_K = 35;

    @Value("${app.policy.show-all:false}")
    private boolean showAllPolicies;

    @Autowired
    private PolicyRegionService policyRegionService;

    /**
     * 활성(마감 전) 정책만 필터링.
     */
    public boolean isActivePolicy(Policy policy, LocalDate today) {
        if (showAllPolicies) {
            return true;
        }
        if (policy.getApplicationEnd() == null) {
            return true;
        }
        try {
            return !policy.getApplicationEnd().toLocalDate().isBefore(today);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 사용자 프로필 × 정책 속성으로 점수 계산.
     */
    public double scorePolicy(Policy policy, UserProfileResponse profile, LocalDate today) {
        double score = BASE_SCORE;

        Integer age = profile != null ? profile.getAge() : null;
        String userRegion = profile != null ? profile.getRegion() : null;
        List<String> interests = profile != null && profile.getInterests() != null
                ? profile.getInterests()
                : List.of();
        List<String> regionKeywords = extractRegionKeywords(userRegion);

        Integer ageStart = policy.getAgeStart();
        Integer ageEnd = policy.getAgeEnd();
        if (age != null && ageStart != null && ageEnd != null) {
            if (age >= ageStart && age <= ageEnd) {
                score += AGE_MATCH_BONUS;
            } else {
                score -= AGE_MISMATCH_PENALTY;
            }
        } else if (age != null) {
            score += AGE_UNKNOWN_BONUS;
        }

        String policyRegion = policy.getRegion();
        if (userRegion != null && policyRegion != null) {
            if (policyRegionService.regionsAlign(userRegion, policyRegion)) {
                score += POLICY_REGION_MATCH_BONUS;
            } else if (policyRegionService.isNationwideRegion(policyRegion)) {
                score += NATIONWIDE_REGION_BONUS;
            }
        } else if (policyRegion != null && policyRegionService.isNationwideRegion(policyRegion)) {
            score += NATIONWIDE_REGION_BONUS;
        }

        if (!regionKeywords.isEmpty()) {
            if (containsAnyRegionKeyword(policy.getTitle(), regionKeywords)) {
                score += TITLE_REGION_MATCH_BONUS;
            }
            if (containsAnyRegionKeyword(policy.getSummary(), regionKeywords)) {
                score += SUMMARY_REGION_MATCH_BONUS;
            }
        }

        score += policyRegionService.computeEligibilityRegionMatchBonus(userRegion, policy);

        String policyCategory = policy.getCategory();
        if (!interests.isEmpty() && policyCategory != null && interests.contains(policyCategory)) {
            score += INTEREST_CATEGORY_BONUS;
        }

        if (policy.getApplicationEnd() != null) {
            LocalDate endDate = policy.getApplicationEnd().toLocalDate();
            long daysUntilEnd = ChronoUnit.DAYS.between(today, endDate);
            if (daysUntilEnd >= 0) {
                score += Math.max(0.0, DEADLINE_BONUS_MAX - Math.min(daysUntilEnd, (long) DEADLINE_BONUS_MAX));
            }
        }

        if (!hasApplicationLink(policy)) {
            score -= NO_APPLICATION_LINK_PENALTY;
        }

        score -= policyRegionService.computeEligibilityForeignPenalty(userRegion, policy);
        score -= policyRegionService.computeForeignRegionPenalty(userRegion, policy);

        return Math.max(0.0, score);
    }

    /**
     * 정책 목록을 점수 순으로 정렬해 상위 limit건 반환 (Policy + score).
     */
    public List<ScoredPolicy> rankPolicies(
            List<Policy> policies,
            UserProfileResponse profile,
            String category,
            int limit) {
        LocalDate today = LocalDate.now();
        int maxResults = limit > 0 ? limit : DEFAULT_TOP_K;

        return policies.stream()
                .filter(p -> category == null || category.isEmpty()
                        || (p.getCategory() != null && p.getCategory().equals(category)))
                .filter(p -> isActivePolicy(p, today))
                .map(p -> new ScoredPolicy(p, scorePolicy(p, profile, today)))
                .sorted(Comparator
                        .comparingDouble(ScoredPolicy::score).reversed()
                        .thenComparing(sp -> {
                            if (sp.policy().getApplicationEnd() == null) {
                                return LocalDate.MAX;
                            }
                            return sp.policy().getApplicationEnd().toLocalDate();
                        }))
                .limit(maxResults)
                .collect(Collectors.toList());
    }

    public boolean hasApplicationLink(Policy policy) {
        if (policy.getHasApplicationLink() != null) {
            return policy.getHasApplicationLink();
        }
        boolean link1Empty = policy.getLink1() == null || policy.getLink1().trim().isEmpty();
        boolean link2Empty = policy.getLink2() == null || policy.getLink2().trim().isEmpty();
        return !(link1Empty && link2Empty);
    }

    public List<String> extractRegionKeywords(String userRegion) {
        if (userRegion == null || userRegion.isBlank()) {
            return List.of();
        }
        Set<String> keywords = new LinkedHashSet<>();
        for (String part : userRegion.trim().split("\\s+")) {
            String normalized = normalizeRegionToken(part);
            if (normalized != null && normalized.length() >= 2) {
                keywords.add(normalized);
            }
        }
        return new ArrayList<>(keywords);
    }

    private String normalizeRegionToken(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        String t = token.trim();
        String[] suffixes = {"특별자치시", "특별자치도", "광역시", "특별시", "도", "시", "군", "구"};
        for (String suffix : suffixes) {
            if (t.endsWith(suffix) && t.length() > suffix.length()) {
                t = t.substring(0, t.length() - suffix.length());
                break;
            }
        }
        return t.isEmpty() ? null : t;
    }

    private boolean containsAnyRegionKeyword(String text, List<String> keywords) {
        if (text == null || text.isBlank() || keywords.isEmpty()) {
            return false;
        }
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    public record ScoredPolicy(Policy policy, double score) {}
}
