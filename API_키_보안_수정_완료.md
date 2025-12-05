# API 키 보안 수정 완료

## ✅ 수정 완료 사항

### 1. 새로운 API 키 암호화
- **새 API 키**: `AIzaSyA8zK9e_qwLinryntXYmc37q4UWBhuAt98`
- **암호화된 키**: `9HZFLOrxj42v41LUObdcdbe9tN7NkhSZYESgpXDyCyfLDft85vfS7+flo7ylq3KC`
- **암호화 키**: `TksJPfalBU2+pIU/A87y1nlswEHECggq5hS6JOOGZ2M=`

### 2. 수정된 파일
- ✅ `run-local.ps1` - 새로운 암호화된 API 키로 업데이트
- ✅ `application.yml` - 하드코딩된 API 키 제거 (환경 변수 필수)
- ✅ `로컬_서버_실행_가이드.md` - 하드코딩된 키 제거, 예시로만 표시
- ✅ `로컬_환경변수_설정_수동.ps1` - 하드코딩된 키 제거, 예시로만 표시
- ✅ `.gitignore` - 보안 주의사항 추가

### 3. 보안 개선 사항
- `application.yml`에서 기본값 제거 (환경 변수 필수)
- 모든 문서에서 실제 키 대신 예시만 표시
- `.gitignore`에 보안 주의사항 추가

## ⚠️ 추가 작업 필요

다음 파일들에서도 하드코딩된 API 키가 있을 수 있습니다. 확인 후 제거하세요:

- `로컬_서버_실행_간단_방법_최종.md`
- `서버_업데이트_필요_사항.md`
- `로컬_암호화_재실행.md`
- `로컬_새_암호화_키.md`
- `로컬_서버_실행_간단_방법.md`
- `로컬_개발_환경_설정.md`
- `서버_배포_준비_완료.md`
- 기타 마크다운 파일들

## 📝 사용 방법

### 로컬 개발 환경
```powershell
# run-local.ps1 스크립트 사용 (권장)
.\run-local.ps1
```

### 수동 설정
```powershell
$env:ENCRYPTION_KEY = 'TksJPfalBU2+pIU/A87y1nlswEHECggq5hS6JOOGZ2M='
$env:GEMINI_API_KEY = '9HZFLOrxj42v41LUObdcdbe9tN7NkhSZYESgpXDyCyfLDft85vfS7+flo7ylq3KC'
.\gradlew bootRun
```

## 🔒 보안 권장사항

1. **절대 Git에 커밋하지 마세요**
   - `run-local.ps1`에 실제 키를 하드코딩하지 마세요
   - 마크다운 파일에 실제 키를 포함하지 마세요

2. **환경 변수 사용**
   - 로컬: `.env` 파일 사용 (`.gitignore`에 포함)
   - 서버: 시스템 환경 변수 또는 `.env` 파일 사용

3. **키 교체 시**
   - 새로운 키를 암호화하여 사용
   - 모든 환경(로컬, 서버)에서 동시에 업데이트

## 📌 참고

- 암호화 도구: `ApiKeyCryptoTool.java`
- 암호화 스크립트: `API_키_암호화_스크립트.ps1`

