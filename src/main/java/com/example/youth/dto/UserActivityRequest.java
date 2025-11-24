package com.example.youth.dto;

import com.example.youth.DB.UserActivity.ActivityType;
import com.example.youth.common.ContentType;
import lombok.Data;

@Data
public class UserActivityRequest {
    private ActivityType activityType;  // CLICK, VIEW, SEARCH, BOOKMARK 등
    private ContentType contentType;    // policy, housing (선택)
    private String contentId;           // 정책 ID 또는 주택 ID (선택)
    private String searchKeyword;       // 검색 키워드 (검색 활동인 경우)
    private String metadata;            // 추가 메타데이터 (JSON 형식, 선택)
}

