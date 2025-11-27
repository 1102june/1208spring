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
public class AIRecommendationResponse {

    private Long recId;
    private String contentType;   // "policy" 또는 "housing"
    private String contentId;
    private LocalDateTime createdAt;

    private PolicyResponse policy;
    private HousingResponse housing;
}


