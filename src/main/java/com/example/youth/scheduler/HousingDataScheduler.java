package com.example.youth.scheduler;

import com.example.youth.service.HousingSyncService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 임대주택 데이터 자동 동기화 스케줄러
 * 매일 새벽 3시에 실행 (필요시 주기 변경 가능)
 */
@Component
public class HousingDataScheduler {

    private static final Logger logger = LoggerFactory.getLogger(HousingDataScheduler.class);

    @Autowired
    private HousingSyncService housingSyncService;

    /**
     * 매일 새벽 3시에 임대주택 데이터 동기화
     * cron 표현식: 초 분 시 일 월 요일
     */
    @Scheduled(cron = "0 0 3 * * *") // 매일 새벽 3시
    public void syncHousingDataDaily() {
        logger.info("임대주택 데이터 자동 동기화 시작");
        try {
            // 전체 데이터 동기화 (지역 코드 없이)
            housingSyncService.syncLHRentalHouseData(null, null);
            logger.info("임대주택 데이터 자동 동기화 완료");
        } catch (Exception e) {
            logger.error("임대주택 데이터 자동 동기화 실패", e);
        }
    }

    /**
     * 테스트용: 1시간마다 실행 (개발 단계에서만 사용)
     * 운영 환경에서는 주석 처리하거나 삭제
     */
    // @Scheduled(fixedRate = 3600000) // 1시간마다 (밀리초)
    // public void syncHousingDataHourly() {
    //     logger.info("임대주택 데이터 동기화 (1시간 주기)");
    //     housingSyncService.syncLHRentalHouseData(null, null);
    // }
}

