package com.example.youth.service;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.auth.FirebaseAuthException;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;

@Service
public class FirebaseAuthService {

    @PostConstruct
    public void initialize() {
        try {
            // 클래스패스에서 리소스 로드 (JAR 패키징 시에도 동작)
            ClassPathResource resource = new ClassPathResource("firebase-admin.json");
            
            // 파일이 존재하는지 확인
            if (!resource.exists()) {
                System.out.println("⚠️ Firebase 설정 파일(firebase-admin.json)이 없습니다. Firebase 기능이 비활성화됩니다.");
                return;
            }
            
            InputStream serviceAccount = resource.getInputStream();

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            // 이미 초기화되어 있지 않은 경우에만 초기화
            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
                System.out.println("✅ Firebase 초기화 완료");
            }
        } catch (IOException e) {
            System.err.println("⚠️ Firebase 초기화 실패: " + e.getMessage() + " - Firebase 기능이 비활성화됩니다.");
            // Firebase가 없어도 애플리케이션이 실행되도록 예외를 던지지 않음
        }
    }

    // Firebase ID Token 검증
    public FirebaseToken verifyToken(String idToken) {
        if (idToken == null || idToken.isEmpty()) {
            throw new IllegalArgumentException("ID Token이 제공되지 않았습니다.");
        }
        
        // Firebase가 초기화되지 않은 경우
        if (FirebaseApp.getApps().isEmpty()) {
            throw new RuntimeException("Firebase가 초기화되지 않았습니다. firebase-admin.json 파일을 확인해주세요.");
        }
        
        try {
            return FirebaseAuth.getInstance().verifyIdToken(idToken);
        } catch (FirebaseAuthException e) {
            throw new RuntimeException("Firebase Token 검증 실패: " + e.getMessage(), e);
        }
    }
}