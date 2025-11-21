package com.example.youth.repository;

import com.example.youth.DB.InterestCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InterestCategoryRepository extends JpaRepository<InterestCategory, Long> {
    
    // 사용자 ID로 관심사 목록 조회
    List<InterestCategory> findByUser_UserId(String userId);
}

