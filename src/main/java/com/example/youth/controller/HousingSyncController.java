package com.example.youth.controller;

import com.example.youth.dto.ApiResponse;
import com.example.youth.DB.HousingNotice;
import com.example.youth.DB.HousingComplex;
import com.example.youth.repository.HousingRepository;
import com.example.youth.repository.HousingNoticeRepository;
import com.example.youth.repository.HousingComplexRepository;
import com.example.youth.service.HousingSyncService;
import com.example.youth.service.HousingGeocodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/housing")
public class HousingSyncController {

    @Autowired
    private HousingSyncService housingSyncService;
    
    @Autowired
    private HousingRepository housingRepository; // 기존 호환성 유지용
    
    @Autowired
    private HousingNoticeRepository housingNoticeRepository;
    
    @Autowired
    private HousingComplexRepository housingComplexRepository;

    @Autowired
    private HousingGeocodeService housingGeocodeService;

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
    
    /**
     * 기존 공고문을 상세정보/공급정보 API로 보강 (주소/면적/세대수/난방/입주예정월/공고내용/공급가).
     * 장시간 작업이므로 백그라운드로 실행하고 즉시 응답한다. 진행 상황은 서버 로그로 확인.
     *
     * POST /api/admin/housing/enrich                       (전체, 모든 공고 처리)
     * POST /api/admin/housing/enrich?limit=10              (10건만 - 테스트용)
     * POST /api/admin/housing/enrich?onlyMissing=true      (주소 없는 공고만)
     */
    @PostMapping("/enrich")
    public ResponseEntity<ApiResponse<Object>> enrichNotices(
            @RequestParam(required = false, defaultValue = "0") int limit,
            @RequestParam(required = false, defaultValue = "false") boolean onlyMissing,
            @RequestParam(required = false, defaultValue = "true") boolean async) {
        try {
            if (async) {
                new Thread(() -> {
                    try {
                        housingSyncService.enrichNoticesWithDetailInfo(limit, onlyMissing);
                    } catch (Exception e) {
                        System.err.println("공고 보강 백그라운드 작업 실패: " + e.getMessage());
                        e.printStackTrace();
                    }
                }, "notice-enrich").start();
                return ResponseEntity.ok(ApiResponse.success(
                        "공고 상세/공급 정보 보강을 백그라운드로 시작했습니다. (limit=" + limit
                                + ", onlyMissing=" + onlyMissing + ") 진행 상황은 서버 로그를 확인하세요."));
            } else {
                Map<String, Object> result = housingSyncService.enrichNoticesWithDetailInfo(limit, onlyMissing);
                return ResponseEntity.ok(ApiResponse.success("공고 상세/공급 정보 보강 완료", result));
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("공고 보강 중 오류 발생: " + e.getMessage()));
        }
    }

    /**
     * housing_notice와 housing_complex를 매칭하여 housing 테이블에 저장
     * POST /api/admin/housing/match-and-save
     */
    @PostMapping("/match-and-save")
    public ResponseEntity<ApiResponse<String>> matchAndSaveHousingData() {
        try {
            housingSyncService.matchAndSaveHousingData();
            return ResponseEntity.ok(ApiResponse.success("housing_notice와 housing_complex 매칭 및 housing 테이블 저장이 완료되었습니다."));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("매칭 및 저장 중 오류 발생: " + e.getMessage()));
        }
    }

    /**
     * housing_complex 좌표 배치 지오코딩 (Kakao Local API).
     *
     * POST /api/admin/housing/geocode?limit=100&onlyMissing=true&async=true
     */
    @PostMapping("/geocode")
    public ResponseEntity<ApiResponse<Object>> geocodeComplexes(
            @RequestParam(required = false, defaultValue = "100") int limit,
            @RequestParam(required = false, defaultValue = "true") boolean onlyMissing,
            @RequestParam(required = false, defaultValue = "true") boolean async) {
        try {
            if (async) {
                new Thread(() -> {
                    try {
                        housingGeocodeService.batchGeocode(limit, onlyMissing);
                    } catch (Exception e) {
                        System.err.println("단지 지오코딩 백그라운드 작업 실패: " + e.getMessage());
                        e.printStackTrace();
                    }
                }, "housing-geocode").start();
                return ResponseEntity.ok(ApiResponse.success(
                        "단지 지오코딩을 백그라운드로 시작했습니다. (limit=" + limit
                                + ", onlyMissing=" + onlyMissing + ")"));
            }
            Map<String, Object> result = housingGeocodeService.batchGeocode(limit, onlyMissing);
            return ResponseEntity.ok(ApiResponse.success("단지 지오코딩 완료", result));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("단지 지오코딩 중 오류 발생: " + e.getMessage()));
        }
    }
    
    /**
     * housing 테이블의 모든 데이터 삭제
     * DELETE /api/admin/housing/clear
     */
    @DeleteMapping("/clear")
    public ResponseEntity<ApiResponse<Object>> clearHousingData() {
        try {
            long count = housingRepository.count();
            housingRepository.deleteAll();
            
            Map<String, Object> result = new java.util.HashMap<>();
            result.put("deletedCount", count);
            result.put("message", "housing 테이블의 모든 데이터가 삭제되었습니다.");
            
            return ResponseEntity.ok(ApiResponse.success("housing 테이블 데이터 삭제 완료", result));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("데이터 삭제 중 오류 발생: " + e.getMessage()));
        }
    }
    
    /**
     * 동기화 상태 확인 (테스트용)
     * GET /api/admin/housing/stats
     * 새로운 두 테이블 구조(housing_notice, housing_complex)의 통계를 반환
     */
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Object>> getSyncStats() {
        try {
            // 새로운 테이블 구조 통계
            long noticeCount = housingNoticeRepository.count();
            long complexCount = housingComplexRepository.count();
            
            // 기존 테이블 통계 (호환성 유지)
            long oldHousingCount = housingRepository.count();
            
            java.util.Map<String, Object> stats = new java.util.HashMap<>();
            
            // 새로운 테이블 통계
            stats.put("housingNoticeCount", noticeCount);
            stats.put("housingComplexCount", complexCount);
            
            // 기존 테이블 통계
            stats.put("oldHousingCount", oldHousingCount);
            
            // 실제 매칭 통계 수집
            try {
                Map<String, Object> matchingStats = housingSyncService.getMatchingStatistics();
                stats.put("matchingStats", matchingStats);
                
                // 하위 호환성을 위한 예상 매칭 수 (실제 매칭 수 사용)
                Object totalMatched = matchingStats.get("totalMatched");
                stats.put("estimatedMatchedCount", totalMatched != null ? totalMatched : Math.min(noticeCount, complexCount));
            } catch (Exception e) {
                System.err.println("매칭 통계 수집 중 오류: " + e.getMessage());
                e.printStackTrace();
                // 오류 발생 시 기본값 사용
                stats.put("estimatedMatchedCount", Math.min(noticeCount, complexCount));
                stats.put("matchingStatsError", e.getMessage());
            }
            
            // 데이터 완성도
            if (complexCount > 0) {
                long complexWithAddress = housingComplexRepository.findAll().stream()
                        .filter(c -> c.getRnAdres() != null && !c.getRnAdres().isEmpty())
                        .count();
                long complexWithCoordinates = housingComplexRepository.countWithCoordinates();
                long complexNeedingGeocode = housingComplexRepository.countNeedingGeocode();
                stats.put("complexWithAddress", complexWithAddress);
                stats.put("complexAddressRate", String.format("%.2f%%", (complexWithAddress * 100.0 / complexCount)));
                stats.put("complexWithCoordinates", complexWithCoordinates);
                stats.put("complexCoordinateRate", String.format("%.2f%%", (complexWithCoordinates * 100.0 / complexCount)));
                stats.put("complexNeedingGeocode", complexNeedingGeocode);
            }
            
            if (noticeCount > 0) {
                long noticeWithApplicationPeriod = housingNoticeRepository.findAll().stream()
                        .filter(n -> n.getApplicationStart() != null && n.getApplicationEnd() != null)
                        .count();
                stats.put("noticeWithApplicationPeriod", noticeWithApplicationPeriod);
                stats.put("noticeApplicationPeriodRate", String.format("%.2f%%", (noticeWithApplicationPeriod * 100.0 / noticeCount)));
            }
            
            return ResponseEntity.ok(ApiResponse.success("동기화 상태 조회 성공", stats));
        } catch (Exception e) {
            e.printStackTrace();
            // 에러 발생 시 기본 정보만 반환
            try {
                long noticeCount = housingNoticeRepository.count();
                long complexCount = housingComplexRepository.count();
                java.util.Map<String, Object> stats = new java.util.HashMap<>();
                stats.put("housingNoticeCount", noticeCount);
                stats.put("housingComplexCount", complexCount);
                stats.put("error", e.getMessage());
                return ResponseEntity.ok(ApiResponse.success("동기화 상태 조회 (부분 성공)", stats));
            } catch (Exception e2) {
                return ResponseEntity.internalServerError()
                        .body(ApiResponse.error("상태 조회 중 오류 발생: " + e.getMessage()));
            }
        }
    }
    
    /**
     * housing_complex 테이블 샘플 데이터 조회 (디버깅용)
     * GET /api/admin/housing/complex/sample?limit=10
     */
    @GetMapping("/complex/sample")
    public ResponseEntity<ApiResponse<Object>> getComplexSample(@RequestParam(defaultValue = "10") int limit) {
        try {
            List<HousingComplex> complexes = housingComplexRepository.findAll()
                    .stream()
                    .limit(limit)
                    .collect(Collectors.toList());
            
            List<Map<String, Object>> sampleData = complexes.stream()
                    .map(complex -> {
                        Map<String, Object> data = new java.util.HashMap<>();
                        data.put("complexId", complex.getComplexId());
                        data.put("hsmpNm", complex.getHsmpNm());
                        data.put("hsmpSn", complex.getComplexId()); // complexId는 hsmpSn
                        data.put("rnAdres", complex.getRnAdres());
                        data.put("brtcNm", complex.getBrtcNm());
                        data.put("signguNm", complex.getSignguNm());
                        return data;
                    })
                    .collect(Collectors.toList());
            
            Map<String, Object> result = new java.util.HashMap<>();
            result.put("count", complexes.size());
            result.put("totalCount", housingComplexRepository.count());
            result.put("data", sampleData);
            
            return ResponseEntity.ok(ApiResponse.success("단지정보 샘플 데이터 조회 성공", result));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("샘플 데이터 조회 중 오류 발생: " + e.getMessage()));
        }
    }
    
    /**
     * housing_notice 테이블 샘플 데이터 조회 (디버깅용)
     * GET /api/admin/housing/notice/sample?limit=10
     */
    @GetMapping("/notice/sample")
    public ResponseEntity<ApiResponse<Object>> getNoticeSample(@RequestParam(defaultValue = "10") int limit) {
        try {
            List<HousingNotice> notices = housingNoticeRepository.findAll()
                    .stream()
                    .limit(limit)
                    .collect(Collectors.toList());
            
            List<Map<String, Object>> sampleData = notices.stream()
                    .map(notice -> {
                        Map<String, Object> data = new java.util.HashMap<>();
                        data.put("noticeId", notice.getNoticeId());
                        data.put("panId", notice.getPanId());
                        data.put("panNm", notice.getPanNm());
                        data.put("hsmpSn", notice.getHsmpSn());
                        data.put("hsmpNm", notice.getHsmpNm());
                        data.put("cnpCdNm", notice.getCnpCdNm());
                        data.put("dtlUrl", notice.getDtlUrl());
                        data.put("applicationStart", notice.getApplicationStart());
                        data.put("applicationEnd", notice.getApplicationEnd());
                        return data;
                    })
                    .collect(Collectors.toList());
            
            Map<String, Object> result = new java.util.HashMap<>();
            result.put("count", notices.size());
            result.put("totalCount", housingNoticeRepository.count());
            result.put("data", sampleData);
            
            // hsmpSn과 hsmpNm이 null인 개수 확인
            long hsmpSnNullCount = housingNoticeRepository.findAll().stream()
                    .filter(n -> n.getHsmpSn() == null || n.getHsmpSn().isEmpty())
                    .count();
            long hsmpNmNullCount = housingNoticeRepository.findAll().stream()
                    .filter(n -> n.getHsmpNm() == null || n.getHsmpNm().isEmpty())
                    .count();
            
            result.put("hsmpSnNullCount", hsmpSnNullCount);
            result.put("hsmpNmNullCount", hsmpNmNullCount);
            
            return ResponseEntity.ok(ApiResponse.success("공고문 샘플 데이터 조회 성공", result));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("샘플 데이터 조회 중 오류 발생: " + e.getMessage()));
        }
    }
}

