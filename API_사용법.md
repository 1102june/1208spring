# Housing Complex API 사용법

## 방법 1: PowerShell 스크립트 사용 (가장 쉬움)

### 전체 지역 동기화
```powershell
cd C:\Users\USER\IdeaProjects\Youth
powershell -ExecutionPolicy Bypass -File .\sync-housing-complex.ps1
```

### 특정 지역 동기화 (예: 서울 종로구)
```powershell
powershell -ExecutionPolicy Bypass -File .\sync-housing-complex.ps1 -brtcCode "11" -signguCode "110"
```

---

## 방법 2: curl 명령어

### 전체 지역 동기화
```bash
curl -X POST http://localhost:8080/api/housing/complex/sync
```

### 특정 지역 동기화
```bash
curl -X POST "http://localhost:8080/api/housing/complex/sync?brtcCode=11&signguCode=110"
```

---

## 방법 3: Postman 사용

1. Postman 실행
2. 새 요청 생성
3. 설정:
   - **Method**: `POST`
   - **URL**: `http://localhost:8080/api/housing/complex/sync`
   - **Params** 탭에서 (선택사항):
     - `brtcCode`: `11`
     - `signguCode`: `110`
4. **Send** 버튼 클릭

---

## 방법 4: PowerShell 직접 명령어

### 전체 지역 동기화
```powershell
Invoke-WebRequest -Uri "http://localhost:8080/api/housing/complex/sync" -Method POST -ContentType "application/json"
```

### 특정 지역 동기화
```powershell
Invoke-WebRequest -Uri "http://localhost:8080/api/housing/complex/sync?brtcCode=11&signguCode=110" -Method POST -ContentType "application/json"
```

---

## 주요 광역시도 코드

- 서울특별시: `11`
- 부산광역시: `26`
- 대구광역시: `27`
- 인천광역시: `28`
- 광주광역시: `29`
- 대전광역시: `30`
- 울산광역시: `31`
- 경기도: `41`
- 강원도: `42`
- 충청북도: `43`
- 충청남도: `44`
- 전라북도: `45`
- 전라남도: `46`
- 경상북도: `47`
- 경상남도: `48`
- 제주특별자치도: `50`

---

## 참고사항

- 서버가 실행 중이어야 합니다 (`http://localhost:8080`)
- 동기화는 백그라운드에서 실행되므로 시간이 걸릴 수 있습니다
- 서버 로그를 확인하여 진행 상황을 확인하세요



