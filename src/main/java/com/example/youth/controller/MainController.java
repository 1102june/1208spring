package com.example.youth.controller;

import com.example.youth.dto.ApiResponse;
import com.example.youth.dto.MainPageResponse;
import com.example.youth.service.MainService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/main")
public class MainController {

    @Autowired
    private MainService mainService;

    /**
     * 메인 페이지 데이터 조회
     * - AI 추천 정책 목록 (스와이프 카드)
     * - 읽지 않은 알림 개수
     */
    @GetMapping
    public ResponseEntity<ApiResponse<MainPageResponse>> getMainPage(
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        
        // TODO: 실제로는 JWT 토큰에서 userId를 추출해야 함
        // 현재는 헤더에서 받거나, 추후 인증 필터에서 주입
        if (userId == null || userId.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("사용자 ID가 필요합니다."));
        }

        MainPageResponse mainPageData = mainService.getMainPageData(userId);
        return ResponseEntity.ok(ApiResponse.success("메인 페이지 데이터 조회 성공", mainPageData));
    }
}

