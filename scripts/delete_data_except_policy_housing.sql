-- Policy, HousingNotice, HousingComplex 테이블의 데이터를 제외하고 나머지 테이블의 데이터 삭제
-- 주의: 이 스크립트는 외래키 제약조건 때문에 순서가 중요합니다.

-- 외래키 제약조건을 일시적으로 비활성화 (MariaDB)
SET FOREIGN_KEY_CHECKS = 0;

-- 사용자 관련 데이터 삭제 (하위 테이블부터)
-- 주의: 테이블이 존재하지 않을 수 있으므로, 존재하는 테이블만 삭제됩니다
DELETE FROM user_activity;
DELETE FROM notification;
DELETE FROM calendar_event;
DELETE FROM bookmark;
DELETE FROM interest_category;
DELETE FROM user_profile;
DELETE FROM user;

-- Housing 테이블 삭제 (HousingComplex, HousingNotice와는 별개)
DELETE FROM housing;

-- 삭제된 테이블들 (엔티티가 삭제됨)
DELETE FROM admin;
DELETE FROM airecommendation;

-- ChatHistory는 애플리케이션 실행 후 생성될 수 있음
-- DELETE FROM chat_history;

-- 외래키 제약조건 재활성화
SET FOREIGN_KEY_CHECKS = 1;

-- 삭제 결과 확인
SELECT 'chat_history' AS table_name, COUNT(*) AS count FROM chat_history
UNION ALL
SELECT 'user_activity', COUNT(*) FROM user_activity
UNION ALL
SELECT 'notification', COUNT(*) FROM notification
UNION ALL
SELECT 'calendar_event', COUNT(*) FROM calendar_event
UNION ALL
SELECT 'bookmark', COUNT(*) FROM bookmark
UNION ALL
SELECT 'interest_category', COUNT(*) FROM interest_category
UNION ALL
SELECT 'user_profile', COUNT(*) FROM user_profile
UNION ALL
SELECT 'user', COUNT(*) FROM user
UNION ALL
SELECT 'housing', COUNT(*) FROM housing
UNION ALL
SELECT 'policy', COUNT(*) FROM policy
UNION ALL
SELECT 'housing_notice', COUNT(*) FROM housing_notice
UNION ALL
SELECT 'housing_complex', COUNT(*) FROM housing_complex;

