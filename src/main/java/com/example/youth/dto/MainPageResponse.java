package com.example.youth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MainPageResponse {
    private List<AIRecommendationResponse> aiRecommendedPolicies; // AI 추천 정책 목록
    private Integer unreadNotificationCount; // 읽지 않은 알림 개수
}

