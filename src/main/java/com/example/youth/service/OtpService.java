package com.example.youth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

@Service
@RequiredArgsConstructor
public class OtpService {

    // EmailService가 없어도 동작하도록 선택적 주입
    @Autowired(required = false)
    private EmailService emailService;

    // 임시 저장소 (Redis로 전환 예정)
    private final Map<String, String> otpStore = new HashMap<>();

    /** OTP 발급 */
    public void generateAndSendOtp(String email) {
        String otp = String.format("%06d", new Random().nextInt(999999));

        otpStore.put(email, otp);

        // 5분 후 자동 삭제
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                otpStore.remove(email);
            }
        }, 5 * 60 * 1000);

        // EmailService가 있으면 이메일 발송, 없으면 로그만 출력
        if (emailService != null) {
            emailService.sendOtp(email, otp);
        } else {
            System.out.println("⚠️ EmailService가 설정되지 않았습니다. OTP: " + otp + " (이메일: " + email + ")");
        }
    }

    /** OTP 검증 */
    public boolean verifyOtp(String email, String otp) {
        if (!otpStore.containsKey(email)) return false;
        return otpStore.get(email).equals(otp);
    }
}
