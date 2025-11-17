package com.example.youth.DB;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class User {

    @Id
    @Column(length = 50)
    private String userId; // Firebase UID

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false, length = 255)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    @Builder.Default
    private LoginType loginType = LoginType.local;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    @Builder.Default
    private OSType osType = OSType.android;

    @Column(length = 10)
    private String appVersion;

    @Column(length = 255)
    private String pushToken;

    @Column(length = 100)
    private String deviceId;

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

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

