package com.example.youth.repository;

import com.example.youth.DB.ActiveStatus;
import com.example.youth.DB.CalendarEvent;
import com.example.youth.DB.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface CalendarEventRepository extends JpaRepository<CalendarEvent, Long> {
    List<CalendarEvent> findByUser(User user);
    List<CalendarEvent> findByUserAndEndDateBetween(User user, LocalDate startDate, LocalDate endDate);
    
    // 활성화된 이벤트만 조회
    List<CalendarEvent> findByUserAndIsActive(User user, ActiveStatus isActive);
    List<CalendarEvent> findByUserAndEndDateBetweenAndIsActive(User user, LocalDate startDate, LocalDate endDate, ActiveStatus isActive);
}

