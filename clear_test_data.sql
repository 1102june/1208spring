-- 테스트 데이터 삭제 스크립트
-- housing_complex, housing_notice, policy 테이블은 제외하고 나머지 데이터 삭제
-- MariaDB/MySQL용

-- 외래키 제약조건 비활성화
SET FOREIGN_KEY_CHECKS = 0;

-- 자식 테이블부터 삭제 (외래키 참조 순서 고려)
DELETE FROM interest_category;
DELETE FROM bookmark;
DELETE FROM calendar_event;
DELETE FROM user_activity;
DELETE FROM notification;
DELETE FROM ai_recommendation;
DELETE FROM user_profile;
DELETE FROM `user`;  -- user는 예약어이므로 백틱 사용

-- 외래키 제약조건 재활성화
SET FOREIGN_KEY_CHECKS = 1;

-- 삭제 결과 확인
SELECT 'interest_category' AS table_name, COUNT(*) AS remaining_count FROM interest_category
UNION ALL
SELECT 'bookmark', COUNT(*) FROM bookmark
UNION ALL
SELECT 'calendar_event', COUNT(*) FROM calendar_event
UNION ALL
SELECT 'user_activity', COUNT(*) FROM user_activity
UNION ALL
SELECT 'notification', COUNT(*) FROM notification
UNION ALL
SELECT 'ai_recommendation', COUNT(*) FROM ai_recommendation
UNION ALL
SELECT 'user_profile', COUNT(*) FROM user_profile
UNION ALL
SELECT 'user', COUNT(*) FROM `user`;

-- 유지되는 테이블 확인
SELECT 'housing_complex' AS table_name, COUNT(*) AS remaining_count FROM housing_complex
UNION ALL
SELECT 'housing_notice', COUNT(*) FROM housing_notice
UNION ALL
SELECT 'policy', COUNT(*) FROM policy;

