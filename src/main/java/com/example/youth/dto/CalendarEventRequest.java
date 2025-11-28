package com.example.youth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CalendarEventRequest {
    private String userId;
    private String title;
    private String eventType; // "policy" or "housing"
    private String endDate; // "yyyy-MM-dd"
}

