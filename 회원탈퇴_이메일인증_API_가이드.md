# 회원탈퇴 이메일 인증 API 가이드

회원탈퇴 시 이메일 인증을 통한 보안 강화 기능입니다.

## API 엔드포인트

### 1. 회원탈퇴용 인증번호 발송

**POST** `/auth/otp/send/delete-account`

회원탈퇴를 위한 이메일 인증번호를 발송합니다.

#### Request Body
```json
{
  "email": "user@example.com"
}
```

#### Response (성공)
```json
{
  "success": true,
  "message": "인증번호가 이메일로 전송되었습니다. 이메일을 확인해주세요.",
  "data": null
}
```

#### Response (실패 - 등록되지 않은 이메일)
```json
{
  "success": false,
  "message": "등록되지 않은 이메일 주소입니다.",
  "data": null
}
```

#### 특징
- 등록된 이메일만 인증번호 발송 가능
- 회원가입용 OTP API와 달리, 등록된 이메일이어야 함
- 인증번호는 5분간 유효 (설정 변경 가능)

---

### 2. 회원탈퇴 실행

**DELETE** `/auth/account`

이메일 인증번호 검증 후 회원탈퇴를 진행합니다.

#### Request Body
```json
{
  "email": "user@example.com",
  "otp": "123456"
}
```

#### Response (성공)
```json
{
  "success": true,
  "message": "회원탈퇴가 완료되었습니다.",
  "data": null
}
```

#### Response (실패 - 인증번호 불일치)
```json
{
  "success": false,
  "message": "인증번호가 일치하지 않거나 만료되었습니다.",
  "data": null
}
```

#### Response (실패 - 사용자 없음)
```json
{
  "success": false,
  "message": "등록되지 않은 이메일 주소입니다.",
  "data": null
}
```

#### 삭제되는 데이터
- 사용자 정보 (User)
- 사용자 프로필 (UserProfile)
- 관심사 (InterestCategory)
- 북마크 (Bookmark)
- 캘린더 이벤트 (CalendarEvent)
- 사용자 활동 로그 (UserActivity)
- 알림 (Notification)
- AI 추천 (AIRecommendation)

#### 유지되는 데이터
- 임대주택 단지 정보 (housing_complex)
- 임대주택 공고 (housing_notice)
- 청년정책 (policy)

---

## 안드로이드 앱 연동 예시

### 1. 회원탈퇴용 인증번호 발송

```kotlin
// ApiService.kt에 추가
@POST("auth/otp/send/delete-account")
suspend fun sendOtpForDeleteAccount(@Body request: OtpRequest): Response<ApiResponse<String>>

// 사용 예시
suspend fun sendDeleteAccountOtp(email: String) {
    try {
        val response = apiService.sendOtpForDeleteAccount(OtpRequest(email = email))
        if (response.isSuccessful && response.body()?.success == true) {
            // 인증번호 발송 성공
            Toast.makeText(context, "인증번호가 발송되었습니다.", Toast.LENGTH_SHORT).show()
        } else {
            // 발송 실패
            Toast.makeText(context, response.body()?.message ?: "발송 실패", Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        // 오류 처리
    }
}
```

### 2. 회원탈퇴 실행

```kotlin
// ApiService.kt에 추가
@DELETE("auth/account")
suspend fun deleteAccount(@Body request: DeleteAccountRequest): Response<ApiResponse<String>>

// DeleteAccountRequest DTO
data class DeleteAccountRequest(
    val email: String,
    val otp: String
)

// 사용 예시
suspend fun deleteAccount(email: String, otp: String) {
    try {
        val response = apiService.deleteAccount(DeleteAccountRequest(email = email, otp = otp))
        if (response.isSuccessful && response.body()?.success == true) {
            // 회원탈퇴 성공
            // Firebase 로그아웃 및 로그인 화면으로 이동
            FirebaseAuth.getInstance().signOut()
            // 로그인 화면으로 이동
        } else {
            // 탈퇴 실패
            Toast.makeText(context, response.body()?.message ?: "탈퇴 실패", Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        // 오류 처리
    }
}
```

---

## 회원탈퇴 플로우

1. 사용자가 "회원탈퇴" 버튼 클릭
2. 이메일 입력 다이얼로그 표시
3. **POST** `/auth/otp/send/delete-account` 호출하여 인증번호 발송
4. 사용자가 이메일에서 인증번호 확인
5. 인증번호 입력 다이얼로그 표시
6. **DELETE** `/auth/account` 호출하여 회원탈퇴 실행
7. 탈퇴 성공 시 Firebase 로그아웃 및 로그인 화면으로 이동

---

## 주의사항

⚠️ **회원탈퇴는 되돌릴 수 없습니다!**
- 모든 사용자 데이터가 영구적으로 삭제됩니다.
- 단, 정책, 임대주택 단지 정보, 임대주택 공고는 유지됩니다.

---

## 기존 OTP API와의 차이점

| 구분 | 회원가입용 OTP | 회원탈퇴용 OTP |
|------|---------------|---------------|
| 엔드포인트 | `POST /auth/otp/send` | `POST /auth/otp/send/delete-account` |
| 이메일 조건 | 등록되지 않은 이메일 | 등록된 이메일 |
| 검증 API | `POST /auth/otp/verify` | `DELETE /auth/account` (OTP 검증 포함) |

