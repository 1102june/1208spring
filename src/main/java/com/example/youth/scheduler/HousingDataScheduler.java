package com.example.youth.scheduler;

import com.example.youth.service.HousingSyncService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 임대주택 데이터 주간 자동 동기화 스케줄러.
 */
@Component
public class HousingDataScheduler {

    private static final Logger logger = LoggerFactory.getLogger(HousingDataScheduler.class);

    @Autowired
    private HousingSyncService housingSyncService;

    /** 매주 일요일 새벽 3시 */
    @Scheduled(cron = "${app.scheduler.housing-sync-cron:0 0 3 * * SUN}")
    public void syncHousingDataWeekly() {
        logger.info("임대주택 데이터 주간 자동 동기화 시작");
        try {
            housingSyncService.syncLHRentalHouseData(null, null);
            logger.info("임대주택 데이터 주간 자동 동기화 요청 완료 (백그라운드 진행)");
        } catch (Exception e) {
            logger.error("임대주택 데이터 주간 자동 동기화 실패", e);
        }
    }
}
