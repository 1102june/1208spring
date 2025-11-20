package com.example.youth.db;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class User {

    @Id
    @Column(length = 50)
    private String userId; // Firebase UID

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    // 🔥 OTP 인증 여부 관리 (Firebase 이메일 인증 X)
    @Column(nullable = false)
    private boolean emailVerified = false;

    @Column(nullable = true, length = 255)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private LoginType loginType;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private OSType osType;

    @Column(length = 10)
    private String appVersion;

    @Column(length = 255)
    private String pushToken;

    @Column(length = 100)
    private String deviceId;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
    private UserProfile profile;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<InterestCategory> interests;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<Bookmark> bookmarks;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<CalendarEvent> calendarEvents;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<Notification> notifications;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<AIRecommendation> aiRecommendations;
}
