# Gemini API 키 보안 및 사용량 제한 해결 가이드

## 문제 상황

챗봇 기능을 3번 정도 사용하면 "API key denied" 또는 "permission denied" 오류가 발생합니다.

## 원인 분석

### 1. API 키 사용량 제한 (가장 가능성 높음)
- Gemini API는 무료 플랜에서 **일일/분당 요청 수 제한**이 있습니다
- 여러 사용자가 같은 API 키를 공유해서 사용하면 빠르게 제한에 걸릴 수 있습니다
- 제한에 걸리면 403 또는 429 오류가 발생합니다

### 2. API 키가 공개적으로 노출됨
- Gemini API는 API 키가 클라이언트 측(안드로이드 앱)에서 사용되는 것을 감지하면 자동으로 차단합니다
- 현재 구조는 올바르지만, 다음 경우에 노출될 수 있습니다:
  - 서버 로그에 API 키가 출력되는 경우
  - 에러 메시지에 API 키가 포함되는 경우
  - GitHub 등에 API 키가 커밋된 경우

### 3. API 키 제한 설정
- Gemini API 콘솔에서 API 키에 대한 제한 설정(예: 특정 IP만 허용)이 있을 수 있습니다

## 해결 방법

### 방법 1: API 키 사용량 모니터링 및 제한 설정 (권장)

1. **Gemini API 콘솔 확인**
   - https://aistudio.google.com/app/apikey 접속
   - 현재 사용 중인 API 키의 사용량 확인
   - 일일/분당 요청 수 제한 확인

2. **Rate Limiting 구현**
   - 서버 측에서 요청 수를 제한하여 API 키 사용량을 관리
   - 예: 사용자당 분당 5회, 일일 50회 제한

3. **API 키 로테이션**
   - 여러 API 키를 발급받아 로테이션 사용
   - 사용량이 많은 경우 유료 플랜으로 업그레이드 고려

### 방법 2: API 키 노출 확인 및 방지

1. **서버 로그 확인**
   ```bash
   # 서버 로그에서 API 키가 노출되는지 확인
   grep -r "AIza" logs/
   ```

2. **에러 메시지 확인**
   - `GeminiService.java`의 에러 처리 부분에서 API 키가 로그에 출력되지 않도록 확인
   - 현재 코드는 이미 API 키를 마스킹하고 있음 (10자리만 표시)

3. **환경 변수 확인**
   - API 키가 환경 변수로만 관리되고 있는지 확인
   - `.env` 파일이나 설정 파일에 하드코딩되어 있지 않은지 확인

### 방법 3: 새로운 API 키 발급

1. **새로운 API 키 발급**
   - https://aistudio.google.com/app/apikey 접속
   - 새로운 API 키 생성
   - 기존 API 키는 삭제 또는 비활성화

2. **API 키 암호화 및 설정**
   ```bash
   # 새로운 API 키 암호화
   # CryptoUtil을 사용하여 암호화
   ```

3. **환경 변수 업데이트**
   ```bash
   # Windows PowerShell
   $env:GEMINI_API_KEY="암호화된_새로운_API_키"
   ```

### 방법 4: Rate Limiting 구현 (서버 측)

서버 측에서 요청 수를 제한하여 API 키 사용량을 관리할 수 있습니다.

```java
// RateLimiter를 사용한 요청 제한 예시
private final RateLimiter rateLimiter = RateLimiter.create(10.0); // 초당 10회

public Mono<ChatResponse> generateChatResponse(String message, String userId) {
    if (!rateLimiter.tryAcquire()) {
        return Mono.just(ChatResponse.builder()
            .response("요청이 너무 많습니다. 잠시 후 다시 시도해주세요.")
            .actionLinks(emptyList())
            .build());
    }
    // ... 기존 로직
}
```

## 확인 사항 체크리스트

- [ ] Gemini API 콘솔에서 현재 API 키의 사용량 확인
- [ ] 서버 로그에서 API 키가 노출되는지 확인
- [ ] 에러 메시지에 API 키가 포함되는지 확인
- [ ] GitHub 등에 API 키가 커밋되어 있는지 확인
- [ ] API 키 제한 설정 확인 (IP 제한 등)
- [ ] Rate Limiting 구현 여부 확인

## 예방 조치

1. **API 키는 항상 서버 측에서만 사용**
   - 안드로이드 앱에서는 절대 API 키를 사용하지 않음
   - 모든 API 호출은 서버를 통해 이루어짐

2. **API 키 암호화**
   - API 키는 암호화하여 저장
   - 환경 변수로만 관리

3. **로깅 주의**
   - API 키를 로그에 출력하지 않음
   - 에러 메시지에도 API 키가 포함되지 않도록 주의

4. **사용량 모니터링**
   - 정기적으로 API 사용량 확인
   - 제한에 가까워지면 알림 설정

## 참고 자료

- Gemini API 문서: https://ai.google.dev/docs
- API 키 관리: https://aistudio.google.com/app/apikey
- Rate Limiting 가이드: https://ai.google.dev/pricing

