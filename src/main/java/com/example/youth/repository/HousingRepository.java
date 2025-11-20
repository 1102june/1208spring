package com.example.youth.repository;

import com.example.youth.DB.Housing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface HousingRepository extends JpaRepository<Housing, String> {
    
    // 신청 기간 내 임대주택 조회
    @Query("SELECT h FROM Housing h WHERE h.applicationStart <= :currentDate AND h.applicationEnd >= :currentDate")
    List<Housing> findActiveHousing(@Param("currentDate") Date currentDate);
    
    // 지역별 임대주택 조회 (주소에 포함)
    @Query("SELECT h FROM Housing h WHERE h.address LIKE %:region%")
    List<Housing> findHousingByRegion(@Param("region") String region);
    
    // 통계용 카운트 쿼리들
    @Query("SELECT COUNT(h) FROM Housing h WHERE h.address IS NOT NULL AND LENGTH(h.address) > 0")
    long countWithAddress();
    
    @Query("SELECT COUNT(h) FROM Housing h WHERE h.supplyArea IS NOT NULL")
    long countWithSupplyArea();
    
    @Query("SELECT COUNT(h) FROM Housing h WHERE h.applicationStart IS NOT NULL")
    long countWithApplicationStart();
    
    @Query("SELECT COUNT(h) FROM Housing h WHERE h.applicationEnd IS NOT NULL")
    long countWithApplicationEnd();
}

