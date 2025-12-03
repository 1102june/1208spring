# 🔐 API 키 보안 가이드

## ⚠️ 중요 보안 규칙

Google의 Gemini API 키는 **비밀번호처럼 취급**해야 합니다. API 키가 유출되면:
- 다른 사용자가 프로젝트의 할당량을 사용할 수 있습니다
- 청구가 활성화된 경우 요금이 발생할 수 있습니다
- 비공개 데이터에 접근할 수 있습니다

---

## 🛡️ 보안 규칙

### 1. 소스 제어에 API 키를 커밋하지 마세요

❌ **절대 하지 말아야 할 것:**
- Git/GitHub에 API 키를 직접 커밋
- 코드 파일에 API 키를 하드코딩
- 문서 파일에 실제 API 키 노출

✅ **올바른 방법:**
- `.env` 파일에 API 키 저장
- `.env` 파일은 `.gitignore`에 포함 (이미 설정됨)
- 문서 파일에는 `your-api-key-here` 같은 플레이스홀더 사용

### 2. 클라이언트 측에서 API 키를 노출하지 마세요

❌ **절대 하지 말아야 할 것:**
- 안드로이드 앱 코드에 API 키 직접 사용
- JavaScript/TypeScript에서 API 키 노출
- 모바일 앱의 리소스 파일에 API 키 저장

✅ **올바른 방법:**
- **서버 측에서만 API 키 사용** (현재 구현 방식 ✅)
- 안드로이드 앱은 서버 API를 통해 챗봇 호출
- 클라이언트는 서버 엔드포인트만 호출

---

## 📁 현재 프로젝트 구조

### ✅ 안전한 구현 방식

```
안드로이드 앱
  └─> 서버 API 호출 (/api/chat)
       └─> 서버 백엔드 (Spring Boot)
            └─> Gemini API (API 키는 서버에서만 사용)
```

**안드로이드 앱**에서는 Gemini API 키를 **직접 사용하지 않습니다**. 모든 챗봇 요청은 서버를 통해 처리됩니다.

### 📂 API 키 저장 위치

1. **로컬 개발 환경**
   - `.env` 파일 (Git에 커밋되지 않음 ✅)
   - 또는 환경 변수로 설정

2. **서버 환경 (KT Cloud)**
   - `/home/root/wiseyoung-backend/.env` 파일
   - 파일 권한: `chmod 600 .env` (소유자만 읽기/쓰기)

---

## 🔄 API 키 업데이트 방법

### 방법 1: 자동 스크립트 사용 (권장)

```powershell
# PowerShell에서 실행
.\API_키_업데이트_스크립트.ps1 -NewApiKey "새로운_API_키"
```

### 방법 2: 수동 업데이트

#### 서버에서 직접 수정

```bash
# 1. 서버 접속
ssh root@210.104.76.139

# 2. .env 파일 백업
cp /home/root/wiseyoung-backend/.env /home/root/wiseyoung-backend/.env.backup

# 3. .env 파일 수정
nano /home/root/wiseyoung-backend/.env

# 4. GEMINI_API_KEY 줄 찾아서 새 키로 변경
# 변경 전: GEMINI_API_KEY=이전_키
# 변경 후: GEMINI_API_KEY=새로운_키

# 5. 저장 (Ctrl + X, Y, Enter)

# 6. 애플리케이션 재시작
sudo systemctl restart wiseyoung-backend

# 7. 상태 확인
sudo systemctl status wiseyoung-backend
```

#### sed 명령어로 빠르게 변경

```bash
# 서버에서 실행
sed -i 's/^GEMINI_API_KEY=.*/GEMINI_API_KEY=새로운_API_키/' /home/root/wiseyoung-backend/.env

# 확인
cat /home/root/wiseyoung-backend/.env | grep GEMINI_API_KEY

# 재시작
sudo systemctl restart wiseyoung-backend
```

---

## 🔍 API 키 노출 확인 방법

### Git 히스토리에서 API 키 검색

```bash
# Git 저장소에서 API 키 검색
git log --all --full-history -p | grep -i "AIzaSy"

# 특정 파일에서 검색
git log -p -- "파일명" | grep -i "AIzaSy"
```

### 코드베이스에서 API 키 검색

```bash
# 모든 파일에서 Gemini API 키 패턴 검색
grep -r "AIzaSy[A-Za-z0-9_-]*" . --exclude-dir=node_modules --exclude-dir=.git

# 특정 확장자 파일에서만 검색
grep -r "AIzaSy" --include="*.kt" --include="*.java" --include="*.md"
```

---

## 🚨 API 키가 노출된 경우 조치 방법

### 1. 즉시 조치

1. **Google Cloud Console에서 API 키 삭제/비활성화**
   - https://console.cloud.google.com/
   - API 및 서비스 > 사용자 인증 정보
   - 노출된 API 키 선택 > 삭제 또는 제한

2. **새 API 키 발급**
   - Google Cloud Console에서 새 키 생성
   - API 키 제한사항 설정 (가능하면)

3. **코드/문서에서 노출된 키 제거**
   - Git 히스토리에서 제거 (필요시)
   - 문서 파일에서 실제 키 제거

4. **서버에 새 키 업데이트**
   - `.env` 파일에 새 키 설정
   - 애플리케이션 재시작

### 2. 예방 조치

1. **`.gitignore` 확인**
   - `.env` 파일이 포함되어 있는지 확인
   - 모든 환경 변수 파일 패턴 추가

2. **Git pre-commit 훅 설정** (선택사항)
   - API 키 패턴 검사 후 커밋 차단

3. **문서 작성 시 주의**
   - 실제 키 대신 플레이스홀더 사용
   - 예: `GEMINI_API_KEY=your-api-key-here`

---

## 📝 API 키 제한사항 설정 (권장)

Google Cloud Console에서 API 키에 제한사항을 추가할 수 있습니다:

1. **애플리케이션 제한**
   - IP 주소 제한: 서버 IP만 허용
   - HTTP 리퍼러 제한: 특정 도메인만 허용

2. **API 제한**
   - Gemini API만 사용 가능하도록 제한

3. **사용량 할당량 설정**
   - 일일/월간 요청 수 제한

**설정 방법:**
1. Google Cloud Console 접속
2. API 및 서비스 > 사용자 인증 정보
3. API 키 선택
4. "키 제한사항" 섹션에서 설정

---

## ✅ 체크리스트

### 배포 전 확인

- [ ] `.env` 파일이 `.gitignore`에 포함되어 있는가?
- [ ] 문서 파일에 실제 API 키가 없는가?
- [ ] 코드 파일에 하드코딩된 API 키가 없는가?
- [ ] Git 히스토리에 API 키가 노출되지 않았는가?

### 보안 설정

- [ ] 서버 `.env` 파일 권한이 `600`으로 설정되어 있는가?
- [ ] API 키에 제한사항이 설정되어 있는가?
- [ ] 서버에서만 API 키를 사용하는가? (클라이언트에서 직접 사용하지 않음)

---

## 📞 문제 발생 시

API 키 관련 문제가 발생하면:

1. **서버 로그 확인**
   ```bash
   sudo journalctl -u wiseyoung-backend -f | grep -i "gemini\|api key"
   ```

2. **환경 변수 로드 확인**
   ```bash
   # 서버에서 실행
   cat /home/root/wiseyoung-backend/.env | grep GEMINI_API_KEY
   ```

3. **API 키 유효성 확인**
   ```bash
   # 서버에서 실행
   curl "https://generativelanguage.googleapis.com/v1/models/gemini-2.5-flash:generateContent?key=YOUR_API_KEY" \
     -H 'Content-Type: application/json' \
     -d '{"contents":[{"parts":[{"text":"Hello"}]}]}'
   ```

---

## 📚 참고 자료

- [Google Gemini API 문서](https://ai.google.dev/docs)
- [API 키 보안 모범 사례](https://cloud.google.com/docs/authentication/api-keys)
- [Google Cloud Console](https://console.cloud.google.com/)

---

**⚠️ 중요**: API 키를 안전하게 관리하는 것은 개발자의 책임입니다. 노출된 키로 인한 문제가 발생하지 않도록 항상 주의하세요.

