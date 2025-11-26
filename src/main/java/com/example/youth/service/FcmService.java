package com.example.youth.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FcmService {

    @Autowired
    private UserService userService;

    /**
     * 단일 기기에 FCM 알림 발송
     * @param fcmToken FCM 등록 토큰
     * @param title 알림 제목
     * @param body 알림 내용
     * @return 성공 여부
     */
    public boolean sendNotification(String fcmToken, String title, String body) {
        try {
            Message message = Message.builder()
                    .setToken(fcmToken)
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .build();

            String response = FirebaseMessaging.getInstance().send(message);
            System.out.println("FCM 알림 발송 성공: " + response);
            return true;
        } catch (FirebaseMessagingException e) {
            System.err.println("FCM 알림 발송 실패: " + e.getMessage());
            return false;
        }
    }

    /**
     * 특정 사용자에게 FCM 알림 발송
     * @param userId 사용자 ID (Firebase UID)
     * @param title 알림 제목
     * @param body 알림 내용
     * @return 성공 여부
     */
    public boolean sendNotificationToUser(String userId, String title, String body) {
        String fcmToken = userService.getFcmTokenByUserId(userId);
        if (fcmToken == null || fcmToken.isEmpty()) {
            System.err.println("사용자 FCM 토큰을 찾을 수 없습니다: " + userId);
            return false;
        }
        return sendNotification(fcmToken, title, body);
    }

    /**
     * 여러 기기에 FCM 알림 발송 (멀티캐스트)
     * @param fcmTokens FCM 등록 토큰 리스트
     * @param title 알림 제목
     * @param body 알림 내용
     * @return 성공한 발송 수
     */
    public int sendNotificationToMultipleDevices(List<String> fcmTokens, String title, String body) {
        int successCount = 0;
        for (String token : fcmTokens) {
            if (sendNotification(token, title, body)) {
                successCount++;
            }
        }
        return successCount;
    }
}

