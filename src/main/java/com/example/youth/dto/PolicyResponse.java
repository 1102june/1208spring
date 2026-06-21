package com.example.youth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PolicyResponse {
    private String policyId;
    private String title;
    private String summary;
    private String category;
    private String region;
    private Integer ageStart;
    private Integer ageEnd;
    private String eligibility;
    /** UI 표시용 신청 시작 (날짜 없으면 "상시 신청") */
    private String applicationStart;
    /** UI 표시용 신청 마감 (날짜 없으면 "상시 신청") */
    private String applicationEnd;
    /** 카드·상세 공통 신청기간 문구 (날짜 없으면 "상시 신청") */
    private String applicationPeriodText;
    /** applicationPeriodText 와 동일 (Android 호환) */
    private String applicationPeriod;
    /** 카드 표시용 요약 (말줄임 ...) */
    private String displaySummary;
    private String link1;
    private String link2;
    private Boolean isBookmarked;
}
