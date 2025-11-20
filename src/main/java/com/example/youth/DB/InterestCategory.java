package com.example.youth.DB;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InterestCategory {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long interestId;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @Column(length = 30)
    private String category;
}
