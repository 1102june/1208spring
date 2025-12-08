package com.example.youth.service;

import org.springframework.stereotype.Service;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Rate Limiting 서비스
 * API 키 사용량을 제한하여 Gemini API 사용량 제한에 걸리지 않도록 관리
 */
@Service
public class RateLimitService {
    
    // 사용자별 요청 카운터 (분당)
    private final ConcurrentHashMap<String, RequestCounter> userMinuteCounters = new ConcurrentHashMap<>();
    
    // 전역 요청 카운터 (분당) - API 키 전체 사용량 관리
    private final AtomicInteger globalMinuteCounter = new AtomicInteger(0);
    private volatile LocalDateTime globalMinuteStart = LocalDateTime.now();
    
    // 제한 설정
    private static final int MAX_REQUESTS_PER_USER_PER_MINUTE = 5; // 사용자당 분당 5회
    private static final int MAX_REQUESTS_PER_MINUTE = 30; // 전체 분당 30회 (API 키 보호)
    @SuppressWarnings("unused")
    private static final int MAX_REQUESTS_PER_DAY = 1000; // 일일 1000회 (향후 사용 예정)
    
    /**
     * 요청이 허용되는지 확인
     * @param userId 사용자 ID (null이면 IP 기반으로 처리)
     * @return 허용되면 true, 제한되면 false
     */
    public boolean isAllowed(String userId) {
        LocalDateTime now = LocalDateTime.now();
        
        // 전역 분당 제한 확인
        synchronized (globalMinuteCounter) {
            if (ChronoUnit.MINUTES.between(globalMinuteStart, now) >= 1) {
                // 1분이 지났으면 카운터 리셋
                globalMinuteCounter.set(0);
                globalMinuteStart = now;
            }
            
            if (globalMinuteCounter.get() >= MAX_REQUESTS_PER_MINUTE) {
                System.out.println("⚠️ Rate Limit: 전역 분당 제한 초과 (" + MAX_REQUESTS_PER_MINUTE + "회)");
                return false;
            }
        }
        
        // 사용자별 분당 제한 확인
        if (userId != null && !userId.isEmpty()) {
            String key = userId;
            RequestCounter counter = userMinuteCounters.computeIfAbsent(key, k -> new RequestCounter());
            
            synchronized (counter) {
                if (ChronoUnit.MINUTES.between(counter.startTime, now) >= 1) {
                    // 1분이 지났으면 카운터 리셋
                    counter.count = 0;
                    counter.startTime = now;
                }
                
                if (counter.count >= MAX_REQUESTS_PER_USER_PER_MINUTE) {
                    System.out.println("⚠️ Rate Limit: 사용자별 분당 제한 초과 (userId: " + userId + ", " + MAX_REQUESTS_PER_USER_PER_MINUTE + "회)");
                    return false;
                }
            }
        }
        
        return true;
    }
    
    /**
     * 요청 카운터 증가
     * @param userId 사용자 ID
     */
    public void increment(String userId) {
        // 전역 카운터 증가
        globalMinuteCounter.incrementAndGet();
        
        // 사용자별 카운터 증가
        if (userId != null && !userId.isEmpty()) {
            String key = userId;
            RequestCounter counter = userMinuteCounters.computeIfAbsent(key, k -> new RequestCounter());
            synchronized (counter) {
                counter.count++;
            }
        }
    }
    
    /**
     * 사용자별 요청 카운터
     */
    private static class RequestCounter {
        int count = 0;
        LocalDateTime startTime = LocalDateTime.now();
    }
    
    /**
     * 현재 사용량 정보 반환 (디버깅용)
     */
    public String getUsageInfo() {
        return String.format("전역 분당: %d/%d, 사용자별 카운터: %d명", 
            globalMinuteCounter.get(), 
            MAX_REQUESTS_PER_MINUTE,
            userMinuteCounters.size());
    }
}

