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
public class PolicyResponse {
    private String policyId;
    private String title;
    private String summary;
    private String category;
    private String region;
    private Integer ageStart;
    private Integer ageEnd;
    private String eligibility;
    private Date applicationStart;
    private Date applicationEnd;
    /** 카드 표시용 신청기간 (비어 있으면 "상시신청") */
    private String applicationPeriodText;
    /** 카드 표시용 요약 (말줄임 ...) */
    private String displaySummary;
    private String link1;
    private String link2;
    private Boolean isBookmarked; // 북마크 여부
}

