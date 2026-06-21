package com.example.youth.repository;

import com.example.youth.DB.HousingComplex;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HousingComplexRepository extends JpaRepository<HousingComplex, String> {

    // 단지명으로 조회
    // Find by complex name
    Optional<HousingComplex> findByHsmpNm(String hsmpNm);

    // 지역별 조회

    // Search by region name (province or city/district)
    @Query("SELECT c FROM HousingComplex c WHERE c.brtcNm LIKE %:region% OR c.signguNm LIKE %:region%")
    List<HousingComplex> findByRegion(@Param("region") String region);

    // 주소로 조회

    // Search by road address
    @Query("SELECT c FROM HousingComplex c WHERE c.rnAdres LIKE %:address%")
    List<HousingComplex> findByAddress(@Param("address") String address);

    // 단지명으로 검색 (부분 일치)

    // Find complexes with names containing the provided text
    @Query("SELECT c FROM HousingComplex c WHERE c.hsmpNm LIKE %:name%")
    List<HousingComplex> findByHsmpNmContaining(@Param("name") String name);
}
