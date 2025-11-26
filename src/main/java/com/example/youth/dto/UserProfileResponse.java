package com.example.youth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {

    private String userId;
    private String nickname;
    private Integer age;          // 만 나이
    private String region;        // 예: "경기"
    private String education;     // 예: "대학교 재학"
    private String jobStatus;     // 예: "학생", "직장인"
    private List<String> interests; // 관심 카테고리 (일자리, 주거, 복지문화, 교육)
}


