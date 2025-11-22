package com.example.youth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * 이메일 발송 서비스
 * 
 * 이메일 인증번호 발송을 담당합니다.
 * Gmail SMTP를 기본값으로 사용하며, 네이버/카카오/Gmail 등 모든 SMTP 서버를 지원합니다.
 * 
 * 중요: Gmail SMTP를 사용하면 네이버, 카카오, Outlook 등 모든 이메일 주소로 발송 가능합니다.
 * 
 * 설정 방법:
 * 1. application.yml에 SMTP 설정 추가 (기본값: Gmail - smtp.gmail.com)
 * 2. 환경 변수에 EMAIL_USERNAME, EMAIL_PASSWORD 설정
 * 3. Gmail 사용 시 (권장):
 *    - Google 계정 > 보안 > 2단계 인증 활성화
 *    - 앱 비밀번호 생성 후 EMAIL_PASSWORD에 설정
 *    - ✅ Gmail SMTP는 모든 이메일 주소(네이버, 카카오, Outlook 등)로 발송 가능
 * 4. 네이버/카카오 사용 시:
 *    - 각 메일 서비스에서 SMTP 사용 허용 필요
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnBean(JavaMailSender.class)
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    /**
     * OTP(인증번호) 이메일 발송
     * 
     * @param toEmail 수신자 이메일 주소
     * @param otp 인증번호 (6자리)
     */
    public void sendOtp(String toEmail, String otp) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            
            // 발신자 설정 (설정되지 않은 경우 기본값 사용)
            if (fromEmail != null && !fromEmail.isEmpty()) {
                message.setFrom(fromEmail);
            }
            
            message.setTo(toEmail);
            message.setSubject("[WiseYoung] 이메일 인증번호");
            
            // 이메일 본문 작성
            String emailBody = buildOtpEmailBody(otp);
            message.setText(emailBody);

            // 이메일 발송
            mailSender.send(message);
            
            log.info("인증번호 이메일 발송 성공: {}", toEmail);

        } catch (Exception e) {
            log.error("인증번호 이메일 발송 실패: {}", toEmail, e);
            throw new RuntimeException("이메일 발송에 실패했습니다: " + e.getMessage(), e);
        }
    }

    /**
     * OTP 이메일 본문 생성
     * 
     * @param otp 인증번호
     * @return 이메일 본문 텍스트
     */
    private String buildOtpEmailBody(String otp) {
        return String.format(
            "안녕하세요, WiseYoung입니다.\n\n" +
            "요청하신 이메일 인증번호는 아래와 같습니다.\n\n" +
            "인증번호: %s\n\n" +
            "※ 인증번호는 5분간 유효합니다.\n" +
            "※ 인증번호를 타인에게 알려주지 마세요.\n\n" +
            "감사합니다.",
            otp
        );
    }
}
