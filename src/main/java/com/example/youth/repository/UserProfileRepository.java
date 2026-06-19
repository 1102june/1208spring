package com.example.youth.repository;

import com.example.youth.DB.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {
    
    // 사용자 ID로 모든 프로필 조회 (중복 확인용)
    @Query("SELECT p FROM UserProfile p WHERE p.user.userId = :userId ORDER BY p.profileId DESC")
    List<UserProfile> findAllByUser_UserId(@Param("userId") String userId);
    
    // 사용자 ID로 프로필 조회 - native query로 첫 번째만 가져오기
    // MariaDB에서 LIMIT 1을 사용하여 중복 결과 방지
    @Query(value = "SELECT * FROM user_profile WHERE user_id = :userId ORDER BY profile_id DESC LIMIT 1", nativeQuery = true)
    Optional<UserProfile> findByUser_UserId(@Param("userId") String userId);

    @Query("SELECT DISTINCT p.user.userId FROM UserProfile p")
    List<String> findDistinctUserIdsWithProfile();
}

