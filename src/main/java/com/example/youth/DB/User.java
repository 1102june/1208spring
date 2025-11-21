package com.example.youth.DB;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@Table(name = "user")
public class User {

    @Id
    @Column(name = "user_id", length = 50)
    private String userId; // Firebase UID

    @Column(name = "email", nullable = false, unique = true, length = 100)
    private String email;

    // 🔥 OTP 인증 여부 관리 (Firebase 이메일 인증 X)
    // DB 스키마에는 없지만 앱 로직에서 필요하므로 유지
    @Column(nullable = false)
    private boolean emailVerified = false;

    @Column(name = "password_hash", nullable = true, length = 255)
    private String passwordHash; // Google 로그인 시 null 가능

    @Enumerated(EnumType.STRING)
    @Column(name = "login_type", length = 10)
    private LoginType loginType;

    @Enumerated(EnumType.STRING)
    @Column(name = "os_type", length = 10)
    private OSType osType;

    @Column(name = "app_version", length = 10)
    private String appVersion;

    @Column(name = "push_token", length = 255)
    private String pushToken;

    @Column(name = "device_id", length = 100)
    private String deviceId;

    @Column(name = "created_at", nullable = false)
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
