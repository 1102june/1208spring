# curl 사용법 안내

## Windows PowerShell에서 curl 사용하기

### 방법 1: curl.exe 사용 (권장)

Windows 10/11에는 `curl.exe`가 기본으로 포함되어 있습니다.

#### 전체 지역 Housing Complex 동기화
```bash
curl.exe -X POST http://localhost:8080/api/housing/complex/sync
```

#### Policy 동기화
```bash
curl.exe -X POST http://localhost:8080/api/policy/sync
```

#### Housing Notice 동기화
```bash
curl.exe -X POST http://localhost:8080/api/housing/notice/sync
```

#### 특정 지역 Housing Complex 동기화
```bash
curl.exe -X POST "http://localhost:8080/api/housing/complex/sync?brtcCode=11&signguCode=110"
```

**주의**: URL에 `&` 문자가 있으면 큰따옴표로 감싸야 합니다.

---

### 방법 2: PowerShell의 Invoke-WebRequest (별칭: curl)

PowerShell에서 `curl`을 입력하면 실제로는 `Invoke-WebRequest`가 실행됩니다.

#### 전체 지역 Housing Complex 동기화
```powershell
Invoke-WebRequest -Uri "http://localhost:8080/api/housing/complex/sync" -Method POST -ContentType "application/json"
```

#### 간단한 버전 (별칭 사용)
```powershell
curl -Uri "http://localhost:8080/api/housing/complex/sync" -Method POST
```

---

## 실행 순서

1. **PowerShell 열기**
   - Windows 키 + X → "Windows PowerShell" 또는 "터미널" 선택

2. **프로젝트 폴더로 이동**
   ```powershell
   cd C:\Users\USER\IdeaProjects\Youth
   ```

3. **서버가 실행 중인지 확인**
   - Spring Boot 서버가 `http://localhost:8080`에서 실행 중이어야 합니다

4. **curl 명령어 실행**
   ```bash
   curl.exe -X POST http://localhost:8080/api/housing/complex/sync
   ```

---

## 응답 확인

성공하면 다음과 같은 JSON 응답이 표시됩니다:
```json
{
  "success": true,
  "message": "전체 지역 housing_complex 데이터 동기화가 시작되었습니다. (백그라운드에서 실행 중)",
  "data": null
}
```

---

## 문제 해결

### "curl을 찾을 수 없습니다" 오류
- `curl.exe`로 명시적으로 실행하세요
- 또는 PowerShell의 `Invoke-WebRequest`를 사용하세요

### "서버에 연결할 수 없습니다" 오류
- Spring Boot 서버가 실행 중인지 확인하세요
- `http://localhost:8080`이 접근 가능한지 확인하세요

### "timeout" 오류
- 서버가 응답하는 데 시간이 걸릴 수 있습니다
- 서버 로그를 확인하여 진행 상황을 확인하세요










