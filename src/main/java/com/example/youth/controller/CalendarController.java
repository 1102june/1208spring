package com.example.youth.controller;

import com.example.youth.dto.ApiResponse;
import com.example.youth.dto.CalendarEventRequest;
import com.example.youth.dto.CalendarEventResponse;
import com.example.youth.service.CalendarService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/calendar")
public class CalendarController {

    @Autowired
    private CalendarService calendarService;

    @PostMapping
    public ResponseEntity<ApiResponse<CalendarEventResponse>> addEvent(
            @RequestHeader("X-User-Id") String userId,
            @RequestBody CalendarEventRequest request) {
        try {
            CalendarEventResponse response = calendarService.addEvent(userId, request);
            return ResponseEntity.ok(ApiResponse.success("일정이 추가되었습니다.", response));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CalendarEventResponse>>> getEvents(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam int year,
            @RequestParam int month) {
        try {
            List<CalendarEventResponse> responses = calendarService.getEvents(userId, year, month);
            return ResponseEntity.ok(ApiResponse.success("일정 목록 조회 성공", responses));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/{eventId}")
    public ResponseEntity<ApiResponse<Void>> deleteEvent(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable Long eventId) {
        try {
            calendarService.deleteEvent(userId, eventId);
            return ResponseEntity.ok(ApiResponse.success("일정이 삭제되었습니다.", null));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error(e.getMessage()));
        }
    }

    // 관리자용: 모든 캘린더 이벤트 삭제
    @DeleteMapping("/admin/all")
    public ResponseEntity<ApiResponse<String>> deleteAllEvents() {
        try {
            calendarService.deleteAllEvents();
            return ResponseEntity.ok(ApiResponse.success("모든 캘린더 이벤트가 삭제되었습니다.", null));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error(e.getMessage()));
        }
    }
}

