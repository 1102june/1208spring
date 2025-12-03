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
    
    // 신청 기간 내 정책 조회 (마감일이 남아있는 정책만, applicationEnd가 null이거나 지난 정책은 제외)
    @Query("SELECT p FROM Policy p WHERE p.applicationStart <= :currentDate AND p.applicationEnd >= :currentDate")
    List<Policy> findActivePolicies(@Param("currentDate") Date currentDate);
    
    // 신청 기간 내 + 카테고리별 정책 조회 (마감일이 남아있는 정책만, applicationEnd가 null이거나 지난 정책은 제외)
    @Query("SELECT p FROM Policy p WHERE p.applicationStart <= :currentDate AND p.applicationEnd >= :currentDate AND p.category = :category")
    List<Policy> findActivePoliciesByCategory(@Param("currentDate") Date currentDate, @Param("category") String category);
    
    // 나이 범위에 맞는 정책 조회
    @Query("SELECT p FROM Policy p WHERE p.ageStart <= :age AND p.ageEnd >= :age")
    List<Policy> findPoliciesByAge(@Param("age") Integer age);
    
    // 지역별 정책 조회
    List<Policy> findByRegion(String region);
    
    // 카테고리별 정책 조회
    List<Policy> findByCategory(String category);
    
    // 카테고리 포함 검색 (부분 일치)
    List<Policy> findByCategoryContaining(String category);
    
    // 제목이 null이거나 빈 문자열인 정책 조회
    @Query("SELECT p FROM Policy p WHERE p.title IS NULL OR p.title = ''")
    List<Policy> findPoliciesWithNullOrEmptyTitle();
    
    // 잘못 생성된 ID를 가진 정책 조회 (POLICY_로 시작하는 ID)
    @Query("SELECT p FROM Policy p WHERE p.policyId LIKE 'POLICY_%'")
    List<Policy> findPoliciesWithInvalidId();
}

