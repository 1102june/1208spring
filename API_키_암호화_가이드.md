# 🔐 API 키 암호화 가이드

## 개요

Gemini API 키를 암호화하여 저장하고, 애플리케이션에서 복호화해서 사용하는 방법입니다.

---

## 🔑 암호화 방식

- **암호화 알고리즘**: AES-256
- **키 생성**: SecureRandom을 사용한 256비트 키
- **인코딩**: Base64

---

## 📝 로컬에서 API 키 암호화하기

### 방법 1: PowerShell 스크립트 사용 (권장)

```powershell
# 새 API 키 암호화
.\API_키_암호화_스크립트.ps1 -PlainApiKey "AIzaSyC2SzM_GRKI0pr1AT4PkT7ID29hIM3hFhw"
```

이 스크립트는:
1. 암호화 키를 자동 생성합니다
2. API 키를 암호화합니다
3. 결과를 파일로 저장합니다 (`encrypted_api_key_YYYYMMDD_HHmmss.txt`)
4. 서버 설정 안내를 제공합니다

### 방법 2: 기존 암호화 키 사용

이미 암호화 키가 있다면:

```powershell
.\API_키_암호화_스크립트.ps1 -PlainApiKey "새로운_API_키" -EncryptionKey "기존_암호화_키"
```

---

## 🖥️ 서버 설정 방법

### 1. 암호화 키와 암호화된 API 키 확인

암호화 스크립트 실행 후 생성된 파일 또는 화면 출력에서:
- `ENCRYPTION_KEY`: 복호화에 사용할 키
- `GEMINI_API_KEY`: 암호화된 API 키

### 2. 서버 .env 파일 업데이트

서버에 SSH 접속 후:

```bash
# 서버 접속
ssh root@210.104.76.139

# .env 파일 편집
nano /home/root/wiseyoung-backend/.env
```

다음 두 줄을 추가/수정:

```bash
# 암호화 키 (복호화에 사용)
ENCRYPTION_KEY=생성된_암호화_키_여기에_붙여넣기

# 암호화된 Gemini API 키
GEMINI_API_KEY=암호화된_API_키_여기에_붙여넣기
```

**중요**: 
- `ENCRYPTION_KEY`와 `GEMINI_API_KEY` 둘 다 설정해야 합니다
- `ENCRYPTION_KEY`는 서버에서만 사용하므로 절대 노출되면 안 됩니다
- `.env` 파일 권한 설정: `chmod 600 .env`

### 3. 서비스 재시작

```bash
# 서비스 재시작
sudo systemctl restart wiseyoung-backend

# 상태 확인
sudo systemctl status wiseyoung-backend

# 로그 확인 (복호화 성공 여부 확인)
sudo journalctl -u wiseyoung-backend -f | grep -i "gemini\|복호화"
```

로그에서 다음 메시지가 보이면 성공:
```
Gemini API 초기화:
  - Base URL: https://generativelanguage.googleapis.com/v1
  - Model: gemini-2.5-flash
  - API Key: AIzaSyC2Sz... (복호화 완료)
```

---

## 🔄 API 키 변경 시

### 새 API 키로 교체하기

1. **로컬에서 새 키 암호화**
   ```powershell
   # 기존 암호화 키 재사용
   .\API_키_암호화_스크립트.ps1 -PlainApiKey "새로운_API_키" -EncryptionKey "기존_ENCRYPTION_KEY"
   ```

2. **서버 .env 파일 업데이트**
   ```bash
   # 서버에서 실행
   sed -i 's/^GEMINI_API_KEY=.*/GEMINI_API_KEY=새로운_암호화된_키/' /home/root/wiseyoung-backend/.env
   ```

3. **서비스 재시작**
   ```bash
   sudo systemctl restart wiseyoung-backend
   ```

---

## 🔐 보안 모범 사례

### 1. 암호화 키 관리

- ✅ **서버 환경 변수로만 관리** (`.env` 파일)
- ✅ **`.env` 파일 권한 설정**: `chmod 600 .env`
- ❌ **Git에 커밋하지 않기**
- ❌ **문서에 실제 키 노출하지 않기**
- ❌ **암호화 키와 암호화된 키를 같은 파일에 저장하지 않기** (선택사항)

### 2. 키 분리 저장 (선택사항)

더 높은 보안을 위해:
- `ENCRYPTION_KEY`: 시스템 환경 변수로 설정
- `GEMINI_API_KEY`: `.env` 파일에 저장

```bash
# 시스템 환경 변수로 설정
sudo systemctl edit wiseyoung-backend

# [Service] 섹션에 추가
Environment="ENCRYPTION_KEY=암호화_키"
```

### 3. 정기적인 키 로테이션

- 주기적으로 암호화 키 변경
- 새 키로 재암호화 후 배포

---

## 🛠️ 문제 해결

### 오류: "ENCRYPTION_KEY 환경 변수가 설정되지 않았습니다"

**원인**: 서버 `.env` 파일에 `ENCRYPTION_KEY`가 없습니다.

**해결**:
```bash
# .env 파일 확인
cat /home/root/wiseyoung-backend/.env | grep ENCRYPTION_KEY

# 없으면 추가
echo "ENCRYPTION_KEY=암호화_키" >> /home/root/wiseyoung-backend/.env
```

### 오류: "복호화 실패"

**원인**: 
- 잘못된 암호화 키 사용
- 암호화된 키가 손상됨

**해결**:
1. 암호화 키 확인: `.env` 파일의 `ENCRYPTION_KEY` 값이 올바른지 확인
2. 암호화된 키 확인: `.env` 파일의 `GEMINI_API_KEY` 값이 올바른지 확인
3. 재암호화: 로컬에서 새로 암호화 후 서버에 업데이트

### 오류: "API 키 복호화 실패"

**원인**: 암호화 키와 암호화된 키가 일치하지 않습니다.

**해결**:
1. 암호화 당시 사용한 `ENCRYPTION_KEY` 확인
2. 서버의 `ENCRYPTION_KEY`와 일치하는지 확인
3. 일치하지 않으면 새로 암호화하거나 올바른 `ENCRYPTION_KEY`로 업데이트

---

## 📚 참고 자료

- [AES 암호화 (Wikipedia)](https://en.wikipedia.org/wiki/Advanced_Encryption_Standard)
- [Java Cryptography Architecture](https://docs.oracle.com/javase/8/docs/technotes/guides/security/crypto/CryptoSpec.html)
- [Spring Boot 환경 변수 관리](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.external-config)

---

**⚠️ 중요**: 암호화 키(`ENCRYPTION_KEY`)를 잃어버리면 복호화할 수 없습니다. 반드시 안전하게 백업하세요!

