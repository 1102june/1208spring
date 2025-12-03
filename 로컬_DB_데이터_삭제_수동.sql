-- 로컬 MariaDB 데이터 삭제 SQL 스크립트
-- 다음 테이블의 데이터를 모두 삭제합니다:
-- - user
-- - user_profile
-- - interest_category
-- - bookmark
-- - calendar_event
-- - chat_history

USE wise_young;

-- 외래키 체크 비활성화
SET FOREIGN_KEY_CHECKS = 0;

-- 데이터 삭제 (외래키 순서 고려)
DELETE FROM bookmark;
ALTER TABLE bookmark AUTO_INCREMENT = 1;

DELETE FROM calendar_event;
ALTER TABLE calendar_event AUTO_INCREMENT = 1;

DELETE FROM interest_category;
ALTER TABLE interest_category AUTO_INCREMENT = 1;

DELETE FROM user_profile;
ALTER TABLE user_profile AUTO_INCREMENT = 1;

DELETE FROM chat_history;
ALTER TABLE chat_history AUTO_INCREMENT = 1;

DELETE FROM user;
ALTER TABLE user AUTO_INCREMENT = 1;

-- 외래키 체크 활성화
SET FOREIGN_KEY_CHECKS = 1;

-- 삭제 결과 확인
SELECT 'bookmark' as table_name, COUNT(*) as count FROM bookmark
UNION ALL
SELECT 'calendar_event', COUNT(*) FROM calendar_event
UNION ALL
SELECT 'interest_category', COUNT(*) FROM interest_category
UNION ALL
SELECT 'user_profile', COUNT(*) FROM user_profile
UNION ALL
SELECT 'chat_history', COUNT(*) FROM chat_history
UNION ALL
SELECT 'user', COUNT(*) FROM user;

