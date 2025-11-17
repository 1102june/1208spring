package com.example.youth.service;

import com.example.youth.DB.Housing;
import com.example.youth.dto.HousingResponse;
import com.example.youth.repository.BookmarkRepository;
import com.example.youth.repository.HousingRepository;
import com.example.youth.common.ContentType;
import com.example.youth.DB.ActiveStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
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
    private HousingRepository housingRepository;

    @Autowired
    private BookmarkRepository bookmarkRepository;

    // 임대주택을 HousingResponse로 변환 (북마크 여부 포함)
    private HousingResponse convertToResponse(Housing housing, String userId) {
        return convertToResponse(housing, userId, null, null);
    }
    
    // 임대주택을 HousingResponse로 변환 (거리 계산 포함)
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
                .housingType(housing.getHousingType())
                .depositRefund(housing.getDepositRefund());
        
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

    // ID로 임대주택 조회
    public HousingResponse getHousingById(String housingId, String userId) {
        Housing housing = housingRepository.findById(housingId)
                .orElseThrow(() -> new RuntimeException("임대주택을 찾을 수 없습니다: " + housingId));
        return convertToResponse(housing, userId);
    }

    // 활성 임대주택 목록 조회
    public List<HousingResponse> getActiveHousing(String userId) {
        Date currentDate = new Date();
        List<Housing> housingList = housingRepository.findActiveHousing(currentDate);
        return housingList.stream()
                .map(housing -> convertToResponse(housing, userId))
                .collect(Collectors.toList());
    }

    // 지역별 임대주택 조회
    public List<HousingResponse> getHousingByRegion(String region, String userId) {
        List<Housing> housingList = housingRepository.findHousingByRegion(region);
        return housingList.stream()
                .map(housing -> convertToResponse(housing, userId))
                .collect(Collectors.toList());
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
        
        Date currentDate = new Date();
        List<Housing> activeHousing = housingRepository.findActiveHousing(currentDate);
        
        int searchRadius = radius != null ? radius : 5000; // 기본 5km
        int maxResults = limit != null ? limit : 50;
        
        return activeHousing.stream()
                .filter(housing -> {
                    // 좌표가 있는 경우만 필터링
                    if (housing.getLatitude() == null || housing.getLongitude() == null) {
                        return false;
                    }
                    // 사용자 위치가 제공된 경우 반경 내 필터링
                    if (userLat != null && userLon != null) {
                        double distance = DistanceCalculator.calculateDistance(
                                userLat, userLon,
                                housing.getLatitude(), housing.getLongitude()
                        );
                        return distance <= searchRadius;
                    }
                    return true;
                })
                .map(housing -> convertToResponse(housing, userId, userLat, userLon))
                .sorted((h1, h2) -> {
                    // 거리순 정렬 (거리가 가까운 순)
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
    
    /**
     * ID로 임대주택 상세 조회 (거리 계산 포함)
     */
    public HousingResponse getHousingDetail(String housingId, String userId, Double userLat, Double userLon) {
        Housing housing = housingRepository.findById(housingId)
                .orElseThrow(() -> new RuntimeException("임대주택을 찾을 수 없습니다: " + housingId));
        return convertToResponse(housing, userId, userLat, userLon);
    }
}

