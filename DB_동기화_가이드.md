# 데이터베이스 동기화 가이드 (로컬 → KT Cloud 서버)

## 📋 개요

로컬 MariaDB 데이터를 KT Cloud 개발 서버로 동기화하는 방법입니다.

---

## 🚀 자동 동기화 (스크립트 사용)

### PowerShell 스크립트 실행

```powershell
cd C:\WiseYoung_backend

# 기본 설정으로 실행
.\DB_동기화_스크립트.ps1

# 또는 파라미터 지정
.\DB_동기화_스크립트.ps1 `
    -ServerIP "210.104.76.139" `
    -ServerUser "root" `
    -LocalDBUser "root" `
    -LocalDBPassword "로컬DB비밀번호" `
    -ServerDBUser "root" `
    -ServerDBPassword "서버DB비밀번호" `
    -DatabaseName "wise_young"
```

### 스크립트 동작 과정

1. **로컬 데이터베이스 덤프**: `mysqldump`로 데이터베이스 덤프 생성
2. **서버로 전송**: SCP로 덤프 파일을 서버로 전송
3. **서버에서 복원**: 서버에서 덤프 파일을 복원
4. **임시 파일 정리**: 덤프 파일 삭제 (선택)

---

## 🔧 수동 동기화

### 1단계: 로컬에서 데이터베이스 덤프

```powershell
# PowerShell에서 실행
mysqldump -u root -p --databases wise_young --single-transaction --routines --triggers --add-drop-database --default-character-set=utf8mb4 > wise_young_dump.sql

# 비밀번호 입력 후 덤프 파일 생성 확인
```

### 2단계: 서버로 덤프 파일 전송

```powershell
# SCP로 전송
scp wise_young_dump.sql root@210.104.76.139:/tmp/wise_young_dump.sql

# 비밀번호 입력: Ahk3678@ as!@ #
```

### 3단계: 서버에서 데이터베이스 복원

```bash
# 서버 접속
ssh root@210.104.76.139

# 기존 데이터베이스 백업 (선택사항)
mysqldump -u root -p --databases wise_young > /tmp/wise_young_backup_$(date +%Y%m%d_%H%M%S).sql

# 데이터베이스 복원
mysql -u root -p < /tmp/wise_young_dump.sql

# 또는 특정 데이터베이스만 복원
mysql -u root -p wise_young < /tmp/wise_young_dump.sql

# 복원 확인
mysql -u root -p wise_young -e "SHOW TABLES;"
```

---

## ⚠️ 주의사항

### 1. 기존 데이터 백업

서버에 이미 데이터가 있는 경우, 복원 전에 백업하세요:

```bash
# 서버에서 실행
mysqldump -u root -p --databases wise_young > /tmp/wise_young_backup_$(date +%Y%m%d_%H%M%S).sql
```

### 2. 데이터베이스 덮어쓰기

`--add-drop-database` 옵션을 사용하면 기존 데이터베이스를 삭제하고 새로 생성합니다.

**기존 데이터를 유지하려면:**
- `--add-drop-database` 옵션 제거
- 또는 특정 테이블만 덤프/복원

### 3. 특정 테이블만 동기화

```powershell
# 특정 테이블만 덤프
mysqldump -u root -p wise_young user policy > user_policy_dump.sql

# 서버로 전송
scp user_policy_dump.sql root@210.104.76.139:/tmp/

# 서버에서 복원
mysql -u root -p wise_young < /tmp/user_policy_dump.sql
```

### 4. 데이터만 동기화 (스키마 제외)

```powershell
# 데이터만 덤프 (스키마 제외)
mysqldump -u root -p --no-create-info --databases wise_young > wise_young_data_only.sql

# 또는 특정 테이블 데이터만
mysqldump -u root -p --no-create-info wise_young user policy > user_policy_data.sql
```

---

## 🔍 동기화 확인

### 서버에서 확인

```bash
# 테이블 목록 확인
mysql -u root -p wise_young -e "SHOW TABLES;"

# 특정 테이블 데이터 개수 확인
mysql -u root -p wise_young -e "SELECT COUNT(*) FROM user;"
mysql -u root -p wise_young -e "SELECT COUNT(*) FROM policy;"

# 최근 데이터 확인
mysql -u root -p wise_young -e "SELECT * FROM user ORDER BY created_at DESC LIMIT 5;"
```

---

## 🆘 문제 해결

### 덤프 파일이 너무 큰 경우

```powershell
# 압축하여 전송
mysqldump -u root -p --databases wise_young | gzip > wise_young_dump.sql.gz
scp wise_young_dump.sql.gz root@210.104.76.139:/tmp/

# 서버에서 압축 해제 후 복원
ssh root@210.104.76.139 "gunzip < /tmp/wise_young_dump.sql.gz | mysql -u root -p"
```

### 권한 오류

```bash
# 서버에서 MySQL 사용자 권한 확인
mysql -u root -p -e "SHOW GRANTS FOR 'root'@'localhost';"

# 권한 부여
mysql -u root -p -e "GRANT ALL PRIVILEGES ON wise_young.* TO 'root'@'localhost';"
mysql -u root -p -e "FLUSH PRIVILEGES;"
```

### 문자 인코딩 문제

```powershell
# UTF-8로 명시적으로 덤프
mysqldump -u root -p --default-character-set=utf8mb4 --databases wise_young > wise_young_dump.sql
```

---

## 📝 빠른 참조

### 전체 데이터베이스 동기화

```powershell
# 1. 덤프
mysqldump -u root -p --databases wise_young > dump.sql

# 2. 전송
scp dump.sql root@210.104.76.139:/tmp/

# 3. 복원
ssh root@210.104.76.139 "mysql -u root -p < /tmp/dump.sql"
```

### 특정 테이블만 동기화

```powershell
# 1. 덤프
mysqldump -u root -p wise_young user policy > tables_dump.sql

# 2. 전송
scp tables_dump.sql root@210.104.76.139:/tmp/

# 3. 복원
ssh root@210.104.76.139 "mysql -u root -p wise_young < /tmp/tables_dump.sql"
```

---

## 🔄 역방향 동기화 (서버 → 로컬)

서버 데이터를 로컬로 가져오려면:

```bash
# 서버에서 덤프
ssh root@210.104.76.139 "mysqldump -u root -p --databases wise_young > /tmp/server_dump.sql"

# 로컬로 다운로드
scp root@210.104.76.139:/tmp/server_dump.sql ./

# 로컬에서 복원
mysql -u root -p < server_dump.sql
```

