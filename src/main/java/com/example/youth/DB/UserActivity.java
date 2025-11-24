package com.example.youth.DB;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import com.example.youth.common.ContentType;

/**
 * 사용자 행동 데이터 수집 엔티티
 * AI 추천을 위한 사용자 행동 로그 (클릭, 조회, 검색 등)
 */
@Entity
@Table(name = "user_activity", indexes = {
    @Index(name = "idx_user_activity_user_id", columnList = "user_id"),
    @Index(name = "idx_user_activity_content_type", columnList = "content_type"),
    @Index(name = "idx_user_activity_created_at", columnList = "created_at")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserActivity {

    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long activityId;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_type", nullable = false, length = 20)
    private ActivityType activityType; // CLICK, VIEW, SEARCH, BOOKMARK 등

    @Enumerated(EnumType.STRING)
    @Column(name = "content_type", length = 20)
    private ContentType contentType; // policy, housing, null (검색 등)

    @Column(name = "content_id", length = 100)
    private String contentId; // 정책 ID 또는 주택 ID

    @Column(name = "search_keyword", length = 200)
    private String searchKeyword; // 검색 키워드 (검색 활동인 경우)

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata; // 추가 메타데이터 (JSON 형식)

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    /**
     * 사용자 활동 타입
     */
    public enum ActivityType {
        CLICK,      // 클릭
        VIEW,       // 조회
        SEARCH,     // 검색
        BOOKMARK,   // 북마크
        SHARE,      // 공유
        APPLY       // 신청
    }
}

