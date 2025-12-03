# 🔐 API 키 암호화 - 간단한 방법

## 방법 1: Java 클래스 직접 실행 (권장)

### 1. 프로젝트 빌드

```powershell
cd C:\WiseYoung_backend
.\gradlew build -x test
```

### 2. 암호화 키 생성

```powershell
# 빌드된 클래스 사용
java -cp "build/classes/java/main" com.example.youth.util.ApiKeyCryptoTool generate
```

출력된 키를 복사하세요 (예: `abcd1234...`)

### 3. API 키 암호화

```powershell
# 새 암호화 키로 API 키 암호화
java -cp "build/classes/java/main" com.example.youth.util.ApiKeyCryptoTool encrypt "AIzaSyC2SzM_GRKI0pr1AT4PkT7ID29hIM3hFhw" "생성된_암호화_키"
```

### 4. 결과 확인

- 암호화 키: `ENCRYPTION_KEY` (2단계에서 생성한 키)
- 암호화된 API 키: `GEMINI_API_KEY` (3단계에서 생성한 암호화된 문자열)

---

## 방법 2: PowerShell 스크립트 사용

```powershell
.\API_키_암호화_스크립트.ps1 -PlainApiKey "AIzaSyC2SzM_GRKI0pr1AT4PkT7ID29hIM3hFhw"
```

---

## 방법 3: 수동 암호화 (테스트용)

복호화 테스트:

```powershell
java -cp "build/classes/java/main" com.example.youth.util.ApiKeyCryptoTool decrypt "암호화된_키" "암호화_키"
```

원본 API 키가 출력되면 성공입니다.

---

## 서버 설정

생성된 두 값을 서버 `.env` 파일에 설정:

```bash
ENCRYPTION_KEY=생성된_암호화_키
GEMINI_API_KEY=암호화된_API_키
```

---

## 전체 예제

```powershell
# 1. 빌드
.\gradlew build -x test

# 2. 암호화 키 생성 (출력값 복사)
$encKey = java -cp "build/classes/java/main" com.example.youth.util.ApiKeyCryptoTool generate
Write-Host "암호화 키: $encKey"

# 3. API 키 암호화 (출력값 복사)
$encrypted = java -cp "build/classes/java/main" com.example.youth.util.ApiKeyCryptoTool encrypt "AIzaSyC2SzM_GRKI0pr1AT4PkT7ID29hIM3hFhw" $encKey
Write-Host "암호화된 키: $encrypted"

# 4. 서버 .env 파일에 설정
# ENCRYPTION_KEY=$encKey
# GEMINI_API_KEY=$encrypted
```

