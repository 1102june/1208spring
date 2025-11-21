package com.example.youth.DB;

import jakarta.persistence.*;
import lombok.*;

import java.sql.Date;

/**
 * 단지정보 API 데이터를 저장하는 엔티티
 * LH 공공데이터 API의 임대주택 단지정보
 */
@Entity
@Table(name = "housing_complex")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class HousingComplex {

    @Id
    @Column(length = 100)
    private String complexId; // hsmpSn (단지 식별자)

    @Column(length = 255, nullable = false)
    private String hsmpNm; // 단지명

    @Column(length = 50)
    private String insttNm; // 기관명

    @Column(length = 50)
    private String brtcCode; // 광역시도 코드

    @Column(length = 100)
    private String brtcNm; // 광역시도명

    @Column(length = 50)
    private String signguCode; // 시군구 코드

    @Column(length = 100)
    private String signguNm; // 시군구명

    @Column(length = 500)
    private String rnAdres; // 도로명 주소

    @Column(length = 50)
    private String pnu; // PNU 코드

    @Column(length = 20)
    private String competDe; // 준공 일자 (YYYYMMDD)

    private Date completeDate; // 준공 일자 (파싱된 값)

    @Column(length = 20)
    private String hshldCo; // 세대수

    private Integer totalUnits; // 세대수 (파싱된 값)

    @Column(length = 100)
    private String suplyTyNm; // 공급 유형명 (예: 50년임대)

    @Column(length = 100)
    private String styleNm; // 형명

    @Column(length = 50)
    private String suplyPrvuseAr; // 공급 전용 면적 (㎡)

    private Double supplyArea; // 공급 면적 (파싱된 값)

    @Column(length = 50)
    private String suplyCmnuseAr; // 공급 공용 면적 (㎡)

    @Column(length = 100)
    private String houseTyNm; // 주택 유형명 (예: 아파트)

    @Column(length = 100)
    private String heatMthdDetailNm; // 난방 방식

    @Column(length = 100)
    private String buldStleNm; // 건물 형태

    @Column(length = 50)
    private String elvtrInstlAtNm; // 승강기 설치여부

    private Boolean elevator; // 승강기 설치여부 (파싱된 값)

    @Column(length = 20)
    private String parkngCo; // 주차수

    private Integer parkingSpaces; // 주차수 (파싱된 값)

    @Column(length = 50)
    private String bassRentGtn; // 기본 임대보증금

    private Integer deposit; // 기본 임대보증금 (파싱된 값)

    @Column(length = 50)
    private String bassMtRntchrg; // 기본 월임대료

    private Integer monthlyRent; // 기본 월임대료 (파싱된 값)

    @Column(length = 50)
    private String bassCnvrsGtnLmt; // 기본 전환보증금

    // 지도 표시를 위한 좌표
    private Double latitude;  // 위도
    private Double longitude; // 경도

    @Column(name = "created_at", updatable = false)
    private Date createdAt; // 생성일시

    @Column(name = "updated_at")
    private Date updatedAt; // 수정일시

    @PrePersist
    protected void onCreate() {
        createdAt = new Date(System.currentTimeMillis());
        updatedAt = new Date(System.currentTimeMillis());
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = new Date(System.currentTimeMillis());
    }
}

