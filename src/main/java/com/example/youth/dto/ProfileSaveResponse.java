package com.example.youth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileSaveResponse {
    /** 프로필 저장 후 Top-K 추천 캐시가 즉시 갱신되었는지 */
    private boolean recommendationsRefreshed;
    /** 갱신된 추천 정책 캐시 건수 (Top-K) */
    private int recommendationCount;
}
