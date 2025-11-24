package com.example.youth.service;

import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

/**
 * 비속어 필터링 서비스
 * 요구사항: CHAT_002 - 비속어 필터링
 */
@Service
public class ProfanityFilterService {

    // 비속어 목록 (실제로는 DB나 파일에서 관리하는 것이 좋습니다)
    private static final List<String> PROFANITY_LIST = Arrays.asList(
        "바보", "멍청이", "미친", "개새끼", "시발", "좆", "병신", "지랄", "개소리"
        // 추가 비속어는 여기에 추가
    );

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

