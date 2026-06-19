package com.example.youth.scheduler;

import com.example.youth.service.PolicySyncService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 청년정책 주간 동기화 스케줄러.
 * sync → 정책 전처리 → 전 사용자 Top-K 재계산 파이프라인은 PolicySyncService 내부에서 실행된다.
 */
@Component
public class PolicySyncScheduler {

    private static final Logger logger = LoggerFactory.getLogger(PolicySyncScheduler.class);

    @Autowired
    private PolicySyncService policySyncService;

    /** 매주 일요일 새벽 2시 */
    @Scheduled(cron = "${app.scheduler.policy-sync-cron:0 0 2 * * SUN}")
    public void syncPolicyDataWeekly() {
        logger.info("청년정책 주간 자동 동기화 시작");
        try {
            policySyncService.syncYouthPolicyData();
            logger.info("청년정책 주간 동기화 요청 완료 (API 수집·후처리는 백그라운드 진행)");
        } catch (Exception e) {
            logger.error("청년정책 주간 동기화 실패", e);
        }
    }
}
