package com.example.youth.DB;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import com.example.youth.common.ContentType;



@Entity
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Bookmark {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long bookmarkId;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    private ContentType contentType; // policy or housing

    private String contentId;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ActiveStatus isActive = ActiveStatus.Y;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
