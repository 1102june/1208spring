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
     * 1. 공고문 API로 공고 데이터 조회
     * 2. 단지정보 API로 전체 지역 데이터 조회 (0-99 × 0-999)
     * 3. 두 데이터를 병합하여 저장
     * 
     * @param brtcCode 광역시도 코드 (옵션, 공고문 API용)
     * @param signguCode 시군구 코드 (옵션, 공고문 API용)
     */
    @Transactional
    public void syncLHRentalHouseData(String brtcCode, String signguCode) {
        System.out.println("========================================");
        System.out.println("임대주택 데이터 동기화 시작 (공고문 → 단지정보)");
        System.out.println("========================================");
        
        // 1단계: 공고문 API 호출
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
                            
                            // 공고문 데이터에서 실제 사용되는 지역명 추출 및 코드 변환
                            Set<String> uniqueBrtcCodes = new HashSet<>(); // 광역시도 코드 Set
                            Map<String, Set<String>> brtcToSignguCodes = new HashMap<>(); // 광역시도 코드 -> 시군구 코드 Set
                            Map<String, LHRentalNoticeResponse.Item> noticeMap = new HashMap<>();
                            
                            // 지역 코드 매핑 서비스 초기화
                            try {
                                System.out.println("지역 코드 매핑 서비스 초기화 시작...");
                                regionCodeMappingService.initializeMapping();
                                System.out.println("지역 코드 매핑 서비스 초기화 완료");
                            } catch (Exception e) {
                                System.err.println("⚠️ 지역 코드 매핑 서비스 초기화 실패: " + e.getMessage());
                                e.printStackTrace();
                            }
                            
                            for (LHRentalNoticeResponse.Item notice : noticeItems) {
                                // 공고문 데이터를 Map으로 변환
                                String key = notice.getPanId() != null ? notice.getPanId() : "";
                                if (!key.isEmpty()) {
                                    if (!noticeMap.containsKey(key) || isNewerNotice(notice, noticeMap.get(key))) {
                                        noticeMap.put(key, notice);
                                    }
                                }
                                
                                // CNP_CD_NM (지역명) 추출 및 광역시도 코드로 변환
                                String cnpCdNm = notice.getCnpCdNm(); // 예: "충청북도", "서울특별시"
                                if (cnpCdNm != null && !cnpCdNm.isEmpty()) {
                                    String mappedBrtcCode = regionCodeMappingService.convertCnpCdNmToBrtcCode(cnpCdNm);
                                    if (mappedBrtcCode != null) {
                                        uniqueBrtcCodes.add(mappedBrtcCode);
                                        
                                        // 해당 광역시도에 속한 실제 시군구 코드 목록 가져오기
                                        Map<String, String> signguCodes = regionCodeMappingService.getSignguCodesByBrtcCode(mappedBrtcCode);
                                        if (!signguCodes.isEmpty()) {
                                            brtcToSignguCodes.put(mappedBrtcCode, new HashSet<>(signguCodes.values()));
                                        }
                                    } else {
                                        System.out.println("⚠️ 지역명 '" + cnpCdNm + "'에 대한 광역시도 코드를 찾을 수 없습니다.");
                                    }
                                }
                            }
                            
                            System.out.println("공고문 데이터 Map 변환 완료: " + noticeMap.size() + "건");
                            System.out.println("추출된 광역시도 코드: " + uniqueBrtcCodes.size() + "개 - " + uniqueBrtcCodes);
                            System.out.println("광역시도별 시군구 코드 매핑: " + brtcToSignguCodes.size() + "개");
                            
                            // 2단계: 추출된 광역시도 코드와 실제 시군구 코드로 단지정보 API 호출
                            List<LHRentalHouseListResponse.Item> allHouseItems = new ArrayList<>();
                            
                            if (uniqueBrtcCodes.isEmpty()) {
                                System.out.println("⚠️ 광역시도 코드가 없어 단지정보 API를 호출할 수 없습니다.");
                                System.out.println("   공고문 데이터만 저장합니다.");
                                // 공고문 데이터만 저장
                                processApiResults(new LHRentalHouseListResponse(), noticeResponse);
                                return;
                            }
                            
                            // 각 광역시도 코드에 대해 실제 시군구 코드만 조회
                            int totalCalls = 0;
                            for (Set<String> signguSet : brtcToSignguCodes.values()) {
                                totalCalls += signguSet.size();
                            }
                            // 시군구 코드가 없는 광역시도는 기본값(000)으로 1번 호출
                            totalCalls += uniqueBrtcCodes.size() - brtcToSignguCodes.size();
                            
                            int processedCount = 0;
                            int successCount = 0;
                            
                            System.out.println("단지정보 API 호출 시작: 총 " + totalCalls + "개 조합");
                            
                            for (String brtc : uniqueBrtcCodes) {
                                Set<String> signguCodes = brtcToSignguCodes.get(brtc);
                                
                                if (signguCodes != null && !signguCodes.isEmpty()) {
                                    // 실제 시군구 코드만 조회
                                    for (String signgu : signguCodes) {
                                        processedCount++;
                                        
                                        // 진행 상황 출력 (50개마다)
                                        if (processedCount % 50 == 0) {
                                            System.out.println("단지정보 API 진행: " + processedCount + "/" + totalCalls + 
                                                             " (" + (processedCount * 100 / totalCalls) + "%) - 성공: " + successCount + "개");
                                        }
                                        
                                        try {
                                            LHRentalHouseListResponse houseResponse = publicDataApiService.getLHRentalHouseList(
                                                    1, 100, brtc, signgu, null).block();
                                            
                                            List<LHRentalHouseListResponse.Item> items = houseResponse != null ? houseResponse.getItems() : null;
                                            if (items != null && !items.isEmpty()) {
                                                allHouseItems.addAll(items);
                                                successCount++;
                                            }
                                        } catch (Exception e) {
                                            // 에러는 무시하고 계속 진행
                                        }
                                        
                                        // API 호출 제한 방지 (100ms 딜레이)
                                        try {
                                            Thread.sleep(100);
                                        } catch (InterruptedException e) {
                                            Thread.currentThread().interrupt();
                                            break;
                                        }
                                    }
                                } else {
                                    // 시군구 코드가 없는 경우 기본값(000)으로 1번 호출
                                    processedCount++;
                                    try {
                                        LHRentalHouseListResponse houseResponse = publicDataApiService.getLHRentalHouseList(
                                                1, 100, brtc, "000", null).block();
                                        
                                        List<LHRentalHouseListResponse.Item> items = houseResponse != null ? houseResponse.getItems() : null;
                                        if (items != null && !items.isEmpty()) {
                                            allHouseItems.addAll(items);
                                            successCount++;
                                        }
                                    } catch (Exception e) {
                                        // 에러는 무시하고 계속 진행
                                    }
                                    
                                    try {
                                        Thread.sleep(100);
                                    } catch (InterruptedException e) {
                                        Thread.currentThread().interrupt();
                                        break;
                                    }
                                }
                            }
                            
                            System.out.println("단지정보 API 호출 완료: " + processedCount + "개 중 " + successCount + "개 성공");
                            System.out.println("수집된 단지정보: " + allHouseItems.size() + "건");
                            
                            // 3단계: 결과 처리
                            LHRentalHouseListResponse houseListResponse = new LHRentalHouseListResponse();
                            if (!allHouseItems.isEmpty()) {
                                LHRentalHouseListResponse.Body body = new LHRentalHouseListResponse.Body();
                                LHRentalHouseListResponse.Items items = new LHRentalHouseListResponse.Items();
                                items.setItem(allHouseItems);
                                body.setItems(items);
                                LHRentalHouseListResponse.Response response = new LHRentalHouseListResponse.Response();
                                response.setBody(body);
                                houseListResponse.setResponse(response);
                            }
                            
                            processApiResults(houseListResponse, noticeResponse);
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
     * 전체 지역 데이터 조회 (모든 광역시도 코드와 시군구 코드 조합 순회)
     * @return 수집된 단지정보 리스트
     */
    private List<LHRentalHouseListResponse.Item> syncAllRegionsForHouseList() {
        int totalCombinations = 100 * 1000; // 100,000개 조합
        int processedCount = 0;
        int successCount = 0;
        List<LHRentalHouseListResponse.Item> allHouseItems = new ArrayList<>();
        
        System.out.println("총 " + totalCombinations + "개 조합을 순회합니다. (시간이 오래 걸릴 수 있습니다)");
        
        // 모든 광역시도 코드(0-99)와 시군구 코드(0-999) 조합 순회
        for (int brtc = 0; brtc < 100; brtc++) {
            String brtcCode = String.format("%02d", brtc); // 2자리로 포맷 (00, 01, ..., 99)
            
            for (int signgu = 0; signgu < 1000; signgu++) {
                String signguCode = String.format("%03d", signgu); // 3자리로 포맷 (000, 001, ..., 999)
                processedCount++;
                
                // 진행 상황 출력 (1000개마다)
                if (processedCount % 1000 == 0) {
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
                    }
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
        Map<String, LHRentalNoticeResponse.Item> noticeMap = new HashMap<>();
        if (noticeResponse != null && noticeResponse.getDsList() != null) {
            List<LHRentalNoticeResponse.Item> noticeItems = noticeResponse.getDsList();
            System.out.println("공고문 데이터 수집: " + noticeItems.size() + "건");
            
            // 공고문 데이터를 단지 식별자로 매핑
            for (LHRentalNoticeResponse.Item notice : noticeItems) {
                // 단지 식별자(hsmpSn)를 우선 사용, 없으면 단지명(hsmpNm), 없으면 공고ID(PAN_ID) 사용
                String key = notice.getHsmpSn() != null && !notice.getHsmpSn().isEmpty()
                        ? notice.getHsmpSn()
                        : (notice.getHsmpNm() != null && !notice.getHsmpNm().isEmpty()
                            ? notice.getHsmpNm()
                            : (notice.getPanId() != null ? notice.getPanId() : ""));
                if (!key.isEmpty()) {
                    // 이미 존재하는 경우 더 최신 공고문으로 업데이트
                    if (!noticeMap.containsKey(key) || 
                        isNewerNotice(notice, noticeMap.get(key))) {
                        noticeMap.put(key, notice);
                    }
                }
            }
            System.out.println("공고문 데이터 Map 변환 완료: " + noticeMap.size() + "건");
        }
        
        // 단지정보가 있는 경우 처리
        if (houseItems != null && !houseItems.isEmpty()) {
            // 단지정보 데이터를 Housing 엔티티로 변환하여 저장
            int savedCount = 0;
            int updatedCount = 0;
            
            for (LHRentalHouseListResponse.Item item : houseItems) {
                try {
                    Housing housing = convertToHousing(item);
                    
                    // 공고문 데이터와 병합 (단지 식별자 또는 단지명으로 매칭)
                    String housingId = housing.getHousingId();
                    String housingName = housing.getName();
                    
                    // 1순위: 단지 식별자로 매칭
                    if (housingId != null && noticeMap.containsKey(housingId)) {
                        updateHousingWithNotice(housing, noticeMap.get(housingId));
                    }
                    // 2순위: 단지명으로 매칭
                    else if (housingName != null && noticeMap.containsKey(housingName)) {
                        updateHousingWithNotice(housing, noticeMap.get(housingName));
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
                }
            }
            
            System.out.println("========================================");
            System.out.println("임대주택 데이터 동기화 완료:");
            System.out.println("  - 단지정보: " + houseItems.size() + "건");
            System.out.println("  - 공고문: " + noticeMap.size() + "건");
            System.out.println("  - 저장: " + savedCount + "건, 업데이트: " + updatedCount + "건");
            System.out.println("========================================");
        } 
        // 단지정보가 없고 공고문만 있는 경우
        else if (noticeMap != null && !noticeMap.isEmpty()) {
            // 공고문 데이터만으로 Housing 엔티티 생성
            int savedCount = 0;
            int updatedCount = 0;
            
            for (LHRentalNoticeResponse.Item notice : noticeMap.values()) {
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
            System.out.println("  - 공고문: " + noticeMap.size() + "건");
            System.out.println("  - 저장: " + savedCount + "건, 업데이트: " + updatedCount + "건");
            System.out.println("========================================");
        } else {
            // 데이터가 없는 경우는 조용히 넘어감 (전체 조회 시 많은 조합이 데이터가 없을 수 있음)
        }
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
        // 신청 시작일 파싱
        if (notice.getRceptBgnde() != null && !notice.getRceptBgnde().isEmpty()) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
                java.util.Date date = sdf.parse(notice.getRceptBgnde());
                housing.setApplicationStart(new Date(date.getTime()));
            } catch (ParseException e) {
                // 파싱 실패 시 무시
            }
        }
        
        // 신청 종료일 파싱
        if (notice.getRceptEndde() != null && !notice.getRceptEndde().isEmpty()) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
                java.util.Date date = sdf.parse(notice.getRceptEndde());
                housing.setApplicationEnd(new Date(date.getTime()));
            } catch (ParseException e) {
                // 파싱 실패 시 무시
            }
        }
        
        // 공고 URL이 있으면 link 업데이트
        if (notice.getPblancUrl() != null && !notice.getPblancUrl().isEmpty()) {
            housing.setLink(notice.getPblancUrl());
        }
        
        // 공고일이 있으면 참고용으로 저장 (필요시)
        // housing.setPblancDe(notice.getPblancDe());
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
}

