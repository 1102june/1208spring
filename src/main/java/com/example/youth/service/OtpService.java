package com.example.youth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * OTP(One-Time Password) 서비스
 * 
 * 이메일 인증번호 발송 및 검증을 담당합니다.
 * 
 * 동작 방식:
 * 1. generateAndSendOtp(): 6자리 인증번호 생성 후 이메일로 발송, 만료 시간 저장
 * 2. verifyOtp(): 입력된 인증번호 검증 (일치 여부 및 만료 시간 확인)
 * 3. 검증 성공 시 인증번호 자동 삭제 (재사용 방지)
 * 4. 만료된 인증번호는 자동으로 삭제됨
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OtpService {

    // EmailService가 없어도 동작하도록 선택적 주입
    @Autowired(required = false)
    private EmailService emailService;

    // 인증번호 저장소 (email -> OtpData)
    private final Map<String, OtpData> otpStore = new ConcurrentHashMap<>();

    // 만료 시간 관리용 스케줄러
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    // 설정값 (기본값: 5분, 6자리)
    @Value("${otp.expiry-minutes:5}")
    private int expiryMinutes;

    @Value("${otp.length:6}")
    private int otpLength;

    /**
     * OTP 데이터 클래스 (인증번호와 만료 시간 저장)
     */
    private static class OtpData {
        String otp;
        LocalDateTime expiresAt;

        OtpData(String otp, LocalDateTime expiresAt) {
            this.otp = otp;
            this.expiresAt = expiresAt;
        }

        boolean isExpired() {
            return LocalDateTime.now().isAfter(expiresAt);
        }
    }

    /**
     * 인증번호 생성 및 이메일 발송
     * 
     * @param email 사용자 이메일 주소
     * @throws RuntimeException 이메일 발송 실패 시
     */
    public void generateAndSendOtp(String email) {
        // 이메일 형식 검증 (간단한 검증)
        if (email == null || !email.contains("@")) {
            throw new IllegalArgumentException("유효하지 않은 이메일 주소입니다.");
        }

        // 6자리 랜덤 인증번호 생성
        String otp = generateOtp();
        
        // 만료 시간 설정 (현재 시간 + 설정된 만료 시간)
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(expiryMinutes);

        // 기존 인증번호가 있으면 삭제
        otpStore.remove(email);

        // 새 인증번호 저장
        otpStore.put(email, new OtpData(otp, expiresAt));

        // 만료 시간 후 자동 삭제 스케줄링
        scheduler.schedule(() -> {
            OtpData data = otpStore.get(email);
            if (data != null && data.isExpired()) {
                otpStore.remove(email);
            }
        }, expiryMinutes, TimeUnit.MINUTES);

        // 개발 환경에서 항상 콘솔에 인증번호 출력 (이메일 발송 성공/실패 여부와 관계없이)
        System.err.println("==========================================");
        System.err.println("🔐 인증번호 생성 완료");
        System.err.println("==========================================");
        System.err.println("📧 수신 이메일: " + email);
        System.err.println("🔑 인증번호: " + otp);
        System.err.println("⏰ 만료 시간: " + expiresAt);
        System.err.println("==========================================");
        
        // 이메일 발송
        try {
            if (emailService != null) {
                try {
                    emailService.sendOtp(email, otp);
                    log.info("✅ 인증번호 이메일 발송 성공: {} (OTP: {})", email, otp);
                } catch (Exception e) {
                    // 이메일 발송 실패 시에도 인증번호는 콘솔에 출력되었으므로 계속 진행
                    log.error("❌ 이메일 발송 실패: {} (OTP: {}). 콘솔에 인증번호가 출력되었습니다.", email, otp, e);
                    log.error("⚠️ 이메일 발송 오류: {}", e.getMessage());
                    System.err.println("⚠️ 이메일 발송 실패: " + e.getMessage());
                    System.err.println("   → 콘솔에 출력된 인증번호를 사용하세요");
                    System.err.println("==========================================");
                    // 개발 환경에서는 이메일 발송 실패해도 계속 진행 (콘솔 출력으로 대체)
                    log.warn("개발 환경: 이메일 발송 실패했지만 인증번호는 콘솔에 출력되었으므로 계속 진행합니다.");
                }
            } else {
                // EmailService가 없는 경우: 콘솔에만 출력
                log.warn("⚠️ EmailService가 설정되지 않았습니다. 인증번호는 콘솔에만 출력됩니다.");
                System.err.println("⚠️ EmailService 미설정 - 이메일 발송 불가");
                System.err.println("==========================================");
                System.err.println("📧 이메일 발송을 위해 환경 변수 설정:");
                System.err.println("   EMAIL_USERNAME=your-email@gmail.com");
                System.err.println("   EMAIL_PASSWORD=your-app-password");
                System.err.println("==========================================");
            }
        } catch (Exception e) {
            // 예상치 못한 오류 발생 시에만 예외 throw
            otpStore.remove(email); // 예상치 못한 오류 시 저장된 인증번호 삭제
            log.error("인증번호 발송 중 예상치 못한 오류 발생: {} (OTP: {})", email, otp, e);
            System.err.println("❌ 치명적 오류 발생: " + e.getMessage());
            throw new RuntimeException("인증번호 발송에 실패했습니다: " + e.getMessage(), e);
        }
    }

    /**
     * 인증번호 검증
     * 
     * @param email 사용자 이메일 주소
     * @param otp 입력된 인증번호
     * @return 검증 성공 여부
     */
    public boolean verifyOtp(String email, String otp) {
        // 저장된 인증번호가 없는 경우
        OtpData data = otpStore.get(email);
        if (data == null) {
            return false;
        }

        // 만료된 인증번호인 경우
        if (data.isExpired()) {
            otpStore.remove(email);
            return false;
        }

        // 인증번호 일치 확인
        boolean isValid = data.otp.equals(otp);

        // 검증 성공 시 인증번호 삭제 (재사용 방지)
        if (isValid) {
            otpStore.remove(email);
        }

        return isValid;
    }

    /**
     * 랜덤 인증번호 생성
     * 
     * @return 설정된 자릿수의 인증번호 (예: 6자리 -> "123456")
     */
    private String generateOtp() {
        int min = (int) Math.pow(10, otpLength - 1);
        int max = (int) Math.pow(10, otpLength) - 1;
        int otp = new Random().nextInt(max - min + 1) + min;
        return String.format("%0" + otpLength + "d", otp);
    }

    /**
     * 특정 이메일의 인증번호가 존재하는지 확인 (선택적 기능)
     * 
     * @param email 사용자 이메일 주소
     * @return 인증번호 존재 여부
     */
    public boolean hasOtp(String email) {
        OtpData data = otpStore.get(email);
        return data != null && !data.isExpired();
    }
}
