package com.example.youth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookmarkResponse {
    private Long bookmarkId;
    private String userId;
    private String contentType;
    private String contentId;
    private String isActive;
    private LocalDateTime createdAt;
    /** contentType=policy 일 때 정책 상세 */
    private PolicyResponse policy;
    /** contentType=housing 일 때 임대주택 상세 */
    private HousingResponse housing;
}

