package com.example.youth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

@Service
@RequiredArgsConstructor
public class OtpService {

    private final EmailService emailService;

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

        emailService.sendOtp(email, otp);
    }

    /** OTP 검증 */
    public boolean verifyOtp(String email, String otp) {
        if (!otpStore.containsKey(email)) return false;
        return otpStore.get(email).equals(otp);
    }
}
