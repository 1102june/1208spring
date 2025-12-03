-- 로컬 MariaDB 데이터 삭제 SQL 스크립트 (간단 버전)
-- chat_history 제외 (테이블이 없을 수 있음)

USE wise_young;

SET FOREIGN_KEY_CHECKS = 0;

DELETE FROM bookmark;
DELETE FROM calendar_event;
DELETE FROM interest_category;
DELETE FROM user_profile;
DELETE FROM user;

SET FOREIGN_KEY_CHECKS = 1;

-- AUTO_INCREMENT 초기화
ALTER TABLE bookmark AUTO_INCREMENT = 1;
ALTER TABLE calendar_event AUTO_INCREMENT = 1;
ALTER TABLE interest_category AUTO_INCREMENT = 1;
ALTER TABLE user_profile AUTO_INCREMENT = 1;
ALTER TABLE user AUTO_INCREMENT = 1;

-- 결과 확인
SELECT 'bookmark' as table_name, COUNT(*) as count FROM bookmark
UNION ALL
SELECT 'calendar_event', COUNT(*) FROM calendar_event
UNION ALL
SELECT 'interest_category', COUNT(*) FROM interest_category
UNION ALL
SELECT 'user_profile', COUNT(*) FROM user_profile
UNION ALL
SELECT 'user', COUNT(*) FROM user;

