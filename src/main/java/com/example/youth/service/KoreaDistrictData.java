package com.example.youth.service;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 행정구역 구·군 목록 — 사용자 지역 키워드 확장 및 정책 텍스트 매칭용.
 */
final class KoreaDistrictData {

    /** 여러 광역시에 공통으로 존재해 단독 키워드로 쓰면 오매칭 위험이 있는 구 이름 */
    static final Set<String> AMBIGUOUS_DISTRICT_ROOTS = Set.of(
            "중", "동", "서", "남", "북", "강서"
    );

    private static final Map<String, List<String>> PROVINCE_DISTRICTS = buildProvinceDistricts();

    private KoreaDistrictData() {
    }

    static Map<String, List<String>> provinceDistricts() {
        return PROVINCE_DISTRICTS;
    }

    static List<String> districtsForProvince(String provinceKey) {
        return PROVINCE_DISTRICTS.getOrDefault(provinceKey, List.of());
    }

    static Map<String, String> buildDistrictToProvince() {
        Map<String, String> map = new LinkedHashMap<>();
        PROVINCE_DISTRICTS.forEach((province, districts) -> {
            for (String district : districts) {
                String root = districtRoot(district);
                if (root != null && !AMBIGUOUS_DISTRICT_ROOTS.contains(root)) {
                    map.putIfAbsent(district, province);
                    map.putIfAbsent(root, province);
                }
            }
        });
        return Collections.unmodifiableMap(map);
    }

    static String districtRoot(String district) {
        if (district == null || district.isBlank()) {
            return null;
        }
        if (district.endsWith("구") || district.endsWith("군")) {
            return district.substring(0, district.length() - 1);
        }
        return district;
    }

    private static Map<String, List<String>> buildProvinceDistricts() {
        Map<String, List<String>> map = new LinkedHashMap<>();

        map.put("서울", List.of(
                "종로구", "중구", "용산구", "성동구", "광진구", "동대문구", "중랑구", "성북구", "강북구", "도봉구",
                "노원구", "은평구", "서대문구", "마포구", "양천구", "강서구", "구로구", "금천구", "영등포구", "동작구",
                "관악구", "서초구", "강남구", "송파구", "강동구"));

        map.put("부산", List.of(
                "중구", "서구", "동구", "영도구", "부산진구", "동래구", "남구", "북구", "해운대구", "사하구",
                "금정구", "강서구", "연제구", "수영구", "사상구", "기장군"));

        map.put("대구", List.of(
                "중구", "동구", "서구", "남구", "북구", "수성구", "달서구", "달성군"));

        map.put("인천", List.of(
                "중구", "동구", "미추홀구", "연수구", "남동구", "부평구", "계양구", "서구", "강화군", "옹진군"));

        map.put("광주", List.of("동구", "서구", "남구", "북구", "광산구"));

        map.put("대전", List.of("동구", "서구", "유성구", "중구", "대덕구"));

        map.put("울산", List.of("중구", "남구", "동구", "북구", "울주군"));

        map.put("세종", List.of());
        map.put("제주", List.of());

        map.put("경기", List.of(
                "장안구", "권선구", "팔달구", "영통구",
                "수정구", "중원구", "분당구",
                "덕양구", "일산동구", "일산서구",
                "처인구", "기흥구", "수지구",
                "상록구", "단원구",
                "만안구", "동안구",
                "가평군", "양평군", "연천군"));

        map.put("강원", List.of(
                "홍천군", "횡성군", "영월군", "평창군", "정선군", "철원군", "화천군", "양구군", "인제군", "고성군", "양양군"));

        map.put("충북", List.of(
                "상당구", "서원구", "흥덕구", "청원구",
                "보은군", "옥천군", "영동군", "증평군", "진천군", "괴산군", "음성군", "단양군"));

        map.put("충남", List.of(
                "동남구", "서북구",
                "금산군", "부여군", "서천군", "청양군", "홍성군", "예산군", "태안군"));

        map.put("전북", List.of(
                "완산구", "덕진구",
                "완주군", "진안군", "무주군", "장수군", "임실군", "순창군", "고창군", "부안군"));

        map.put("전남", List.of(
                "담양군", "곡성군", "구례군", "고흥군", "보성군", "화순군", "장흥군", "강진군", "해남군", "영암군",
                "무안군", "함평군", "영광군", "장성군", "완도군", "진도군", "신안군"));

        map.put("경북", List.of(
                "남구", "북구",
                "군위군", "의성군", "청송군", "영양군", "영덕군", "청도군", "고령군", "성주군", "칠곡군", "예천군",
                "봉화군", "울진군", "울릉군"));

        map.put("경남", List.of(
                "의창구", "성산구", "마산합포구", "마산회원구", "진해구",
                "의령군", "함안군", "창녕군", "고성군", "남해군", "하동군", "산청군", "함양군", "거창군", "합천군"));

        return Collections.unmodifiableMap(map);
    }
}
