# 🔐 서버 ENCRYPTION_KEY 설정 가이드

## 문제 해결

`ENCRYPTION_KEY 환경 변수가 설정되지 않았습니다` 오류가 발생하는 경우 해결 방법입니다.

---

## ✅ 해결 방법

### 1단계: 서버 .env 파일 확인

서버에 SSH 접속 후 `.env` 파일 확인:

```bash
# 서버 접속
ssh root@210.104.76.139

# .env 파일 확인
cat /home/root/wiseyoung-backend/.env | grep -E "ENCRYPTION_KEY|GEMINI_API_KEY"
```

다음 두 줄이 모두 있어야 합니다:

```bash
ENCRYPTION_KEY=암호화_키_값
GEMINI_API_KEY=암호화된_API_키_값
```

### 2단계: .env 파일에 ENCRYPTION_KEY 추가 (없는 경우)

```bash
# .env 파일 편집
nano /home/root/wiseyoung-backend/.env
```

다음 두 줄을 추가/확인:

```bash
# 암호화 키 (복호화에 사용)
ENCRYPTION_KEY=여기에_암호화_키_붙여넣기

# 암호화된 Gemini API 키
GEMINI_API_KEY=여기에_암호화된_API_키_붙여넣기
```

**중요**: 
- `ENCRYPTION_KEY`와 `GEMINI_API_KEY` 둘 다 필요합니다
- 값에 공백이나 특수문자가 있으면 따옴표로 감싸세요
- `#`으로 시작하는 줄은 주석입니다

### 3단계: .env 파일 형식 확인

`.env` 파일 형식이 올바른지 확인:

```bash
# .env 파일 전체 내용 확인 (비밀번호는 가려집니다)
cat /home/root/wiseyoung-backend/.env
```

올바른 형식 예시:

```bash
DB_USERNAME=root
DB_PASSWORD=비밀번호
EMAIL_USERNAME=subpark12@gmail.com
EMAIL_PASSWORD=비밀번호
ENCRYPTION_KEY=abcd1234...암호화키
GEMINI_API_KEY=xyz789...암호화된키
```

**잘못된 형식**:
- ❌ `ENCRYPTION_KEY = 값` (공백 있음)
- ❌ `export ENCRYPTION_KEY=값` (export 불필요)
- ❌ `ENCRYPTION_KEY 값` (= 없음)

### 4단계: Systemd 서비스 파일 확인

systemd 서비스가 `.env` 파일을 제대로 로드하는지 확인:

```bash
# 서비스 파일 확인
cat /etc/systemd/system/wiseyoung-backend.service
```

다음 줄이 있어야 합니다:

```ini
EnvironmentFile=/home/root/wiseyoung-backend/.env
```

없으면 추가:

```bash
sudo nano /etc/systemd/system/wiseyoung-backend.service
```

`[Service]` 섹션에 추가:

```ini
[Service]
Type=simple
User=root
WorkingDirectory=/home/root/wiseyoung-backend
EnvironmentFile=/home/root/wiseyoung-backend/.env
ExecStart=/usr/bin/java -jar -Dspring.profiles.active=prod /home/root/wiseyoung-backend/app.jar
```

### 5단계: Systemd 데몬 리로드

```bash
# 설정 변경 후 반드시 실행
sudo systemctl daemon-reload
```

### 6단계: 서비스 재시작

```bash
# 서비스 재시작
sudo systemctl restart wiseyoung-backend

# 상태 확인
sudo systemctl status wiseyoung-backend

# 로그 확인
sudo journalctl -u wiseyoung-backend -n 50 --no-pager
```

### 7단계: 환경 변수 로드 확인

서비스가 환경 변수를 제대로 로드하는지 확인:

```bash
# 서비스의 환경 변수 확인
sudo systemctl show wiseyoung-backend --property=Environment
```

또는 직접 확인:

```bash
# 환경 변수 테스트
cd /home/root/wiseyoung-backend
source .env
echo $ENCRYPTION_KEY
echo $GEMINI_API_KEY
```

---

## 🔍 문제 진단

### 환경 변수가 로드되지 않는 경우

1. **.env 파일 권한 확인**
   ```bash
   ls -la /home/root/wiseyoung-backend/.env
   ```
   권한이 `-rw-------` (600)이어야 합니다. 아니면:
   ```bash
   chmod 600 /home/root/wiseyoung-backend/.env
   ```

2. **.env 파일 형식 확인**
   ```bash
   # 각 줄이 KEY=VALUE 형식인지 확인
   cat /home/root/wiseyoung-backend/.env | grep -v "^#" | grep -v "^$"
   ```

3. **Systemd 로그 확인**
   ```bash
   sudo journalctl -u wiseyoung-backend -n 100 --no-pager | grep -i "encryption\|error"
   ```

---

## 📝 전체 설정 예시

서버 `.env` 파일 전체 내용 예시:

```bash
# 데이터베이스 설정
DB_USERNAME=root
DB_PASSWORD=비밀번호

# 이메일 설정
EMAIL_USERNAME=subpark12@gmail.com
EMAIL_PASSWORD=이메일_비밀번호

# 공공데이터 API 키
LH_SERVICE_KEY=서비스_키
YOUTH_POLICY_SERVICE_KEY=346f78cb-6fd7-435e-af26-5a471734b3ac

# 카카오맵 API 키
KAKAO_REST_API_KEY=7b587003c2c00f816243e86c138e7fcb

# 🔐 암호화 키 (복호화에 사용)
ENCRYPTION_KEY=생성된_암호화_키_여기에

# 🔐 암호화된 Gemini API 키
GEMINI_API_KEY=암호화된_API_키_여기에
```

---

## ⚠️ 중요 사항

1. **ENCRYPTION_KEY와 GEMINI_API_KEY 둘 다 필요**
   - 하나라도 없으면 애플리케이션 시작 실패

2. **.env 파일 권한 설정**
   ```bash
   chmod 600 /home/root/wiseyoung-backend/.env
   ```

3. **설정 변경 후 반드시**
   ```bash
   sudo systemctl daemon-reload
   sudo systemctl restart wiseyoung-backend
   ```

4. **값에 특수문자가 있는 경우**
   - 따옴표로 감싸지 마세요
   - systemd는 따옴표를 포함해서 읽습니다

---

## 🆘 여전히 오류가 발생하는 경우

1. **로그 확인**
   ```bash
   sudo journalctl -u wiseyoung-backend -f
   ```

2. **환경 변수 직접 확인**
   ```bash
   sudo systemctl show wiseyoung-backend --property=Environment | grep ENCRYPTION_KEY
   ```

3. **수동 테스트**
   ```bash
   cd /home/root/wiseyoung-backend
   source .env
   java -jar -Dspring.profiles.active=prod app.jar
   ```

문제가 계속되면 로그를 확인하고 공유해주세요.

