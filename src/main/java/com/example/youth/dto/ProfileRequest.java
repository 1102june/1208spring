package com.example.youth.dto;

import lombok.Data;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Data
public class ProfileRequest {
    private String idToken;
    private String birthDate; // "1999-01-01" 형식
    private String nickname; // 닉네임
    private String gender; // "male" or "female"
    private String province; // "강원"
    private String city; // "춘천시"
    private String education; // "대학교 재학"
    private String employment; // "학생"
    private List<String> interests; // ["창업", "취업"]
    private String category; // 안드로이드 호환: 콤마로 구분된 문자열 (예: "창업,취업")
    private String appVersion; // 앱 버전
    private String deviceId; // 디바이스 ID
    private String pushToken; // FCM 푸시 토큰 (선택)
    
    /**
     * interests와 category 중 하나를 사용하여 관심사 목록 반환
     * category가 있으면 콤마로 분리하여 사용, 없으면 interests 사용
     */
    public List<String> getInterestsList() {
        if (category != null && !category.isEmpty()) {
            // category를 콤마로 분리하여 리스트로 변환
            return Arrays.stream(category.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
        }
        return interests != null ? interests : List.of();
    }
}

