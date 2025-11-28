package com.example.youth.repository;

import com.example.youth.DB.HousingNotice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface HousingNoticeRepository extends JpaRepository<HousingNotice, String> {

    // 단지 식별자로 조회
    // Find notices by complex identifier
    List<HousingNotice> findByHsmpSn(String hsmpSn);

    // 단지명으로 조회

    // Find notices by complex name
    List<HousingNotice> findByHsmpNm(String hsmpNm);

    // 신청 기간 내 공고 조회 (마감일이 남아있는 공고만, applicationEnd가 null이거나 지난 공고는 제외)

    // Fetch notices whose application window includes the provided date
    @Query("SELECT n FROM HousingNotice n WHERE n.applicationStart <= :currentDate AND n.applicationEnd >= :currentDate")
    List<HousingNotice> findActiveNotices(@Param("currentDate") Date currentDate);

    // 단지 식별자 또는 단지명으로 조회 (매칭용)

    // Lookup by either complex identifier or complex name (for matching)
    @Query("SELECT n FROM HousingNotice n WHERE n.hsmpSn = :identifier OR n.hsmpNm = :identifier")
    List<HousingNotice> findByIdentifier(@Param("identifier") String identifier);
}