package com.example.youth.repository;

import com.example.youth.DB.AIRecommendation;
import com.example.youth.common.ContentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AIRecommendationRepository extends JpaRepository<AIRecommendation, Long> {
    
    // 사용자별 AI 추천 목록 조회
    List<AIRecommendation> findByUser_UserIdOrderByCreatedAtDesc(String userId);
    
    // 사용자별 특정 타입의 AI 추천 조회
    List<AIRecommendation> findByUser_UserIdAndContentTypeOrderByCreatedAtDesc(String userId, ContentType contentType);
}

