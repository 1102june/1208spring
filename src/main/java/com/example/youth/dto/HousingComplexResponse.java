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
public class HousingComplexResponse {
    private String complexId;
    private String hsmpNm; // 단지명
    private String insttNm; // 기관명
    private String brtcNm; // 광역시도명
    private String signguNm; // 시군구명
    private String rnAdres; // 도로명 주소
    private Date completeDate; // 준공 일자
    private Integer totalUnits; // 세대수
    private String suplyTyNm; // 공급 유형명
    private String styleNm; // 형명
    private Double supplyArea; // 공급 면적
    private String houseTyNm; // 주택 유형명
    private String heatMthdDetailNm; // 난방 방식
    private String buldStleNm; // 건물 형태
    private Boolean elevator; // 승강기 설치여부
    private Integer parkingSpaces; // 주차수
    private Integer deposit; // 기본 임대보증금
    private Integer monthlyRent; // 기본 월임대료
    private Double latitude; // 위도
    private Double longitude; // 경도
    private Boolean isBookmarked; // 북마크 여부
    private Double distanceFromUser; // 사용자로부터의 거리 (미터 단위)

    /** Android 호환 표시 필드 */
    private String name;
    private String organization;
    private String region;
    private String address;
    private String housingType;
    private String heatingType;
    private Boolean hasElevator;
    private String completionDate;
}

