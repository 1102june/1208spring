package com.example.youth.dto;

import lombok.Data;

@Data
public class ChatRequest {
    private String message;  // 사용자 질문
    private String userId;   // 사용자 ID (선택)
    private String conversationId;  // 대화 세션 ID (선택)
}

