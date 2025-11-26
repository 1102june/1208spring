package com.example.youth.DB;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * AI 챗봇 대화 기록을 저장하는 엔티티
 * 사용자별로 질문과 답변을 저장
 */
@Entity
@Table(name = "chat_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long chatId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String userMessage; // 사용자 질문

    @Column(nullable = false, columnDefinition = "TEXT")
    private String botResponse; // 챗봇 응답

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    // 선택적 필드: 응답에 포함된 액션 링크 정보 (JSON 형태로 저장 가능)
    @Column(columnDefinition = "TEXT")
    private String actionLinks; // JSON 형태로 저장 (정책 ID, 주택 ID 등)
}

