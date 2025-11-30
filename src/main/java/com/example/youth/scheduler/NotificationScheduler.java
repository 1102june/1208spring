package com.example.youth.scheduler;

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

@Slf4j
@Component
public class NotificationScheduler {

    @Autowired
    private CalendarEventRepository calendarEventRepository;

    @Autowired
    private FcmService fcmService;

    // 매 분마다 실행 (초 단위 0일 때)
    @Scheduled(cron = "0 * * * * *")
    @Transactional(readOnly = true)
    public void checkAndSendNotifications() {
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();
        String currentTimeStr = now.format(DateTimeFormatter.ofPattern("HH:mm"));

        log.info("알림 스케줄러 실행: {} {}", today, currentTimeStr);

        // 모든 이벤트를 가져와서 필터링하는 것은 비효율적이지만, 일단 간단하게 구현.
        // 실제로는 DB 쿼리로 대상만 가져오는 것이 좋음.
        // 예: findAllByEndDateAfter(today)
        List<CalendarEvent> events = calendarEventRepository.findAll();

        for (CalendarEvent event : events) {
            try {
                // 1. 7일 전 알림
                if (event.isSevenDaysAlert() && event.getEndDate().minusDays(7).isEqual(today)) {
                    if (currentTimeStr.equals(event.getSevenDaysAlertTime())) {
                        sendNotification(event, "7일 남았습니다!");
                    }
                }

                // 2. 1일 전 알림
                if (event.isOneDayAlert() && event.getEndDate().minusDays(1).isEqual(today)) {
                    if (currentTimeStr.equals(event.getOneDayAlertTime())) {
                        sendNotification(event, "1일(내일) 마감입니다!");
                    }
                }

                // 3. 사용자 지정 알림
                if (event.isCustomAlert() && event.getCustomAlertDays() != null) {
                    if (event.getEndDate().minusDays(event.getCustomAlertDays()).isEqual(today)) {
                        if (currentTimeStr.equals(event.getCustomAlertTime())) {
                            sendNotification(event, event.getCustomAlertDays() + "일 남았습니다!");
                        }
                    }
                }
            } catch (Exception e) {
                log.error("알림 발송 중 오류 발생 (eventId: {}): {}", event.getEventId(), e.getMessage());
            }
        }
    }

    private void sendNotification(CalendarEvent event, String dDayMessage) {
        String userId = event.getUser().getUserId();
        String title = "청년정책 알림";
        String body = String.format("[%s] %s", event.getTitle(), dDayMessage);
        
        log.info("푸시 알림 발송 시도: User={}, Msg={}", userId, body);

        // FcmService를 통해 알림 발송 (userId로 토큰 조회하여 발송한다고 가정)
        try {
            fcmService.sendNotificationToUser(userId, title, body);
        } catch (Exception e) {
            log.error("FCM 발송 실패: {}", e.getMessage());
        }
    }
}

