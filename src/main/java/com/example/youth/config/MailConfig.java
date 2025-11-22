package com.example.youth.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.mail.MailProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

/**
 * 이메일 설정 구성
 * 
 * 환경 변수가 설정되어 있으면 실제 JavaMailSender를 생성하고,
 * 없으면 더미 JavaMailSender를 생성하여 EmailService가 작동하도록 함.
 * (실제 이메일 발송은 실패하지만 콘솔에 인증번호 출력됨)
 */
@Configuration
public class MailConfig {

    @Value("${spring.mail.host:smtp.gmail.com}")
    private String host;

    @Value("${spring.mail.port:587}")
    private int port;

    @Value("${spring.mail.username:}")
    private String username;

    @Value("${spring.mail.password:}")
    private String password;

    /**
     * JavaMailSender 빈 생성
     * 환경 변수가 설정되어 있으면 실제 SMTP 서버에 연결하고,
     * 없으면 더미 JavaMailSender를 생성 (실제 발송은 실패하지만 빈은 생성됨)
     */
    @Bean
    @ConditionalOnMissingBean(JavaMailSender.class)
    public JavaMailSender javaMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(host);
        mailSender.setPort(port);

        // 환경 변수가 설정되어 있으면 실제 인증 정보 사용
        if (username != null && !username.isEmpty() && 
            password != null && !password.isEmpty()) {
            mailSender.setUsername(username);
            mailSender.setPassword(password);
        } else {
            // 환경 변수가 없으면 더미 값 사용 (실제 발송은 실패하지만 빈은 생성됨)
            mailSender.setUsername("dummy@example.com");
            mailSender.setPassword("dummy");
        }

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.connectiontimeout", "5000");
        props.put("mail.smtp.timeout", "5000");
        props.put("mail.smtp.writetimeout", "5000");

        return mailSender;
    }
}

