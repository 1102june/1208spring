package com.example.youth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HousingNoticeResponse {
    private String noticeId;
    private String hsmpSn; // 단지 식별자
    private String hsmpNm; // 단지명
    private String panId; // 공고ID
    private String panNm; // 공고명
    private String dtlUrl; // 공고 상세 URL
    private String panNtStDt; // 공고게시일
    private String clsgDt; // 공고마감일
    private String panDt; // 공고일
    private Date applicationStart; // 신청 시작일
    private Date applicationEnd; // 신청 종료일
    private String cnpCdNm; // 지역명
    /** housing_complex 매칭 광역시도명 */
    private String brtcNm;
    /** housing_complex 매칭 시군구명 */
    private String signguNm;
    /** 표시용 지역 (예: 경기도 부천시) */
    private String region;
    /** 사용자 프로필 지역과 일치 여부 (정렬·UI용) */
    private Boolean matchesUserRegion;
    private String uppAisTpNm; // 상위 공고유형명
    private String aisTpCdNm; // 공고유형명
    private String panSs; // 공고상태
    private Boolean isBookmarked; // 북마크 여부
    // 매칭된 단지정보 (있는 경우)
    private HousingComplexResponse matchedComplex;
}

