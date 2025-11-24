package com.example.youth.repository;

import com.example.youth.DB.UserActivity;
import com.example.youth.DB.UserActivity.ActivityType;
import com.example.youth.common.ContentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface UserActivityRepository extends JpaRepository<UserActivity, Long> {
    
    // 사용자별 활동 목록 조회 (최신순)
    List<UserActivity> findByUser_UserIdOrderByCreatedAtDesc(String userId);
    
    // 사용자별 특정 타입의 활동 조회
    List<UserActivity> findByUser_UserIdAndActivityTypeOrderByCreatedAtDesc(String userId, ActivityType activityType);
    
    // 사용자별 특정 기간 내 활동 조회
    List<UserActivity> findByUser_UserIdAndCreatedAtBetweenOrderByCreatedAtDesc(
        String userId, LocalDateTime startDate, LocalDateTime endDate);
    
    // 사용자별 특정 콘텐츠 타입의 활동 조회
    List<UserActivity> findByUser_UserIdAndContentTypeOrderByCreatedAtDesc(String userId, ContentType contentType);
    
    // 특정 콘텐츠에 대한 모든 사용자 활동 조회 (인기도 계산용)
    List<UserActivity> findByContentTypeAndContentId(ContentType contentType, String contentId);
    
    // 사용자별 가장 많이 조회한 콘텐츠 ID 목록
    @Query("SELECT ua.contentId, COUNT(ua) as cnt " +
           "FROM UserActivity ua " +
           "WHERE ua.user.userId = :userId " +
           "AND ua.contentType = :contentType " +
           "AND ua.activityType IN ('VIEW', 'CLICK') " +
           "GROUP BY ua.contentId " +
           "ORDER BY cnt DESC")
    List<Object[]> findMostViewedContentIds(@Param("userId") String userId, @Param("contentType") ContentType contentType);
    
    // 사용자별 가장 많이 북마크한 콘텐츠 ID 목록
    @Query("SELECT ua.contentId, COUNT(ua) as cnt " +
           "FROM UserActivity ua " +
           "WHERE ua.user.userId = :userId " +
           "AND ua.contentType = :contentType " +
           "AND ua.activityType = 'BOOKMARK' " +
           "GROUP BY ua.contentId " +
           "ORDER BY cnt DESC")
    List<Object[]> findMostBookmarkedContentIds(@Param("userId") String userId, @Param("contentType") ContentType contentType);
}

