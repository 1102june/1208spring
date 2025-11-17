package com.example.youth.repository;

import com.example.youth.DB.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    
    // 사용자별 알림 조회
    List<Notification> findByUser_UserIdOrderBySendDateDesc(String userId);
    
    // 사용자별 읽지 않은 알림 조회
    List<Notification> findByUser_UserIdAndIsRead(String userId, Boolean isRead);
}

