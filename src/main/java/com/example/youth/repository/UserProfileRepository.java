package com.example.youth.repository;

import com.example.youth.DB.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {
    
    // 사용자 ID로 프로필 조회
    Optional<UserProfile> findByUser_UserId(String userId);
}

