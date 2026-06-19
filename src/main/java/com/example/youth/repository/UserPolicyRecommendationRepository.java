package com.example.youth.repository;

import com.example.youth.DB.UserPolicyRecommendation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserPolicyRecommendationRepository extends JpaRepository<UserPolicyRecommendation, Long> {

    List<UserPolicyRecommendation> findByUserIdOrderByRankOrderAsc(String userId);

    @Modifying
    @Query("DELETE FROM UserPolicyRecommendation r WHERE r.userId = :userId")
    int deleteByUserId(@Param("userId") String userId);
}
