package com.example.youth.service;

import com.example.youth.DB.Housing;
import com.example.youth.dto.publicdata.LHRentalHouseListResponse;
import com.example.youth.dto.publicdata.LHRentalNoticeResponse;
import com.example.youth.repository.HousingRepository;
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
    private HousingRepository housingRepository; // 사용됨: convertToHousing, updateHousingData에서 사용

    /**
     * LH 공공데이터에서 임대주택 정보를 가져와 DB에 동기화
     * 1. 공고문 API로 공고 데이터 조회 및 DB에 저장
     * 2. DB에 저장된 공고문 데이터를 기준으로 단지정보 API 조회
     * 3. 단지정보 API 데이터로 DB housing 테이블의 나머지 필드 업데이트
     * 
     * @param brtcCode 광역시도 코드 (옵션, 공고문 API용)
     * @param signguCode 시군구 코드 (옵션, 공고문 API용)
     * 
     * 주의: @Transactional 제거 - 긴 작업이므로 메서드 전체에 트랜잭션을 걸지 않음
     *       DB 저장 시점에만 트랜잭션 적용
     */
    public void syncLHRentalHouseData(String brtcCode, String signguCode) {
        System.out.println("========================================");
        System.out.println("임대주택 데이터 동기화 시작 (공고문 저장 → 단지정보 업데이트)");
        System.out.println("========================================");
        
        // 1단계: 공고문 API 호출 및 DB에 저장
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
                            System.out.println("공고문 데이터 수집: " + noticeItems.size() + "건");
                            
                            // 지역 코드 매핑 서비스 초기화
                            try {
                                System.out.println("지역 코드 매핑 서비스 초기화 시작...");
                                regionCodeMappingService.initializeMapping();
                                System.out.println("지역 코드 매핑 서비스 초기화 완료");
                            } catch (Exception e) {
                                System.err.println("⚠️ 지역 코드 매핑 서비스 초기화 실패: " + e.getMessage());
                                e.printStackTrace();
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
                            
                            // 공고문 데이터를 DB에 저장
                            System.out.println("공고문 데이터를 DB에 저장 중...");
                            saveNoticeDataOnly(noticeMapByHsmpSn, noticeMapByHsmpNm);
                            
                            // 2단계: DB에 저장된 housing 데이터를 읽어서 단지정보 API 조회
                            System.out.println("DB에 저장된 housing 데이터 조회 중...");
                            List<Housing> savedHousings = housingRepository.findAll();
                            
                            if (savedHousings.isEmpty()) {
                                System.out.println("⚠️ DB에 저장된 housing 데이터가 없습니다.");
                                return;
                            }
                            
                            System.out.println("DB에 저장된 housing 데이터: " + savedHousings.size() + "건");
                            
                            // 저장된 housing 데이터에서 지역 정보 추출
                            Map<String, Set<String>> brtcToSignguCodes = extractRegionsFromHousings(savedHousings);
                            
                            if (brtcToSignguCodes.isEmpty()) {
                                System.out.println("⚠️ 지역 정보를 추출할 수 없어 단지정보 조회를 건너뜁니다.");
                                return;
                            }
                            
                            System.out.println("추출된 광역시도 코드: " + brtcToSignguCodes.size() + "개");
                            
                            // 3단계: 단지정보 API 조회
                            System.out.println("단지정보 API 조회 시작 (DB housing 데이터 기준)");
                            List<LHRentalHouseListResponse.Item> allHouseItems = syncTargetRegions(brtcToSignguCodes);
                            
                            System.out.println("수집된 단지정보: " + allHouseItems.size() + "건");
                            
                            // 4단계: 단지정보 API 데이터로 DB housing 업데이트
                            updateHousingsWithHouseInfo(savedHousings, allHouseItems);
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
                    // System.err.println("API 호출 에러 (" + brtcCode + "-" + signguCode + "): " + e.getMessage());
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
     * 공고문 데이터만으로 Housing 엔티티 생성 및 DB에 저장 (트랜잭션 적용)
     */
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
        existing.setLink(newData.getLink());
        existing.setHousingType(newData.getHousingType());
        // 신청 기간 업데이트
        if (newData.getApplicationStart() != null) {
            existing.setApplicationStart(newData.getApplicationStart());
        }
        if (newData.getApplicationEnd() != null) {
            existing.setApplicationEnd(newData.getApplicationEnd());
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
}

