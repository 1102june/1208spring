package com.example.youth.dto;

import lombok.Data;
import java.util.List;

@Data
public class ProfileRequest {
    private String idToken;
    private String birthDate; // "1999-01-01" 형식
    private String gender; // "male" or "female"
    private String province; // "강원"
    private String city; // "춘천시"
    private String education; // "대학교 재학"
    private String employment; // "학생"
    private List<String> interests; // ["창업", "취업"]
    private String appVersion; // 앱 버전
    private String deviceId; // 디바이스 ID
}

