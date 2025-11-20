package com.example.youth.db;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

import com.example.youth.common.ContentType;


@Entity
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AIRecommendation {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long recId;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    private ContentType contentType;

    private String contentId;
    private LocalDateTime createdAt = LocalDateTime.now();
}
