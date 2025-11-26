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
    private List<?> aiRecommendedPolicies; // AI 추천 기능 제거됨 (빈 리스트 반환)
    private Integer unreadNotificationCount; // 읽지 않은 알림 개수
}

