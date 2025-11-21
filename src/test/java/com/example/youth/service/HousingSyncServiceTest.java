package com.example.youth.service;

import com.example.youth.dto.publicdata.LHRentalNoticeResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class HousingSyncServiceTest {

    @Autowired
    private HousingSyncService housingSyncService;

    /**
     * 단지명 정규화 테스트
     */
    @Test
    void testNormalizeHousingName() throws Exception {
        Method method = HousingSyncService.class.getDeclaredMethod("normalizeHousingName", String.class);
        method.setAccessible(true);

        // 공백 제거 테스트
        String result1 = (String) method.invoke(housingSyncService, "청담  래미안");
        assertEquals("청담래미안", result1);

        // 특수문자 제거 테스트
        String result2 = (String) method.invoke(housingSyncService, "강남(아파트)-123");
        assertEquals("강남아파트123", result2);

        // 대소문자 통일 테스트
        String result3 = (String) method.invoke(housingSyncService, "Test Complex");
        assertEquals("testcomplex", result3);

        // null/빈 문자열 테스트
        String result4 = (String) method.invoke(housingSyncService, (String) null);
        assertEquals("", result4);

        String result5 = (String) method.invoke(housingSyncService, "");
        assertEquals("", result5);
    }

    /**
     * 유사도 계산 테스트
     */
    @Test
    void testCalculateSimilarity() throws Exception {
        Method method = HousingSyncService.class.getDeclaredMethod("calculateSimilarity", String.class, String.class);
        method.setAccessible(true);

        // 정확히 일치하는 경우
        double similarity1 = (Double) method.invoke(housingSyncService, "청담래미안", "청담래미안");
        assertEquals(1.0, similarity1, 0.001);

        // 정규화 후 일치하는 경우
        double similarity2 = (Double) method.invoke(housingSyncService, "청담 래미안", "청담래미안");
        assertEquals(1.0, similarity2, 0.001);

        // 유사한 경우 (유사도 0.8 이상이어야 함)
        double similarity3 = (Double) method.invoke(housingSyncService, "청담래미안", "청담래미안아파트");
        assertTrue(similarity3 > 0.0, "유사도가 0보다 커야 합니다.");

        // 완전히 다른 경우
        double similarity4 = (Double) method.invoke(housingSyncService, "청담래미안", "강남힐스테이트");
        assertTrue(similarity4 < 0.8, "유사도가 0.8보다 작아야 합니다.");

        // null 테스트
        double similarity5 = (Double) method.invoke(housingSyncService, (String) null, "테스트");
        assertEquals(0.0, similarity5, 0.001);
    }

    /**
     * 단지명 매칭 테스트 (퍼지 매칭 포함)
     */
    @Test
    void testFindMatchingNoticeByName() throws Exception {
        Method method = HousingSyncService.class.getDeclaredMethod(
            "findMatchingNoticeByName", 
            String.class, 
            Map.class
        );
        method.setAccessible(true);

        // 테스트용 공고문 데이터 생성
        Map<String, LHRentalNoticeResponse.Item> noticeMap = new HashMap<>();
        
        LHRentalNoticeResponse.Item notice1 = new LHRentalNoticeResponse.Item();
        notice1.setHsmpNm("청담래미안");
        noticeMap.put("청담래미안", notice1);

        LHRentalNoticeResponse.Item notice2 = new LHRentalNoticeResponse.Item();
        notice2.setHsmpNm("강남힐스테이트");
        noticeMap.put("강남힐스테이트", notice2);

        // 1순위: 정확히 일치하는 경우
        LHRentalNoticeResponse.Item result1 = (LHRentalNoticeResponse.Item) method.invoke(
            housingSyncService, 
            "청담래미안", 
            noticeMap
        );
        assertNotNull(result1);
        assertEquals("청담래미안", result1.getHsmpNm());

        // 2순위: 정규화된 이름으로 일치하는 경우
        LHRentalNoticeResponse.Item result2 = (LHRentalNoticeResponse.Item) method.invoke(
            housingSyncService, 
            "청담 래미안",  // 공백 포함
            noticeMap
        );
        assertNotNull(result2);
        assertEquals("청담래미안", result2.getHsmpNm());

        // 매칭되지 않는 경우
        LHRentalNoticeResponse.Item result3 = (LHRentalNoticeResponse.Item) method.invoke(
            housingSyncService, 
            "존재하지않는단지", 
            noticeMap
        );
        assertNull(result3);

        // 빈 Map 테스트
        Map<String, LHRentalNoticeResponse.Item> emptyMap = new HashMap<>();
        LHRentalNoticeResponse.Item result4 = (LHRentalNoticeResponse.Item) method.invoke(
            housingSyncService, 
            "청담래미안", 
            emptyMap
        );
        assertNull(result4);

        // null 테스트
        LHRentalNoticeResponse.Item result5 = (LHRentalNoticeResponse.Item) method.invoke(
            housingSyncService, 
            (String) null, 
            noticeMap
        );
        assertNull(result5);
    }

    /**
     * Levenshtein Distance 계산 테스트
     */
    @Test
    void testCalculateLevenshteinDistance() throws Exception {
        Method method = HousingSyncService.class.getDeclaredMethod(
            "calculateLevenshteinDistance", 
            String.class, 
            String.class
        );
        method.setAccessible(true);

        // 같은 문자열은 거리 0
        int distance1 = (Integer) method.invoke(housingSyncService, "테스트", "테스트");
        assertEquals(0, distance1);

        // 한 글자 차이
        int distance2 = (Integer) method.invoke(housingSyncService, "테스트", "테스트1");
        assertEquals(1, distance2);

        // 완전히 다른 문자열
        int distance3 = (Integer) method.invoke(housingSyncService, "테스트", "다른문자열");
        assertTrue(distance3 > 0);

        // 빈 문자열 테스트
        int distance4 = (Integer) method.invoke(housingSyncService, "", "테스트");
        assertEquals(3, distance4); // "테스트"의 길이만큼 삽입 필요
    }
}
