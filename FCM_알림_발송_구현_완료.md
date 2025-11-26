# FCM 알림 발송 구현 완료

## ✅ 구현된 파일

1. **FcmService.java** (신규)
   - 위치: `src/main/java/com/example/youth/service/FcmService.java`
   - 기능: FCM 알림 발송 서비스

2. **UserService.java** (수정)
   - `saveFcmToken()` - FCM 토큰 저장
   - `getFcmTokenByUserId()` - FCM 토큰 조회

3. **UserController.java** (수정)
   - `/auth/push-token` - FCM 토큰 저장 (기존)
   - `/auth/test-notification` - 테스트 알림 발송 (신규)
   - `/auth/test-notification-by-token` - 토큰으로 직접 알림 발송 (신규)

## 🚀 사용 방법

### 1. FCM 토큰으로 직접 테스트
```bash
POST /auth/test-notification-by-token
{
  "fcmToken": "FCM_등록_토큰",
  "title": "테스트 알림",
  "body": "알림 내용"
}
```

### 2. 사용자 ID로 테스트
```bash
POST /auth/test-notification
{
  "idToken": "Firebase_ID_Token",
  "title": "테스트 알림",
  "body": "알림 내용"
}
```

## 📝 참고

- Firebase Admin SDK는 이미 설정되어 있음 (`firebase-admin.json`)
- FCM V1 API를 사용하여 알림 발송
- Firebase 콘솔 UI 없이도 서버에서 직접 알림 테스트 가능

