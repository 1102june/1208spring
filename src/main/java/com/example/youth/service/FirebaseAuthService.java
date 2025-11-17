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
            InputStream serviceAccount = resource.getInputStream();

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            // 이미 초기화되어 있지 않은 경우에만 초기화
            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
            }
        } catch (IOException e) {
            throw new RuntimeException("Firebase 초기화 실패: firebase-admin.json 파일을 확인해주세요.", e);
        }
    }

    // Firebase ID Token 검증
    public FirebaseToken verifyToken(String idToken) {
        if (idToken == null || idToken.isEmpty()) {
            throw new IllegalArgumentException("ID Token이 제공되지 않았습니다.");
        }
        
        try {
            return FirebaseAuth.getInstance().verifyIdToken(idToken);
        } catch (FirebaseAuthException e) {
            throw new RuntimeException("Firebase Token 검증 실패: " + e.getMessage(), e);
        }
    }
}