package com.example.youth.service;

import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 비속어 필터링 및 비정상 요청 필터링 서비스
 * 요구사항: CHAT_002 - 비속어 필터링
 */
@Service
public class ProfanityFilterService {

    // 비속어 목록 (실제로는 DB나 파일에서 관리하는 것이 좋습니다)
    private static final List<String> PROFANITY_LIST = Arrays.asList(
        "바보", "멍청이", "미친", "개새끼", "시발", "좆", "병신", "지랄", "개소리",
        "씨발", "좆같", "개같", "미친놈", "병신놈", "지랄하", "개새", "좆만",
        "빠가", "등신", "멍청", "바보같", "개돼지", "씹", "좃", "시바"
    );

    // 비정상 요청 패턴 (SQL 인젝션, 스크립트 인젝션 등)
    private static final List<Pattern> SUSPICIOUS_PATTERNS = Arrays.asList(
        Pattern.compile("(?i)(union|select|insert|update|delete|drop|create|alter|exec|execute)"),
        Pattern.compile("(?i)(<script|javascript:|onerror=|onclick=|onload=)"),
        Pattern.compile("(?i)(eval\\(|document\\.|window\\.|alert\\()"),
        Pattern.compile("(?i)(\\bor\\b|\\band\\b).*=.*="), // SQL 인젝션 패턴
        Pattern.compile("['\";].*--"), // SQL 주석
        Pattern.compile("(?i)(\\bor\\b|\\band\\b).*1.*=.*1") // SQL 인젝션
    );

    // 반복 문자 패턴 (스팸 방지)
    private static final Pattern REPEAT_PATTERN = Pattern.compile("(.)\\1{10,}"); // 같은 문자 10번 이상 반복

    /**
     * 메시지에 비속어가 포함되어 있는지 확인
     * @param message 확인할 메시지
     * @return 비속어 포함 여부
     */
    public boolean containsProfanity(String message) {
        if (message == null || message.isEmpty()) {
            return false;
        }

        String lowerMessage = message.toLowerCase();
        
        for (String profanity : PROFANITY_LIST) {
            if (lowerMessage.contains(profanity.toLowerCase())) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * 비정상 요청인지 확인 (SQL 인젝션, XSS 등)
     * @param message 확인할 메시지
     * @return 비정상 요청 여부
     */
    public boolean isSuspiciousRequest(String message) {
        if (message == null || message.isEmpty()) {
            return false;
        }

        // 반복 문자 체크 (스팸 방지)
        if (REPEAT_PATTERN.matcher(message).find()) {
            return true;
        }

        // 의심스러운 패턴 체크
        for (Pattern pattern : SUSPICIOUS_PATTERNS) {
            if (pattern.matcher(message).find()) {
                return true;
            }
        }

        return false;
    }

    /**
     * 메시지가 비정상적인지 종합적으로 확인
     * @param message 확인할 메시지
     * @return 비정상 여부
     */
    public boolean isInvalidRequest(String message) {
        return containsProfanity(message) || isSuspiciousRequest(message);
    }

    /**
     * 메시지에서 비속어를 마스킹 처리
     * @param message 원본 메시지
     * @return 비속어가 마스킹된 메시지
     */
    public String maskProfanity(String message) {
        if (message == null || message.isEmpty()) {
            return message;
        }

        String result = message;
        for (String profanity : PROFANITY_LIST) {
            result = result.replaceAll("(?i)" + profanity, "***");
        }
        
        return result;
    }
}

