package com.example.youth.service;

import com.example.youth.DB.ActiveStatus;
import com.example.youth.DB.CalendarEvent;
import com.example.youth.DB.User;
import com.example.youth.common.ContentType;
import com.example.youth.dto.CalendarEventRequest;
import com.example.youth.dto.CalendarEventResponse;
import com.example.youth.repository.CalendarEventRepository;
import com.example.youth.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CalendarService {

    @Autowired
    private CalendarEventRepository calendarEventRepository;

    @Autowired
    private UserRepository userRepository;

    @Transactional
    public CalendarEventResponse addEvent(String userId, CalendarEventRequest request) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));

        ContentType eventType = ContentType.valueOf(request.getEventType());
        LocalDate endDate = LocalDate.parse(request.getEndDate());

        CalendarEvent event = CalendarEvent.builder()
                .user(user)
                .title(request.getTitle())
                .eventType(eventType)
                .endDate(endDate)
                // 알림 설정 저장 (null 체크 및 기본값 처리)
                .isSevenDaysAlert(Boolean.TRUE.equals(request.getIsSevenDaysAlert()))
                .sevenDaysAlertTime(request.getSevenDaysAlertTime())
                .isOneDayAlert(Boolean.TRUE.equals(request.getIsOneDayAlert()))
                .oneDayAlertTime(request.getOneDayAlertTime())
                .isCustomAlert(Boolean.TRUE.equals(request.getIsCustomAlert()))
                .customAlertDays(request.getCustomAlertDays())
                .customAlertTime(request.getCustomAlertTime())
                .build();

        CalendarEvent saved = calendarEventRepository.save(event);

        return CalendarEventResponse.builder()
                .eventId(saved.getEventId())
                .userId(user.getUserId())
                .title(saved.getTitle())
                .eventType(saved.getEventType().name())
                .endDate(saved.getEndDate())
                .createdAt(saved.getCreatedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public List<CalendarEventResponse> getEvents(String userId, int year, int month) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));

        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.plusMonths(1).minusDays(1);

        // 활성화된 이벤트만 조회
        List<CalendarEvent> events = calendarEventRepository.findByUserAndEndDateBetweenAndIsActive(
                user, startDate, endDate, ActiveStatus.Y);

        return events.stream()
                .map(e -> CalendarEventResponse.builder()
                        .eventId(e.getEventId())
                        .userId(e.getUser().getUserId())
                        .title(e.getTitle())
                        .eventType(e.getEventType().name())
                        .endDate(e.getEndDate())
                        .createdAt(e.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteEvent(String userId, Long eventId) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));
        
        CalendarEvent event = calendarEventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("일정을 찾을 수 없습니다: " + eventId));
        
        // 사용자 본인의 일정만 비활성화 가능
        if (!event.getUser().getUserId().equals(userId)) {
            throw new IllegalArgumentException("본인의 일정만 삭제할 수 있습니다.");
        }
        
        // 삭제 대신 비활성화 처리
        event.setIsActive(ActiveStatus.N);
        calendarEventRepository.save(event);
    }

    @Transactional
    public void deleteAllEvents() {
        // 모든 이벤트 비활성화 (실제 삭제 대신)
        List<CalendarEvent> allEvents = calendarEventRepository.findAll();
        allEvents.forEach(event -> event.setIsActive(ActiveStatus.N));
        calendarEventRepository.saveAll(allEvents);
    }
}
