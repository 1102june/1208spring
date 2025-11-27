package com.example.youth.service;

import com.example.youth.dto.AIRecommendationResponse;
import com.example.youth.dto.MainPageResponse;
import com.example.youth.dto.PolicyResponse;
import com.example.youth.dto.UserProfileResponse;
import com.example.youth.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MainService {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private PolicyService policyService;

    // 메인 페이지 데이터 조회
    public MainPageResponse getMainPageData(String userId) {
        // 1. 읽지 않은 알림 개수 조회
        long unreadCount = notificationRepository.findByUser_UserIdAndIsRead(userId, false).size();

        // 2. 사용자 프로필 기반 추천 정책 생성
        UserProfileResponse profile = userService.getUserProfile(userId);
        Integer age = profile.getAge();
        List<String> interests = profile.getInterests();

        // 기본 후보: 나이 기준 정책, 없으면 활성 정책 전체
        List<PolicyResponse> candidatePolicies;
        if (age != null) {
            candidatePolicies = policyService.getPoliciesByAge(age, userId);
        } else {
            candidatePolicies = policyService.getActivePolicies(userId);
        }

        // 관심 카테고리가 설정되어 있으면 해당 카테고리 위주로 필터링
        if (interests != null && !interests.isEmpty()) {
            candidatePolicies = candidatePolicies.stream()
                    .filter(p -> p.getCategory() != null && interests.contains(p.getCategory()))
                    .collect(Collectors.toList());
        }

        // 마감 임박 순으로 정렬 (applicationEnd 오름차순)
        candidatePolicies = candidatePolicies.stream()
                .sorted(Comparator.comparing(PolicyResponse::getApplicationEnd,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());

        // 상위 5개만 추천으로 사용
        int maxRecommendations = 5;
        List<AIRecommendationResponse> aiRecommendations = candidatePolicies.stream()
                .limit(maxRecommendations)
                .map(policy -> AIRecommendationResponse.builder()
                        .recId(System.currentTimeMillis()) // 간단한 ID (시간 기반)
                        .contentType("policy")
                        .contentId(policy.getPolicyId())
                        .createdAt(LocalDateTime.now())
                        .policy(policy)
                        .build())
                .collect(Collectors.toList());

        return MainPageResponse.builder()
                .aiRecommendedPolicies(aiRecommendations)
                .unreadNotificationCount((int) unreadCount)
                .build();
    }
}

