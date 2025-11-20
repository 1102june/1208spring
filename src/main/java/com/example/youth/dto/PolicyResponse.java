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
    private String link1;
    private String link2;
    private Boolean isBookmarked; // 북마크 여부
}

