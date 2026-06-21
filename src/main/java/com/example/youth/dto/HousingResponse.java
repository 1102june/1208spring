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
public class HousingResponse {
    private String housingId;
    private String name;
    private String address;
    private Double supplyArea;
    private Date completeDate;
    private String organization;
    private Date applicationStart;
    private Date applicationEnd;
    private String heatingType;
    private Boolean elevator;
    private Integer parkingSpaces;
    private Integer deposit;
    private Integer monthlyRent;
    private Integer totalUnits;
    private String link;
    private Boolean isBookmarked; // 북마크 여부
    
    // 지도 표시용
    private Double latitude;  // 위도
    private Double longitude; // 경도
    private Double distanceFromUser; // 사용자로부터의 거리 (미터 단위)
    
    // 상세 정보
    private String housingType; // 주택유형
}

