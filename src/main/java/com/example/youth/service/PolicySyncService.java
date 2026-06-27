package com.example.youth.service;

import com.example.youth.DB.Policy;
import com.example.youth.dto.publicdata.YouthPolicyResponse;
import com.example.youth.repository.PolicyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.sql.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

@Service
public class PolicySyncService {

    @Autowired
    private PublicDataApiService publicDataApiService;

    @Autowired
    private PolicyRepository policyRepository;

    @Autowired
    private PolicyPreprocessorService policyPreprocessorService;

    @Autowired
    private PolicyRegionService policyRegionService;

    @Autowired
    private UserPolicyRecommendationService userPolicyRecommendationService;

    /**
     * 잘못 저장된 null 값이 있는 정책 데이터 삭제
     * - 제목이 null이거나 빈 문자열인 정책
     * - 잘못 생성된 ID를 가진 정책 (POLICY_로 시작)
     */
    @Transactional
    public int deleteInvalidPolicies() {
        System.out.println("========================================");
        System.out.println("잘못 저장된 정책 데이터 삭제 시작...");
        System.out.println("========================================");

        // 제목이 null이거나 빈 문자열인 정책 찾기
        List<Policy> nullTitlePolicies = policyRepository.findPoliciesWithNullOrEmptyTitle();
        System.out.println("제목이 null이거나 빈 문자열인 정책: " + nullTitlePolicies.size() + "건");

        // 잘못 생성된 ID를 가진 정책 찾기
        List<Policy> invalidIdPolicies = policyRepository.findPoliciesWithInvalidId();
        System.out.println("잘못 생성된 ID를 가진 정책: " + invalidIdPolicies.size() + "건");

        // 중복 제거 (두 조건에 모두 해당하는 경우)
        List<String> policyIdsToDelete = new ArrayList<>();
        for (Policy policy : nullTitlePolicies) {
            if (!policyIdsToDelete.contains(policy.getPolicyId())) {
                policyIdsToDelete.add(policy.getPolicyId());
            }
        }
        for (Policy policy : invalidIdPolicies) {
            if (!policyIdsToDelete.contains(policy.getPolicyId())) {
                policyIdsToDelete.add(policy.getPolicyId());
            }
        }

        System.out.println("삭제할 정책 총 개수: " + policyIdsToDelete.size() + "건");

        // 삭제 실행
        int deletedCount = 0;
        for (String policyId : policyIdsToDelete) {
            try {
                policyRepository.deleteById(policyId);
                deletedCount++;
            } catch (Exception e) {
                System.err.println("정책 삭제 실패: " + policyId + " - " + e.getMessage());
            }
        }

        System.out.println("========================================");
        System.out.println("잘못 저장된 정책 데이터 삭제 완료:");
        System.out.println("  - 삭제된 정책: " + deletedCount + "건");
        System.out.println("========================================");

        return deletedCount;
    }

    /**
     * 온통청년 API에서 청년정책 정보를 가져와 DB에 동기화
     * 전체 데이터를 페이징 처리하여 모두 가져옴
     */
    @Transactional
    public void syncYouthPolicyData() {
        final int pageSize = 100; // 페이지당 데이터 개수 (파이썬 코드 참고)

        System.out.println("========================================");
        System.out.println("청년정책 데이터 동기화 시작...");
        System.out.println("========================================");

        // 첫 번째 페이지로 전체 개수 확인
        publicDataApiService.getYouthPolicyList(1, pageSize)
                .flatMap(firstResponse -> {
                    // 전체 개수 확인 (파이썬 코드 참고: result.pagging.totCount)
                    Integer totalCount = 0;
                    if (firstResponse != null
                            && firstResponse.getResult() != null
                            && firstResponse.getResult().getPagging() != null) {
                        totalCount = firstResponse.getResult().getPagging().getTotCount();
                        if (totalCount == null) {
                            totalCount = 0;
                        }
                    }
                    
                    System.out.println("전체 정책 개수: " + totalCount + "건");
                    
                    // 전체 페이지 수 계산
                    int totalPages = (int) Math.ceil((double) totalCount / pageSize);
                    System.out.println("총 페이지 수: " + totalPages);

                    // 첫 페이지 데이터 수집 (파이썬 코드 참고: result.youthPolicyList)
                    List<YouthPolicyResponse.Item> firstPageItems = new ArrayList<>();
                    if (firstResponse != null
                            && firstResponse.getResult() != null
                            && firstResponse.getResult().getYouthPolicyList() != null) {
                        firstPageItems.addAll(firstResponse.getResult().getYouthPolicyList());
                        System.out.println("첫 페이지 수집: " + firstPageItems.size() + "건");
                    }

                    // 나머지 페이지들을 순차적으로 가져와서 리스트로 수집
                    if (totalPages > 1) {
                        List<Mono<List<YouthPolicyResponse.Item>>> pageMonos = new ArrayList<>();
                        
                        for (int page = 2; page <= totalPages; page++) {
                            final int currentPage = page;
                            Mono<List<YouthPolicyResponse.Item>> pageMono = publicDataApiService.getYouthPolicyList(currentPage, pageSize)
                                    .map(response -> {
                                        if (response != null
                                                && response.getResult() != null
                                                && response.getResult().getYouthPolicyList() != null) {
                                            System.out.println("페이지 " + currentPage + "/" + totalPages + " 수집: " + response.getResult().getYouthPolicyList().size() + "건");
                                            return response.getResult().getYouthPolicyList();
                                        }
                                        return new ArrayList<YouthPolicyResponse.Item>();
                                    })
                                    .onErrorResume(error -> {
                                        System.err.println("페이지 " + currentPage + " 조회 실패: " + error.getMessage());
                                        return Mono.just(new ArrayList<YouthPolicyResponse.Item>());
                                    });
                            pageMonos.add(pageMono);
                        }
                        
                        // 모든 페이지를 순차적으로 수집
                        return Flux.fromIterable(pageMonos)
                                .concatMap(mono -> mono) // 순차 처리
                                .collectList()
                                .map(pageResults -> {
                                    // 모든 페이지 결과를 합치기
                                    List<YouthPolicyResponse.Item> allItems = new ArrayList<>(firstPageItems);
                                    for (List<YouthPolicyResponse.Item> pageItems : pageResults) {
                                        allItems.addAll(pageItems);
                                    }
                                    return allItems;
                                });
                    } else {
                        return Mono.just(firstPageItems);
                    }
                })
                .subscribe(
                        allItems -> {
                            // 모든 페이지의 데이터를 수집한 후 DB에 저장
                            System.out.println("수집된 총 정책 개수: " + allItems.size() + "건");
                            System.out.println("DB 저장 시작...");

                            int savedCount = 0;
                            int updatedCount = 0;

                            for (YouthPolicyResponse.Item item : allItems) {
                                try {
                                    // 디버깅: 첫 번째 아이템의 필드값 확인
                                    if (allItems.indexOf(item) == 0) {
                                        System.out.println("=== 첫 번째 정책 아이템 필드 확인 ===");
                                        System.out.println("plcyNo: " + item.getPlcyNo());
                                        System.out.println("plcyNm: " + item.getPlcyNm());
                                        System.out.println("plcyExplnCn: " + (item.getPlcyExplnCn() != null && item.getPlcyExplnCn().length() > 50 ? item.getPlcyExplnCn().substring(0, 50) : item.getPlcyExplnCn()));
                                        System.out.println("lclsfNm: " + item.getLclsfNm());
                                        System.out.println("mclsfNm: " + item.getMclsfNm());
                                        System.out.println("sprtTrgtMinAge: " + item.getSprtTrgtMinAge());
                                        System.out.println("sprtTrgtMaxAge: " + item.getSprtTrgtMaxAge());
                                    }
                                    
                                    Policy policy = convertToPolicy(item);

                                    // DB에 저장 (이미 존재하면 업데이트)
                                    boolean exists = policyRepository.existsById(policy.getPolicyId());
                                    policyRepository.findById(policy.getPolicyId())
                                            .ifPresentOrElse(
                                                    existing -> {
                                                        // 기존 데이터 업데이트
                                                        updatePolicyData(existing, policy);
                                                        policyRepository.save(existing);
                                                    },
                                                    () -> {
                                                        // 새 데이터 저장
                                                        policyRepository.save(policy);
                                                    }
                                            );
                                    if (exists) {
                                        updatedCount++;
                                    } else {
                                        savedCount++;
                                    }
                                } catch (Exception e) {
                                    System.err.println("정책 저장 실패: " + (item.getPlcyNm() != null ? item.getPlcyNm() : "알 수 없음") + " - " + e.getMessage());
                                }
                            }

                            System.out.println("========================================");
                            System.out.println("청년정책 데이터 동기화 완료:");
                            System.out.println("  - 수집된 정책: " + allItems.size() + "건");
                            System.out.println("  - 저장: " + savedCount + "건, 업데이트: " + updatedCount + "건");
                            System.out.println("========================================");

                            runPostSyncPipeline();
                        },
                        error -> {
                            System.err.println("========================================");
                            System.err.println("청년정책 API 호출 실패: " + error.getMessage());
                            System.err.println("========================================");
                            error.printStackTrace();
                        }
                );
    }

    /**
     * sync 없이 DB policy.region만 제목·요약 등에서 backfill.
     */
    public int backfillPolicyRegions() {
        return policyPreprocessorService.backfillAllPolicyRegions();
    }

    /**
     * sync 직후: 정책 전처리 → 전 사용자 Top-K 배치 재계산.
     */
    public void runPostSyncPipeline() {
        System.out.println("========================================");
        System.out.println("정책 sync 후처리 시작 (전처리 + Top-K 배치)");
        System.out.println("========================================");
        try {
            policyPreprocessorService.preprocessAllPolicies();
            userPolicyRecommendationService.recomputeAllUsers();
            System.out.println("정책 sync 후처리 완료");
        } catch (Exception e) {
            System.err.println("정책 sync 후처리 실패: " + e.getMessage());
            e.printStackTrace();
        }
        System.out.println("========================================");
    }

    /**
     * YouthPolicyResponse.Item을 Policy 엔티티로 변환
     * 실제 API 응답 필드명에 맞춰 수정
     */
    private Policy convertToPolicy(YouthPolicyResponse.Item item) {
        Policy.PolicyBuilder builder = Policy.builder();

        // PolicyId: plcyNo를 사용 (정책번호)
        builder.policyId(item.getPlcyNo() != null && !item.getPlcyNo().isEmpty() 
                ? item.getPlcyNo() 
                : generatePolicyId(item));

        // Title: 정책명 (plcyNm)
        builder.title(item.getPlcyNm() != null ? item.getPlcyNm() : "");

        // Summary: 정책설명내용 (plcyExplnCn)
        builder.summary(item.getPlcyExplnCn() != null ? item.getPlcyExplnCn() : "");

        // Category: 대분류명 또는 중분류명
        if (item.getLclsfNm() != null && !item.getLclsfNm().isEmpty()) {
            builder.category(item.getLclsfNm());
        } else if (item.getMclsfNm() != null && !item.getMclsfNm().isEmpty()) {
            builder.category(item.getMclsfNm());
        } else {
            builder.category(null);
        }

        // Region: 기관명·제목에서 지역 파싱
        String region = policyRegionService.inferRegion(
                item.getPlcyNm(),
                item.getSprvsnInstCdNm(),
                item.getOperInstCdNm(),
                item.getRgtrInstCdNm());
        builder.region(region);

        // AgeStart, AgeEnd: 지원대상연령
        if (item.getSprtTrgtMinAge() != null && !item.getSprtTrgtMinAge().isEmpty()) {
            try {
                builder.ageStart(Integer.parseInt(item.getSprtTrgtMinAge()));
            } catch (NumberFormatException e) {
                System.err.println("최소연령 파싱 실패: " + item.getSprtTrgtMinAge());
            }
        }
        if (item.getSprtTrgtMaxAge() != null && !item.getSprtTrgtMaxAge().isEmpty()) {
            try {
                builder.ageEnd(Integer.parseInt(item.getSprtTrgtMaxAge()));
            } catch (NumberFormatException e) {
                System.err.println("최대연령 파싱 실패: " + item.getSprtTrgtMaxAge());
            }
        }

        // Eligibility: 정책지원내용, 추가신청자격조건내용, 제출서류내용 등을 합쳐서 저장
        StringBuilder eligibilityBuilder = new StringBuilder();
        if (item.getPlcySprtCn() != null && !item.getPlcySprtCn().isEmpty()) {
            eligibilityBuilder.append("지원내용: ").append(item.getPlcySprtCn()).append("\n");
        }
        if (item.getAddAplyQlfcCndCn() != null && !item.getAddAplyQlfcCndCn().isEmpty()) {
            eligibilityBuilder.append("추가신청자격조건: ").append(item.getAddAplyQlfcCndCn()).append("\n");
        }
        if (item.getSbmsnDcmntCn() != null && !item.getSbmsnDcmntCn().isEmpty()) {
            eligibilityBuilder.append("제출서류: ").append(item.getSbmsnDcmntCn()).append("\n");
        }
        if (item.getSrngMthdCn() != null && !item.getSrngMthdCn().isEmpty()) {
            eligibilityBuilder.append("선정방법: ").append(item.getSrngMthdCn()).append("\n");
        }
        if (item.getPlcyAplyMthdCn() != null && !item.getPlcyAplyMthdCn().isEmpty()) {
            eligibilityBuilder.append("신청방법: ").append(item.getPlcyAplyMthdCn()).append("\n");
        }
        if (item.getEtcMttrCn() != null && !item.getEtcMttrCn().isEmpty()) {
            eligibilityBuilder.append("기타사항: ").append(item.getEtcMttrCn()).append("\n");
        }
        builder.eligibility(eligibilityBuilder.length() > 0 ? eligibilityBuilder.toString() : null);

        // ApplicationStart, ApplicationEnd: 신청일자 또는 사업기간 파싱
        // aplyYmd 형식: "20230201 ~ 20230228" 또는 "20250101 ~ 20250331"
        if (item.getAplyYmd() != null && !item.getAplyYmd().isEmpty()) {
            parseApplicationPeriod(item.getAplyYmd(), builder);
        } else {
            // aplyYmd가 없으면 사업기간 사용
            if (item.getBizPrdBgngYmd() != null && !item.getBizPrdBgngYmd().isEmpty() &&
                item.getBizPrdEndYmd() != null && !item.getBizPrdEndYmd().isEmpty()) {
                parseDateRange(item.getBizPrdBgngYmd(), item.getBizPrdEndYmd(), builder);
            }
        }

        // Link1: 신청URL주소
        builder.link1(item.getAplyUrlAddr());

        // Link2: 참고URL주소1 또는 참고URL주소2
        if (item.getRefUrlAddr1() != null && !item.getRefUrlAddr1().isEmpty()) {
            builder.link2(item.getRefUrlAddr1());
        } else {
            builder.link2(item.getRefUrlAddr2());
        }

        return builder.build();
    }

    /**
     * 연령 정보 파싱 (예: "19~39세" -> ageStart=19, ageEnd=39)
     */
    private void parseAgeRange(String ageInfo, Policy.PolicyBuilder builder) {
        if (ageInfo == null || ageInfo.isEmpty()) {
            // minAge, maxAge 필드 확인
            return;
        }

        try {
            // "19~39세", "19세 이상", "39세 이하" 등의 형식 처리
            ageInfo = ageInfo.trim();
            if (ageInfo.contains("~")) {
                String[] parts = ageInfo.split("~");
                if (parts.length == 2) {
                    String startStr = parts[0].replaceAll("[^0-9]", "");
                    String endStr = parts[1].replaceAll("[^0-9]", "");
                    if (!startStr.isEmpty()) {
                        builder.ageStart(Integer.parseInt(startStr));
                    }
                    if (!endStr.isEmpty()) {
                        builder.ageEnd(Integer.parseInt(endStr));
                    }
                }
            } else if (ageInfo.contains("이상")) {
                String startStr = ageInfo.replaceAll("[^0-9]", "");
                if (!startStr.isEmpty()) {
                    builder.ageStart(Integer.parseInt(startStr));
                }
            } else if (ageInfo.contains("이하")) {
                String endStr = ageInfo.replaceAll("[^0-9]", "");
                if (!endStr.isEmpty()) {
                    builder.ageEnd(Integer.parseInt(endStr));
                }
            }
        } catch (Exception e) {
            System.err.println("연령 정보 파싱 실패: " + ageInfo + " - " + e.getMessage());
        }
    }

    /**
     * 신청기간 파싱 (예: "20230201 ~ 20230228" -> applicationStart, applicationEnd)
     */
    private void parseApplicationPeriod(String aplyYmd, Policy.PolicyBuilder builder) {
        if (aplyYmd == null || aplyYmd.isEmpty()) {
            return;
        }

        try {
            aplyYmd = aplyYmd.trim();

            if (aplyYmd.contains("~")) {
                String[] parts = aplyYmd.split("~");
                if (parts.length == 2) {
                    String startStr = parts[0].trim();
                    String endStr = parts[1].trim();
                    
                    // yyyyMMdd 형식 파싱
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
                    try {
                        java.util.Date startDate = sdf.parse(startStr);
                        builder.applicationStart(new Date(startDate.getTime()));
                    } catch (ParseException e) {
                        System.err.println("신청시작일 파싱 실패: " + startStr);
                    }
                    try {
                        java.util.Date endDate = sdf.parse(endStr);
                        builder.applicationEnd(new Date(endDate.getTime()));
                    } catch (ParseException e) {
                        System.err.println("신청종료일 파싱 실패: " + endStr);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("신청기간 파싱 실패: " + aplyYmd + " - " + e.getMessage());
        }
    }

    /**
     * 날짜 범위 파싱 (yyyyMMdd 형식)
     */
    private void parseDateRange(String startYmd, String endYmd, Policy.PolicyBuilder builder) {
        if (startYmd == null || startYmd.isEmpty() || endYmd == null || endYmd.isEmpty()) {
            return;
        }

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
            try {
                java.util.Date startDate = sdf.parse(startYmd);
                builder.applicationStart(new Date(startDate.getTime()));
            } catch (ParseException e) {
                System.err.println("시작일 파싱 실패: " + startYmd);
            }
            try {
                java.util.Date endDate = sdf.parse(endYmd);
                builder.applicationEnd(new Date(endDate.getTime()));
            } catch (ParseException e) {
                System.err.println("종료일 파싱 실패: " + endYmd);
            }
        } catch (Exception e) {
            System.err.println("날짜 범위 파싱 실패: " + startYmd + " ~ " + endYmd + " - " + e.getMessage());
        }
    }

    /**
     * PolicyId 생성 (plcyNo가 없을 경우)
     */
    private String generatePolicyId(YouthPolicyResponse.Item item) {
        // plcyNo가 없으면 정책명과 타임스탬프를 조합하여 ID 생성
        String base = item.getPlcyNm() != null ? item.getPlcyNm() : "POLICY";
        String timestamp = String.valueOf(System.currentTimeMillis());
        String id = (base + "_" + timestamp).replaceAll("[^a-zA-Z0-9_]", "_");
        return id.length() > 50 ? id.substring(0, 50) : id;
    }

    /**
     * 기존 Policy 데이터 업데이트
     */
    private void updatePolicyData(Policy existing, Policy newData) {
        existing.setTitle(newData.getTitle());
        existing.setSummary(newData.getSummary());
        existing.setCategory(newData.getCategory());
        existing.setRegion(newData.getRegion());
        existing.setAgeStart(newData.getAgeStart());
        existing.setAgeEnd(newData.getAgeEnd());
        existing.setEligibility(newData.getEligibility());
        existing.setApplicationStart(newData.getApplicationStart());
        existing.setApplicationEnd(newData.getApplicationEnd());
        existing.setLink1(newData.getLink1());
        existing.setLink2(newData.getLink2());
        policyPreprocessorService.preprocessPolicy(existing);
    }
}

