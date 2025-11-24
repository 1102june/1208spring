package com.example.youth.controller;

import com.example.youth.dto.ApiResponse;
import com.example.youth.dto.ChatRequest;
import com.example.youth.dto.ChatResponse;
import com.example.youth.service.GeminiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    @Autowired
    private GeminiService geminiService;

    /**
     * AI 챗봇 질의응답
     * POST /api/chat
     * 
     * 요구사항: CHAT_001 - 청년정책 Q&A
     */
    @PostMapping
    public Mono<ResponseEntity<ApiResponse<ChatResponse>>> chat(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestBody ChatRequest request) {
        
        // 입력 검증
        if (request.getMessage() == null || request.getMessage().trim().isEmpty()) {
            return Mono.just(ResponseEntity.badRequest()
                    .body(ApiResponse.error("메시지를 입력해주세요.")));
        }

        // 헤더 또는 요청에서 userId 가져오기
        String finalUserId = userId != null ? userId : request.getUserId();

        // Gemini API 호출
        return geminiService.generateChatResponse(request.getMessage(), finalUserId)
                .map(response -> ResponseEntity.ok(
                        ApiResponse.success("챗봇 응답 생성 성공", response)))
                .onErrorResume(e -> Mono.just(ResponseEntity.internalServerError()
                        .body(ApiResponse.error("챗봇 응답 생성 중 오류 발생: " + e.getMessage()))));
    }
}

