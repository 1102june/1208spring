package com.example.youth.service;

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

        List<CalendarEvent> events = calendarEventRepository.findByUserAndEndDateBetween(user, startDate, endDate);

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
}
