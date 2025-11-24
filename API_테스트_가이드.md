# API 테스트 가이드

## 📌 서버 정보
- **Base URL**: `http://localhost:8080`
- **포트**: 8080
- **데이터베이스**: MariaDB (localhost:3306/wise_young)

---

## 🔐 인증 관련 API (`/auth`)

### 1. 회원가입
```http
POST /auth/register
Content-Type: application/json

{
  "userId": "user123",
  "email": "test@example.com",
  "passwordHash": "hashed_password",
  "emailVerified": false,
  "loginType": "google",
  "osType": "android"
}
```

### 2. 로그인
```http
POST /auth/login
Content-Type: application/json

{
  "idToken": "firebase_id_token_here"
}
```

**응답 예시:**
- 성공: `"LOGIN_SUCCESS"`
- 사용자 없음: `"USER_NOT_FOUND"`
- 이메일 미인증: `"EMAIL_NOT_VERIFIED"`

### 3. 프로필 저장/업데이트
```http
POST /auth/profile
Content-Type: application/json

{
  "idToken": "firebase_id_token_here",
  "appVersion": "1.0.0",
  "deviceId": "device123",
  "profile": {
    "name": "홍길동",
    "birthDate": "1995-01-01",
    "phoneNumber": "010-1234-5678",
    "address": "서울시 강남구"
  }
}
```

### 4. FCM Push Token 저장
```http
POST /auth/push-token
Content-Type: application/json

{
  "idToken": "firebase_id_token_here",
  "pushToken": "fcm_push_token_here"
}
```

---

## 📧 OTP 인증 API (`/auth/otp`)

### 1. 이메일 중복 확인
```http
GET /auth/otp/email/check?email=test@example.com
```

**응답:**
```json
{
  "success": true,
  "data": true  // true: 이미 사용 중, false: 사용 가능
}
```

### 2. 인증번호 발송
```http
POST /auth/otp/send
Content-Type: application/json

{
  "email": "test@example.com"
}
```

**응답:**
```json
{
  "success": true,
  "message": "인증번호가 이메일로 전송되었습니다. 이메일을 확인해주세요."
}
```

### 3. 인증번호 검증
```http
POST /auth/otp/verify
Content-Type: application/json

{
  "email": "test@example.com",
  "otp": "123456"
}
```

**응답:**
```json
{
  "success": true,
  "message": "이메일 인증이 완료되었습니다."
}
```

### 4. 이메일 인증 여부 확인
```http
GET /auth/otp/status?email=test@example.com
```

**응답:**
```json
{
  "success": true,
  "data": true  // true: 인증 완료, false: 미인증
}
```

---

## 🏠 주택 관련 API (`/api/housing`)

### 1. 추천 주택 목록 조회
```http
GET /api/housing/recommended?userId=user123&lat=37.5665&lon=126.9780&radius=5000&limit=10
X-User-Id: user123  # 헤더 또는 쿼리 파라미터로 전달 가능
```

**파라미터:**
- `userId` (필수): 사용자 ID
- `lat` (선택): 위도
- `lon` (선택): 경도
- `radius` (선택): 반경 (미터, 기본값: 5000)
- `limit` (선택): 최대 개수

**응답:**
```json
{
  "success": true,
  "message": "임대주택 추천 목록 조회 성공",
  "data": [
    {
      "housingId": "housing123",
      "name": "LH공사 임대주택",
      "address": "서울시 강남구",
      "distance": 1234.5,
      "deposit": 10000000,
      "monthlyRent": 500000
    }
  ]
}
```

### 2. 주택 상세 조회
```http
GET /api/housing/{housingId}?userId=user123&lat=37.5665&lon=126.9780
X-User-Id: user123
```

**예시:**
```http
GET /api/housing/housing123?userId=user123&lat=37.5665&lon=126.9780
```

### 3. 활성 주택 목록 조회
```http
GET /api/housing/active?userId=user123
X-User-Id: user123
```

---

## 📋 정책 관련 API (`/api/policy`)

### 1. 활성 정책 목록 조회
```http
GET /api/policy/active?userId=user123
```

**응답:**
```json
{
  "success": true,
  "data": [
    {
      "policyId": "policy123",
      "title": "청년 주거 지원 정책",
      "description": "청년을 위한 주거 지원",
      "isBookmarked": false
    }
  ]
}
```

### 2. 전체 정책 목록 조회 (테스트용)
```http
GET /api/policy/all?userId=user123
```

### 3. 정책 상세 조회
```http
GET /api/policy/{policyId}?userId=user123
```

**예시:**
```http
GET /api/policy/policy123?userId=user123
```

---

## 🏠 메인 페이지 API (`/api/main`)

### 메인 페이지 데이터 조회
```http
GET /api/main
X-User-Id: user123
```

**응답:**
```json
{
  "success": true,
  "message": "메인 페이지 데이터 조회 성공",
  "data": {
    "recommendedPolicies": [...],
    "unreadNotificationCount": 5
  }
}
```

---

## 🔧 관리자 API

### 주택 데이터 동기화 (`/api/admin/housing`)

#### 1. 주택 데이터 동기화
```http
POST /api/admin/housing/sync?brtcCode=11&signguCode=110
```

**파라미터:**
- `brtcCode` (선택): 광역시도 코드 (예: 11=서울)
- `signguCode` (선택): 시군구 코드

#### 2. 주택 데이터 매칭 및 저장
```http
POST /api/admin/housing/match-and-save
```

#### 3. 주택 데이터 삭제
```http
DELETE /api/admin/housing/clear
```

#### 4. 동기화 상태 확인
```http
GET /api/admin/housing/stats
```

#### 5. 단지정보 샘플 조회
```http
GET /api/admin/housing/complex/sample?limit=10
```

#### 6. 공고문 샘플 조회
```http
GET /api/admin/housing/notice/sample?limit=10
```

### 정책 데이터 동기화 (`/api/admin/policy`)

#### 1. 정책 데이터 동기화
```http
POST /api/admin/policy/sync
```

#### 2. 잘못된 정책 데이터 삭제
```http
POST /api/admin/policy/cleanup
```

---

## 🧪 테스트 방법

### 1. cURL로 테스트

#### 서버 상태 확인
```bash
curl http://localhost:8080/api/admin/housing/stats
```

#### 주택 목록 조회
```bash
curl "http://localhost:8080/api/housing/active?userId=test-user"
```

#### 정책 목록 조회
```bash
curl "http://localhost:8080/api/policy/active?userId=test-user"
```

#### OTP 발송
```bash
curl -X POST http://localhost:8080/auth/otp/send \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com"}'
```

### 2. Postman으로 테스트

1. **새 Collection 생성**: "Youth API"
2. **Environment 설정**:
   - `base_url`: `http://localhost:8080`
   - `user_id`: `test-user`
3. **각 API 엔드포인트 추가**하여 테스트

### 3. 안드로이드 앱에서 테스트

#### Retrofit 예시
```kotlin
interface ApiService {
    @GET("/api/housing/active")
    suspend fun getActiveHousing(
        @Header("X-User-Id") userId: String,
        @Query("userId") userIdParam: String? = null
    ): Response<ApiResponse<List<HousingResponse>>>
    
    @GET("/api/policy/active")
    suspend fun getActivePolicies(
        @Query("userId") userId: String = "test-user"
    ): Response<ApiResponse<List<PolicyResponse>>>
}

// 사용 예시
val retrofit = Retrofit.Builder()
    .baseUrl("http://10.0.2.2:8080/")  // Android 에뮬레이터용
    // .baseUrl("http://192.168.0.100:8080/")  // 실제 기기용 (PC의 로컬 IP)
    .addConverterFactory(GsonConverterFactory.create())
    .build()

val apiService = retrofit.create(ApiService::class.java)
```

**주의사항:**
- **에뮬레이터**: `http://10.0.2.2:8080` 사용
- **실제 기기**: PC의 로컬 IP 주소 사용 (예: `http://192.168.0.100:8080`)
- **AndroidManifest.xml**에 인터넷 권한 추가:
  ```xml
  <uses-permission android:name="android.permission.INTERNET" />
  ```

---

## 📱 안드로이드 프로젝트 연동 체크리스트

- [ ] 서버 실행 확인 (`http://localhost:8080`)
- [ ] 네트워크 권한 추가 (`AndroidManifest.xml`)
- [ ] Base URL 설정 (에뮬레이터: `10.0.2.2`, 실제 기기: PC IP)
- [ ] Retrofit/OkHttp 의존성 추가
- [ ] API 인터페이스 정의
- [ ] DTO 클래스 생성 (ApiResponse, HousingResponse 등)
- [ ] 에러 처리 구현
- [ ] 로딩 상태 처리

---

## 🔍 디버깅 팁

1. **서버 로그 확인**: Spring Boot 콘솔에서 SQL 쿼리 및 에러 확인
2. **Postman으로 먼저 테스트**: API가 정상 작동하는지 확인
3. **안드로이드 Logcat 확인**: 네트워크 에러 및 응답 확인
4. **서버 상태 확인**: `/api/admin/housing/stats`로 데이터 존재 여부 확인

---

## 📝 참고사항

- 모든 API는 `ApiResponse<T>` 형태로 응답
- 인증이 필요한 API는 `X-User-Id` 헤더 또는 쿼리 파라미터로 `userId` 전달
- Firebase 인증이 필요한 API는 `idToken`을 Body에 포함
- 서버 포트는 `application.yml`에서 변경 가능 (기본값: 8080)

