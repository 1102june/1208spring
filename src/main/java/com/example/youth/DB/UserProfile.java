package com.example.youth.DB;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Table(name = "user_profile")
public class UserProfile {

    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "profile_id")
    private Long profileId;

    @OneToOne
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "birth_year")
    private LocalDate birthYear;
    
    @Column(name = "gender", length = 10)
    private String gender;
    
    @Column(name = "region", length = 10)
    private String region;
    
    @Column(name = "education", length = 10)
    private String education;
    
    @Column(name = "job_status", length = 10)
    private String jobStatus;
}
