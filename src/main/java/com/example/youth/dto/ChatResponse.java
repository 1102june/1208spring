package com.example.youth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {
    private String response;  // 챗봇 응답
    private String conversationId;  // 대화 세션 ID
    private List<ActionLink> actionLinks;  // 응답 후 액션 링크 (정책 ID, 주택 ID 등)
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActionLink {
        private String type;  // "policy" 또는 "housing"
        private String id;    // 정책 ID 또는 주택 ID
        private String title; // 링크 제목
    }
}

