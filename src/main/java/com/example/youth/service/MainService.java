package com.example.youth.service;

import com.example.youth.dto.MainPageResponse;
import com.example.youth.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class MainService {

    @Autowired
    private NotificationRepository notificationRepository;

    // 메인 페이지 데이터 조회
    public MainPageResponse getMainPageData(String userId) {
        // 1. 읽지 않은 알림 개수 조회
        long unreadCount = notificationRepository.findByUser_UserIdAndIsRead(userId, false).size();

        return MainPageResponse.builder()
                .aiRecommendedPolicies(new ArrayList<>()) // AI 추천 기능 제거됨
                .unreadNotificationCount((int) unreadCount)
                .build();
    }
}

