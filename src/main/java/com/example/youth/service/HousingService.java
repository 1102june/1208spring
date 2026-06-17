package com.example.youth.service;

import com.example.youth.DB.ActiveStatus;
import com.example.youth.DB.Housing;
import com.example.youth.DB.HousingComplex;
import com.example.youth.DB.HousingNotice;
import com.example.youth.common.ContentType;
import com.example.youth.dto.HousingResponse;
import com.example.youth.dto.HousingComplexResponse;
import com.example.youth.dto.HousingNoticeResponse;
import com.example.youth.dto.UserProfileResponse;
import com.example.youth.repository.BookmarkRepository;
import com.example.youth.repository.HousingComplexRepository;
import com.example.youth.repository.HousingNoticeRepository;
import com.example.youth.repository.HousingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.Comparator;
import java.util.stream.Collectors;

/**
 * 두 지점 간 거리 계산 (Haversine 공식)
 */
class DistanceCalculator {
    private static final double EARTH_RADIUS = 6371000; // 지구 반지름 (미터)
    
    public static double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return EARTH_RADIUS * c; // 미터 단위
    }
}

@Service
public class HousingService {

    @Autowired
    private HousingRepository housingRepository; // 기존 호환성 유지용 (deprecated)

    @Autowired
    private HousingNoticeRepository housingNoticeRepository;

    @Autowired
    private HousingComplexRepository housingComplexRepository;

    @Autowired
    private BookmarkRepository bookmarkRepository;

    @Autowired
    private UserService userService;

    // true이면 신청기간(날짜) 필터를 무시하고 DB의 모든 공고를 노출 (비공개 테스트용)
    @Value("${app.housing.show-all:false}")
    private boolean showAllHousing;

    /**
     * HousingNotice와 HousingComplex를 병합하여 HousingResponse로 변환
     */
    private HousingResponse convertToResponse(HousingNotice notice, HousingComplex complex, String userId, Double userLat, Double userLon) {
        // housingId는 complex의 complexId를 우선 사용, 없으면 notice의 noticeId 사용
        String housingId = (complex != null && complex.getComplexId() != null) 
                ? complex.getComplexId() 
                : (notice != null ? notice.getNoticeId() : null);
        
        // 북마크 여부 확인
        boolean isBookmarked = false;
        if (housingId != null && userId != null) {
            isBookmarked = bookmarkRepository
                    .findByUser_UserIdAndContentTypeAndContentId(userId, ContentType.housing, housingId)
                    .map(bookmark -> bookmark.getIsActive() == ActiveStatus.Y)
                    .orElse(false);
        }

        HousingResponse.HousingResponseBuilder builder = HousingResponse.builder()
                .housingId(housingId)
                .isBookmarked(isBookmarked);
        
        // 단지명: complex 우선, 없으면 notice
        String name = (complex != null && complex.getHsmpNm() != null) 
                ? complex.getHsmpNm() 
                : (notice != null ? notice.getHsmpNm() : null);
        builder.name(name);
        
        // 주소: complex에서 가져옴
        builder.address(complex != null ? complex.getRnAdres() : null);
        
        // 단지정보 (complex) 필드들
        if (complex != null) {
            builder.supplyArea(complex.getSupplyArea())
                   .completeDate(complex.getCompleteDate())
                   .organization(complex.getInsttNm())
                   .heatingType(complex.getHeatMthdDetailNm())
                   .elevator(complex.getElevator())
                   .parkingSpaces(complex.getParkingSpaces())
                   .deposit(complex.getDeposit())
                   .monthlyRent(complex.getMonthlyRent())
                   .totalUnits(complex.getTotalUnits())
                   .latitude(complex.getLatitude())
                   .longitude(complex.getLongitude())
                   .housingType(complex.getSuplyTyNm());
        }
        
        // 공고문 (notice) 필드들
        if (notice != null) {
            builder.applicationStart(notice.getApplicationStart())
                   .applicationEnd(notice.getApplicationEnd())
                   .link(notice.getDtlUrl());
            
            // 단지정보에 없는 필드만 공고문에서 채움
            if (builder.build().getHousingType() == null && notice.getAisTpCdNm() != null) {
                builder.housingType(notice.getAisTpCdNm());
            }
        }
        
        HousingResponse response = builder.build();
        
        // 사용자 위치가 제공된 경우 거리 계산
        if (userLat != null && userLon != null && 
            response.getLatitude() != null && response.getLongitude() != null) {
            double distance = DistanceCalculator.calculateDistance(
                    userLat, userLon, 
                    response.getLatitude(), response.getLongitude()
            );
            response.setDistanceFromUser(distance);
        }
        
        return response;
    }
    
    // 기존 Housing 엔티티 변환 (호환성 유지용)
    @Deprecated
    private HousingResponse convertToResponse(Housing housing, String userId) {
        return convertToResponse(housing, userId, null, null);
    }
    
    @Deprecated
    private HousingResponse convertToResponse(Housing housing, String userId, Double userLat, Double userLon) {
        boolean isBookmarked = bookmarkRepository
                .findByUser_UserIdAndContentTypeAndContentId(userId, ContentType.housing, housing.getHousingId())
                .map(bookmark -> bookmark.getIsActive() == ActiveStatus.Y)
                .orElse(false);

        HousingResponse.HousingResponseBuilder builder = HousingResponse.builder()
                .housingId(housing.getHousingId())
                .name(housing.getName())
                .address(housing.getAddress())
                .supplyArea(housing.getSupplyArea())
                .completeDate(housing.getCompleteDate())
                .organization(housing.getOrganization())
                .applicationStart(housing.getApplicationStart())
                .applicationEnd(housing.getApplicationEnd())
                .heatingType(housing.getHeatingType())
                .elevator(housing.getElevator())
                .parkingSpaces(housing.getParkingSpaces())
                .deposit(housing.getDeposit())
                .monthlyRent(housing.getMonthlyRent())
                .totalUnits(housing.getTotalUnits())
                .link(housing.getLink())
                .isBookmarked(isBookmarked)
                .latitude(housing.getLatitude())
                .longitude(housing.getLongitude())
                .housingType(housing.getHousingType());
        
        // 사용자 위치가 제공된 경우 거리 계산
        if (userLat != null && userLon != null && 
            housing.getLatitude() != null && housing.getLongitude() != null) {
            double distance = DistanceCalculator.calculateDistance(
                    userLat, userLon, 
                    housing.getLatitude(), housing.getLongitude()
            );
            builder.distanceFromUser(distance);
        }
        
        return builder.build();
    }

    /**
     * 두 테이블을 병합하여 조회하는 헬퍼 메서드
     */
    private List<HousingResponse> mergeNoticesAndComplexes(
            List<HousingNotice> notices, 
            List<HousingComplex> complexes, 
            String userId, 
            Double userLat, 
            Double userLon) {
        
        // hsmpSn 또는 hsmpNm으로 매칭
        Map<String, HousingNotice> noticeMap = new HashMap<>();
        Map<String, HousingComplex> complexMap = new HashMap<>();
        
        // 공고문을 hsmpSn 또는 hsmpNm으로 매핑
        for (HousingNotice notice : notices) {
            if (notice.getHsmpSn() != null && !notice.getHsmpSn().isEmpty()) {
                noticeMap.put(notice.getHsmpSn(), notice);
            } else if (notice.getHsmpNm() != null && !notice.getHsmpNm().isEmpty()) {
                noticeMap.put(notice.getHsmpNm(), notice);
            }
        }
        
        // 단지정보를 complexId(hsmpSn) 또는 hsmpNm으로 매핑
        for (HousingComplex complex : complexes) {
            if (complex.getComplexId() != null) {
                complexMap.put(complex.getComplexId(), complex);
            }
            if (complex.getHsmpNm() != null && !complex.getHsmpNm().isEmpty()) {
                complexMap.put(complex.getHsmpNm(), complex);
            }
        }
        
        // 모든 단지정보를 기준으로 병합 (단지정보가 있으면 공고문과 매칭)
        Set<String> processedIds = new HashSet<>();
        List<HousingResponse> result = new ArrayList<>();
        
        // 단지정보 기준으로 병합
        for (HousingComplex complex : complexes) {
            String key = complex.getComplexId() != null ? complex.getComplexId() : complex.getHsmpNm();
            if (key == null || processedIds.contains(key)) continue;
            
            HousingNotice matchedNotice = noticeMap.get(complex.getComplexId());
            if (matchedNotice == null && complex.getHsmpNm() != null) {
                matchedNotice = noticeMap.get(complex.getHsmpNm());
            }
            
            result.add(convertToResponse(matchedNotice, complex, userId, userLat, userLon));
            processedIds.add(key);
        }
        
        // 공고문만 있고 단지정보가 없는 경우
        for (HousingNotice notice : notices) {
            String key = notice.getHsmpSn() != null ? notice.getHsmpSn() : notice.getHsmpNm();
            if (key == null || processedIds.contains(key)) continue;
            
            if (!complexMap.containsKey(key)) {
                result.add(convertToResponse(notice, null, userId, userLat, userLon));
                processedIds.add(key);
            }
        }
        
        return result;
    }

    // ID로 임대주택 조회
    public HousingResponse getHousingById(String housingId, String userId) {
        // 먼저 단지정보에서 조회 (complexId로)
        Optional<HousingComplex> complexOpt = housingComplexRepository.findById(housingId);
        HousingComplex complex = complexOpt.orElse(null);
        
        // 공고문에서 조회 (hsmpSn 또는 hsmpNm으로)
        List<HousingNotice> notices = housingNoticeRepository.findByIdentifier(housingId);
        HousingNotice notice = notices.isEmpty() ? null : notices.get(0);
        
        // 단지명으로도 조회 시도
        if (complex == null) {
            complexOpt = housingComplexRepository.findByHsmpNm(housingId);
            complex = complexOpt.orElse(null);
        }
        
        if (complex == null && notice == null) {
            System.err.println("HousingService: 임대주택 조회 실패 - housingId=" + housingId + ", userId=" + userId);
            System.err.println("  - complexId로 조회: 실패");
            System.err.println("  - hsmpSn/hsmpNm으로 조회: 실패");
            System.err.println("  - 단지명으로 조회: 실패");
            throw new RuntimeException("임대주택을 찾을 수 없습니다: " + housingId);
        }
        
        return convertToResponse(notice, complex, userId, null, null);
    }

    // 활성 임대주택 목록 조회 (show-all 모드면 날짜 무시하고 전체 공고)
    public List<HousingResponse> getActiveHousing(String userId) {
        List<HousingNotice> notices = showAllHousing
                ? housingNoticeRepository.findAll()
                : housingNoticeRepository.findActiveNotices(new java.util.Date());
        List<HousingComplex> allComplexes = housingComplexRepository.findAll();
        
        return mergeNoticesAndComplexes(notices, allComplexes, userId, null, null);
    }

    // 지역별 임대주택 조회
    public List<HousingResponse> getHousingByRegion(String region, String userId) {
        List<HousingComplex> complexes = housingComplexRepository.findByRegion(region);
        List<HousingNotice> allNotices = housingNoticeRepository.findAll();
        
        return mergeNoticesAndComplexes(allNotices, complexes, userId, null, null);
    }
    
    /**
     * 사용자 위치 기반 임대주택 추천 목록 조회
     * @param userId 사용자 ID
     * @param userLat 사용자 위도
     * @param userLon 사용자 경도
     * @param radius 반경 (미터, 기본값: 5000m = 5km)
     * @param limit 최대 개수 (기본값: 50)
     * @return 임대주택 목록 (거리순 정렬)
     */
    public List<HousingResponse> getRecommendedHousing(
            String userId, 
            Double userLat, 
            Double userLon, 
            Integer radius, 
            Integer limit) {
        
        List<HousingNotice> activeNotices = showAllHousing
                ? housingNoticeRepository.findAll()
                : housingNoticeRepository.findActiveNotices(new java.util.Date());
        List<HousingComplex> allComplexes = housingComplexRepository.findAll();

        List<HousingResponse> merged = mergeNoticesAndComplexes(activeNotices, allComplexes, userId, userLat, userLon);

        int searchRadius = radius != null ? radius : 5000; // 기본 5km
        int maxResults = limit != null ? limit : 50;

        // 프로필 기반 지역 추천을 위해 사용자 프로필 조회 (실패 시에는 지역 필터 없이 진행)
        String profileRegion = null;
        try {
            UserProfileResponse profile = userService.getUserProfile(userId);
            if (profile != null) {
                profileRegion = profile.getRegion();
            }
        } catch (Exception e) {
            // 프로필 조회 실패 시 지역 필터 없이 진행
            System.err.println("프로필 조회 중 오류 (지역 필터 없이 진행): " + e.getMessage());
        }
        final String userRegion = profileRegion;

        // 신청 기간이 현재 날짜 기준으로 남아있는 주택만 사용 (마감일 null=상시 포함, show-all이면 전체)
        LocalDate today = LocalDate.now();
        List<HousingResponse> activeOnly = merged.stream()
                .filter(housing -> {
                    if (showAllHousing) {
                        return true;
                    }
                    if (housing.getApplicationEnd() == null) {
                        return true;
                    }
                    // java.sql.Date를 LocalDate로 직접 변환
                    LocalDate endDate = housing.getApplicationEnd().toLocalDate();
                    return !endDate.isBefore(today);
                })
                .collect(Collectors.toList());

        // 사용자 위치가 있는 경우: 반경 + 거리순 추천
        if (userLat != null && userLon != null) {
            return activeOnly.stream()
                    .filter(housing -> {
                        if (housing.getLatitude() == null || housing.getLongitude() == null) {
                            return false;
                        }
                        double distance = DistanceCalculator.calculateDistance(
                                userLat, userLon,
                                housing.getLatitude(), housing.getLongitude()
                        );
                        return distance <= searchRadius;
                    })
                    .sorted((h1, h2) -> {
                        if (h1.getDistanceFromUser() == null && h2.getDistanceFromUser() == null) {
                            return 0;
                        }
                        if (h1.getDistanceFromUser() == null) return 1;
                        if (h2.getDistanceFromUser() == null) return -1;
                        return Double.compare(h1.getDistanceFromUser(), h2.getDistanceFromUser());
                    })
                    .limit(maxResults)
                    .collect(Collectors.toList());
        }

        // 위치 정보가 없을 때: 사용자 지역(도/시) 기준 필터 + 마감일 순 정렬
        return activeOnly.stream()
                .filter(housing -> {
                    // 사용자 지역 정보가 있을 경우, 주소에 해당 지역이 포함된 것 우선
                    if (userRegion != null && housing.getAddress() != null) {
                        return housing.getAddress().contains(userRegion);
                    }
                    // 지역 정보가 없으면 전체 활성 임대주택을 후보로 유지
                    return true;
                })
                .sorted((h1, h2) -> {
                    // 우선 신청 마감일 기준 오름차순 정렬
                    LocalDate end1 = h1.getApplicationEnd() != null
                            ? h1.getApplicationEnd().toLocalDate()
                            : LocalDate.MAX;
                        LocalDate end2 = h2.getApplicationEnd() != null
                            ? h2.getApplicationEnd().toLocalDate()
                            : LocalDate.MAX;
                    return end1.compareTo(end2);
                })
                .limit(maxResults)
                .collect(Collectors.toList());
    }
    
    /**
     * ID로 임대주택 상세 조회 (거리 계산 포함)
     */
    public HousingResponse getHousingDetail(String housingId, String userId, Double userLat, Double userLon) {
        // 먼저 단지정보에서 조회
        Optional<HousingComplex> complexOpt = housingComplexRepository.findById(housingId);
        HousingComplex complex = complexOpt.orElse(null);
        
        // 공고문에서 조회 (hsmpSn 또는 hsmpNm으로)
        List<HousingNotice> notices = housingNoticeRepository.findByIdentifier(housingId);
        HousingNotice notice = notices.isEmpty() ? null : notices.get(0);
        
        if (complex == null && notice == null) {
            throw new RuntimeException("임대주택을 찾을 수 없습니다: " + housingId);
        }
        
        return convertToResponse(notice, complex, userId, userLat, userLon);
    }
    
    /**
     * 단지정보를 HousingComplexResponse로 변환
     */
    private HousingComplexResponse convertComplexToResponse(HousingComplex complex, String userId, Double userLat, Double userLon) {
        boolean isBookmarked = false;
        if (complex.getComplexId() != null && userId != null) {
            isBookmarked = bookmarkRepository
                    .findByUser_UserIdAndContentTypeAndContentId(userId, ContentType.housing, complex.getComplexId())
                    .map(bookmark -> bookmark.getIsActive() == ActiveStatus.Y)
                    .orElse(false);
        }
        
        HousingComplexResponse.HousingComplexResponseBuilder builder = HousingComplexResponse.builder()
                .complexId(complex.getComplexId())
                .hsmpNm(complex.getHsmpNm())
                .insttNm(complex.getInsttNm())
                .brtcNm(complex.getBrtcNm())
                .signguNm(complex.getSignguNm())
                .rnAdres(complex.getRnAdres())
                .completeDate(complex.getCompleteDate())
                .totalUnits(complex.getTotalUnits())
                .suplyTyNm(complex.getSuplyTyNm())
                .styleNm(complex.getStyleNm())
                .supplyArea(complex.getSupplyArea())
                .houseTyNm(complex.getHouseTyNm())
                .heatMthdDetailNm(complex.getHeatMthdDetailNm())
                .buldStleNm(complex.getBuldStleNm())
                .elevator(complex.getElevator())
                .parkingSpaces(complex.getParkingSpaces())
                .deposit(complex.getDeposit())
                .monthlyRent(complex.getMonthlyRent())
                .latitude(complex.getLatitude())
                .longitude(complex.getLongitude())
                .isBookmarked(isBookmarked);
        
        HousingComplexResponse response = builder.build();
        
        // 사용자 위치가 제공된 경우 거리 계산
        if (userLat != null && userLon != null && 
            response.getLatitude() != null && response.getLongitude() != null) {
            double distance = DistanceCalculator.calculateDistance(
                    userLat, userLon, 
                    response.getLatitude(), response.getLongitude()
            );
            response.setDistanceFromUser(distance);
        }
        
        return response;
    }
    
    /**
     * 공고문을 HousingNoticeResponse로 변환
     */
    private HousingNoticeResponse convertNoticeToResponse(HousingNotice notice, String userId) {
        boolean isBookmarked = false;
        if (notice.getNoticeId() != null && userId != null) {
            isBookmarked = bookmarkRepository
                    .findByUser_UserIdAndContentTypeAndContentId(userId, ContentType.housing, notice.getNoticeId())
                    .map(bookmark -> bookmark.getIsActive() == ActiveStatus.Y)
                    .orElse(false);
        }
        
        // 매칭된 단지정보 조회
        HousingComplexResponse matchedComplex = null;
        if (notice.getHsmpSn() != null) {
            Optional<HousingComplex> complexOpt = housingComplexRepository.findById(notice.getHsmpSn());
            if (complexOpt.isPresent()) {
                matchedComplex = convertComplexToResponse(complexOpt.get(), userId, null, null);
            }
        }
        if (matchedComplex == null && notice.getHsmpNm() != null) {
            Optional<HousingComplex> complexOpt = housingComplexRepository.findByHsmpNm(notice.getHsmpNm());
            if (complexOpt.isPresent()) {
                matchedComplex = convertComplexToResponse(complexOpt.get(), userId, null, null);
            }
        }
        
        return HousingNoticeResponse.builder()
                .noticeId(notice.getNoticeId())
                .hsmpSn(notice.getHsmpSn())
                .hsmpNm(notice.getHsmpNm())
                .panId(notice.getPanId())
                .panNm(notice.getPanNm())
                .dtlUrl(notice.getDtlUrl())
                .panNtStDt(notice.getPanNtStDt())
                .clsgDt(notice.getClsgDt())
                .panDt(notice.getPanDt())
                .applicationStart(notice.getApplicationStart())
                .applicationEnd(notice.getApplicationEnd())
                .cnpCdNm(notice.getCnpCdNm())
                .uppAisTpNm(notice.getUppAisTpNm())
                .aisTpCdNm(notice.getAisTpCdNm())
                .panSs(notice.getPanSs())
                .isBookmarked(isBookmarked)
                .matchedComplex(matchedComplex)
                .build();
    }
    
    /**
     * 단지정보 목록 조회 (사용자 위치 기반)
     */
    public List<HousingComplexResponse> getHousingComplexes(
            String userId, 
            Double userLat, 
            Double userLon, 
            Integer radius, 
            Integer limit) {
        
        List<HousingComplex> allComplexes = housingComplexRepository.findAll();
        
        int searchRadius = radius != null ? radius : 5000; // 기본 5km
        int maxResults = limit != null ? limit : 50;
        
        // 사용자 위치가 있는 경우: 반경 + 거리순 추천
        if (userLat != null && userLon != null) {
            return allComplexes.stream()
                    .filter(complex -> {
                        if (complex.getLatitude() == null || complex.getLongitude() == null) {
                            return false;
                        }
                        double distance = DistanceCalculator.calculateDistance(
                                userLat, userLon,
                                complex.getLatitude(), complex.getLongitude()
                        );
                        return distance <= searchRadius;
                    })
                    .map(complex -> convertComplexToResponse(complex, userId, userLat, userLon))
                    .sorted((h1, h2) -> {
                        if (h1.getDistanceFromUser() == null && h2.getDistanceFromUser() == null) {
                            return 0;
                        }
                        if (h1.getDistanceFromUser() == null) return 1;
                        if (h2.getDistanceFromUser() == null) return -1;
                        return Double.compare(h1.getDistanceFromUser(), h2.getDistanceFromUser());
                    })
                    .limit(maxResults)
                    .collect(Collectors.toList());
        }
        
        // 위치 정보가 없을 때: 전체 목록 반환
        return allComplexes.stream()
                .map(complex -> convertComplexToResponse(complex, userId, null, null))
                .limit(maxResults)
                .collect(Collectors.toList());
    }
    
    /**
     * 공고문 목록 조회 (활성 공고만)
     */
    public List<HousingNoticeResponse> getHousingNotices(
            String userId, 
            Integer limit) {
        
        try {
            System.out.println("HousingService: 공고문 조회 시작, userId = " + userId);
            List<HousingNotice> activeNotices = showAllHousing
                    ? housingNoticeRepository.findAll()
                    : housingNoticeRepository.findActiveNotices(new java.util.Date());
            System.out.println("HousingService: 활성 공고문 개수 = " + (activeNotices != null ? activeNotices.size() : 0));
            
            int maxResults = limit != null ? limit : 50;
            
            // 신청 기간이 현재 날짜 기준으로 남아있는 공고만 사용 (마감일 null=상시 포함, show-all이면 전체)
            LocalDate today = LocalDate.now();
            List<HousingNoticeResponse> result = activeNotices.stream()
                .filter(notice -> {
                    if (showAllHousing) {
                        return true;
                    }
                    // applicationEnd가 null이면 상시로 간주하여 포함
                    if (notice.getApplicationEnd() == null) {
                        return true;
                    }
                    // java.sql.Date를 LocalDate로 직접 변환
                    LocalDate endDate = notice.getApplicationEnd().toLocalDate();
                    // 마감일이 오늘 이후인 공고만 포함 (오늘 포함)
                    return !endDate.isBefore(today);
                })
                .sorted((n1, n2) -> {
                    // 신청 마감일 기준 오름차순 정렬
                    LocalDate end1 = n1.getApplicationEnd() != null
                            ? n1.getApplicationEnd().toLocalDate()
                            : LocalDate.MAX;
                    LocalDate end2 = n2.getApplicationEnd() != null
                            ? n2.getApplicationEnd().toLocalDate()
                            : LocalDate.MAX;
                    return end1.compareTo(end2);
                })
                .map(notice -> {
                    try {
                        return convertNoticeToResponse(notice, userId);
                    } catch (Exception e) {
                        System.err.println("HousingService: 공고문 변환 중 오류: " + e.getMessage());
                        return null;
                    }
                })
                .filter(response -> response != null)
                .limit(maxResults)
                .collect(Collectors.toList());
            
            System.out.println("HousingService: 최종 공고문 개수 = " + result.size());
            return result;
        } catch (Exception e) {
            System.err.println("HousingService: 공고문 조회 중 오류: " + e.getMessage());
            e.printStackTrace();
            return new java.util.ArrayList<>();
        }
    }
}

