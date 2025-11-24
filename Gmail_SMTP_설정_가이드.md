# Gmail SMTP 설정 가이드

## 문제 상황
이메일 인증을 눌러도 이메일이 전송되지 않고 로그에만 출력되는 경우, Gmail SMTP 환경 변수가 설정되지 않았기 때문입니다.

## 해결 방법

### 1단계: Gmail 앱 비밀번호 생성

1. **Google 계정 관리 페이지 접속**
   - https://myaccount.google.com/ 접속
   - 또는 Google 계정 > 보안 메뉴

2. **2단계 인증 활성화** (필수)
   - 보안 > 2단계 인증
   - 2단계 인증이 활성화되어 있어야 앱 비밀번호를 생성할 수 있습니다

3. **앱 비밀번호 생성**
   - 보안 > 2단계 인증 > 앱 비밀번호
   - 앱 선택: **메일**
   - 기기 선택: **기타(맞춤 이름)** → "WiseYoung" 입력
   - **생성** 버튼 클릭
   - **16자리 비밀번호 복사** (예: `abcd efgh ijkl mnop`)

### 2단계: 환경 변수 설정

#### Windows PowerShell (현재 세션만 유지)
```powershell
$env:EMAIL_USERNAME="your-email@gmail.com"
$env:EMAIL_PASSWORD="abcdefghijklmnop"  # 앱 비밀번호 (공백 제거)
```

#### Windows PowerShell (영구 설정)
```powershell
[System.Environment]::SetEnvironmentVariable("EMAIL_USERNAME", "your-email@gmail.com", "User")
[System.Environment]::SetEnvironmentVariable("EMAIL_PASSWORD", "abcdefghijklmnop", "User")
```

#### Windows CMD
```cmd
setx EMAIL_USERNAME "your-email@gmail.com"
setx EMAIL_PASSWORD "abcdefghijklmnop"
```

**주의**: `setx` 사용 후 **새 터미널 창**을 열어야 환경 변수가 적용됩니다.

### 3단계: Spring Boot 서버 재시작

환경 변수를 설정한 후 Spring Boot 서버를 재시작하세요.

### 4단계: 테스트

이메일 인증을 다시 시도하면 이메일이 정상적으로 전송됩니다.

## 확인 방법

서버 시작 시 로그에서 다음 메시지를 확인하세요:

- ✅ **정상**: `✅ Gmail SMTP 설정 완료: your-email@gmail.com`
- ❌ **오류**: `⚠️ Gmail SMTP 환경 변수가 설정되지 않았습니다!`

## 대안: application.yml에 직접 설정 (비권장)

보안상 권장하지 않지만, 테스트 목적으로 `application.yml`에 직접 설정할 수도 있습니다:

```yaml
spring:
  mail:
    username: your-email@gmail.com  # 직접 입력 (비권장)
    password: abcdefghijklmnop       # 직접 입력 (비권장)
```

**주의**: 이 방법은 Git에 커밋하지 마세요! `.gitignore`에 추가하거나 환경 변수를 사용하세요.

## 문제 해결

### 문제 1: "Authentication failed" 오류
- **원인**: 앱 비밀번호가 잘못되었거나 2단계 인증이 비활성화됨
- **해결**: 앱 비밀번호를 다시 생성하고 환경 변수에 정확히 입력

### 문제 2: "Connection timeout" 오류
- **원인**: 방화벽이나 네트워크 문제
- **해결**: 포트 587이 열려있는지 확인

### 문제 3: 환경 변수가 적용되지 않음
- **원인**: `setx` 사용 후 새 터미널을 열지 않음
- **해결**: 터미널을 완전히 종료하고 새로 열기

## 참고

- Gmail SMTP는 모든 이메일 주소(네이버, 카카오, Outlook 등)로 발송 가능합니다
- 앱 비밀번호는 일반 Gmail 비밀번호가 아닙니다
- 앱 비밀번호는 공백 없이 16자리로 입력해야 합니다

