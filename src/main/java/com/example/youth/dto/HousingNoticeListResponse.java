package com.example.youth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 임대주택 공고 지역 필터 UI용 응답.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HousingNoticeListResponse {
    /** 사용자 프로필 지역 (정렬 기준) */
    private String userRegion;
    /** 적용된 지역 필터 (null이면 전체) */
    private String appliedRegionFilter;
    /** 필터 선택용 광역시도 목록 */
    private List<String> availableRegions;
    private List<HousingNoticeResponse> notices;
}
