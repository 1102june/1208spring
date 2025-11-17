package com.example.youth.controller;

import com.example.youth.dto.ApiResponse;
import com.example.youth.service.HousingSyncService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/housing")
public class HousingSyncController {

    @Autowired
    private HousingSyncService housingSyncService;

    /**
     * 임대주택 데이터 동기화 (수동 실행)
     * @param brtcCode 광역시도 코드 (옵션, 전체 조회 시 생략)
     * @param signguCode 시군구 코드 (옵션)
     */
    @PostMapping("/sync")
    public ResponseEntity<ApiResponse<String>> syncHousingData(
            @RequestParam(required = false) String brtcCode,
            @RequestParam(required = false) String signguCode) {
        
        try {
            housingSyncService.syncLHRentalHouseData(brtcCode, signguCode);
            return ResponseEntity.ok(ApiResponse.success("임대주택 데이터 동기화가 시작되었습니다."));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("동기화 중 오류 발생: " + e.getMessage()));
        }
    }
}

