package com.example.youth.service;

import com.example.youth.DB.AIRecommendation;
import com.example.youth.DB.Policy;
import com.example.youth.DB.Housing;
import com.example.youth.dto.AIRecommendationResponse;
import com.example.youth.dto.MainPageResponse;
import com.example.youth.dto.PolicyResponse;
import com.example.youth.dto.HousingResponse;
import com.example.youth.repository.AIRecommendationRepository;
import com.example.youth.repository.NotificationRepository;
import com.example.youth.repository.PolicyRepository;
import com.example.youth.repository.HousingRepository;
import com.example.youth.common.ContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class MainService {

    @Autowired
    private AIRecommendationRepository aiRecommendationRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private PolicyRepository policyRepository;

    @Autowired
    private HousingRepository housingRepository;

    @Autowired
    private PolicyService policyService;

    @Autowired
    private HousingService housingService;

    // 메인 페이지 데이터 조회
    public MainPageResponse getMainPageData(String userId) {
        // 1. AI 추천 정책 목록 조회 (최신순, 최대 10개)
        List<AIRecommendation> recommendations = aiRecommendationRepository
                .findByUser_UserIdOrderByCreatedAtDesc(userId)
                .stream()
                .limit(10)
                .collect(Collectors.toList());

        // 2. AI 추천을 Response로 변환
        List<AIRecommendationResponse> aiRecommendedPolicies = recommendations.stream()
                .map(rec -> convertToRecommendationResponse(rec, userId))
                .filter(rec -> rec.getPolicy() != null || rec.getHousing() != null) // null인 항목 제거
                .collect(Collectors.toList());

        // 3. AI 추천이 없거나 부족하면 기본 추천 정책 제공
        if (aiRecommendedPolicies.isEmpty()) {
            // 기본 추천: 맞춤 정책 추천 사용
            List<PolicyResponse> defaultPolicies = policyService.getPersonalizedPolicies(userId, null, 10);
            aiRecommendedPolicies = defaultPolicies.stream()
                    .map(policy -> AIRecommendationResponse.builder()
                            .contentType(ContentType.policy)
                            .contentId(policy.getPolicyId())
                            .policy(policy)
                            .build())
                    .collect(Collectors.toList());
        }

        // 4. 읽지 않은 알림 개수 조회
        long unreadCount = notificationRepository.findByUser_UserIdAndIsRead(userId, false).size();

        return MainPageResponse.builder()
                .aiRecommendedPolicies(aiRecommendedPolicies)
                .unreadNotificationCount((int) unreadCount)
                .build();
    }

    // AIRecommendation을 AIRecommendationResponse로 변환
    private AIRecommendationResponse convertToRecommendationResponse(AIRecommendation rec, String userId) {
        AIRecommendationResponse.AIRecommendationResponseBuilder builder = AIRecommendationResponse.builder()
                .recId(rec.getRecId())
                .contentType(rec.getContentType())
                .contentId(rec.getContentId())
                .createdAt(rec.getCreatedAt());

        // contentType에 따라 Policy 또는 Housing 정보 추가
        if (rec.getContentType() == ContentType.policy) {
            Policy policy = policyRepository.findById(rec.getContentId())
                    .orElse(null);
            if (policy != null) {
                PolicyResponse policyResponse = policyService.getPolicyById(policy.getPolicyId(), userId);
                builder.policy(policyResponse);
            }
        } else if (rec.getContentType() == ContentType.housing) {
            Housing housing = housingRepository.findById(rec.getContentId())
                    .orElse(null);
            if (housing != null) {
                HousingResponse housingResponse = housingService.getHousingById(housing.getHousingId(), userId);
                builder.housing(housingResponse);
            }
        }

        return builder.build();
    }
}

