package com.example.youth.repository;

import com.example.youth.DB.Policy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface PolicyRepository extends JpaRepository<Policy, String> {
    
    // 신청 기간 내 정책 조회
    @Query("SELECT p FROM Policy p WHERE p.applicationStart <= :currentDate AND p.applicationEnd >= :currentDate")
    List<Policy> findActivePolicies(@Param("currentDate") Date currentDate);
    
    // 나이 범위에 맞는 정책 조회
    @Query("SELECT p FROM Policy p WHERE p.ageStart <= :age AND p.ageEnd >= :age")
    List<Policy> findPoliciesByAge(@Param("age") Integer age);
    
    // 지역별 정책 조회
    List<Policy> findByRegion(String region);
    
    // 카테고리별 정책 조회
    List<Policy> findByCategory(String category);
}

