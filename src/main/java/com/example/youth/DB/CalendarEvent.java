package com.example.youth.DB;

import com.example.youth.common.ContentType;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CalendarEvent {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long eventId;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    private String title;

    @Enumerated(EnumType.STRING)
    private ContentType eventType;

    private LocalDate endDate;
    
    // 활성 상태 (Y: 활성, N: 비활성)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ActiveStatus isActive = ActiveStatus.Y;
    
    // 알림 설정
    private boolean isSevenDaysAlert;
    private String sevenDaysAlertTime; // HH:mm

    private boolean isOneDayAlert;
    private String oneDayAlertTime; // HH:mm

    private boolean isCustomAlert;
    private Integer customAlertDays; // D-N
    private String customAlertTime; // HH:mm

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
