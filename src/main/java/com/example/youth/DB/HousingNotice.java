package com.example.youth.DB;

import jakarta.persistence.*;
import lombok.*;

import java.sql.Date;

/**
 * 공고문 API 데이터를 저장하는 엔티티
 * LH 공공데이터 API의 임대주택 공고문 정보
 */
@Entity
@Table(name = "housing_notice")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class HousingNotice {

    @Id
    @Column(length = 100)
    private String noticeId; // PAN_ID 또는 hsmpSn + panId 조합

    @Column(length = 100)
    private String hsmpSn; // 단지 식별자 (매칭 키)

    @Column(length = 255)
    private String hsmpNm; // 단지명 (매칭 키)

    @Column(length = 100)
    private String panId; // 공고ID

    @Column(length = 255)
    private String panNm; // 공고명

    @Column(length = 500)
    private String dtlUrl; // 공고 상세 URL

    @Column(length = 20)
    private String panNtStDt; // 공고게시일 (YYYY.MM.DD)

    @Column(length = 20)
    private String clsgDt; // 공고마감일 (YYYY.MM.DD)

    @Column(length = 20)
    private String panDt; // 공고일 (YYYYMMDD)

    private Date applicationStart; // 신청 시작일 (파싱된 값)

    private Date applicationEnd; // 신청 종료일 (파싱된 값)

    @Column(length = 50)
    private String cnpCd; // 지역코드

    @Column(length = 100)
    private String cnpCdNm; // 지역명

    @Column(length = 50)
    private String uppAisTpCd; // 상위 공고유형코드

    @Column(length = 100)
    private String uppAisTpNm; // 상위 공고유형명

    @Column(length = 50)
    private String aisTpCd; // 공고유형코드

    @Column(length = 100)
    private String aisTpCdNm; // 공고유형명

    @Column(length = 50)
    private String panSs; // 공고상태

    @Column(length = 20)
    private String allCnt; // 전체조회건수

    // === 상세/공급 정보 API 호출에 필요한 키 ===
    @Column(length = 10)
    private String ccrCnntSysDsCd; // 고객센터연계시스템구분코드

    @Column(length = 10)
    private String splInfTpCd; // 공급정보구분코드

    // === 상세정보(lhLeaseNoticeDtlInfo1) API로 채우는 단지 상세 ===
    @Column(length = 500)
    private String rnAdres; // 단지 주소 (LCT_ARA_ADR)

    @Column(length = 100)
    private String scAr; // 전용면적 범위 (SC_AR)

    @Column(length = 50)
    private String hshldCo; // 총세대수 (HSH_CNT)

    @Column(length = 100)
    private String htnFmlaNm; // 난방방식 (HTN_FMLA_DS_CD_NM)

    @Column(length = 50)
    private String mvinXpcYm; // 입주예정월 (MVIN_XPC_YM)

    @Column(columnDefinition = "TEXT")
    private String panDtlCts; // 공고 상세내용 (PAN_DTL_CTS)

    // === 공급정보(lhLeaseNoticeSplInfo1) API로 채우는 대표 공급가 ===
    @Column(length = 50)
    private String splXpcAmt; // 대표 예정가격/공급금액 (SPL_XPC_AMT)

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

