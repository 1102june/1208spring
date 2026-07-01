package com.example.youth.service;

import com.example.youth.DB.HousingComplex;
import com.example.youth.repository.HousingComplexRepository;
import com.example.youth.util.HousingFormatUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class HousingGeocodeService {

    private static final long GEOCODE_DELAY_MS = 120L;
    private static final int ON_DEMAND_GEOCODE_CAP = 15;

    @Autowired
    private HousingComplexRepository housingComplexRepository;

    @Autowired
    private KakaoMapService kakaoMapService;

    /**
     * 좌표 없는 단지 배치 지오코딩 (Kakao Local API).
     */
    @Transactional
    public Map<String, Object> batchGeocode(int limit, boolean onlyMissing) {
        int batchSize = limit > 0 ? limit : Integer.MAX_VALUE;
        List<HousingComplex> targets = onlyMissing
                ? housingComplexRepository.findNeedingGeocode(PageRequest.of(0, batchSize))
                : housingComplexRepository.findAll().stream().limit(batchSize).toList();

        int success = 0;
        int fail = 0;
        List<String> failedIds = new ArrayList<>();

        for (HousingComplex complex : targets) {
            if (geocodeAndSave(complex)) {
                success++;
            } else {
                fail++;
                if (failedIds.size() < 20) {
                    failedIds.add(complex.getComplexId());
                }
            }
            sleepForRateLimit();
        }

        Map<String, Object> result = new HashMap<>();
        result.put("processed", targets.size());
        result.put("success", success);
        result.put("fail", fail);
        result.put("failedSampleIds", failedIds);
        result.put("remainingWithoutCoordinates", housingComplexRepository.countNeedingGeocode());
        result.put("withCoordinates", housingComplexRepository.countWithCoordinates());
        return result;
    }

    /**
     * API 응답 직전 — 반환 목록 중 좌표 없는 항목만 제한적으로 지오코딩.
     */
    @Transactional
    public void geocodeOnDemandForResponse(List<HousingComplex> complexes) {
        if (complexes == null || complexes.isEmpty()) {
            return;
        }
        int geocoded = 0;
        for (HousingComplex complex : complexes) {
            if (geocoded >= ON_DEMAND_GEOCODE_CAP) {
                break;
            }
            if (complex.getLatitude() != null && complex.getLongitude() != null) {
                continue;
            }
            if (geocodeAndSave(complex)) {
                geocoded++;
                sleepForRateLimit();
            }
        }
    }

    public boolean geocodeAndSave(HousingComplex complex) {
        if (complex == null) {
            return false;
        }
        if (complex.getLatitude() != null && complex.getLongitude() != null) {
            return true;
        }
        String address = HousingFormatUtils.resolveGeocodeAddress(complex);
        if (address == null || address.isBlank()) {
            return false;
        }
        Double[] coords = kakaoMapService.geocodeAddressSync(address);
        if (coords == null || coords.length < 2 || coords[0] == null || coords[1] == null) {
            return false;
        }
        complex.setLatitude(coords[0]);
        complex.setLongitude(coords[1]);
        housingComplexRepository.save(complex);
        return true;
    }

    private void sleepForRateLimit() {
        try {
            Thread.sleep(GEOCODE_DELAY_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
