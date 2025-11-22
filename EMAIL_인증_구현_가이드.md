# 이메일 인증 구현 가이드

## 개요

이 문서는 WiseYoung 프로젝트의 이메일 인증(OTP) 구현 방식을 설명합니다.

## 구현 방식

### 1. 전체 플로우

```
[사용자] 
  ↓
[이메일 입력] 
  ↓
[인증번호 발송 버튼 클릭]
  ↓
[Android 앱] → POST /auth/otp/send → [Spring Boot 서버]
  ↓
[서버] 인증번호 생성 (6자리 랜덤)
  ↓
[서버] 이메일 발송 (Gmail SMTP)
  ↓
[사용자 이메일함]에서 인증번호 확인
  ↓
[사용자] 인증번호 입력 → [확인 버튼 클릭]
  ↓
[Android 앱] → POST /auth/otp/verify → [Spring Boot 서버]
  ↓
[서버] 인증번호 검증 (일치 여부, 만료 시간 확인)
  ↓
[검증 성공] → 이메일 인증 완료
```

### 2. 기술 스택

- **Spring Boot Mail**: Gmail SMTP를 통한 이메일 발송
- **In-Memory Storage**: 인증번호 임시 저장 (HashMap)
  - 추후 Redis로 전환 가능
- **ScheduledExecutorService**: 만료된 인증번호 자동 삭제

### 3. 주요 컴포넌트

#### 3.1 OtpService

**역할**: 인증번호 생성, 저장, 검증, 만료 관리

**주요 메서드**:
- `generateAndSendOtp(String email)`: 인증번호 생성 및 이메일 발송
- `verifyOtp(String email, String otp)`: 인증번호 검증
- `hasOtp(String email)`: 인증번호 존재 여부 확인

**특징**:
- 6자리 랜덤 인증번호 생성
- 만료 시간: 5분 (설정 가능)
- 검증 성공 시 인증번호 자동 삭제 (재사용 방지)
- 만료된 인증번호 자동 삭제

#### 3.2 OtpController

**API 엔드포인트**:
- `POST /auth/otp/send`: 인증번호 발송
- `POST /auth/otp/verify`: 인증번호 검증
- `GET /auth/otp/status`: 이메일 인증 여부 확인

**요청 형식**:
```json
// POST /auth/otp/send
{
  "email": "user@example.com"
}

// POST /auth/otp/verify
{
  "email": "user@example.com",
  "otp": "123456"
}
```

**응답 형식**:
```json
{
  "success": true,
  "message": "인증번호가 이메일로 전송되었습니다.",
  "data": null
}
```

#### 3.3 EmailService

**역할**: Gmail SMTP를 통한 이메일 발송

**설정 필요**:
- Gmail 계정 설정
- 앱 비밀번호 생성 (2단계 인증 필수)

## 설정 방법

### 1. Gmail SMTP 설정

#### 1.1 Gmail 앱 비밀번호 생성

1. Google 계정 관리 → 보안
2. 2단계 인증 활성화
3. 앱 비밀번호 생성
   - 앱 선택: 메일
   - 기기 선택: 기타(맞춤 이름)
   - 생성된 16자리 비밀번호 복사

#### 1.2 application.yml 설정

```yaml
spring:
  mail:
    host: smtp.gmail.com
    port: 587
    username: ${EMAIL_USERNAME:your-email@gmail.com}  # Gmail 주소
    password: ${EMAIL_PASSWORD:your-app-password}      # 앱 비밀번호
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true
```

#### 1.3 환경 변수 설정 (선택)

```bash
# Windows (PowerShell)
$env:EMAIL_USERNAME="your-email@gmail.com"
$env:EMAIL_PASSWORD="your-app-password"

# Linux/Mac
export EMAIL_USERNAME="your-email@gmail.com"
export EMAIL_PASSWORD="your-app-password"
```

### 2. OTP 설정 (선택)

`application.yml`에서 OTP 만료 시간 및 자릿수 설정:

```yaml
otp:
  expiry-minutes: 5  # 만료 시간 (분)
  length: 6          # 인증번호 자릿수
```

## 보안 고려사항

### 1. 인증번호 보안

- ✅ 랜덤 6자리 숫자 생성
- ✅ 만료 시간 설정 (5분)
- ✅ 검증 성공 시 즉시 삭제 (재사용 방지)
- ✅ 동일 이메일 재발송 시 기존 인증번호 무효화

### 2. 보안 개선 방안

- 🔄 **Redis 사용**: In-Memory 대신 Redis로 전환하여 서버 재시작 시에도 유지
- 🔄 **Rate Limiting**: 동일 IP/이메일당 발송 횟수 제한
- 🔄 **IP 기반 제한**: 의심스러운 IP에서의 요청 차단
- 🔄 **이메일 중복 확인**: 회원가입 시 이미 존재하는 이메일인지 확인

## 사용 예시

### 1. Android 앱에서 인증번호 발송

```kotlin
fun sendOtpToServer(email: String, context: Context) {
    val client = OkHttpClient()
    val json = """{"email":"$email"}"""
    val body = RequestBody.create("application/json".toMediaType(), json)
    
    val request = Request.Builder()
        .url(Config.getUrl(Config.Api.OTP_SEND))
        .post(body)
        .build()
    
    client.newCall(request).enqueue(object : Callback {
        override fun onResponse(call: Call, response: Response) {
            // 성공 처리
        }
        override fun onFailure(call: Call, e: IOException) {
            // 실패 처리
        }
    })
}
```

### 2. Android 앱에서 인증번호 검증

```kotlin
fun verifyOtpWithServer(email: String, otp: String, callback: (Boolean) -> Unit) {
    val client = OkHttpClient()
    val json = """{"email":"$email","otp":"$otp"}"""
    val body = RequestBody.create("application/json".toMediaType(), json)
    
    val request = Request.Builder()
        .url(Config.getUrl(Config.Api.OTP_VERIFY))
        .post(body)
        .build()
    
    client.newCall(request).enqueue(object : Callback {
        override fun onResponse(call: Call, response: Response) {
            callback(response.isSuccessful)
        }
        override fun onFailure(call: Call, e: IOException) {
            callback(false)
        }
    })
}
```

## 트러블슈팅

### 1. 이메일이 발송되지 않는 경우

**문제**: `EmailService가 설정되지 않았습니다` 메시지

**해결 방법**:
1. `application.yml`에 `spring.mail` 설정 확인
2. `EMAIL_USERNAME`, `EMAIL_PASSWORD` 환경 변수 설정 확인
3. Gmail 앱 비밀번호 정확성 확인
4. Gmail 2단계 인증 활성화 확인

### 2. 인증번호 검증 실패

**원인**:
- 만료된 인증번호 (5분 초과)
- 잘못된 인증번호 입력
- 이메일 주소 불일치 (대소문자 구분)

**해결 방법**:
- 인증번호 재발송
- 이메일 주소 정확히 입력 (소문자 변환 권장)

### 3. 이메일 발송 실패 오류

**문제**: `Authentication failed`

**해결 방법**:
1. Gmail 앱 비밀번호 재생성
2. `application.yml`의 `username`, `password` 확인
3. Gmail 계정 보안 설정 확인

## 향후 개선 사항

1. **Redis 연동**: 인증번호 저장을 Redis로 전환
2. **Rate Limiting**: 발송 횟수 제한 추가
3. **HTML 이메일**: 텍스트 대신 HTML 형식 이메일 지원
4. **다중 언어**: 이메일 내용 다국어 지원
5. **로깅 강화**: 발송/검증 로그 상세 기록

## 관련 파일

- `OtpController.java`: API 엔드포인트
- `OtpService.java`: 비즈니스 로직
- `EmailService.java`: 이메일 발송
- `application.yml`: 설정 파일
- `OtpRequest.java`: 요청 DTO

