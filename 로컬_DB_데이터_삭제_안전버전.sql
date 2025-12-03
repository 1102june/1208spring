-- 로컬 MariaDB 데이터 삭제 SQL 스크립트 (안전 버전)
-- chat_history 테이블이 없을 수 있으므로 제외
-- 다음 테이블의 데이터를 모두 삭제합니다:
-- - user
-- - user_profile
-- - interest_category
-- - bookmark
-- - calendar_event

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

-- chat_history 테이블이 존재하는 경우에만 삭제
-- 테이블이 없으면 오류 무시
SET @table_exists = (SELECT COUNT(*) FROM information_schema.tables 
                     WHERE table_schema = 'wise_young' 
                     AND table_name = 'chat_history');

SET @sql = IF(@table_exists > 0, 
              'DELETE FROM chat_history; ALTER TABLE chat_history AUTO_INCREMENT = 1;', 
              'SELECT "chat_history 테이블이 존재하지 않습니다." as message;');

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

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
SELECT 'user', COUNT(*) FROM user;

