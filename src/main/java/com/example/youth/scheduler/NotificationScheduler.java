package com.example.youth.scheduler;

import com.example.youth.DB.ActiveStatus;
import com.example.youth.DB.CalendarEvent;
import com.example.youth.repository.CalendarEventRepository;
import com.example.youth.service.FcmService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class NotificationScheduler {

    @Autowired
    private CalendarEventRepository calendarEventRepository;

    @Autowired
    private FcmService fcmService;

    // 중복 발송 방지를 위한 발송 이력 (메모리 기반, 서버 재시작 시 초기화됨)
    // 실제 운영 환경에서는 DB에 발송 이력을 저장하는 것을 권장
    private final ConcurrentHashMap<String, LocalDateTime> sentNotifications = new ConcurrentHashMap<>();

    // 매 분마다 실행 (초 단위 0일 때)
    @Scheduled(cron = "0 * * * * *")
    @Transactional(readOnly = true)
    public void checkAndSendNotifications() {
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();
        String currentTimeStr = now.format(DateTimeFormatter.ofPattern("HH:mm"));

        log.debug("알림 스케줄러 실행: {} {}", today, currentTimeStr);

        // 활성화된 이벤트만 조회 (비활성화된 이벤트는 제외)
        List<CalendarEvent> events = calendarEventRepository.findAll()
                .stream()
                .filter(event -> event.getIsActive() == ActiveStatus.Y)
                .toList();

        for (CalendarEvent event : events) {
            try {
                // 1. 7일 전 알림
                if (event.isSevenDaysAlert() && event.getSevenDaysAlertTime() != null) {
                    LocalDate alertDate = event.getEndDate().minusDays(7);
                    if (alertDate.isEqual(today) && currentTimeStr.equals(event.getSevenDaysAlertTime())) {
                        String notificationKey = event.getEventId() + "_7days";
                        if (!isNotificationSent(notificationKey, now)) {
                            sendNotification(event, "7일 남았습니다!", notificationKey);
                        }
                    }
                }

                // 2. 1일 전 알림
                if (event.isOneDayAlert() && event.getOneDayAlertTime() != null) {
                    LocalDate alertDate = event.getEndDate().minusDays(1);
                    if (alertDate.isEqual(today) && currentTimeStr.equals(event.getOneDayAlertTime())) {
                        String notificationKey = event.getEventId() + "_1day";
                        if (!isNotificationSent(notificationKey, now)) {
                            sendNotification(event, "1일(내일) 마감입니다!", notificationKey);
                        }
                    }
                }

                // 3. 사용자 지정 알림
                if (event.isCustomAlert() && event.getCustomAlertDays() != null && event.getCustomAlertTime() != null) {
                    LocalDate alertDate = event.getEndDate().minusDays(event.getCustomAlertDays());
                    if (alertDate.isEqual(today) && currentTimeStr.equals(event.getCustomAlertTime())) {
                        String notificationKey = event.getEventId() + "_custom_" + event.getCustomAlertDays();
                        if (!isNotificationSent(notificationKey, now)) {
                            sendNotification(event, event.getCustomAlertDays() + "일 남았습니다!", notificationKey);
                        }
                    }
                }
            } catch (Exception e) {
                log.error("알림 발송 중 오류 발생 (eventId: {}): {}", event.getEventId(), e.getMessage(), e);
            }
        }
    }

    /**
     * 알림이 이미 발송되었는지 확인
     * 같은 날 같은 시간에 발송된 경우 중복 방지
     */
    private boolean isNotificationSent(String notificationKey, LocalDateTime now) {
        LocalDateTime lastSent = sentNotifications.get(notificationKey);
        if (lastSent == null) {
            return false;
        }
        // 같은 날 같은 시간에 발송된 경우만 중복으로 간주
        return lastSent.toLocalDate().equals(now.toLocalDate()) 
                && lastSent.getHour() == now.getHour() 
                && lastSent.getMinute() == now.getMinute();
    }

    /**
     * 알림 발송 및 발송 이력 저장
     */
    private void sendNotification(CalendarEvent event, String dDayMessage, String notificationKey) {
        String userId = event.getUser().getUserId();
        String title = "청년정책 알림";
        String body = String.format("[%s] %s", event.getTitle(), dDayMessage);
        
        log.info("푸시 알림 발송 시도: User={}, EventId={}, Msg={}", userId, event.getEventId(), body);

        // FcmService를 통해 알림 발송
        try {
            boolean success = fcmService.sendNotificationToUser(userId, title, body);
            if (success) {
                // 발송 성공 시 이력 저장
                sentNotifications.put(notificationKey, LocalDateTime.now());
                log.info("푸시 알림 발송 성공: User={}, EventId={}", userId, event.getEventId());
            } else {
                log.warn("푸시 알림 발송 실패: User={}, EventId={}", userId, event.getEventId());
            }
        } catch (Exception e) {
            log.error("FCM 발송 중 예외 발생: User={}, EventId={}, Error={}", 
                    userId, event.getEventId(), e.getMessage(), e);
        }
    }

    /**
     * 오래된 발송 이력 정리 (메모리 관리)
     * 하루가 지난 이력은 삭제
     */
    @Scheduled(cron = "0 0 1 * * *") // 매일 새벽 1시에 실행
    public void cleanupOldNotificationHistory() {
        LocalDateTime oneDayAgo = LocalDateTime.now().minusDays(1);
        sentNotifications.entrySet().removeIf(entry -> entry.getValue().isBefore(oneDayAgo));
        log.info("오래된 알림 발송 이력 정리 완료. 남은 이력 수: {}", sentNotifications.size());
    }
}


