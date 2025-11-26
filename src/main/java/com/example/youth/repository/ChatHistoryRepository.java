package com.example.youth.repository;

import com.example.youth.DB.ChatHistory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatHistoryRepository extends JpaRepository<ChatHistory, Long> {
    
    /**
     * 사용자별 챗봇 대화 기록 조회 (최신순)
     */
    List<ChatHistory> findByUser_UserIdOrderByCreatedAtDesc(String userId);
    
    /**
     * 사용자별 최근 N개 대화 기록 조회
     */
    List<ChatHistory> findTopByUser_UserIdOrderByCreatedAtDesc(String userId, Pageable pageable);
    
    /**
     * 사용자별 대화 기록 삭제
     */
    void deleteByUser_UserId(String userId);
}

