package com.example.youth.DB;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Policy {

    @Id
    @Column(length = 50)
    private String policyId;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String summary;

    private String category;
    private String region;
    private Integer ageStart;
    private Integer ageEnd;

    @Column(columnDefinition = "TEXT")
    private String eligibility;

    private java.sql.Date applicationStart;
    private java.sql.Date applicationEnd;
    private String link1;
    private String link2;

    /** sync 시 전처리: link1·link2 중 하나라도 있으면 true */
    private Boolean hasApplicationLink;

    /** 정책 전처리(링크·메타 갱신) 시각 */
    private java.time.LocalDateTime preprocessedAt;
}
