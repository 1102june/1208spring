package com.example.youth.DB;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import com.example.youth.DB.User;
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
    private ActiveStatus isActive = ActiveStatus.Y;

    private LocalDateTime createdAt = LocalDateTime.now();
}


enum ActiveStatus { Y, N }
