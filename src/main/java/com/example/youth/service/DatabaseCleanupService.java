package com.example.youth.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

/**
 * 데이터베이스 정리 서비스
 * Policy, HousingNotice, HousingComplex를 제외한 나머지 테이블의 데이터를 삭제
 */
@Service
public class DatabaseCleanupService {

    @Autowired
    private EntityManager entityManager;

    /**
     * Policy, HousingNotice, HousingComplex를 제외한 모든 테이블의 데이터 삭제
     * 주의: 이 메서드는 모든 사용자 데이터를 삭제합니다!
     */
    @Transactional
    public void deleteAllDataExceptPolicyAndHousing() {
        // 외래키 제약조건을 일시적으로 비활성화
        Query disableFK = entityManager.createNativeQuery("SET FOREIGN_KEY_CHECKS = 0");
        disableFK.executeUpdate();

        try {
            // 사용자 관련 데이터 삭제 (하위 테이블부터)
            entityManager.createNativeQuery("DELETE FROM chat_history").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM user_activity").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM notification").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM calendar_event").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM bookmark").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM interest_category").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM user_profile").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM user").executeUpdate();

            // Housing 테이블 삭제 (HousingComplex, HousingNotice와는 별개)
            entityManager.createNativeQuery("DELETE FROM housing").executeUpdate();

            System.out.println("✅ 데이터 삭제 완료: Policy, HousingNotice, HousingComplex를 제외한 모든 데이터가 삭제되었습니다.");
        } finally {
            // 외래키 제약조건 재활성화
            Query enableFK = entityManager.createNativeQuery("SET FOREIGN_KEY_CHECKS = 1");
            enableFK.executeUpdate();
        }
    }

    /**
     * 각 테이블의 데이터 개수 확인
     */
    public void printTableCounts() {
        String[] tables = {
            "chat_history", "user_activity", "notification", "calendar_event",
            "bookmark", "interest_category", "user_profile", "user",
            "housing", "policy", "housing_notice", "housing_complex"
        };

        System.out.println("\n=== 테이블별 데이터 개수 ===");
        for (String table : tables) {
            Query query = entityManager.createNativeQuery("SELECT COUNT(*) FROM " + table);
            Long count = ((Number) query.getSingleResult()).longValue();
            System.out.println(table + ": " + count);
        }
        System.out.println("==========================\n");
    }
}

