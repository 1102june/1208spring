package com.example.youth.scheduler;

import com.example.youth.service.HousingSyncService;
import com.example.youth.service.HousingGeocodeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 임대주택 데이터 주간 자동 동기화 스케줄러.
 */
@Component
public class HousingDataScheduler {

    private static final Logger logger = LoggerFactory.getLogger(HousingDataScheduler.class);

    @Autowired
    private HousingSyncService housingSyncService;

    @Autowired
    private HousingGeocodeService housingGeocodeService;

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

    /** 매주 일요일 새벽 4시 — 동기화 후 좌표 없는 단지 배치 지오코딩 */
    @Scheduled(cron = "${app.scheduler.housing-geocode-cron:0 0 4 * * SUN}")
    public void geocodeComplexesWeekly() {
        logger.info("임대주택 단지 배치 지오코딩 시작");
        try {
            Map<String, Object> result = housingGeocodeService.batchGeocode(500, true);
            logger.info("임대주택 단지 배치 지오코딩 완료: {}", result);
        } catch (Exception e) {
            logger.error("임대주택 단지 배치 지오코딩 실패", e);
        }
    }
}
