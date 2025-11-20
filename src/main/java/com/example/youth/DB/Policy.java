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
}
