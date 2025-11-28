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
import java.util.Set;
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
        // AI 추천 테이블에 데이터가 없으므로, 맞춤 정책 추천을 직접 사용 (최대 10개)
        List<AIRecommendationResponse> aiRecommendedPolicies = new java.util.ArrayList<>();
        
        try {
            // 맞춤 정책 추천 사용 (마감일이 남아있는 정책만)
            System.out.println("MainService: 맞춤 정책 추천 조회 시작, userId = " + userId);
            List<PolicyResponse> recommendedPolicies = policyService.getPersonalizedPolicies(userId, null, 10);
            System.out.println("MainService: 맞춤 정책 추천 조회 완료, 개수 = " + (recommendedPolicies != null ? recommendedPolicies.size() : 0));
            
            if (recommendedPolicies != null && !recommendedPolicies.isEmpty()) {
                // PolicyResponse를 AIRecommendationResponse로 변환
                aiRecommendedPolicies = recommendedPolicies.stream()
                        .map(policy -> {
                            try {
                                return AIRecommendationResponse.builder()
                                        .contentType(ContentType.policy)
                                        .contentId(policy.getPolicyId())
                                        .policy(policy)
                                        .build();
                            } catch (Exception e) {
                                System.err.println("MainService: AIRecommendationResponse 변환 중 오류: " + e.getMessage());
                                return null;
                            }
                        })
                        .filter(response -> response != null)
                        .collect(Collectors.toList());
            }
            
            System.out.println("MainService: 최종 AI 추천 정책 개수 = " + aiRecommendedPolicies.size());
        } catch (Exception e) {
            System.err.println("MainService: 맞춤 정책 추천 조회 중 오류: " + e.getMessage());
            e.printStackTrace();
            // 오류 발생 시 빈 리스트로 계속 진행
        }

        // 4. 읽지 않은 알림 개수 조회
        long unreadCount = 0;
        try {
            unreadCount = notificationRepository.findByUser_UserIdAndIsRead(userId, false).size();
        } catch (Exception e) {
            System.err.println("알림 개수 조회 중 오류: " + e.getMessage());
            e.printStackTrace();
            // 오류 발생 시 0으로 설정
        }

        // null 체크 및 기본값 설정
        if (aiRecommendedPolicies == null) {
            aiRecommendedPolicies = new java.util.ArrayList<>();
        }
        
        return MainPageResponse.builder()
                .aiRecommendedPolicies(aiRecommendedPolicies)
                .unreadNotificationCount((int) unreadCount)
                .build();
    }

    // AIRecommendation을 AIRecommendationResponse로 변환
    private AIRecommendationResponse convertToRecommendationResponse(AIRecommendation rec, String userId) {
        if (rec == null) {
            return null;
        }
        
        AIRecommendationResponse.AIRecommendationResponseBuilder builder = AIRecommendationResponse.builder()
                .recId(rec.getRecId())
                .contentType(rec.getContentType())
                .contentId(rec.getContentId())
                .createdAt(rec.getCreatedAt());

        // contentType에 따라 Policy 또는 Housing 정보 추가
        try {
            if (rec.getContentType() == ContentType.policy) {
                if (rec.getContentId() != null) {
                    Policy policy = policyRepository.findById(rec.getContentId())
                            .orElse(null);
                    if (policy != null) {
                        PolicyResponse policyResponse = policyService.getPolicyById(policy.getPolicyId(), userId);
                        builder.policy(policyResponse);
                    }
                }
            } else if (rec.getContentType() == ContentType.housing) {
                if (rec.getContentId() != null) {
                    Housing housing = housingRepository.findById(rec.getContentId())
                            .orElse(null);
                    if (housing != null) {
                        HousingResponse housingResponse = housingService.getHousingById(housing.getHousingId(), userId);
                        builder.housing(housingResponse);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("콘텐츠 조회 중 오류 (contentId: " + rec.getContentId() + ", contentType: " + rec.getContentType() + "): " + e.getMessage());
            e.printStackTrace();
            // 오류 발생 시 policy/housing 없이 계속 진행
        }

        return builder.build();
    }
}

