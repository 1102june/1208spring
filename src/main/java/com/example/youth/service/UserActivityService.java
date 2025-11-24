package com.example.youth.service;

import com.example.youth.DB.User;
import com.example.youth.DB.UserActivity;
import com.example.youth.dto.UserActivityRequest;
import com.example.youth.repository.UserActivityRepository;
import com.example.youth.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 사용자 행동 데이터 수집 서비스
 * AI 추천을 위한 사용자 행동 로그 수집
 */
@Service
public class UserActivityService {

    @Autowired
    private UserActivityRepository userActivityRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * 사용자 활동 로그 저장
     * @param userId 사용자 ID
     * @param request 활동 요청 정보
     */
    @Transactional
    public void logActivity(String userId, UserActivityRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + userId));

        UserActivity activity = UserActivity.builder()
                .user(user)
                .activityType(request.getActivityType())
                .contentType(request.getContentType())
                .contentId(request.getContentId())
                .searchKeyword(request.getSearchKeyword())
                .metadata(request.getMetadata())
                .createdAt(LocalDateTime.now())
                .build();

        userActivityRepository.save(activity);
    }

    /**
     * 사용자별 활동 목록 조회
     * @param userId 사용자 ID
     * @return 활동 목록
     */
    public List<UserActivity> getUserActivities(String userId) {
        return userActivityRepository.findByUser_UserIdOrderByCreatedAtDesc(userId);
    }

    /**
     * 사용자별 특정 기간 내 활동 조회
     * @param userId 사용자 ID
     * @param days 최근 며칠간
     * @return 활동 목록
     */
    public List<UserActivity> getUserActivitiesByDays(String userId, int days) {
        LocalDateTime endDate = LocalDateTime.now();
        LocalDateTime startDate = endDate.minusDays(days);
        return userActivityRepository.findByUser_UserIdAndCreatedAtBetweenOrderByCreatedAtDesc(
                userId, startDate, endDate);
    }
}

