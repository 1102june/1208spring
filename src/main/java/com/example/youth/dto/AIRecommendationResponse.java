package com.example.youth.dto;

import com.example.youth.common.ContentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AIRecommendationResponse {
    private Long recId;
    private ContentType contentType;
    private String contentId;
    private LocalDateTime createdAt;
    private PolicyResponse policy; // contentType이 policy인 경우
    private HousingResponse housing; // contentType이 housing인 경우
}

