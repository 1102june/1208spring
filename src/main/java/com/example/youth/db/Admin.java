package com.example.youth.db;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Admin {

    @Id
    @Column(length = 50)
    private String adminId;

    @Column(unique = true, length = 100)
    private String email;

    @Column(nullable = false, length = 255)
    private String passwordHash;

    private LocalDateTime createdAt = LocalDateTime.now();
}
