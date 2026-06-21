package com.example.youth.service;

import com.example.youth.DB.Policy;
import com.example.youth.DB.ActiveStatus;
import com.example.youth.DB.UserPolicyRecommendation;
import com.example.youth.common.ContentType;
import com.example.youth.dto.PolicyResponse;
import com.example.youth.dto.UserProfileResponse;
import com.example.youth.repository.BookmarkRepository;
import com.example.youth.repository.PolicyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

@Service
public class PolicyService {

    private static final int DISPLAY_SUMMARY_MAX_LEN = 80;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy.MM.dd");

    @Autowired
    private PolicyRepository policyRepository;

    @Autowired
    private BookmarkRepository bookmarkRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private PolicyScoringService policyScoringService;

    @Autowired
    private UserPolicyRecommendationService userPolicyRecommendationService;

    @Value("${app.policy.show-all:false}")
    private boolean showAllPolicies;

    private PolicyResponse convertToResponse(Policy policy, String userId) {
        boolean isBookmarked = bookmarkRepository
                .findByUser_UserIdAndContentTypeAndContentId(userId, ContentType.policy, policy.getPolicyId())
                .map(bookmark -> bookmark.getIsActive() == ActiveStatus.Y)
                .orElse(false);

        String applicationPeriodText = buildApplicationPeriodText(policy);
        String displaySummary = truncateSummary(policy.getSummary());

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
                .applicationPeriodText(applicationPeriodText)
                .displaySummary(displaySummary)
                .link1(policy.getLink1())
                .link2(policy.getLink2())
                .isBookmarked(isBookmarked)
                .build();
    }

    /**
     * 북마크 등 외부 서비스에서 정책 DTO 변환 시 사용.
     */
    public PolicyResponse toPolicyResponse(Policy policy, String userId) {
        return convertToResponse(policy, userId);
    }

    public PolicyResponse toPolicyResponse(String policyId, String userId) {
        return policyRepository.findById(policyId)
                .map(p -> convertToResponse(p, userId))
                .orElse(null);
    }

    static String buildApplicationPeriodText(Policy policy) {
        if (policy == null) {
            return "상시신청";
        }
        java.sql.Date start = policy.getApplicationStart();
        java.sql.Date end = policy.getApplicationEnd();
        if (start == null && end == null) {
            return "상시신청";
        }
        if (start != null && end != null) {
            return start.toLocalDate().format(DATE_FMT) + " ~ " + end.toLocalDate().format(DATE_FMT);
        }
        if (end != null) {
            return " ~ " + end.toLocalDate().format(DATE_FMT);
        }
        if (start != null) {
            return start.toLocalDate().format(DATE_FMT) + " ~";
        }
        return "상시신청";
    }

    static String truncateSummary(String summary) {
        if (summary == null || summary.isBlank()) {
            return "";
        }
        String trimmed = summary.trim().replaceAll("\\s+", " ");
        if (trimmed.length() <= DISPLAY_SUMMARY_MAX_LEN) {
            return trimmed;
        }
        return trimmed.substring(0, DISPLAY_SUMMARY_MAX_LEN - 3) + "...";
    }

    public PolicyResponse getPolicyById(String policyId, String userId) {
        Policy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new RuntimeException("정책을 찾을 수 없습니다: " + policyId));
        return convertToResponse(policy, userId);
    }

    public List<PolicyResponse> getActivePolicies(String userId) {
        List<Policy> policies = showAllPolicies
                ? policyRepository.findAll()
                : policyRepository.findActivePolicies(new Date());
        return policies.stream()
                .map(policy -> convertToResponse(policy, userId))
                .collect(Collectors.toList());
    }

    public List<PolicyResponse> getActivePoliciesByCategory(String category, String userId) {
        List<Policy> policies = showAllPolicies
                ? policyRepository.findByCategory(category)
                : policyRepository.findActivePoliciesByCategory(new Date(), category);
        return policies.stream()
                .map(policy -> convertToResponse(policy, userId))
                .collect(Collectors.toList());
    }

    public List<PolicyResponse> getPoliciesByAge(Integer age, String userId) {
        List<Policy> policies = policyRepository.findPoliciesByAge(age);
        return policies.stream()
                .map(policy -> convertToResponse(policy, userId))
                .collect(Collectors.toList());
    }

    public List<PolicyResponse> getAllPolicies(String userId) {
        List<Policy> policies = policyRepository.findAll();
        return policies.stream()
                .map(policy -> convertToResponse(policy, userId))
                .collect(Collectors.toList());
    }

    /**
     * 맞춤 정책 Top-K 조회.
     * - category 없음: DB 캐시(user_policy_recommendation) 우선, 없으면 즉시 재계산 후 저장.
     * - category 있음: 캐시 미사용, PolicyScoringService로 실시간 계산.
     */
    public List<PolicyResponse> getPersonalizedPolicies(String userId, String category, Integer limit) {
        try {
            UserProfileResponse profile;
            try {
                profile = userService.getUserProfile(userId);
            } catch (Exception e) {
                System.err.println("PolicyService: 프로필 조회 중 오류: " + e.getMessage());
                return (category != null && !category.isEmpty())
                        ? getActivePoliciesByCategory(category, userId)
                        : getActivePolicies(userId);
            }

            if (profile == null) {
                return (category != null && !category.isEmpty())
                        ? getActivePoliciesByCategory(category, userId)
                        : getActivePolicies(userId);
            }

            int maxResults = (limit != null && limit > 0) ? limit : PolicyScoringService.DEFAULT_TOP_K;

            if (category != null && !category.isEmpty()) {
                return computeLiveAndConvert(userId, profile, category, maxResults);
            }

            List<UserPolicyRecommendation> cached =
                    userPolicyRecommendationService.getCachedRecommendations(userId);
            if (cached.isEmpty()) {
                userPolicyRecommendationService.recomputeForUser(userId);
                cached = userPolicyRecommendationService.getCachedRecommendations(userId);
            }
            if (cached.isEmpty()) {
                return computeLiveAndConvert(userId, profile, null, maxResults);
            }

            List<PolicyResponse> result = new ArrayList<>();
            for (UserPolicyRecommendation row : cached) {
                if (result.size() >= maxResults) {
                    break;
                }
                userPolicyRecommendationService.findPolicyById(row.getPolicyId())
                        .map(p -> convertToResponse(p, userId))
                        .ifPresent(result::add);
            }
            return result;
        } catch (Exception e) {
            System.err.println("PolicyService: getPersonalizedPolicies 전체 오류: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    private List<PolicyResponse> computeLiveAndConvert(
            String userId, UserProfileResponse profile, String category, int maxResults) {
        List<Policy> policies;
        Date currentDate = new Date();
        if (showAllPolicies) {
            policies = category != null && !category.isEmpty()
                    ? policyRepository.findByCategory(category)
                    : policyRepository.findAll();
        } else if (category != null && !category.isEmpty()) {
            policies = policyRepository.findActivePoliciesByCategory(currentDate, category);
        } else {
            policies = policyRepository.findActivePolicies(currentDate);
        }
        if (policies == null) {
            policies = List.of();
        }

        return policyScoringService.rankPolicies(policies, profile, category, maxResults).stream()
                .map(sp -> convertToResponse(sp.policy(), userId))
                .collect(Collectors.toCollection(ArrayList::new));
    }
}
