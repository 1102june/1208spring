package com.example.youth.DB;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 사용자별 정책 추천 Top-K 캐시 (점수·순위만 저장, 가중치 공식은 코드에 유지).
 */
@Entity
@Table(
        name = "user_policy_recommendation",
        uniqueConstraints = @UniqueConstraint(name = "uk_upr_user_policy", columnNames = {"user_id", "policy_id"}),
        indexes = @Index(name = "idx_upr_user_rank", columnList = "user_id, rank_order")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPolicyRecommendation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, length = 128)
    private String userId;

    @Column(name = "policy_id", nullable = false, length = 50)
    private String policyId;

    @Column(nullable = false)
    private Double score;

    @Column(name = "rank_order", nullable = false)
    private Integer rankOrder;

    @Column(name = "computed_at", nullable = false)
    private LocalDateTime computedAt;
}
