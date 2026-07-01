package com.example.youth.service;

import com.example.youth.DB.Housing;
import com.example.youth.DB.HousingNotice;
import com.example.youth.DB.HousingComplex;
import com.example.youth.dto.publicdata.LHNoticeEnrichment;
import com.example.youth.dto.publicdata.LHRentalHouseListResponse;
import com.example.youth.dto.publicdata.LHRentalNoticeResponse;
import com.example.youth.repository.HousingRepository;
import com.example.youth.repository.HousingNoticeRepository;
import com.example.youth.repository.HousingComplexRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.sql.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class HousingSyncService {

    @Autowired
    private PublicDataApiService publicDataApiService;
    
    @Autowired
    private RegionCodeMappingService regionCodeMappingService;

    @Autowired
    private HousingRepository housingRepository; // 기존 호환성 유지용 (deprecated)

    @Autowired
    private HousingNoticeRepository housingNoticeRepository;

    @Autowired
    private HousingComplexRepository housingComplexRepository;

    /**
     * 모든 지역의 단지정보(housing_complex) 동기화
     * 주요 광역시도 코드(17개)와 각 시도별 실제 시군구 코드를 사용하여 전체 조회
     */
    public void syncAllHousingComplex() {
        System.out.println("========================================");
        System.out.println("전체 지역 단지정보(housing_complex) 동기화 시작");
        System.out.println("========================================");
        
        // 지역 코드 매핑 서비스 초기화
        try {
            System.out.println("지역 코드 매핑 서비스 초기화 시작...");
            regionCodeMappingService.initializeMapping();
            System.out.println("지역 코드 매핑 서비스 초기화 완료");
        } catch (Exception e) {
            System.err.println("⚠️ 지역 코드 매핑 서비스 초기화 실패: " + e.getMessage());
            e.printStackTrace();
        }
        
        // 주요 광역시도 코드 목록 (17개 시도)
        List<String> brtcCodes = List.of(
            "11", // 서울특별시
            "26", // 부산광역시
            "27", // 대구광역시
            "28", // 인천광역시
            "29", // 광주광역시
            "30", // 대전광역시
            "31", // 울산광역시
            "41", // 경기도
            "42", // 강원도
            "43", // 충청북도
            "44", // 충청남도
            "45", // 전라북도
            "46", // 전라남도
            "47", // 경상북도
            "48", // 경상남도
            "50"  // 제주특별자치도
        );
        
        Map<String, Set<String>> allRegions = new HashMap<>();
        
        // 각 광역시도별로 실제 존재하는 시군구 코드 가져오기
        for (String brtcCode : brtcCodes) {
            try {
                Map<String, String> signguCodes = regionCodeMappingService.getSignguCodesByBrtcCode(brtcCode);
                if (signguCodes != null && !signguCodes.isEmpty()) {
                    allRegions.put(brtcCode, new HashSet<>(signguCodes.values()));
                    System.out.println("  - " + brtcCode + ": " + signguCodes.size() + "개 시군구");
                }
            } catch (Exception e) {
                System.err.println("  ⚠️ " + brtcCode + " 시군구 코드 조회 실패: " + e.getMessage());
            }
        }
        
        System.out.println("총 " + allRegions.size() + "개 광역시도, " + 
                          allRegions.values().stream().mapToInt(Set::size).sum() + "개 시군구 조합");
        
        // 모든 지역 조합으로 단지정보 조회
        List<LHRentalHouseListResponse.Item> allHouseItems = syncTargetRegions(allRegions);
        
        // DB에 저장
        if (!allHouseItems.isEmpty()) {
            System.out.println("단지정보 데이터 저장 시작...");
            saveComplexData(allHouseItems);
            System.out.println("✅ 전체 지역 단지정보 데이터 저장 완료!");
        } else {
            System.err.println("⚠️ 단지정보 데이터가 없습니다.");
        }
    }

    /**
     * 단지정보(housing_complex)만 직접 동기화
     * 공고문 없이도 특정 지역의 단지정보를 조회하여 DB에 저장
     * 
     * @param brtcCode 광역시도 코드 (필수)
     * @param signguCode 시군구 코드 (필수)
     */
    public void syncHousingComplexOnly(String brtcCode, String signguCode) {
        System.out.println("========================================");
        System.out.println("단지정보(housing_complex) 직접 동기화 시작");
        System.out.println("  - brtcCode: " + brtcCode);
        System.out.println("  - signguCode: " + signguCode);
        System.out.println("========================================");
        
        if (brtcCode == null || brtcCode.isEmpty()) {
            System.err.println("⚠️ brtcCode(광역시도 코드)는 필수입니다.");
            return;
        }
        if (signguCode == null || signguCode.isEmpty()) {
            System.err.println("⚠️ signguCode(시군구 코드)는 필수입니다.");
            return;
        }
        
        // 단지정보 API 호출
        try {
            LHRentalHouseListResponse houseResponse = publicDataApiService.getLHRentalHouseList(
                    1, 1000, brtcCode, signguCode, null).block();
            
            List<LHRentalHouseListResponse.Item> items = houseResponse != null ? houseResponse.getItems() : null;
            if (items == null || items.isEmpty()) {
                System.out.println("⚠️ 단지정보 데이터가 없습니다.");
                return;
            }
            
            System.out.println("✅ 단지정보 API 데이터 수집: " + items.size() + "건");
            System.out.println("단지정보 데이터 저장 시작...");
            saveComplexData(items);
            System.out.println("✅ 단지정보 데이터 저장 완료!");
        } catch (Exception e) {
            System.err.println("⚠️ 단지정보 API 호출 실패: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * LH 공공데이터에서 임대주택 정보를 가져와 DB에 동기화
     * 1. 공고문 API 전체 데이터 조회 → housing_notice 테이블에 저장
     * 2. 공고문 데이터에서 지역 정보 추출
     * 3. 추출한 지역 정보로 단지정보 API 전체 데이터 조회 → housing_complex 테이블에 저장
     * 
     * 각 테이블은 독립적으로 관리되며, 앱에서 조회 시 두 테이블을 조인/병합하여 제공
     * 
     * @param brtcCode 광역시도 코드 (옵션, 공고문 API용)
     * @param signguCode 시군구 코드 (옵션, 공고문 API용)
     * 
     * 주의: @Transactional 제거 - 긴 작업이므로 메서드 전체에 트랜잭션을 걸지 않음
     *       DB 저장 시점에만 트랜잭션 적용
     */
    public void syncLHRentalHouseData(String brtcCode, String signguCode) {
        System.out.println("========================================");
        System.out.println("임대주택 데이터 동기화 시작 (공고문 + 단지정보 각각 독립 저장)");
        System.out.println("========================================");
        
        // 지역 코드 매핑 서비스 초기화
        try {
            System.out.println("지역 코드 매핑 서비스 초기화 시작...");
            regionCodeMappingService.initializeMapping();
            System.out.println("지역 코드 매핑 서비스 초기화 완료");
        } catch (Exception e) {
            System.err.println("⚠️ 지역 코드 매핑 서비스 초기화 실패: " + e.getMessage());
            e.printStackTrace();
        }
        
        // 1단계: 공고문 API 전체 데이터 조회
        publicDataApiService.getAllLHRentalNoticeList(brtcCode, signguCode)
                .doOnError(error -> {
                    System.err.println("공고문 API 호출 실패: " + error.getMessage());
                })
                .onErrorResume(error -> {
                    return Mono.just(new LHRentalNoticeResponse());
                })
                .subscribe(
                        noticeResponse -> {
                            if (noticeResponse == null || noticeResponse.getDsList() == null || noticeResponse.getDsList().isEmpty()) {
                                System.out.println("공고문 데이터가 없습니다.");
                                return;
                            }
                            
                            List<LHRentalNoticeResponse.Item> noticeItems = noticeResponse.getDsList();
                            System.out.println("✅ 공고문 API 전체 데이터 수집: " + noticeItems.size() + "건");
                            
                            // 디버깅: 첫 번째 공고문의 실제 필드 값 확인
                            if (!noticeItems.isEmpty()) {
                                LHRentalNoticeResponse.Item firstNotice = noticeItems.get(0);
                                System.out.println("=== 공고문 API 응답 필드 확인 (첫 번째 항목) ===");
                                System.out.println("  - panId: " + firstNotice.getPanId());
                                System.out.println("  - panNm: " + firstNotice.getPanNm());
                                System.out.println("  - hsmpSn: " + firstNotice.getHsmpSn());
                                System.out.println("  - hsmpNm: " + firstNotice.getHsmpNm());
                                System.out.println("  - cnpCdNm: " + firstNotice.getCnpCdNm());
                                System.out.println("  - dtlUrl: " + (firstNotice.getDtlUrl() != null && firstNotice.getDtlUrl().length() > 100 ? firstNotice.getDtlUrl().substring(0, 100) + "..." : firstNotice.getDtlUrl()));
                                System.out.println("=============================================");
                            }
                            
                            // 공고문 데이터를 Map으로 변환
                            Map<String, LHRentalNoticeResponse.Item> noticeMapByHsmpSn = new HashMap<>();
                            Map<String, LHRentalNoticeResponse.Item> noticeMapByHsmpNm = new HashMap<>();
                            
                            for (LHRentalNoticeResponse.Item notice : noticeItems) {
                                // 1순위: hsmpSn (단지 식별자)로 매핑
                                if (notice.getHsmpSn() != null && !notice.getHsmpSn().isEmpty()) {
                                    String key = notice.getHsmpSn();
                                    if (!noticeMapByHsmpSn.containsKey(key) || isNewerNotice(notice, noticeMapByHsmpSn.get(key))) {
                                        noticeMapByHsmpSn.put(key, notice);
                                    }
                                }
                                // 2순위: hsmpNm (단지명)으로 매핑 (hsmpSn이 없을 때만)
                                else if (notice.getHsmpNm() != null && !notice.getHsmpNm().isEmpty()) {
                                    String key = notice.getHsmpNm();
                                    if (!noticeMapByHsmpNm.containsKey(key) || isNewerNotice(notice, noticeMapByHsmpNm.get(key))) {
                                        noticeMapByHsmpNm.put(key, notice);
                                    }
                                }
                            }
                            
                            System.out.println("공고문 데이터 Map 변환 완료:");
                            System.out.println("  - hsmpSn 기준: " + noticeMapByHsmpSn.size() + "건");
                            System.out.println("  - hsmpNm 기준: " + noticeMapByHsmpNm.size() + "건");
                            
                            // 2단계: 공고문 데이터에서 지역 정보 추출
                            System.out.println("공고문 데이터에서 지역 정보 추출 시작...");
                            Map<String, Set<String>> brtcToSignguCodes = extractRegionsFromNotices(noticeItems);
                            System.out.println("지역 정보 추출 완료: " + brtcToSignguCodes.size() + "개 광역시도");
                            
                            // 공고문 데이터 저장 (지역 정보 추출 전에 먼저 저장)
                            System.out.println("공고문 데이터 저장 시작...");
                            saveNoticeData(noticeItems);
                            
                            if (brtcToSignguCodes.isEmpty()) {
                                System.err.println("⚠️ 공고문에서 지역 정보를 추출할 수 없습니다. 단지정보 조회를 건너뜁니다.");
                                System.err.println("⚠️ 공고문 데이터 샘플 확인:");
                                if (!noticeItems.isEmpty()) {
                                    LHRentalNoticeResponse.Item sample = noticeItems.get(0);
                                    System.err.println("  - 샘플 cnpCdNm: " + sample.getCnpCdNm());
                                    System.err.println("  - 샘플 hsmpNm: " + sample.getHsmpNm());
                                    System.err.println("  - 샘플 hsmpSn: " + sample.getHsmpSn());
                                }
                                return;
                            }
                            
                            System.out.println("추출된 광역시도 코드: " + brtcToSignguCodes.size() + "개");
                            // 각 광역시도의 시군구 코드 수 출력
                            for (Map.Entry<String, Set<String>> entry : brtcToSignguCodes.entrySet()) {
                                System.out.println("  - " + entry.getKey() + ": " + entry.getValue().size() + "개 시군구");
                            }
                            
                            // 3단계: 단지정보 API 전체 데이터 조회 (공고문에서 추출한 지역 정보 기반)
                            // 리액티브 스레드에서 block() 호출을 피하기 위해 별도 스레드에서 실행
                            System.out.println("단지정보 API 전체 데이터 조회 시작 (공고문 지역 정보 기반)");
                            
                            // CompletableFuture를 사용하여 별도 스레드에서 실행
                            java.util.concurrent.CompletableFuture<List<LHRentalHouseListResponse.Item>> future = 
                                java.util.concurrent.CompletableFuture.supplyAsync(() -> {
                                    return syncTargetRegions(brtcToSignguCodes);
                                });
                            
                            // 비동기로 결과 처리
                            future.whenComplete((allHouseItems, throwable) -> {
                                if (throwable != null) {
                                    System.err.println("⚠️ 단지정보 API 조회 중 오류 발생: " + throwable.getMessage());
                                    throwable.printStackTrace();
                                    return;
                                }
                                
                                System.out.println("✅ 단지정보 API 전체 데이터 수집: " + allHouseItems.size() + "건");
                                
                                // 4단계: 각 테이블에 독립적으로 저장
                                if (allHouseItems.isEmpty()) {
                                    System.err.println("⚠️ 단지정보 API에서 데이터를 수집하지 못했습니다. 저장을 건너뜁니다.");
                                    System.err.println("⚠️ 지역 정보나 API 응답 형식에 문제가 있을 수 있습니다.");
                                } else {
                                    System.out.println("단지정보 데이터 저장 시작...");
                                    saveComplexData(allHouseItems);
                                    System.out.println("✅ 단지정보 데이터 저장 완료!");
                                }
                            });
                        },
                        error -> {
                            System.err.println("========================================");
                            System.err.println("LH API 동기화 중 치명적인 오류 발생: " + error.getMessage());
                            System.err.println("========================================");
                            error.printStackTrace();
                        }
                );
    }
    
    /**
     * 공고가 있는 지역만 단지정보 API로 조회 (성능 최적화)
     * 10만 번 루프 -> 수백 번으로 감소
     * 
     * @param targetRegions 공고가 있는 지역 코드 맵 (광역시도 코드 -> 시군구 코드 Set)
     * @return 수집된 단지정보 리스트
     */
    private List<LHRentalHouseListResponse.Item> syncTargetRegions(Map<String, Set<String>> targetRegions) {
        List<LHRentalHouseListResponse.Item> allHouseItems = new ArrayList<>();
        
        // 총 조합 수 계산
        int totalCount = targetRegions.values().stream().mapToInt(Set::size).sum();
        int processedCount = 0;
        int successCount = 0;
        
        System.out.println("========================================");
        System.out.println("단지정보 API 조회 시작 (공고가 있는 지역만)");
        System.out.println("  - 총 조회 대상: " + totalCount + "개 지역 조합");
        System.out.println("  - 광역시도 수: " + targetRegions.size() + "개");
        System.out.println("========================================");
        
        // 공고가 있는 지역만 순회
        for (Map.Entry<String, Set<String>> entry : targetRegions.entrySet()) {
            String brtcCode = entry.getKey();
            Set<String> signguCodes = entry.getValue();
            
            for (String signguCode : signguCodes) {
                processedCount++;
                
                // 진행 상황 출력 (100개마다)
                if (processedCount % 100 == 0) {
                    System.out.println("단지정보 API 진행: " + processedCount + "/" + totalCount + 
                                     " (" + (processedCount * 100 / totalCount) + "%) - 성공: " + successCount + "개");
                }
                
                // 각 조합에 대해 API 호출 (동기 방식으로 순차 처리)
                try {
                    LHRentalHouseListResponse houseResponse = publicDataApiService.getLHRentalHouseList(
                            1, 100, brtcCode, signguCode, null).block();
                    
                    // getItems() 메서드를 사용하여 새 형식/기존 형식 모두 지원
                    List<LHRentalHouseListResponse.Item> items = houseResponse != null ? houseResponse.getItems() : null;
                    if (items != null && !items.isEmpty()) {
                        allHouseItems.addAll(items);
                        successCount++;
                        // 성공한 조합 로그 출력 (처음 10개만)
                        if (successCount <= 10) {
                            System.out.println("✅ 데이터 수집 성공: brtcCode=" + brtcCode + ", signguCode=" + signguCode + ", 데이터 개수=" + items.size() + "건");
                        }
                    }
                } catch (Exception e) {
                    // 에러는 무시하고 계속 진행 (존재하지 않는 코드 조합일 수 있음)
                    // 하지만 처음 10개 에러는 로그 출력
                    if (processedCount <= 10) {
                        System.err.println("  ⚠️ API 호출 에러 (" + brtcCode + "-" + signguCode + "): " + e.getClass().getSimpleName() + " - " + e.getMessage());
                    }
                }
                
                // API 호출 제한을 피하기 위해 짧은 딜레이 추가 (50ms)
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    System.err.println("단지정보 API 조회가 중단되었습니다.");
                    break;
                }
            }
        }
        
        System.out.println("========================================");
        System.out.println("단지정보 API 조회 완료");
        System.out.println("  - 처리된 조합: " + processedCount + "개");
        System.out.println("  - 성공한 조합: " + successCount + "개");
        System.out.println("  - 수집된 단지정보: " + allHouseItems.size() + "건");
        System.out.println("========================================");
        
        return allHouseItems;
    }
    
    /**
     * 전체 지역 데이터 조회 (모든 광역시도 코드와 시군구 코드 조합 순회)
     * 주의: 이 메서드는 성능 문제로 인해 사용하지 않음 (10만 번 루프)
     * 대신 syncTargetRegions() 사용 권장
     * 
     * @deprecated 성능 문제로 인해 사용하지 않음. syncTargetRegions() 사용 권장
     */
    @Deprecated
    private List<LHRentalHouseListResponse.Item> syncAllRegionsForHouseList() {
        int totalCombinations = 100 * 1000; // 100,000개 조합
        int processedCount = 0;
        int successCount = 0;
        List<LHRentalHouseListResponse.Item> allHouseItems = new ArrayList<>();
        
        System.out.println("총 " + totalCombinations + "개 조합을 순회합니다. (시간이 오래 걸릴 수 있습니다)");
        
        // 모든 광역시도 코드(0-99)와 시군구 코드(0-999) 조합 순회
        for (int brtc = 0; brtc < 100; brtc++) {
            String brtcCode = String.format("%02d", brtc); // 2자리로 포맷 (00, 01, ..., 99)
            
            // 광역시도별로 연속 빈 응답 카운트 (최적화: 연속 100개 빈 응답이면 해당 광역시도 건너뛰기)
            int consecutiveEmptyCount = 0;
            int brtcSuccessCount = 0;
            
            for (int signgu = 0; signgu < 1000; signgu++) {
                String signguCode = String.format("%03d", signgu); // 3자리로 포맷 (000, 001, ..., 999)
                processedCount++;
                
                // 진행 상황 출력 (10000개마다, 로그 최소화)
                if (processedCount % 10000 == 0) {
                    System.out.println("단지정보 API 진행: " + processedCount + "/" + totalCombinations + 
                                     " (" + (processedCount * 100 / totalCombinations) + "%) - 성공: " + successCount + "개");
                }
                
                // 각 조합에 대해 API 호출 (동기 방식으로 순차 처리)
                try {
                    LHRentalHouseListResponse houseResponse = publicDataApiService.getLHRentalHouseList(
                            1, 100, brtcCode, signguCode, null).block();
                    
                    // getItems() 메서드를 사용하여 새 형식/기존 형식 모두 지원
                    List<LHRentalHouseListResponse.Item> items = houseResponse != null ? houseResponse.getItems() : null;
                    if (items != null && !items.isEmpty()) {
                        allHouseItems.addAll(items);
                        successCount++;
                        brtcSuccessCount++;
                        consecutiveEmptyCount = 0; // 성공 시 카운트 리셋
                        // 성공한 조합 로그 출력 (처음 10개만)
                        if (successCount <= 10) {
                            System.out.println("✅ 데이터 수집 성공: brtcCode=" + brtcCode + ", signguCode=" + signguCode + ", 데이터 개수=" + items.size() + "건");
                        }
                    } else {
                        consecutiveEmptyCount++;
                        // 연속 100개 빈 응답이면 해당 광역시도의 나머지 시군구는 건너뛰기 (로그 제거)
                        if (consecutiveEmptyCount >= 100 && brtcSuccessCount == 0) {
                            // 로그 제거 (전체 조회 시 로그가 너무 많아짐)
                            break; // 해당 광역시도의 나머지 시군구 건너뛰기
                        }
                    }
                } catch (Exception e) {
                    // 에러는 무시하고 계속 진행 (존재하지 않는 코드 조합일 수 있음)
                    consecutiveEmptyCount++;
                }
                
                // API 호출 제한을 피하기 위해 짧은 딜레이 추가 (50ms로 단축)
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            
            // 광역시도별 결과 출력 (데이터가 있는 경우만)
            if (brtcSuccessCount > 0) {
                System.out.println("광역시도 " + brtcCode + ": " + brtcSuccessCount + "개 시군구에서 데이터 수집");
            }
        }
        
        System.out.println("========================================");
        System.out.println("단지정보 API 전체 지역 조회 완료");
        System.out.println("  - 처리된 조합: " + processedCount + "개");
        System.out.println("  - 성공한 조합: " + successCount + "개");
        System.out.println("  - 수집된 단지정보: " + allHouseItems.size() + "건");
        System.out.println("========================================");
        
        return allHouseItems;
    }
    
    /**
     * 특정 지역의 데이터만 동기화
     */
    private void syncSingleRegion(String brtcCode, String signguCode) {
        // 단지정보 API 호출 (실패해도 공고문 API는 실행)
        Mono<LHRentalHouseListResponse> houseListMono = publicDataApiService.getAllLHRentalHouseList(brtcCode, signguCode)
                .doOnError(error -> {
                    System.err.println("단지정보 API 호출 실패, 공고문 API만 사용합니다: " + error.getMessage());
                })
                .onErrorResume(error -> {
                    // 에러 시 빈 응답 Mono 반환
                    return Mono.just(new LHRentalHouseListResponse());
                });
        
        // 공고문 API 호출 (실패해도 단지정보는 저장)
        Mono<LHRentalNoticeResponse> noticeMono = publicDataApiService.getAllLHRentalNoticeList(brtcCode, signguCode)
                .doOnError(error -> {
                    System.err.println("공고문 API 호출 실패, 단지정보만 저장합니다: " + error.getMessage());
                })
                .onErrorResume(error -> {
                    // 에러 시 빈 응답 Mono 반환
                    return Mono.just(new LHRentalNoticeResponse());
                });
        
        // 두 API를 병렬로 호출하고 결과를 합침
        Mono.zip(houseListMono, noticeMono)
                .subscribe(
                        tuple -> processApiResults(tuple.getT1(), tuple.getT2()),
                        error -> {
                            System.err.println("========================================");
                            System.err.println("LH API 호출 실패: " + error.getMessage());
                            System.err.println("========================================");
                            error.printStackTrace();
                        }
                );
    }
    
    /**
     * 전체 지역 데이터 조회 (모든 광역시도 코드와 시군구 코드 조합 순회)
     */
    private void syncAllRegions() {
        int totalCombinations = 100 * 1000; // 100,000개 조합
        int processedCount = 0;
        int successCount = 0;
        
        System.out.println("총 " + totalCombinations + "개 조합을 순회합니다. (시간이 오래 걸릴 수 있습니다)");
        
        // 모든 광역시도 코드(0-99)와 시군구 코드(0-999) 조합 순회
        for (int brtc = 0; brtc < 100; brtc++) {
            String brtcCode = String.format("%02d", brtc); // 2자리로 포맷 (00, 01, ..., 99)
            
            for (int signgu = 0; signgu < 1000; signgu++) {
                String signguCode = String.format("%03d", signgu); // 3자리로 포맷 (000, 001, ..., 999)
                processedCount++;
                
                // 진행 상황 출력 (1000개마다)
                if (processedCount % 1000 == 0) {
                    System.out.println("진행 상황: " + processedCount + "/" + totalCombinations + 
                                     " (" + (processedCount * 100 / totalCombinations) + "%)");
                }
                
                // 각 조합에 대해 API 호출 (동기 방식으로 순차 처리)
                try {
                    syncSingleRegionSync(brtcCode, signguCode);
                    successCount++;
                } catch (Exception e) {
                    // 에러는 무시하고 계속 진행 (존재하지 않는 코드 조합일 수 있음)
                }
                
                // API 호출 제한을 피하기 위해 짧은 딜레이 추가 (100ms)
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        
        System.out.println("========================================");
        System.out.println("전체 지역 데이터 조회 완료");
        System.out.println("  - 처리된 조합: " + processedCount + "개");
        System.out.println("  - 성공한 조합: " + successCount + "개");
        System.out.println("========================================");
    }
    
    /**
     * 특정 지역의 데이터를 동기 방식으로 동기화 (전체 조회 시 사용)
     */
    private void syncSingleRegionSync(String brtcCode, String signguCode) {
        // 단지정보 API 호출
        LHRentalHouseListResponse houseListResponse = null;
        try {
            houseListResponse = publicDataApiService.getAllLHRentalHouseList(brtcCode, signguCode).block();
        } catch (Exception e) {
            // 에러는 무시 (존재하지 않는 코드 조합일 수 있음)
        }
        
        // 공고문 API 호출
        LHRentalNoticeResponse noticeResponse = null;
        try {
            noticeResponse = publicDataApiService.getAllLHRentalNoticeList(brtcCode, signguCode).block();
        } catch (Exception e) {
            // 에러는 무시
        }
        
        // 결과 처리
        if (houseListResponse != null || noticeResponse != null) {
            processApiResults(houseListResponse != null ? houseListResponse : new LHRentalHouseListResponse(),
                            noticeResponse != null ? noticeResponse : new LHRentalNoticeResponse());
        }
    }
    
    /**
     * API 결과 처리 (공통 로직)
     */
    private void processApiResults(LHRentalHouseListResponse houseListResponse, LHRentalNoticeResponse noticeResponse) {
        // 단지정보 처리 (getItems() 메서드를 사용하여 새 형식/기존 형식 모두 지원)
        List<LHRentalHouseListResponse.Item> houseItems = houseListResponse != null ? houseListResponse.getItems() : null;
        if (houseItems != null && !houseItems.isEmpty()) {
            System.out.println("✅ 단지정보 데이터 수집: " + houseItems.size() + "건");
        }
        
        // 공고문 데이터를 Map으로 변환 (단지 식별자 기준)
        // 키: hsmpSn (우선) 또는 hsmpNm (보조)
        Map<String, LHRentalNoticeResponse.Item> noticeMapByHsmpSn = new HashMap<>(); // hsmpSn 기준
        Map<String, LHRentalNoticeResponse.Item> noticeMapByHsmpNm = new HashMap<>(); // hsmpNm 기준 (hsmpSn이 없을 때)
        
        if (noticeResponse != null && noticeResponse.getDsList() != null) {
            List<LHRentalNoticeResponse.Item> noticeItems = noticeResponse.getDsList();
            System.out.println("공고문 데이터 수집: " + noticeItems.size() + "건");
            
            // 공고문 데이터를 단지 식별자로 매핑
            for (LHRentalNoticeResponse.Item notice : noticeItems) {
                // 1순위: hsmpSn (단지 식별자)로 매핑
                if (notice.getHsmpSn() != null && !notice.getHsmpSn().isEmpty()) {
                    String key = notice.getHsmpSn();
                    // 이미 존재하는 경우 더 최신 공고문으로 업데이트
                    if (!noticeMapByHsmpSn.containsKey(key) || 
                        isNewerNotice(notice, noticeMapByHsmpSn.get(key))) {
                        noticeMapByHsmpSn.put(key, notice);
                    }
                }
                // 2순위: hsmpNm (단지명)으로 매핑 (hsmpSn이 없을 때만)
                else if (notice.getHsmpNm() != null && !notice.getHsmpNm().isEmpty()) {
                    String key = notice.getHsmpNm();
                    // 이미 존재하는 경우 더 최신 공고문으로 업데이트
                    if (!noticeMapByHsmpNm.containsKey(key) || 
                        isNewerNotice(notice, noticeMapByHsmpNm.get(key))) {
                        noticeMapByHsmpNm.put(key, notice);
                    }
                }
            }
            System.out.println("공고문 데이터 Map 변환 완료:");
            System.out.println("  - hsmpSn 기준: " + noticeMapByHsmpSn.size() + "건");
            System.out.println("  - hsmpNm 기준: " + noticeMapByHsmpNm.size() + "건");
        }
        
        // 단지정보가 있는 경우 처리
        if (houseItems != null && !houseItems.isEmpty()) {
            // 단지정보 데이터를 Housing 엔티티로 변환하여 저장
            saveHousingDataWithNotices(houseItems, noticeMapByHsmpSn, noticeMapByHsmpNm);
        } 
        // 단지정보가 없고 공고문만 있는 경우
        else if ((noticeMapByHsmpSn != null && !noticeMapByHsmpSn.isEmpty()) || 
                 (noticeMapByHsmpNm != null && !noticeMapByHsmpNm.isEmpty())) {
            // 공고문 데이터만으로 Housing 엔티티 생성 및 저장
            saveNoticeDataOnly(noticeMapByHsmpSn, noticeMapByHsmpNm);
        } else {
            // 데이터가 없는 경우는 조용히 넘어감 (전체 조회 시 많은 조합이 데이터가 없을 수 있음)
        }
    }

    /**
     * 공고문 데이터에서 지역 정보 추출
     * @param noticeItems 공고문 API Item 리스트
     * @return 광역시도 코드 -> 시군구 코드 Set 맵
     */
    private Map<String, Set<String>> extractRegionsFromNotices(List<LHRentalNoticeResponse.Item> noticeItems) {
        Map<String, Set<String>> brtcToSignguCodes = new HashMap<>();
        int processedCount = 0;
        int mappedCount = 0;
        int failedMappingCount = 0;
        
        System.out.println("지역 정보 추출: 공고문 " + noticeItems.size() + "건 처리 시작...");
        
        for (LHRentalNoticeResponse.Item notice : noticeItems) {
            processedCount++;
            // CNP_CD_NM (지역명) 추출 및 광역시도 코드로 변환
            String cnpCdNm = notice.getCnpCdNm(); // 예: "충청북도", "서울특별시"
            if (cnpCdNm != null && !cnpCdNm.isEmpty()) {
                String mappedBrtcCode = regionCodeMappingService.convertCnpCdNmToBrtcCode(cnpCdNm);
                if (mappedBrtcCode != null) {
                    // 해당 광역시도에 속한 실제 시군구 코드 목록 가져오기
                    Map<String, String> signguCodes = regionCodeMappingService.getSignguCodesByBrtcCode(mappedBrtcCode);
                    if (!signguCodes.isEmpty()) {
                        brtcToSignguCodes.putIfAbsent(mappedBrtcCode, new HashSet<>());
                        brtcToSignguCodes.get(mappedBrtcCode).addAll(signguCodes.values());
                        mappedCount++;
                        // 처음 10개만 상세 로그
                        if (mappedCount <= 10) {
                            System.out.println("  ✅ 매핑 성공: " + cnpCdNm + " -> " + mappedBrtcCode + " (" + signguCodes.size() + "개 시군구)");
                        }
                    } else {
                        failedMappingCount++;
                        if (failedMappingCount <= 5) {
                            System.err.println("  ⚠️ 매핑 실패 (시군구 코드 없음): " + cnpCdNm + " -> " + mappedBrtcCode);
                        }
                    }
                } else {
                    failedMappingCount++;
                    if (failedMappingCount <= 5) {
                        System.err.println("  ⚠️ 매핑 실패 (광역시도 코드 없음): " + cnpCdNm);
                    }
                }
            } else {
                // cnpCdNm이 없는 경우도 카운트
                if (processedCount <= 5) {
                    System.err.println("  ⚠️ cnpCdNm 필드 없음: hsmpNm=" + notice.getHsmpNm());
                }
            }
        }
        
        System.out.println("지역 정보 추출 완료:");
        System.out.println("  - 처리된 공고문: " + processedCount + "건");
        System.out.println("  - 매핑 성공: " + mappedCount + "건");
        System.out.println("  - 매핑 실패: " + failedMappingCount + "건");
        System.out.println("  - 추출된 광역시도: " + brtcToSignguCodes.size() + "개");
        
        return brtcToSignguCodes;
    }

    /**
     * DB에 저장된 housing 데이터에서 지역 정보 추출
     * @param housings DB에 저장된 housing 리스트
     * @return 광역시도 코드 -> 시군구 코드 Set 맵
     */
    private Map<String, Set<String>> extractRegionsFromHousings(List<Housing> housings) {
        Map<String, Set<String>> brtcToSignguCodes = new HashMap<>();
        
        for (Housing housing : housings) {
            String address = housing.getAddress();
            if (address == null || address.isEmpty()) {
                continue;
            }
            
            // 주소에서 광역시도명 추출 (예: "서울특별시 강남구" -> "서울특별시")
            String[] addressParts = address.split("\\s+");
            if (addressParts.length == 0) {
                continue;
            }
            
            String brtcName = addressParts[0]; // 첫 번째 부분이 광역시도명
            
            // 광역시도명을 코드로 변환
            String brtcCode = regionCodeMappingService.getBrtcCode(brtcName);
            if (brtcCode == null) {
                // convertCnpCdNmToBrtcCode도 시도
                brtcCode = regionCodeMappingService.convertCnpCdNmToBrtcCode(brtcName);
            }
            
            if (brtcCode != null) {
                // 해당 광역시도의 모든 시군구 코드 가져오기
                Map<String, String> signguCodes = regionCodeMappingService.getSignguCodesByBrtcCode(brtcCode);
                if (!signguCodes.isEmpty()) {
                    brtcToSignguCodes.putIfAbsent(brtcCode, new HashSet<>());
                    brtcToSignguCodes.get(brtcCode).addAll(signguCodes.values());
                }
            }
        }
        
        return brtcToSignguCodes;
    }

    /**
     * 단지정보 API 데이터로 DB housing 업데이트 (트랜잭션 적용)
     * @param savedHousings DB에 저장된 housing 리스트
     * @param houseItems 단지정보 API에서 받아온 데이터
     */
    @Transactional
    private void updateHousingsWithHouseInfo(
            List<Housing> savedHousings,
            List<LHRentalHouseListResponse.Item> houseItems) {
        
        if (houseItems == null || houseItems.isEmpty()) {
            System.out.println("단지정보 API 데이터가 없어 업데이트를 건너뜁니다.");
            return;
        }
        
        // 단지정보 API 데이터를 Map으로 변환 (housingId 또는 name 기준)
        Map<String, LHRentalHouseListResponse.Item> houseMapByHsmpSn = new HashMap<>();
        Map<String, LHRentalHouseListResponse.Item> houseMapByHsmpNm = new HashMap<>();
        
        for (LHRentalHouseListResponse.Item item : houseItems) {
            // 1순위: hsmpSn (단지 식별자)로 매핑
            if (item.getHsmpSn() != null && !item.getHsmpSn().isEmpty()) {
                houseMapByHsmpSn.put(item.getHsmpSn(), item);
            }
            // 2순위: hsmpNm (단지명)으로 매핑
            if (item.getHsmpNm() != null && !item.getHsmpNm().isEmpty()) {
                houseMapByHsmpNm.put(item.getHsmpNm(), item);
            }
        }
        
        int updatedCount = 0;
        int matchedCount = 0;
        
        // DB housing 데이터를 순회하며 단지정보로 업데이트
        for (Housing housing : savedHousings) {
            try {
                LHRentalHouseListResponse.Item matchedHouseItem = null;
                
                // 1순위: hsmpSn (단지 식별자)로 매칭
                String housingId = housing.getHousingId();
                if (housingId != null && houseMapByHsmpSn.containsKey(housingId)) {
                    matchedHouseItem = houseMapByHsmpSn.get(housingId);
                    matchedCount++;
                }
                // 2순위: hsmpNm (단지명)으로 매칭 (퍼지 매칭 포함)
                else {
                    String housingName = housing.getName();
                    if (housingName != null) {
                        matchedHouseItem = findMatchingHouseItemByName(housingName, houseMapByHsmpNm);
                        if (matchedHouseItem != null) {
                            matchedCount++;
                        }
                    }
                }
                
                // 단지정보로 housing 업데이트
                if (matchedHouseItem != null) {
                    updateHousingWithHouseInfo(housing, matchedHouseItem);
                    housingRepository.save(housing);
                    updatedCount++;
                }
            } catch (Exception e) {
                System.err.println("Housing 업데이트 실패: " + 
                        (housing.getName() != null ? housing.getName() : "알 수 없음") + 
                        " - " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        System.out.println("========================================");
        System.out.println("단지정보로 Housing 업데이트 완료:");
        System.out.println("  - DB housing 데이터: " + savedHousings.size() + "건");
        System.out.println("  - 단지정보 API 데이터: " + houseItems.size() + "건");
        System.out.println("  - 매칭 성공: " + matchedCount + "건");
        System.out.println("  - 업데이트: " + updatedCount + "건");
        System.out.println("========================================");
    }

    /**
     * 단지명으로 단지정보 API Item 찾기 (퍼지 매칭 포함)
     * @param housingName 단지명
     * @param houseMap 단지정보 API 데이터 Map (단지명 -> Item)
     * @return 매칭된 Item, 없으면 null
     */
    private LHRentalHouseListResponse.Item findMatchingHouseItemByName(
            String housingName,
            Map<String, LHRentalHouseListResponse.Item> houseMap) {
        
        if (housingName == null || housingName.isEmpty() || houseMap == null || houseMap.isEmpty()) {
            return null;
        }
        
        // 1순위: 정확히 일치하는 경우
        if (houseMap.containsKey(housingName)) {
            return houseMap.get(housingName);
        }
        
        // 2순위: 정규화된 이름으로 일치하는 경우
        String normalizedHousingName = normalizeHousingName(housingName);
        for (Map.Entry<String, LHRentalHouseListResponse.Item> entry : houseMap.entrySet()) {
            String houseName = entry.getKey();
            String normalizedHouseName = normalizeHousingName(houseName);
            
            if (normalizedHousingName.equals(normalizedHouseName)) {
                return entry.getValue();
            }
        }
        
        // 3순위: 퍼지 매칭 (유사도 0.8 이상)
        double bestSimilarity = 0.0;
        LHRentalHouseListResponse.Item bestMatch = null;
        
        for (Map.Entry<String, LHRentalHouseListResponse.Item> entry : houseMap.entrySet()) {
            String houseName = entry.getKey();
            double similarity = calculateSimilarity(housingName, houseName);
            
            if (similarity >= 0.8 && similarity > bestSimilarity) {
                bestSimilarity = similarity;
                bestMatch = entry.getValue();
            }
        }
        
        if (bestMatch != null) {
            System.out.println("✅ 단지정보 퍼지 매칭 성공: '" + housingName + "' <-> '" + 
                    (bestMatch.getHsmpNm() != null ? bestMatch.getHsmpNm() : "알 수 없음") + 
                    "' (유사도: " + String.format("%.2f", bestSimilarity) + ")");
        }
        
        return bestMatch;
    }

    /**
     * 단지정보 API 데이터로 Housing 업데이트 (나머지 필드 채우기)
     * @param housing DB에 저장된 housing
     * @param houseItem 단지정보 API Item
     */
    private void updateHousingWithHouseInfo(Housing housing, LHRentalHouseListResponse.Item houseItem) {
        // 주소 업데이트 (없는 경우에만)
        if ((housing.getAddress() == null || housing.getAddress().isEmpty()) &&
            houseItem.getRnAdres() != null && !houseItem.getRnAdres().isEmpty()) {
            housing.setAddress(houseItem.getRnAdres());
        }
        
        // 공급 면적 업데이트
        if (houseItem.getSuplyPrvuseAr() != null && !houseItem.getSuplyPrvuseAr().isEmpty()) {
            try {
                housing.setSupplyArea(Double.parseDouble(houseItem.getSuplyPrvuseAr()));
            } catch (NumberFormatException e) {
                // 파싱 실패 시 무시
            }
        }
        
        // 준공 일자 업데이트
        if (houseItem.getCompetDe() != null && !houseItem.getCompetDe().isEmpty()) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
                java.util.Date date = sdf.parse(houseItem.getCompetDe());
                housing.setCompleteDate(new Date(date.getTime()));
            } catch (ParseException e) {
                // 파싱 실패 시 무시
            }
        }
        
        // 기관명 업데이트 (없는 경우에만)
        if ((housing.getOrganization() == null || housing.getOrganization().isEmpty()) &&
            houseItem.getInsttNm() != null && !houseItem.getInsttNm().isEmpty()) {
            housing.setOrganization(houseItem.getInsttNm());
        }
        
        // 난방 방식 업데이트
        if (houseItem.getHeatMthdDetailNm() != null && !houseItem.getHeatMthdDetailNm().isEmpty()) {
            housing.setHeatingType(houseItem.getHeatMthdDetailNm());
        }
        
        // 엘리베이터 업데이트
        if (houseItem.getElvtrInstlAtNm() != null) {
            housing.setElevator(houseItem.getElvtrInstlAtNm().contains("설치") || 
                               houseItem.getElvtrInstlAtNm().contains("전체"));
        }
        
        // 주차수 업데이트
        if (houseItem.getParkngCo() != null && !houseItem.getParkngCo().isEmpty()) {
            try {
                housing.setParkingSpaces(Integer.parseInt(houseItem.getParkngCo()));
            } catch (NumberFormatException e) {
                // 파싱 실패 시 무시
            }
        }
        
        // 기본 임대보증금 업데이트
        if (houseItem.getBassRentGtn() != null && !houseItem.getBassRentGtn().isEmpty()) {
            try {
                housing.setDeposit(Integer.parseInt(houseItem.getBassRentGtn()));
            } catch (NumberFormatException e) {
                // 파싱 실패 시 무시
            }
        }
        
        // 기본 월임대료 업데이트
        if (houseItem.getBassMtRntchrg() != null && !houseItem.getBassMtRntchrg().isEmpty()) {
            try {
                housing.setMonthlyRent(Integer.parseInt(houseItem.getBassMtRntchrg()));
            } catch (NumberFormatException e) {
                // 파싱 실패 시 무시
            }
        }
        
        // 세대수 업데이트
        if (houseItem.getHshldCo() != null && !houseItem.getHshldCo().isEmpty()) {
            try {
                housing.setTotalUnits(Integer.parseInt(houseItem.getHshldCo()));
            } catch (NumberFormatException e) {
                // 파싱 실패 시 무시
            }
        }
        
        // 주택유형 업데이트 (없는 경우에만)
        if ((housing.getHousingType() == null || housing.getHousingType().isEmpty()) &&
            houseItem.getSuplyTyNm() != null && !houseItem.getSuplyTyNm().isEmpty()) {
            housing.setHousingType(houseItem.getSuplyTyNm());
        }
    }

    /**
     * 단지정보와 공고문 데이터를 병합하여 DB에 저장 (트랜잭션 적용)
     */
    @Transactional
    private void saveHousingDataWithNotices(
            List<LHRentalHouseListResponse.Item> houseItems,
            Map<String, LHRentalNoticeResponse.Item> noticeMapByHsmpSn,
            Map<String, LHRentalNoticeResponse.Item> noticeMapByHsmpNm) {
        
        int savedCount = 0;
        int updatedCount = 0;
        int matchedCount = 0; // 공고문과 매칭된 건수
        
        for (LHRentalHouseListResponse.Item item : houseItems) {
            try {
                Housing housing = convertToHousing(item);
                
                // 공고문 데이터와 병합 (단지 식별자 또는 단지명으로 매칭)
                String housingId = housing.getHousingId(); // hsmpSn
                String housingName = housing.getName(); // hsmpNm
                
                LHRentalNoticeResponse.Item matchedNotice = null;
                
                // 1순위: hsmpSn (단지 식별자)로 매칭
                if (housingId != null && noticeMapByHsmpSn.containsKey(housingId)) {
                    matchedNotice = noticeMapByHsmpSn.get(housingId);
                    matchedCount++;
                }
                // 2순위: hsmpNm (단지명)으로 매칭 (퍼지 매칭 포함)
                else if (housingName != null) {
                    matchedNotice = findMatchingNoticeByName(housingName, noticeMapByHsmpNm);
                    if (matchedNotice != null) {
                        matchedCount++;
                    }
                }
                
                // 공고문 데이터로 보완
                if (matchedNotice != null) {
                    updateHousingWithNotice(housing, matchedNotice);
                }
                
                // DB에 저장 (이미 존재하면 업데이트)
                boolean exists = housingRepository.existsById(housing.getHousingId());
                housingRepository.findById(housing.getHousingId())
                        .ifPresentOrElse(
                                existing -> {
                                    // 기존 데이터 업데이트
                                    updateHousingData(existing, housing);
                                    housingRepository.save(existing);
                                },
                                () -> {
                                    // 새 데이터 저장
                                    housingRepository.save(housing);
                                }
                        );
                if (exists) {
                    updatedCount++;
                } else {
                    savedCount++;
                }
            } catch (Exception e) {
                System.err.println("단지정보 데이터 저장 실패: " + 
                        (item.getHsmpNm() != null ? item.getHsmpNm() : "알 수 없음") + 
                        " - " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        System.out.println("========================================");
        System.out.println("임대주택 데이터 동기화 완료:");
        System.out.println("  - 단지정보: " + houseItems.size() + "건");
        System.out.println("  - 공고문 매칭: " + matchedCount + "건");
        System.out.println("  - 저장: " + savedCount + "건, 업데이트: " + updatedCount + "건");
        System.out.println("========================================");
    }

    /**
     * 공고문 데이터를 housing_notice 테이블에 저장 (트랜잭션 적용)
     */
    @Transactional
    private void saveNoticeData(List<LHRentalNoticeResponse.Item> noticeItems) {
        int savedCount = 0;
        int updatedCount = 0;
        int skippedExpiredCount = 0;
        java.time.LocalDate today = java.time.LocalDate.now();
        
        for (LHRentalNoticeResponse.Item notice : noticeItems) {
            try {
                HousingNotice housingNotice = convertToHousingNotice(notice);
                
                // 이미 마감된(마감일 < 오늘) 공고는 저장하지 않는다. (만료 데이터 재유입 방지)
                // 마감일(applicationEnd)이 null인 상시 공고는 보존한다.
                if (housingNotice.getApplicationEnd() != null
                        && housingNotice.getApplicationEnd().toLocalDate().isBefore(today)) {
                    skippedExpiredCount++;
                    continue;
                }
                
                // DB에 저장 (이미 존재하면 업데이트)
                boolean exists = housingNoticeRepository.existsById(housingNotice.getNoticeId());
                housingNoticeRepository.findById(housingNotice.getNoticeId())
                        .ifPresentOrElse(
                                existing -> {
                                    // 기존 데이터 업데이트
                                    updateHousingNotice(existing, housingNotice);
                                    housingNoticeRepository.save(existing);
                                },
                                () -> {
                                    // 새 데이터 저장
                                    housingNoticeRepository.save(housingNotice);
                                }
                        );
                if (exists) {
                    updatedCount++;
                } else {
                    savedCount++;
                }
            } catch (Exception e) {
                System.err.println("공고문 데이터 저장 실패: " + 
                        (notice.getHsmpNm() != null ? notice.getHsmpNm() : "알 수 없음") + 
                        " - " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        System.out.println("========================================");
        System.out.println("공고문 데이터 저장 완료:");
        System.out.println("  - 총 공고문: " + noticeItems.size() + "건");
        System.out.println("  - 저장: " + savedCount + "건, 업데이트: " + updatedCount + "건");
        System.out.println("  - 마감 제외(스킵): " + skippedExpiredCount + "건");
        System.out.println("========================================");
    }
    
    /**
     * 단지정보 데이터를 housing_complex 테이블에 저장 (트랜잭션 적용)
     */
    @Transactional
    private void saveComplexData(List<LHRentalHouseListResponse.Item> houseItems) {
        int savedCount = 0;
        int updatedCount = 0;
        
        for (LHRentalHouseListResponse.Item item : houseItems) {
            try {
                HousingComplex housingComplex = convertToHousingComplex(item);
                
                // DB에 저장 (이미 존재하면 업데이트)
                boolean exists = housingComplexRepository.existsById(housingComplex.getComplexId());
                housingComplexRepository.findById(housingComplex.getComplexId())
                        .ifPresentOrElse(
                                existing -> {
                                    // 기존 데이터 업데이트
                                    updateHousingComplex(existing, housingComplex);
                                    housingComplexRepository.save(existing);
                                },
                                () -> {
                                    // 새 데이터 저장
                                    housingComplexRepository.save(housingComplex);
                                }
                        );
                if (exists) {
                    updatedCount++;
                } else {
                    savedCount++;
                }
            } catch (Exception e) {
                System.err.println("단지정보 데이터 저장 실패: " + 
                        (item.getHsmpNm() != null ? item.getHsmpNm() : "알 수 없음") + 
                        " - " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        System.out.println("========================================");
        System.out.println("단지정보 데이터 저장 완료:");
        System.out.println("  - 총 단지정보: " + houseItems.size() + "건");
        System.out.println("  - 저장: " + savedCount + "건, 업데이트: " + updatedCount + "건");
        System.out.println("========================================");
    }

    /**
     * 기존 공고문(housing_notice)을 상세정보(lhLeaseNoticeDtlInfo1) + 공급정보(lhLeaseNoticeSplInfo1) API로 보강.
     * 주소/전용면적/세대수/난방방식/입주예정월/공고내용/대표공급가를 채운다.
     * 각 공고를 개별 트랜잭션(save)으로 커밋하므로 중간 실패해도 진행분은 보존된다.
     *
     * @param limit 처리할 최대 공고 수 (0 이하이면 전체)
     * @param onlyMissing true이면 이미 주소(rnAdres)가 채워진 공고는 건너뜀
     */
    public Map<String, Object> enrichNoticesWithDetailInfo(int limit, boolean onlyMissing) {
        List<HousingNotice> notices = housingNoticeRepository.findAll();
        int processed = 0;
        int enriched = 0;
        int skipped = 0;
        int failed = 0;

        System.out.println("========================================");
        System.out.println("공고 상세/공급 정보 보강 시작 - 대상 공고: " + notices.size() + "건"
                + " (limit=" + limit + ", onlyMissing=" + onlyMissing + ")");

        for (HousingNotice notice : notices) {
            if (limit > 0 && processed >= limit) {
                break;
            }

            // 상세/공급 API 호출 키가 없으면 조회 불가 → 건너뜀
            if (isBlank(notice.getPanId()) || isBlank(notice.getCcrCnntSysDsCd())
                    || isBlank(notice.getSplInfTpCd()) || isBlank(notice.getUppAisTpCd())) {
                skipped++;
                continue;
            }

            // 이미 주소가 있는 공고는 건너뜀 (onlyMissing 모드)
            if (onlyMissing && !isBlank(notice.getRnAdres())) {
                skipped++;
                continue;
            }

            processed++;
            try {
                LHNoticeEnrichment e = publicDataApiService.getLeaseNoticeEnrichment(
                        notice.getPanId(),
                        notice.getCcrCnntSysDsCd(),
                        notice.getSplInfTpCd(),
                        notice.getUppAisTpCd(),
                        notice.getAisTpCd());

                if (e != null && e.hasAnyData()) {
                    if (e.getRnAdres() != null) notice.setRnAdres(e.getRnAdres());
                    if (e.getScAr() != null) notice.setScAr(e.getScAr());
                    if (e.getHshldCo() != null) notice.setHshldCo(e.getHshldCo());
                    if (e.getHtnFmlaNm() != null) notice.setHtnFmlaNm(e.getHtnFmlaNm());
                    if (e.getMvinXpcYm() != null) notice.setMvinXpcYm(e.getMvinXpcYm());
                    if (e.getPanDtlCts() != null) notice.setPanDtlCts(e.getPanDtlCts());
                    if (e.getSplXpcAmt() != null) notice.setSplXpcAmt(e.getSplXpcAmt());
                    housingNoticeRepository.save(notice);
                    enriched++;
                }
            } catch (Exception ex) {
                failed++;
                System.err.println("공고 보강 실패 (panId=" + notice.getPanId() + "): " + ex.getMessage());
            }

            if (processed % 50 == 0) {
                System.out.println("  ...진행 중: processed=" + processed + ", enriched=" + enriched
                        + ", skipped=" + skipped + ", failed=" + failed);
            }

            // 외부 API 부하 완화를 위한 짧은 지연
            try {
                Thread.sleep(50);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("totalNotices", notices.size());
        result.put("processed", processed);
        result.put("enriched", enriched);
        result.put("skipped", skipped);
        result.put("failed", failed);

        System.out.println("공고 상세/공급 정보 보강 완료: " + result);
        System.out.println("========================================");
        return result;
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    /**
     * 공고문 API Item을 HousingNotice 엔티티로 변환
     */
    private HousingNotice convertToHousingNotice(LHRentalNoticeResponse.Item notice) {
        // noticeId 생성: panId가 있으면 panId, 없으면 hsmpSn + panNm 조합
        String noticeId = notice.getPanId() != null && !notice.getPanId().isEmpty()
                ? notice.getPanId()
                : (notice.getHsmpSn() != null ? notice.getHsmpSn() : "") + 
                  (notice.getPanNm() != null ? notice.getPanNm() : "");
        
        if (noticeId.isEmpty()) {
            noticeId = "NOTICE_" + System.currentTimeMillis() + "_" + (notice.getHsmpNm() != null ? notice.getHsmpNm().hashCode() : 0);
        }
        
        // hsmpSn과 hsmpNm이 API 응답에 없을 수 있으므로, 공고명에서 추출 시도
        String hsmpSn = notice.getHsmpSn();
        String hsmpNm = notice.getHsmpNm();
        
        // hsmpNm이 없으면 공고명에서 단지명 추출 시도
        if ((hsmpNm == null || hsmpNm.isEmpty()) && notice.getPanNm() != null && !notice.getPanNm().isEmpty()) {
            // 공고명 예: "2024년 LH공사 공급주택 공고 (서울 양천구 목동단지)"
            // 단지명은 보통 괄호 안에 있거나 특정 패턴을 따름
            String panNm = notice.getPanNm();
            // 괄호 안의 내용 추출
            if (panNm.contains("(") && panNm.contains(")")) {
                int start = panNm.indexOf("(") + 1;
                int end = panNm.indexOf(")");
                if (start < end) {
                    String extracted = panNm.substring(start, end).trim();
                    // "단지", "아파트" 등이 포함된 경우만 단지명으로 인정
                    if (extracted.contains("단지") || extracted.contains("아파트") || extracted.contains("주택")) {
                        hsmpNm = extracted;
                    }
                }
            }
        }
        
        HousingNotice.HousingNoticeBuilder builder = HousingNotice.builder()
                .noticeId(noticeId)
                .hsmpSn(hsmpSn)
                .hsmpNm(hsmpNm)
                .panId(notice.getPanId())
                .panNm(notice.getPanNm())
                .dtlUrl(notice.getDtlUrl())
                .panNtStDt(notice.getPanNtStDt())
                .clsgDt(notice.getClsgDt())
                .panDt(notice.getPanDt())
                .cnpCd(notice.getCnpCd())
                .cnpCdNm(notice.getCnpCdNm())
                .uppAisTpCd(notice.getUppAisTpCd())
                .uppAisTpNm(notice.getUppAisTpNm())
                .aisTpCd(notice.getAisTpCd())
                .aisTpCdNm(notice.getAisTpCdNm())
                .panSs(notice.getPanSs())
                .ccrCnntSysDsCd(notice.getCcrCnntSysDsCd())
                .splInfTpCd(notice.getSplInfTpCd())
                .allCnt(notice.getAllCnt());
        
        // 신청 시작일 파싱
        if (notice.getRceptBgnde() != null && !notice.getRceptBgnde().isEmpty()) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
                java.util.Date date = sdf.parse(notice.getRceptBgnde());
                builder.applicationStart(new Date(date.getTime()));
            } catch (ParseException e) {
                // 파싱 실패 시 무시
            }
        }
        
        // 신청 종료일 파싱
        if (notice.getRceptEndde() != null && !notice.getRceptEndde().isEmpty()) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
                java.util.Date date = sdf.parse(notice.getRceptEndde());
                builder.applicationEnd(new Date(date.getTime()));
            } catch (ParseException e) {
                // 파싱 실패 시 무시
            }
        }
        
        return builder.build();
    }

    /**
     * 단지정보 API Item을 HousingComplex 엔티티로 변환
     */
    private HousingComplex convertToHousingComplex(LHRentalHouseListResponse.Item item) {
        // complexId는 hsmpSn 사용
        String complexId = item.getHsmpSn() != null && !item.getHsmpSn().isEmpty()
                ? item.getHsmpSn()
                : "COMPLEX_" + System.currentTimeMillis() + "_" + (item.getHsmpNm() != null ? item.getHsmpNm().hashCode() : 0);
        
        HousingComplex.HousingComplexBuilder builder = HousingComplex.builder()
                .complexId(complexId)
                .hsmpNm(item.getHsmpNm())
                .insttNm(item.getInsttNm())
                .brtcCode(item.getBrtcCode())
                .brtcNm(item.getBrtcNm())
                .signguCode(item.getSignguCode())
                .signguNm(item.getSignguNm())
                .rnAdres(item.getRnAdres())
                .pnu(item.getPnu())
                .competDe(item.getCompetDe())
                .hshldCo(item.getHshldCo())
                .suplyTyNm(item.getSuplyTyNm())
                .styleNm(item.getStyleNm())
                .suplyPrvuseAr(item.getSuplyPrvuseAr())
                .suplyCmnuseAr(item.getSuplyCmnuseAr())
                .houseTyNm(item.getHouseTyNm())
                .heatMthdDetailNm(item.getHeatMthdDetailNm())
                .buldStleNm(item.getBuldStleNm())
                .elvtrInstlAtNm(item.getElvtrInstlAtNm())
                .parkngCo(item.getParkngCo())
                .bassRentGtn(item.getBassRentGtn())
                .bassMtRntchrg(item.getBassMtRntchrg())
                .bassCnvrsGtnLmt(item.getBassCnvrsGtnLmt());
        
        // 준공 일자 파싱
        if (item.getCompetDe() != null && !item.getCompetDe().isEmpty()) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
                java.util.Date date = sdf.parse(item.getCompetDe());
                builder.completeDate(new Date(date.getTime()));
            } catch (ParseException e) {
                // 파싱 실패 시 무시
            }
        }
        
        // 공급 면적 파싱
        if (item.getSuplyPrvuseAr() != null && !item.getSuplyPrvuseAr().isEmpty()) {
            try {
                builder.supplyArea(Double.parseDouble(item.getSuplyPrvuseAr()));
            } catch (NumberFormatException e) {
                // 파싱 실패 시 무시
            }
        }
        
        // 세대수 파싱
        if (item.getHshldCo() != null && !item.getHshldCo().isEmpty()) {
            try {
                builder.totalUnits(Integer.parseInt(item.getHshldCo()));
            } catch (NumberFormatException e) {
                // 파싱 실패 시 무시
            }
        }
        
        // 승강기 설치여부 파싱
        if (item.getElvtrInstlAtNm() != null) {
            builder.elevator(item.getElvtrInstlAtNm().contains("설치") || 
                           item.getElvtrInstlAtNm().contains("전체"));
        }
        
        // 주차수 파싱
        if (item.getParkngCo() != null && !item.getParkngCo().isEmpty()) {
            try {
                builder.parkingSpaces(Integer.parseInt(item.getParkngCo()));
            } catch (NumberFormatException e) {
                // 파싱 실패 시 무시
            }
        }
        
        // 기본 임대보증금 파싱
        if (item.getBassRentGtn() != null && !item.getBassRentGtn().isEmpty()) {
            try {
                builder.deposit(Integer.parseInt(item.getBassRentGtn()));
            } catch (NumberFormatException e) {
                // 파싱 실패 시 무시
            }
        }
        
        // 기본 월임대료 파싱
        if (item.getBassMtRntchrg() != null && !item.getBassMtRntchrg().isEmpty()) {
            try {
                builder.monthlyRent(Integer.parseInt(item.getBassMtRntchrg()));
            } catch (NumberFormatException e) {
                // 파싱 실패 시 무시
            }
        }
        
        return builder.build();
    }

    /**
     * HousingNotice 업데이트
     */
    private void updateHousingNotice(HousingNotice existing, HousingNotice newData) {
        existing.setHsmpSn(newData.getHsmpSn());
        existing.setHsmpNm(newData.getHsmpNm());
        existing.setPanId(newData.getPanId());
        existing.setPanNm(newData.getPanNm());
        existing.setDtlUrl(newData.getDtlUrl());
        existing.setPanNtStDt(newData.getPanNtStDt());
        existing.setClsgDt(newData.getClsgDt());
        existing.setPanDt(newData.getPanDt());
        existing.setApplicationStart(newData.getApplicationStart());
        existing.setApplicationEnd(newData.getApplicationEnd());
        existing.setCnpCd(newData.getCnpCd());
        existing.setCnpCdNm(newData.getCnpCdNm());
        existing.setUppAisTpCd(newData.getUppAisTpCd());
        existing.setUppAisTpNm(newData.getUppAisTpNm());
        existing.setAisTpCd(newData.getAisTpCd());
        existing.setAisTpCdNm(newData.getAisTpCdNm());
        existing.setPanSs(newData.getPanSs());
        existing.setCcrCnntSysDsCd(newData.getCcrCnntSysDsCd());
        existing.setSplInfTpCd(newData.getSplInfTpCd());
        existing.setAllCnt(newData.getAllCnt());
        // 주의: rnAdres/scAr/hshldCo 등 enrichment 필드는 상세정보 API로만 채우므로 여기서 덮어쓰지 않는다.
    }

    /**
     * HousingComplex 업데이트
     */
    private void updateHousingComplex(HousingComplex existing, HousingComplex newData) {
        existing.setHsmpNm(newData.getHsmpNm());
        existing.setInsttNm(newData.getInsttNm());
        existing.setBrtcCode(newData.getBrtcCode());
        existing.setBrtcNm(newData.getBrtcNm());
        existing.setSignguCode(newData.getSignguCode());
        existing.setSignguNm(newData.getSignguNm());
        existing.setRnAdres(newData.getRnAdres());
        existing.setPnu(newData.getPnu());
        existing.setCompetDe(newData.getCompetDe());
        existing.setCompleteDate(newData.getCompleteDate());
        existing.setHshldCo(newData.getHshldCo());
        existing.setTotalUnits(newData.getTotalUnits());
        existing.setSuplyTyNm(newData.getSuplyTyNm());
        existing.setStyleNm(newData.getStyleNm());
        existing.setSuplyPrvuseAr(newData.getSuplyPrvuseAr());
        existing.setSupplyArea(newData.getSupplyArea());
        existing.setSuplyCmnuseAr(newData.getSuplyCmnuseAr());
        existing.setHouseTyNm(newData.getHouseTyNm());
        existing.setHeatMthdDetailNm(newData.getHeatMthdDetailNm());
        existing.setBuldStleNm(newData.getBuldStleNm());
        existing.setElvtrInstlAtNm(newData.getElvtrInstlAtNm());
        existing.setElevator(newData.getElevator());
        existing.setParkngCo(newData.getParkngCo());
        existing.setParkingSpaces(newData.getParkingSpaces());
        existing.setBassRentGtn(newData.getBassRentGtn());
        existing.setDeposit(newData.getDeposit());
        existing.setBassMtRntchrg(newData.getBassMtRntchrg());
        existing.setMonthlyRent(newData.getMonthlyRent());
        existing.setBassCnvrsGtnLmt(newData.getBassCnvrsGtnLmt());
        if (newData.getLatitude() != null && newData.getLongitude() != null) {
            existing.setLatitude(newData.getLatitude());
            existing.setLongitude(newData.getLongitude());
        }
    }

    /**
     * 공고문 데이터만으로 Housing 엔티티 생성 및 DB에 저장 (트랜잭션 적용)
     * @deprecated 새로운 구조에서는 saveNoticeData 사용
     */
    @Deprecated
    @Transactional
    private void saveNoticeDataOnly(
            Map<String, LHRentalNoticeResponse.Item> noticeMapByHsmpSn,
            Map<String, LHRentalNoticeResponse.Item> noticeMapByHsmpNm) {
        
        int savedCount = 0;
        int updatedCount = 0;
        
        // hsmpSn 기준 공고문 처리
        for (LHRentalNoticeResponse.Item notice : noticeMapByHsmpSn.values()) {
            try {
                Housing housing = convertNoticeToHousing(notice);
                
                // DB에 저장 (이미 존재하면 업데이트)
                boolean exists = housingRepository.existsById(housing.getHousingId());
                housingRepository.findById(housing.getHousingId())
                        .ifPresentOrElse(
                                existing -> {
                                    // 기존 데이터 업데이트
                                    updateHousingDataFromNotice(existing, housing);
                                    housingRepository.save(existing);
                                },
                                () -> {
                                    // 새 데이터 저장
                                    housingRepository.save(housing);
                                }
                        );
                if (exists) {
                    updatedCount++;
                } else {
                    savedCount++;
                }
            } catch (Exception e) {
                System.err.println("공고문 데이터 저장 실패: " + 
                        (notice.getHsmpNm() != null ? notice.getHsmpNm() : "알 수 없음") + 
                        " - " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        // hsmpNm 기준 공고문 처리 (hsmpSn이 없는 경우만)
        for (LHRentalNoticeResponse.Item notice : noticeMapByHsmpNm.values()) {
            try {
                Housing housing = convertNoticeToHousing(notice);
                
                // DB에 저장 (이미 존재하면 업데이트)
                boolean exists = housingRepository.existsById(housing.getHousingId());
                housingRepository.findById(housing.getHousingId())
                        .ifPresentOrElse(
                                existing -> {
                                    // 기존 데이터 업데이트
                                    updateHousingDataFromNotice(existing, housing);
                                    housingRepository.save(existing);
                                },
                                () -> {
                                    // 새 데이터 저장
                                    housingRepository.save(housing);
                                }
                        );
                if (exists) {
                    updatedCount++;
                } else {
                    savedCount++;
                }
            } catch (Exception e) {
                System.err.println("공고문 데이터 저장 실패: " + 
                        (notice.getHsmpNm() != null ? notice.getHsmpNm() : "알 수 없음") + 
                        " - " + e.getMessage());
            }
        }
        
        System.out.println("========================================");
        System.out.println("임대주택 데이터 동기화 완료 (공고문만):");
        System.out.println("  - 공고문 (hsmpSn 기준): " + noticeMapByHsmpSn.size() + "건");
        System.out.println("  - 공고문 (hsmpNm 기준): " + noticeMapByHsmpNm.size() + "건");
        System.out.println("  - 저장: " + savedCount + "건, 업데이트: " + updatedCount + "건");
        System.out.println("========================================");
    }

    /**
     * Housing 리스트를 DB에 저장
     */
    private void saveHousingList(List<LHRentalHouseListResponse.Item> items, Map<String, LHRentalNoticeResponse.Item> noticeMap) {
        List<Housing> housingList = items.stream()
                .map(item -> {
                    Housing housing = convertToHousing(item);
                    if (noticeMap != null && noticeMap.containsKey(housing.getHousingId())) {
                        updateHousingWithNotice(housing, noticeMap.get(housing.getHousingId()));
                    }
                    return housing;
                })
                .collect(Collectors.toList());
        
        for (Housing housing : housingList) {
            housingRepository.findById(housing.getHousingId())
                    .ifPresentOrElse(
                            existing -> {
                                updateHousingData(existing, housing);
                                housingRepository.save(existing);
                            },
                            () -> {
                                housingRepository.save(housing);
                            }
                    );
        }
    }

    /**
     * 공고문 데이터로 Housing 업데이트 (신청 기간 등)
     */
    private void updateHousingWithNotice(Housing housing, LHRentalNoticeResponse.Item notice) {
        // 신청 시작일 파싱 (PAN_NT_ST_DT: 공고게시일)
        if (notice.getRceptBgnde() != null && !notice.getRceptBgnde().isEmpty()) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
                java.util.Date date = sdf.parse(notice.getRceptBgnde());
                housing.setApplicationStart(new Date(date.getTime()));
            } catch (ParseException e) {
                // 파싱 실패 시 무시
            }
        }
        
        // 신청 종료일 파싱 (CLSG_DT: 공고마감일)
        if (notice.getRceptEndde() != null && !notice.getRceptEndde().isEmpty()) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
                java.util.Date date = sdf.parse(notice.getRceptEndde());
                housing.setApplicationEnd(new Date(date.getTime()));
            } catch (ParseException e) {
                // 파싱 실패 시 무시
            }
        }
        
        // 공고 URL이 있으면 link 업데이트 (공고문 API의 URL 우선)
        if (notice.getDtlUrl() != null && !notice.getDtlUrl().isEmpty()) {
            housing.setLink(notice.getDtlUrl());
        } else if (notice.getPblancUrl() != null && !notice.getPblancUrl().isEmpty()) {
            housing.setLink(notice.getPblancUrl());
        }
        
        // 주택유형이 없으면 공고문의 주택유형 사용
        if ((housing.getHousingType() == null || housing.getHousingType().isEmpty()) &&
            notice.getAisTpCdNm() != null && !notice.getAisTpCdNm().isEmpty()) {
            housing.setHousingType(notice.getAisTpCdNm());
        }
        
        // 단지명이 없으면 공고문의 단지명 사용
        if ((housing.getName() == null || housing.getName().isEmpty()) &&
            notice.getHsmpNm() != null && !notice.getHsmpNm().isEmpty()) {
            String name = notice.getHsmpNm();
            if (name.length() > 255) {
                name = name.substring(0, 255);
            }
            housing.setName(name);
        }
    }

    /**
     * 공고문 데이터를 Housing 엔티티로 변환
     */
    private Housing convertNoticeToHousing(LHRentalNoticeResponse.Item notice) {
        // 단지 식별자, 공고번호, 또는 공고ID를 housingId로 사용
        String housingId = notice.getHsmpSn() != null && !notice.getHsmpSn().isEmpty()
                ? notice.getHsmpSn()
                : (notice.getPblancNo() != null ? notice.getPblancNo() 
                   : (notice.getPanId() != null ? notice.getPanId() 
                      : generateHousingId(notice.getBrtcCode(), notice.getSignguCode(), notice.getPanNm() != null ? notice.getPanNm() : "")));

        // name 필드 (최대 255자로 제한)
        String name = notice.getHsmpNm() != null && !notice.getHsmpNm().isEmpty() ? notice.getHsmpNm() : 
                      (notice.getPanNm() != null ? notice.getPanNm() : 
                       (notice.getPblancNm() != null ? notice.getPblancNm() : ""));
        if (name != null && name.length() > 255) {
            name = name.substring(0, 255);
        }
        
        Housing.HousingBuilder builder = Housing.builder()
                .housingId(housingId)
                .name(name)
                .address(buildAddress(notice.getBrtcNm(), notice.getSignguNm()))
                .organization(notice.getInsttNm() != null ? notice.getInsttNm() : "")
                .link(notice.getDtlUrl() != null ? notice.getDtlUrl() : 
                      (notice.getPblancUrl() != null ? notice.getPblancUrl() : ""))
                .housingType(notice.getSuplyTyNm() != null ? notice.getSuplyTyNm() : 
                            (notice.getAisTpCdNm() != null ? notice.getAisTpCdNm() : ""));

        // 신청 시작일 파싱
        if (notice.getRceptBgnde() != null && !notice.getRceptBgnde().isEmpty()) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
                java.util.Date date = sdf.parse(notice.getRceptBgnde());
                builder.applicationStart(new Date(date.getTime()));
            } catch (ParseException e) {
                // 파싱 실패 시 무시
            }
        }

        // 신청 종료일 파싱
        if (notice.getRceptEndde() != null && !notice.getRceptEndde().isEmpty()) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
                java.util.Date date = sdf.parse(notice.getRceptEndde());
                builder.applicationEnd(new Date(date.getTime()));
            } catch (ParseException e) {
                // 파싱 실패 시 무시
            }
        }

        // 주소를 기반으로 좌표 조회 (카카오맵 API 사용)
        // 리액티브 스레드에서 block() 호출 방지를 위해 지오코딩 생략
        // 필요시 별도 스레드에서 비동기로 처리하거나 배치 작업으로 처리
        builder.latitude(null);
        builder.longitude(null);

        return builder.build();
    }

    /**
     * 주소 문자열 생성 (광역시도명 + 시군구명)
     */
    private String buildAddress(String brtcNm, String signguNm) {
        StringBuilder address = new StringBuilder();
        if (brtcNm != null && !brtcNm.isEmpty()) {
            address.append(brtcNm);
        }
        if (signguNm != null && !signguNm.isEmpty()) {
            if (address.length() > 0) {
                address.append(" ");
            }
            address.append(signguNm);
        }
        return address.toString();
    }

    /**
     * 공고문 데이터로 기존 Housing 업데이트
     */
    private void updateHousingDataFromNotice(Housing existing, Housing newData) {
        // 이름 업데이트 (없는 경우에만)
        if ((existing.getName() == null || existing.getName().isEmpty()) && 
            newData.getName() != null && !newData.getName().isEmpty()) {
            existing.setName(newData.getName());
        }
        // 주소 업데이트 (없는 경우에만)
        if ((existing.getAddress() == null || existing.getAddress().isEmpty()) && 
            newData.getAddress() != null && !newData.getAddress().isEmpty()) {
            existing.setAddress(newData.getAddress());
        }
        // 기관명 업데이트
        if (newData.getOrganization() != null && !newData.getOrganization().isEmpty()) {
            existing.setOrganization(newData.getOrganization());
        }
        // 링크 업데이트
        if (newData.getLink() != null && !newData.getLink().isEmpty()) {
            existing.setLink(newData.getLink());
        }
        // 주택유형 업데이트
        if (newData.getHousingType() != null && !newData.getHousingType().isEmpty()) {
            existing.setHousingType(newData.getHousingType());
        }
        // 신청 기간 업데이트
        if (newData.getApplicationStart() != null) {
            existing.setApplicationStart(newData.getApplicationStart());
        }
        if (newData.getApplicationEnd() != null) {
            existing.setApplicationEnd(newData.getApplicationEnd());
        }
        // 좌표 업데이트 (없는 경우에만)
        if (existing.getLatitude() == null && existing.getLongitude() == null &&
            newData.getLatitude() != null && newData.getLongitude() != null) {
            existing.setLatitude(newData.getLatitude());
            existing.setLongitude(newData.getLongitude());
        }
    }

    /**
     * 공고문이 더 최신인지 확인
     */
    private boolean isNewerNotice(LHRentalNoticeResponse.Item newNotice, LHRentalNoticeResponse.Item existingNotice) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
            String newDate = newNotice.getPblancDe() != null ? newNotice.getPblancDe() : newNotice.getRceptBgnde();
            String existingDate = existingNotice.getPblancDe() != null ? existingNotice.getPblancDe() : existingNotice.getRceptBgnde();
            
            if (newDate != null && existingDate != null) {
                return sdf.parse(newDate).after(sdf.parse(existingDate));
            }
        } catch (Exception e) {
            // 파싱 실패 시 false 반환
        }
        return false;
    }

    /**
     * LH API 응답 Item을 Housing 엔티티로 변환
     */
    private Housing convertToHousing(LHRentalHouseListResponse.Item item) {
        // 단지 식별자를 housingId로 사용
        String housingId = item.getHsmpSn() != null ? item.getHsmpSn() : 
                generateHousingId(item.getBrtcCode(), item.getSignguCode(), item.getHsmpNm());

        // name 필드 (최대 255자로 제한)
        String name = item.getHsmpNm() != null ? item.getHsmpNm() : "";
        if (name != null && name.length() > 255) {
            name = name.substring(0, 255);
        }
        
        Housing.HousingBuilder builder = Housing.builder()
                .housingId(housingId)
                .name(name)
                .address(item.getRnAdres() != null ? item.getRnAdres() : "")
                .organization(item.getInsttNm() != null ? item.getInsttNm() : "")
                .heatingType(item.getHeatMthdDetailNm() != null ? item.getHeatMthdDetailNm() : "")
                .elevator(item.getElvtrInstlAtNm() != null && 
                         (item.getElvtrInstlAtNm().contains("설치") || 
                          item.getElvtrInstlAtNm().contains("전체")))
                .link("https://www.myhome.go.kr/hws/portal/cont/selectContRentalView.do#guide=" + item.getHsmpSn());

        // 공급 면적 파싱
        if (item.getSuplyPrvuseAr() != null && !item.getSuplyPrvuseAr().isEmpty()) {
            try {
                builder.supplyArea(Double.parseDouble(item.getSuplyPrvuseAr()));
            } catch (NumberFormatException e) {
                // 파싱 실패 시 무시
            }
        }

        // 준공 일자 파싱
        if (item.getCompetDe() != null && !item.getCompetDe().isEmpty()) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
                java.util.Date date = sdf.parse(item.getCompetDe());
                builder.completeDate(new Date(date.getTime()));
            } catch (ParseException e) {
                // 파싱 실패 시 무시
            }
        }

        // 주차수 파싱
        if (item.getParkngCo() != null && !item.getParkngCo().isEmpty()) {
            try {
                builder.parkingSpaces(Integer.parseInt(item.getParkngCo()));
            } catch (NumberFormatException e) {
                // 파싱 실패 시 무시
            }
        }

        // 세대수 파싱
        if (item.getHshldCo() != null && !item.getHshldCo().isEmpty()) {
            try {
                builder.totalUnits(Integer.parseInt(item.getHshldCo()));
            } catch (NumberFormatException e) {
                // 파싱 실패 시 무시
            }
        }

        // 기본 임대보증금 파싱
        if (item.getBassRentGtn() != null && !item.getBassRentGtn().isEmpty()) {
            try {
                builder.deposit(Integer.parseInt(item.getBassRentGtn()));
            } catch (NumberFormatException e) {
                // 파싱 실패 시 무시
            }
        }

        // 기본 월임대료 파싱
        if (item.getBassMtRntchrg() != null && !item.getBassMtRntchrg().isEmpty()) {
            try {
                builder.monthlyRent(Integer.parseInt(item.getBassMtRntchrg()));
            } catch (NumberFormatException e) {
                // 파싱 실패 시 무시
            }
        }

        // 주택유형 설정
        if (item.getSuplyTyNm() != null && !item.getSuplyTyNm().isEmpty()) {
            builder.housingType(item.getSuplyTyNm());
        }

        // 주소를 기반으로 좌표 조회 (카카오맵 API 사용)
        // 리액티브 스레드에서 block() 호출 방지를 위해 지오코딩 생략
        // 필요시 별도 스레드에서 비동기로 처리하거나 배치 작업으로 처리
        builder.latitude(null);
        builder.longitude(null);

        return builder.build();
    }

    /**
     * Housing ID 생성 (hsmpSn이 없는 경우)
     */
    private String generateHousingId(String brtcCode, String signguCode, String hsmpNm) {
        return (brtcCode != null ? brtcCode : "") + 
               (signguCode != null ? signguCode : "") + 
               (hsmpNm != null ? hsmpNm.hashCode() : System.currentTimeMillis());
    }

    /**
     * 기존 Housing 데이터 업데이트
     */
    private void updateHousingData(Housing existing, Housing newData) {
        existing.setName(newData.getName());
        existing.setAddress(newData.getAddress());
        existing.setSupplyArea(newData.getSupplyArea());
        existing.setCompleteDate(newData.getCompleteDate());
        existing.setOrganization(newData.getOrganization());
        existing.setHeatingType(newData.getHeatingType());
        existing.setElevator(newData.getElevator());
        existing.setParkingSpaces(newData.getParkingSpaces());
        existing.setDeposit(newData.getDeposit());
        existing.setMonthlyRent(newData.getMonthlyRent());
        existing.setTotalUnits(newData.getTotalUnits());
        // link: 무조건 값이 있어야 함 (null이면 기본값)
        if (newData.getLink() != null && !newData.getLink().isEmpty()) {
            existing.setLink(newData.getLink());
        } else if (existing.getLink() == null || existing.getLink().isEmpty()) {
            existing.setLink("https://apply.lh.or.kr/");
        }
        
        existing.setHousingType(newData.getHousingType());
        
        // 신청 기간 업데이트
        if (newData.getApplicationStart() != null) {
            existing.setApplicationStart(newData.getApplicationStart());
        } else if (existing.getApplicationStart() == null) {
            existing.setApplicationStart(new Date(System.currentTimeMillis()));
        }
        
        // applicationEnd: 무조건 값이 있어야 함 (null이면 기본값)
        if (newData.getApplicationEnd() != null) {
            existing.setApplicationEnd(newData.getApplicationEnd());
        } else if (existing.getApplicationEnd() == null) {
            // 기본값: 1년 후
            long oneYearLater = System.currentTimeMillis() + (365L * 24 * 60 * 60 * 1000);
            existing.setApplicationEnd(new Date(oneYearLater));
        }
        // 좌표는 주소가 변경된 경우에만 업데이트
        if (newData.getLatitude() != null && newData.getLongitude() != null) {
            existing.setLatitude(newData.getLatitude());
            existing.setLongitude(newData.getLongitude());
        }
    }

    /**
     * 단지명 정규화 (공백 제거, 특수문자 제거, 대소문자 통일)
     * @param name 원본 단지명
     * @return 정규화된 단지명
     */
    private String normalizeHousingName(String name) {
        if (name == null || name.isEmpty()) {
            return "";
        }
        // 모든 공백 제거
        String normalized = name.replaceAll("\\s+", "");
        // 특수문자 제거 (한글, 영문, 숫자만 유지)
        normalized = normalized.replaceAll("[^가-힣a-zA-Z0-9]", "");
        // 대소문자 통일 (소문자로 변환)
        normalized = normalized.toLowerCase();
        return normalized;
    }

    /**
     * Levenshtein Distance 계산 (두 문자열의 편집 거리)
     * @param s1 첫 번째 문자열
     * @param s2 두 번째 문자열
     * @return 편집 거리
     */
    private int calculateLevenshteinDistance(String s1, String s2) {
        if (s1 == null) s1 = "";
        if (s2 == null) s2 = "";
        
        int len1 = s1.length();
        int len2 = s2.length();
        
        // 동적 프로그래밍을 위한 2D 배열
        int[][] dp = new int[len1 + 1][len2 + 1];
        
        // 초기화: 빈 문자열에서 변환하는 비용
        for (int i = 0; i <= len1; i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= len2; j++) {
            dp[0][j] = j;
        }
        
        // Levenshtein Distance 계산
        for (int i = 1; i <= len1; i++) {
            for (int j = 1; j <= len2; j++) {
                if (s1.charAt(i - 1) == s2.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1];
                } else {
                    dp[i][j] = Math.min(
                        Math.min(dp[i - 1][j] + 1,      // 삭제
                                dp[i][j - 1] + 1),      // 삽입
                        dp[i - 1][j - 1] + 1            // 치환
                    );
                }
            }
        }
        
        return dp[len1][len2];
    }

    /**
     * 두 단지명의 유사도 계산 (0.0 ~ 1.0)
     * @param name1 첫 번째 단지명
     * @param name2 두 번째 단지명
     * @return 유사도 (1.0이 가장 유사, 0.0이 가장 다름)
     */
    private double calculateSimilarity(String name1, String name2) {
        if (name1 == null || name2 == null) {
            return 0.0;
        }
        
        // 정규화된 이름으로 비교
        String normalized1 = normalizeHousingName(name1);
        String normalized2 = normalizeHousingName(name2);
        
        // 정규화 후에도 둘 다 비어있으면 유사도 0
        if (normalized1.isEmpty() && normalized2.isEmpty()) {
            return 1.0;
        }
        if (normalized1.isEmpty() || normalized2.isEmpty()) {
            return 0.0;
        }
        
        // 정규화된 이름이 정확히 일치하면 유사도 1.0
        if (normalized1.equals(normalized2)) {
            return 1.0;
        }
        
        // Levenshtein Distance 기반 유사도 계산
        int distance = calculateLevenshteinDistance(normalized1, normalized2);
        int maxLen = Math.max(normalized1.length(), normalized2.length());
        
        if (maxLen == 0) {
            return 1.0;
        }
        
        // 유사도 = 1 - (편집 거리 / 최대 길이)
        double similarity = 1.0 - ((double) distance / maxLen);
        
        return similarity;
    }

    /**
     * 단지명으로 공고문 찾기 (퍼지 매칭 포함)
     * 1. 정확히 일치하는 경우
     * 2. 정규화된 이름이 일치하는 경우
     * 3. 유사도가 0.8 이상인 경우 (퍼지 매칭)
     * 
     * @param housingName 단지명
     * @param noticeMap 공고문 Map (단지명 -> 공고문)
     * @return 매칭된 공고문, 없으면 null
     */
    private LHRentalNoticeResponse.Item findMatchingNoticeByName(
            String housingName, 
            Map<String, LHRentalNoticeResponse.Item> noticeMap) {
        
        if (housingName == null || housingName.isEmpty() || noticeMap == null || noticeMap.isEmpty()) {
            return null;
        }
        
        // 1순위: 정확히 일치하는 경우
        if (noticeMap.containsKey(housingName)) {
            return noticeMap.get(housingName);
        }
        
        // 2순위: 정규화된 이름으로 일치하는 경우
        String normalizedHousingName = normalizeHousingName(housingName);
        for (Map.Entry<String, LHRentalNoticeResponse.Item> entry : noticeMap.entrySet()) {
            String noticeName = entry.getKey();
            String normalizedNoticeName = normalizeHousingName(noticeName);
            
            if (normalizedHousingName.equals(normalizedNoticeName)) {
                return entry.getValue();
            }
        }
        
        // 3순위: 퍼지 매칭 (유사도 0.8 이상)
        double bestSimilarity = 0.0;
        LHRentalNoticeResponse.Item bestMatch = null;
        
        for (Map.Entry<String, LHRentalNoticeResponse.Item> entry : noticeMap.entrySet()) {
            String noticeName = entry.getKey();
            double similarity = calculateSimilarity(housingName, noticeName);
            
            if (similarity >= 0.8 && similarity > bestSimilarity) {
                bestSimilarity = similarity;
                bestMatch = entry.getValue();
            }
        }
        
        if (bestMatch != null) {
            System.out.println("✅ 퍼지 매칭 성공: '" + housingName + "' <-> '" + 
                    (bestMatch.getHsmpNm() != null ? bestMatch.getHsmpNm() : "알 수 없음") + 
                    "' (유사도: " + String.format("%.2f", bestSimilarity) + ")");
        }
        
        return bestMatch;
    }

    /**
     * housing_complex와 housing_notice 간 매칭 통계 수집
     * @return 매칭 통계 정보
     */
    public Map<String, Object> getMatchingStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        // 모든 데이터 가져오기
        List<HousingComplex> complexes = housingComplexRepository.findAll();
        List<HousingNotice> notices = housingNoticeRepository.findAll();
        
        System.out.println("=== 매칭 통계 수집 시작 ===");
        System.out.println("단지정보 개수: " + complexes.size());
        System.out.println("공고문 개수: " + notices.size());
        
        // 디버깅: 샘플 데이터 확인
        if (!complexes.isEmpty()) {
            HousingComplex sampleComplex = complexes.get(0);
            System.out.println("샘플 단지정보 - complexId: " + sampleComplex.getComplexId() + ", hsmpNm: " + sampleComplex.getHsmpNm());
        }
        if (!notices.isEmpty()) {
            HousingNotice sampleNotice = notices.get(0);
            System.out.println("샘플 공고문 - noticeId: " + sampleNotice.getNoticeId() + ", hsmpSn: " + sampleNotice.getHsmpSn() + ", hsmpNm: " + sampleNotice.getHsmpNm());
        }
        
        // 통계 변수
        int matchedByHsmpSn = 0; // hsmpSn으로 정확히 매칭된 건수
        int matchedByHsmpNmExact = 0; // hsmpNm으로 정확히 일치한 건수
        int matchedByHsmpNmNormalized = 0; // 정규화된 이름으로 일치한 건수
        int matchedByFuzzy = 0; // 퍼지 매칭(유사도 0.8 이상)으로 매칭된 건수
        int unmatchedComplexes = 0; // 매칭되지 않은 단지정보 건수
        int unmatchedNotices = 0; // 매칭되지 않은 공고문 건수
        
        // 매칭된 공고문 추적 (중복 매칭 방지)
        Set<String> matchedNoticeIds = new HashSet<>();
        
        // 공고문을 Map으로 변환 (매칭용)
        Map<String, HousingNotice> noticeMapByHsmpSn = new HashMap<>();
        Map<String, HousingNotice> noticeMapByHsmpNm = new HashMap<>();
        
        for (HousingNotice notice : notices) {
            if (notice.getHsmpSn() != null && !notice.getHsmpSn().isEmpty()) {
                // 가장 최신 공고문만 유지 (같은 hsmpSn에 여러 공고문이 있을 수 있음)
                String key = notice.getHsmpSn();
                if (!noticeMapByHsmpSn.containsKey(key) || 
                    (notice.getApplicationStart() != null && 
                     noticeMapByHsmpSn.get(key).getApplicationStart() != null &&
                     notice.getApplicationStart().after(noticeMapByHsmpSn.get(key).getApplicationStart()))) {
                    noticeMapByHsmpSn.put(key, notice);
                }
            }
            if (notice.getHsmpNm() != null && !notice.getHsmpNm().isEmpty()) {
                String key = notice.getHsmpNm();
                if (!noticeMapByHsmpNm.containsKey(key) || 
                    (notice.getApplicationStart() != null && 
                     noticeMapByHsmpNm.get(key).getApplicationStart() != null &&
                     notice.getApplicationStart().after(noticeMapByHsmpNm.get(key).getApplicationStart()))) {
                    noticeMapByHsmpNm.put(key, notice);
                }
            }
        }
        
        System.out.println("공고문 Map 크기 - hsmpSn: " + noticeMapByHsmpSn.size() + ", hsmpNm: " + noticeMapByHsmpNm.size());
        
        // 디버깅: 실제 데이터 확인 (처음 5개씩)
        System.out.println("처음 5개 단지정보:");
        int complexCount = 0;
        for (HousingComplex complex : complexes) {
            if (complexCount++ >= 5) break;
            System.out.println("  - complexId: '" + complex.getComplexId() + "', hsmpNm: '" + complex.getHsmpNm() + "'");
        }
        
        System.out.println("처음 5개 공고문:");
        int noticeCount = 0;
        for (HousingNotice notice : notices) {
            if (noticeCount++ >= 5) break;
            System.out.println("  - noticeId: '" + notice.getNoticeId() + "', hsmpSn: '" + notice.getHsmpSn() + "', hsmpNm: '" + notice.getHsmpNm() + "'");
        }
        
        // 단지정보 기준으로 매칭 수행
        for (HousingComplex complex : complexes) {
            String complexId = complex.getComplexId();
            String complexName = complex.getHsmpNm();
            
            HousingNotice matchedNotice = null;
            
            // 1순위: hsmpSn으로 정확히 매칭
            if (complexId != null && noticeMapByHsmpSn.containsKey(complexId)) {
                matchedNotice = noticeMapByHsmpSn.get(complexId);
                matchedByHsmpSn++;
            }
            // 2순위: hsmpNm으로 정확히 일치
            else if (complexName != null && noticeMapByHsmpNm.containsKey(complexName)) {
                matchedNotice = noticeMapByHsmpNm.get(complexName);
                matchedByHsmpNmExact++;
            }
            // 3순위: 정규화된 이름으로 일치
            else if (complexName != null) {
                String normalizedComplexName = normalizeHousingName(complexName);
                for (Map.Entry<String, HousingNotice> entry : noticeMapByHsmpNm.entrySet()) {
                    String noticeName = entry.getKey();
                    String normalizedNoticeName = normalizeHousingName(noticeName);
                    
                    if (normalizedComplexName.equals(normalizedNoticeName)) {
                        matchedNotice = entry.getValue();
                        matchedByHsmpNmNormalized++;
                        break;
                    }
                }
            }
            // 4순위: 퍼지 매칭 (유사도 0.8 이상)
            if (matchedNotice == null && complexName != null) {
                double bestSimilarity = 0.0;
                HousingNotice bestMatch = null;
                
                for (Map.Entry<String, HousingNotice> entry : noticeMapByHsmpNm.entrySet()) {
                    String noticeName = entry.getKey();
                    double similarity = calculateSimilarity(complexName, noticeName);
                    
                    if (similarity >= 0.8 && similarity > bestSimilarity) {
                        bestSimilarity = similarity;
                        bestMatch = entry.getValue();
                    }
                }
                
                if (bestMatch != null) {
                    matchedNotice = bestMatch;
                    matchedByFuzzy++;
                }
            }
            
            // 매칭된 공고문 추적
            if (matchedNotice != null) {
                matchedNoticeIds.add(matchedNotice.getNoticeId());
            } else {
                unmatchedComplexes++;
            }
        }
        
        // 매칭되지 않은 공고문 개수
        unmatchedNotices = (int) notices.stream()
                .filter(notice -> !matchedNoticeIds.contains(notice.getNoticeId()))
                .count();
        
        // 총 매칭 건수
        int totalMatched = matchedByHsmpSn + matchedByHsmpNmExact + matchedByHsmpNmNormalized + matchedByFuzzy;
        
        // 통계 저장
        stats.put("totalComplexes", complexes.size());
        stats.put("totalNotices", notices.size());
        stats.put("totalMatched", totalMatched);
        stats.put("matchedByHsmpSn", matchedByHsmpSn);
        stats.put("matchedByHsmpNmExact", matchedByHsmpNmExact);
        stats.put("matchedByHsmpNmNormalized", matchedByHsmpNmNormalized);
        stats.put("matchedByFuzzy", matchedByFuzzy);
        stats.put("unmatchedComplexes", unmatchedComplexes);
        stats.put("unmatchedNotices", unmatchedNotices);
        
        // 매칭률 계산
        if (complexes.size() > 0) {
            double matchRate = (totalMatched * 100.0) / complexes.size();
            stats.put("matchRate", String.format("%.2f%%", matchRate));
        }
        
        if (notices.size() > 0) {
            double noticeMatchRate = (totalMatched * 100.0) / notices.size();
            stats.put("noticeMatchRate", String.format("%.2f%%", noticeMatchRate));
        }
        
        System.out.println("=== 매칭 통계 결과 ===");
        System.out.println("총 매칭 건수: " + totalMatched);
        System.out.println("  - hsmpSn 매칭: " + matchedByHsmpSn);
        System.out.println("  - hsmpNm 정확 매칭: " + matchedByHsmpNmExact);
        System.out.println("  - hsmpNm 정규화 매칭: " + matchedByHsmpNmNormalized);
        System.out.println("  - 퍼지 매칭: " + matchedByFuzzy);
        System.out.println("매칭되지 않은 단지정보: " + unmatchedComplexes);
        System.out.println("매칭되지 않은 공고문: " + unmatchedNotices);
        System.out.println("====================");
        
        return stats;
    }

    /**
     * DB의 housing_notice와 housing_complex 테이블을 매칭하여 housing 테이블에 저장
     * 공고명에서 단지명 추출 및 퍼지 매칭을 포함한 다양한 매칭 전략 사용
     */
    @Transactional
    public void matchAndSaveHousingData() {
        System.out.println("========================================");
        System.out.println("housing_notice와 housing_complex 매칭 시작");
        System.out.println("========================================");
        
        // 모든 데이터 가져오기
        List<HousingComplex> complexes = housingComplexRepository.findAll();
        List<HousingNotice> notices = housingNoticeRepository.findAll();
        
        System.out.println("단지정보 개수: " + complexes.size());
        System.out.println("공고문 개수: " + notices.size());
        
        // 공고문을 Map으로 변환 (매칭용)
        // 1. hsmpSn으로 매핑 (있는 경우)
        Map<String, HousingNotice> noticeMapByHsmpSn = new HashMap<>();
        // 2. hsmpNm으로 매핑 (있는 경우)
        Map<String, HousingNotice> noticeMapByHsmpNm = new HashMap<>();
        // 3. 공고명에서 추출한 단지명으로 매핑
        Map<String, HousingNotice> noticeMapByExtractedName = new HashMap<>();
        // 4. 지역명 + 공고명 조합으로 매핑
        Map<String, HousingNotice> noticeMapByRegionAndName = new HashMap<>();
        
        for (HousingNotice notice : notices) {
            // hsmpSn으로 매핑
            if (notice.getHsmpSn() != null && !notice.getHsmpSn().isEmpty()) {
                noticeMapByHsmpSn.put(notice.getHsmpSn(), notice);
            }
            
            // hsmpNm으로 매핑
            if (notice.getHsmpNm() != null && !notice.getHsmpNm().isEmpty()) {
                noticeMapByHsmpNm.put(notice.getHsmpNm(), notice);
            }
            
            // 공고명에서 단지명 추출
            if (notice.getPanNm() != null && !notice.getPanNm().isEmpty()) {
                String extractedName = extractComplexNameFromNoticeName(notice.getPanNm());
                if (extractedName != null && !extractedName.isEmpty()) {
                    noticeMapByExtractedName.put(extractedName, notice);
                    // 정규화된 이름도 추가
                    String normalizedExtracted = normalizeHousingName(extractedName);
                    if (!normalizedExtracted.equals(normalizeHousingName(extractedName))) {
                        noticeMapByExtractedName.put(normalizedExtracted, notice);
                    }
                }
                
                // 지역명 + 공고명 조합
                if (notice.getCnpCdNm() != null && !notice.getCnpCdNm().isEmpty() && extractedName != null) {
                    String regionAndName = notice.getCnpCdNm() + " " + extractedName;
                    noticeMapByRegionAndName.put(regionAndName, notice);
                }
            }
        }
        
        System.out.println("공고문 Map 크기:");
        System.out.println("  - hsmpSn: " + noticeMapByHsmpSn.size());
        System.out.println("  - hsmpNm: " + noticeMapByHsmpNm.size());
        System.out.println("  - 추출된 단지명: " + noticeMapByExtractedName.size());
        System.out.println("  - 지역명+단지명: " + noticeMapByRegionAndName.size());
        
        int savedCount = 0;
        int updatedCount = 0;
        int matchedCount = 0;
        
        // 단지정보 기준으로 매칭 및 저장
        for (HousingComplex complex : complexes) {
            try {
                HousingNotice matchedNotice = null;
                
                // 1순위: hsmpSn으로 정확히 매칭
                if (complex.getComplexId() != null && noticeMapByHsmpSn.containsKey(complex.getComplexId())) {
                    matchedNotice = noticeMapByHsmpSn.get(complex.getComplexId());
                }
                // 2순위: hsmpNm으로 정확히 일치
                else if (complex.getHsmpNm() != null && noticeMapByHsmpNm.containsKey(complex.getHsmpNm())) {
                    matchedNotice = noticeMapByHsmpNm.get(complex.getHsmpNm());
                }
                // 3순위: 정규화된 이름으로 일치
                else if (complex.getHsmpNm() != null) {
                    String normalizedComplexName = normalizeHousingName(complex.getHsmpNm());
                    for (Map.Entry<String, HousingNotice> entry : noticeMapByHsmpNm.entrySet()) {
                        String noticeName = entry.getKey();
                        String normalizedNoticeName = normalizeHousingName(noticeName);
                        if (normalizedComplexName.equals(normalizedNoticeName)) {
                            matchedNotice = entry.getValue();
                            break;
                        }
                    }
                }
                // 4순위: 추출된 단지명으로 매칭
                if (matchedNotice == null && complex.getHsmpNm() != null) {
                    String normalizedComplexName = normalizeHousingName(complex.getHsmpNm());
                    // 정확히 일치하는 경우
                    for (Map.Entry<String, HousingNotice> entry : noticeMapByExtractedName.entrySet()) {
                        String extractedName = entry.getKey();
                        String normalizedExtracted = normalizeHousingName(extractedName);
                        if (normalizedComplexName.equals(normalizedExtracted)) {
                            matchedNotice = entry.getValue();
                            System.out.println("✅ 추출된 단지명으로 정확 매칭: '" + complex.getHsmpNm() + "' <-> '" + extractedName + "'");
                            break;
                        }
                    }
                    
                    // 부분 매칭 시도 (예: "옥산3단지" <-> "옥산77단지7-8")
                    if (matchedNotice == null) {
                        for (Map.Entry<String, HousingNotice> entry : noticeMapByExtractedName.entrySet()) {
                            String extractedName = entry.getKey();
                            String normalizedExtracted = normalizeHousingName(extractedName);
                            
                            // 공통 접두사가 3자 이상이고, 둘 다 "단지", "아파트", "주택"을 포함하는 경우
                            if (normalizedComplexName.length() >= 3 && normalizedExtracted.length() >= 3) {
                                // 접두사 매칭 (최소 3자)
                                int minLen = Math.min(normalizedComplexName.length(), normalizedExtracted.length());
                                int commonPrefix = 0;
                                for (int i = 0; i < minLen && i < 10; i++) { // 최대 10자까지 비교
                                    if (normalizedComplexName.charAt(i) == normalizedExtracted.charAt(i)) {
                                        commonPrefix++;
                                    } else {
                                        break;
                                    }
                                }
                                
                                // 공통 접두사가 3자 이상이고, 둘 다 같은 키워드(단지/아파트/주택)를 포함
                                if (commonPrefix >= 3) {
                                    boolean bothHaveKeyword = (normalizedComplexName.contains("단지") || normalizedComplexName.contains("아파트") || normalizedComplexName.contains("주택")) &&
                                                             (normalizedExtracted.contains("단지") || normalizedExtracted.contains("아파트") || normalizedExtracted.contains("주택"));
                                    if (bothHaveKeyword) {
                                        matchedNotice = entry.getValue();
                                        System.out.println("✅ 추출된 단지명으로 부분 매칭: '" + complex.getHsmpNm() + "' <-> '" + extractedName + "' (공통 접두사: " + commonPrefix + "자)");
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
                // 5순위: 퍼지 매칭 (유사도 0.8 이상)
                if (matchedNotice == null && complex.getHsmpNm() != null) {
                    double bestSimilarity = 0.0;
                    HousingNotice bestMatch = null;
                    String bestMatchName = null;
                    
                    // hsmpNm으로 퍼지 매칭 (null이므로 스킵됨)
                    for (Map.Entry<String, HousingNotice> entry : noticeMapByHsmpNm.entrySet()) {
                        String noticeName = entry.getKey();
                        double similarity = calculateSimilarity(complex.getHsmpNm(), noticeName);
                        if (similarity >= 0.8 && similarity > bestSimilarity) {
                            bestSimilarity = similarity;
                            bestMatch = entry.getValue();
                            bestMatchName = noticeName;
                        }
                    }
                    
                    // 추출된 단지명으로 퍼지 매칭
                    for (Map.Entry<String, HousingNotice> entry : noticeMapByExtractedName.entrySet()) {
                        String extractedName = entry.getKey();
                        double similarity = calculateSimilarity(complex.getHsmpNm(), extractedName);
                        if (similarity >= 0.8 && similarity > bestSimilarity) {
                            bestSimilarity = similarity;
                            bestMatch = entry.getValue();
                            bestMatchName = extractedName;
                        }
                    }
                    
                    // 공고명 전체로도 퍼지 매칭 시도
                    for (HousingNotice notice : notices) {
                        if (notice.getPanNm() != null && !notice.getPanNm().isEmpty()) {
                            double similarity = calculateSimilarity(complex.getHsmpNm(), notice.getPanNm());
                            if (similarity >= 0.8 && similarity > bestSimilarity) {
                                bestSimilarity = similarity;
                                bestMatch = notice;
                                bestMatchName = notice.getPanNm();
                            }
                        }
                    }
                    
                    // 공고명에서 추출한 단지명과 단지명의 부분 매칭도 시도 (유사도 0.7 이상)
                    for (Map.Entry<String, HousingNotice> entry : noticeMapByExtractedName.entrySet()) {
                        String extractedName = entry.getKey();
                        double similarity = calculateSimilarity(complex.getHsmpNm(), extractedName);
                        if (similarity >= 0.7 && similarity > bestSimilarity) { // 임계값을 0.7로 낮춤
                            bestSimilarity = similarity;
                            bestMatch = entry.getValue();
                            bestMatchName = extractedName;
                        }
                    }
                    
                    if (bestMatch != null) {
                        matchedNotice = bestMatch;
                        System.out.println("✅ 퍼지 매칭 성공: '" + complex.getHsmpNm() + "' <-> '" + bestMatchName + "' (유사도: " + String.format("%.2f", bestSimilarity) + ")");
                    }
                }
                
                // Housing 엔티티 생성 (complex 데이터 기반)
                Housing housing = convertComplexToHousing(complex, matchedNotice);
                
                // DB에 저장 (이미 존재하면 업데이트)
                boolean exists = housingRepository.existsById(housing.getHousingId());
                housingRepository.findById(housing.getHousingId())
                        .ifPresentOrElse(
                                existing -> {
                                    updateHousingData(existing, housing);
                                    housingRepository.save(existing);
                                },
                                () -> {
                                    housingRepository.save(housing);
                                }
                        );
                
                if (exists) {
                    updatedCount++;
                } else {
                    savedCount++;
                }
                
                if (matchedNotice != null) {
                    matchedCount++;
                }
                
            } catch (Exception e) {
                System.err.println("단지정보 저장 실패: " + 
                        (complex.getHsmpNm() != null ? complex.getHsmpNm() : "알 수 없음") + 
                        " - " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        System.out.println("========================================");
        System.out.println("housing 테이블 저장 완료:");
        System.out.println("  - 총 단지정보: " + complexes.size() + "건");
        System.out.println("  - 공고문 매칭: " + matchedCount + "건");
        System.out.println("  - 저장: " + savedCount + "건, 업데이트: " + updatedCount + "건");
        System.out.println("========================================");
    }
    
    /**
     * HousingComplex와 HousingNotice를 병합하여 Housing 엔티티로 변환
     * 모든 필드가 null이 아닌 값으로 채워지도록 보장
     */
    private Housing convertComplexToHousing(HousingComplex complex, HousingNotice notice) {
        // housingId는 complexId 사용 (필수, null 불가)
        String housingId = complex.getComplexId();
        if (housingId == null || housingId.isEmpty()) {
            housingId = "HOUSING_" + System.currentTimeMillis() + "_" + 
                    (complex.getHsmpNm() != null ? complex.getHsmpNm().hashCode() : 0);
        }
        
        // name 필드 (필수, null 불가)
        String name = complex.getHsmpNm();
        if (name == null || name.isEmpty()) {
            name = notice != null && notice.getPanNm() != null ? notice.getPanNm() : "알 수 없는 단지";
        }
        if (name.length() > 255) {
            name = name.substring(0, 255);
        }
        
        Housing.HousingBuilder builder = Housing.builder()
                .housingId(housingId)
                .name(name)
                // address: complex에서 가져오거나 지역명 조합
                .address(buildAddressFromComplex(complex))
                // supplyArea: complex에서 가져오거나 기본값
                .supplyArea(complex.getSupplyArea() != null ? complex.getSupplyArea() : 0.0)
                // completeDate: complex에서 가져오거나 null
                .completeDate(complex.getCompleteDate())
                // organization: complex에서 가져오거나 기본값
                .organization(complex.getInsttNm() != null && !complex.getInsttNm().isEmpty() 
                        ? complex.getInsttNm() 
                        : "LH공사")
                // heatingType: complex에서 가져오거나 기본값
                .heatingType(complex.getHeatMthdDetailNm() != null && !complex.getHeatMthdDetailNm().isEmpty() 
                        ? complex.getHeatMthdDetailNm() 
                        : "중앙난방")
                // elevator: complex에서 가져오거나 기본값 (false)
                .elevator(complex.getElevator() != null ? complex.getElevator() : false)
                // parkingSpaces: complex에서 가져오거나 기본값
                .parkingSpaces(complex.getParkingSpaces() != null ? complex.getParkingSpaces() : 0)
                // deposit: complex에서 가져오거나 기본값
                .deposit(complex.getDeposit() != null ? complex.getDeposit() : 0)
                // monthlyRent: complex에서 가져오거나 기본값
                .monthlyRent(complex.getMonthlyRent() != null ? complex.getMonthlyRent() : 0)
                // totalUnits: complex에서 가져오거나 기본값
                .totalUnits(complex.getTotalUnits() != null ? complex.getTotalUnits() : 0)
                // latitude, longitude: complex에서 가져오거나 null
                .latitude(complex.getLatitude())
                .longitude(complex.getLongitude())
                // housingType: complex에서 가져오거나 공고문에서 가져오거나 기본값
                .housingType(determineHousingType(complex, notice));
        
        // 공고문 데이터로 보완
        if (notice != null) {
            // applicationStart: 공고문에서 가져오거나 기본값 (현재 날짜)
            if (notice.getApplicationStart() != null) {
                builder.applicationStart(notice.getApplicationStart());
            } else {
                // 기본값: 현재 날짜
                builder.applicationStart(new Date(System.currentTimeMillis()));
            }
            
            // applicationEnd: 공고문에서 가져오거나 기본값 (1년 후)
            if (notice.getApplicationEnd() != null) {
                builder.applicationEnd(notice.getApplicationEnd());
            } else {
                // 기본값: 1년 후
                long oneYearLater = System.currentTimeMillis() + (365L * 24 * 60 * 60 * 1000);
                builder.applicationEnd(new Date(oneYearLater));
            }
            
            // link: 공고문에서 가져오거나 기본 URL
            String link = notice.getDtlUrl();
            if (link == null || link.isEmpty()) {
                link = "https://apply.lh.or.kr/";
            }
            builder.link(link);
        } else {
            // 매칭되지 않은 경우 기본값 설정
            builder.applicationStart(new Date(System.currentTimeMillis()));
            long oneYearLater = System.currentTimeMillis() + (365L * 24 * 60 * 60 * 1000);
            builder.applicationEnd(new Date(oneYearLater));
            builder.link("https://apply.lh.or.kr/");
        }
        
        return builder.build();
    }
    
    /**
     * HousingComplex의 정보로 주소 생성
     */
    private String buildAddressFromComplex(HousingComplex complex) {
        // 1순위: rnAdres (도로명 주소)
        if (complex.getRnAdres() != null && !complex.getRnAdres().isEmpty()) {
            return complex.getRnAdres();
        }
        
        // 2순위: 지역명 + 단지명 조합
        StringBuilder address = new StringBuilder();
        if (complex.getBrtcNm() != null && !complex.getBrtcNm().isEmpty()) {
            address.append(complex.getBrtcNm());
        }
        if (complex.getSignguNm() != null && !complex.getSignguNm().isEmpty()) {
            if (address.length() > 0) {
                address.append(" ");
            }
            address.append(complex.getSignguNm());
        }
        if (complex.getHsmpNm() != null && !complex.getHsmpNm().isEmpty()) {
            if (address.length() > 0) {
                address.append(" ");
            }
            address.append(complex.getHsmpNm());
        }
        
        // 최소한 지역명이라도 있으면 반환
        if (address.length() > 0) {
            return address.toString();
        }
        
        // 기본값
        return "주소 정보 없음";
    }
    
    /**
     * 주택유형 결정 (complex 우선, 없으면 notice, 둘 다 없으면 기본값)
     */
    private String determineHousingType(HousingComplex complex, HousingNotice notice) {
        // 1순위: complex의 suplyTyNm
        if (complex.getSuplyTyNm() != null && !complex.getSuplyTyNm().isEmpty()) {
            return complex.getSuplyTyNm();
        }
        
        // 2순위: notice의 aisTpCdNm
        if (notice != null && notice.getAisTpCdNm() != null && !notice.getAisTpCdNm().isEmpty()) {
            return notice.getAisTpCdNm();
        }
        
        // 3순위: complex의 houseTyNm
        if (complex.getHouseTyNm() != null && !complex.getHouseTyNm().isEmpty()) {
            return complex.getHouseTyNm();
        }
        
        // 기본값
        return "공공임대주택";
    }
    
    /**
     * 공고명에서 단지명 추출
     * 예: "청주옥산3 A16, A17, A22, A23단지 건설공사 청주시 청원구 옥산면 입주자모집" -> "옥산3단지" 또는 "옥산3"
     * 예: "2024년 LH공사 공급주택 공고 (서울 양천구 목동단지)" -> "목동단지"
     */
    private String extractComplexNameFromNoticeName(String panNm) {
        if (panNm == null || panNm.isEmpty()) {
            return null;
        }
        
        // 괄호 안의 내용 추출
        if (panNm.contains("(") && panNm.contains(")")) {
            int start = panNm.indexOf("(") + 1;
            int end = panNm.indexOf(")");
            if (start < end) {
                String extracted = panNm.substring(start, end).trim();
                // "단지", "아파트", "주택" 등이 포함된 경우만 단지명으로 인정
                if (extracted.contains("단지") || extracted.contains("아파트") || extracted.contains("주택")) {
                    // 지역명 제거 (예: "서울 양천구 목동단지" -> "목동단지")
                    String[] parts = extracted.split("\\s+");
                    for (int i = parts.length - 1; i >= 0; i--) {
                        if (parts[i].contains("단지") || parts[i].contains("아파트") || parts[i].contains("주택")) {
                            return parts[i];
                        }
                    }
                    return extracted;
                }
            }
        }
        
        // 괄호가 없으면 공고명에서 단지명 패턴 찾기
        // 예: "청주옥산3 A16, A17, A22, A23단지" -> "옥산3단지" 또는 "옥산3"
        // 예: "목동단지 공급주택 공고" -> "목동단지"
        
        // 패턴 1: "XXX단지" 형식 찾기
        java.util.regex.Pattern pattern1 = java.util.regex.Pattern.compile("([가-힣]+\\d*단지)");
        java.util.regex.Matcher matcher1 = pattern1.matcher(panNm);
        if (matcher1.find()) {
            return matcher1.group(1);
        }
        
        // 패턴 2: "XXX아파트" 형식 찾기
        java.util.regex.Pattern pattern2 = java.util.regex.Pattern.compile("([가-힣]+\\d*아파트)");
        java.util.regex.Matcher matcher2 = pattern2.matcher(panNm);
        if (matcher2.find()) {
            return matcher2.group(1);
        }
        
        // 패턴 3: "XXX주택" 형식 찾기
        java.util.regex.Pattern pattern3 = java.util.regex.Pattern.compile("([가-힣]+\\d*주택)");
        java.util.regex.Matcher matcher3 = pattern3.matcher(panNm);
        if (matcher3.find()) {
            return matcher3.group(1);
        }
        
        // 패턴 4: 공백으로 구분된 단어 중 "단지", "아파트", "주택" 포함 단어 찾기
        String[] keywords = {"단지", "아파트", "주택"};
        String[] words = panNm.split("\\s+");
        for (String word : words) {
            for (String keyword : keywords) {
                if (word.contains(keyword)) {
                    // "A16, A17" 같은 것을 제외하고 한글이 포함된 경우만
                    if (word.matches(".*[가-힣]+.*")) {
                        return word.replaceAll("[^가-힣0-9" + keyword + "]", ""); // 특수문자 제거
                    }
                }
            }
        }
        
        // 패턴 5: 앞부분에서 단지명 추출 시도 (예: "청주옥산3" -> "옥산3")
        for (String keyword : keywords) {
            int index = panNm.indexOf(keyword);
            if (index > 0) {
                // 앞부분에서 한글+숫자 패턴 찾기
                String before = panNm.substring(0, index + keyword.length());
                java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("([가-힣]+\\d*" + keyword + ")");
                java.util.regex.Matcher matcher = pattern.matcher(before);
                if (matcher.find()) {
                    return matcher.group(1);
                }
            }
        }
        
        return null;
    }
}

