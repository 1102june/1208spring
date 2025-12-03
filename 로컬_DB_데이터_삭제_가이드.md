# 🗑️ 로컬 DB 데이터 삭제 가이드

다음 테이블의 데이터를 모두 삭제합니다:
- `user`
- `user_profile`
- `interest_category`
- `bookmark`
- `calendar_event`
- `chat_history`

---

## 방법 1: PowerShell 스크립트 사용 (권장)

```powershell
cd C:\WiseYoung_backend
.\로컬_DB_데이터_삭제_스크립트.ps1
```

기본값:
- 호스트: `127.0.0.1`
- 포트: `3306`
- 데이터베이스: `wise_young`
- 사용자: `root`
- 비밀번호: `1234`

다른 값 사용 시:
```powershell
.\로컬_DB_데이터_삭제_스크립트.ps1 -DbPassword "실제_비밀번호"
```

---

## 방법 2: SQL 파일 직접 실행

### MariaDB 명령줄 사용

```powershell
# MariaDB 설치 경로에 따라 경로가 다를 수 있습니다
mysql -u root -p1234 wise_young < 로컬_DB_데이터_삭제_수동.sql
```

또는:

```powershell
# MariaDB 경로가 PATH에 있는 경우
mysql -u root -p1234 wise_young < 로컬_DB_데이터_삭제_수동.sql
```

### MariaDB 클라이언트에서 직접 실행

```bash
mysql -u root -p
```

비밀번호 입력 후:

```sql
USE wise_young;

SET FOREIGN_KEY_CHECKS = 0;

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

SET FOREIGN_KEY_CHECKS = 1;
```

---

## 방법 3: HeidiSQL 또는 다른 DB 클라이언트 사용

1. HeidiSQL 실행
2. 로컬 MariaDB 연결
3. `wise_young` 데이터베이스 선택
4. 각 테이블을 우클릭 > "테이블 비우기" 또는 "데이터 삭제"

**순서**:
1. bookmark
2. calendar_event
3. interest_category
4. user_profile
5. chat_history
6. user

---

## ⚠️ 주의사항

1. **데이터 백업**: 삭제 전에 필요하면 백업하세요
2. **외래키 순서**: 외래키 제약조건 때문에 순서가 중요합니다
3. **AUTO_INCREMENT 초기화**: 삭제 후 AUTO_INCREMENT를 1로 초기화합니다

---

## 삭제 확인

삭제 후 확인:

```sql
SELECT 
    'bookmark' as table_name, COUNT(*) as count FROM bookmark
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
```

모든 테이블의 count가 0이면 삭제 완료입니다.

---

**가장 간단한 방법은 PowerShell 스크립트를 사용하는 것입니다!** 🚀

