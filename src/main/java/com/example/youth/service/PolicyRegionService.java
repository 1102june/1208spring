package com.example.youth.service;

import com.example.youth.DB.Policy;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 정책 지역 추론·매칭 (sync 시 region 저장, 추천 가중치 감점).
 */
@Service
public class PolicyRegionService {

    private static final double TITLE_FOREIGN_PENALTY = 50.0;
    private static final double SUMMARY_FOREIGN_PENALTY = 40.0;
    /** eligibility(지원자격)에 사용자 지역명 포함 — {@link PolicyScoringService#ELIGIBILITY_REGION_MATCH_BONUS} */
    public static final double ELIGIBILITY_REGION_MATCH_BONUS = 10.0;
    /** eligibility(지원자격)에 타 지역명 — {@link PolicyScoringService}에서 별도 차감 */
    public static final double ELIGIBILITY_FOREIGN_PENALTY = 30.0;
    private static final double MAX_FOREIGN_PENALTY = 90.0;

    private static final List<String> NATIONWIDE_MARKERS = List.of(
            "전국", "전체", "국가", "중앙부처", "중앙정부", "고용노동부", "보건복지부",
            "기획재정부", "과학기술정보통신부", "여성가족부", "중소벤처기업부", "국토교통부",
            "문화체육관광부", "농림축산식품부", "교육부", "행정안전부", "국무조정실"
    );

    private static final List<String> LOCAL_REGION_TERMS = buildLocalRegionTerms();

    /** 도(광역시) 키 → 소속 시·군 이름 (시 접미사 제외, 긴 이름 우선 검색용) */
    private static final java.util.Map<String, List<String>> PROVINCE_CITY_ROOTS = java.util.Map.ofEntries(
            java.util.Map.entry("서울", List.of("서울")),
            java.util.Map.entry("부산", List.of("부산")),
            java.util.Map.entry("인천", List.of("인천")),
            java.util.Map.entry("대구", List.of("대구")),
            java.util.Map.entry("광주", List.of("광주")),
            java.util.Map.entry("대전", List.of("대전")),
            java.util.Map.entry("울산", List.of("울산")),
            java.util.Map.entry("세종", List.of("세종")),
            java.util.Map.entry("제주", List.of("제주", "서귀포")),
            java.util.Map.entry("경기", List.of(
                    "고양", "과천", "광명", "광주", "구리", "군포", "김포", "남양주", "동두천", "부천",
                    "성남", "수원", "시흥", "안산", "안성", "안양", "양주", "여주", "오산", "용인",
                    "의왕", "의정부", "이천", "파주", "평택", "포천", "하남", "화성")),
            java.util.Map.entry("강원", List.of("강릉", "동해", "삼척", "속초", "원주", "춘천", "태백")),
            java.util.Map.entry("충북", List.of("제천", "청주", "충주")),
            java.util.Map.entry("충남", List.of("계룡", "공주", "논산", "당진", "보령", "서산", "아산", "천안")),
            java.util.Map.entry("전북", List.of("군산", "김제", "남원", "익산", "전주", "정읍")),
            java.util.Map.entry("전남", List.of("광양", "나주", "목포", "순천", "여수")),
            java.util.Map.entry("경북", List.of("경산", "경주", "구미", "김천", "문경", "상주", "안동", "영주", "영천", "포항")),
            java.util.Map.entry("경남", List.of("거제", "김해", "밀양", "사천", "양산", "진주", "창원", "통영"))
    );

    private static final java.util.Map<String, String> PROVINCE_DISPLAY = java.util.Map.ofEntries(
            java.util.Map.entry("서울", "서울특별시"),
            java.util.Map.entry("부산", "부산광역시"),
            java.util.Map.entry("경기", "경기도"),
            java.util.Map.entry("인천", "인천광역시"),
            java.util.Map.entry("대구", "대구광역시"),
            java.util.Map.entry("광주", "광주광역시"),
            java.util.Map.entry("대전", "대전광역시"),
            java.util.Map.entry("울산", "울산광역시"),
            java.util.Map.entry("강원", "강원특별자치도"),
            java.util.Map.entry("충북", "충청북도"),
            java.util.Map.entry("충남", "충청남도"),
            java.util.Map.entry("전북", "전북특별자치도"),
            java.util.Map.entry("전남", "전라남도"),
            java.util.Map.entry("경북", "경상북도"),
            java.util.Map.entry("경남", "경상남도"),
            java.util.Map.entry("제주", "제주특별자치도"),
            java.util.Map.entry("세종", "세종특별자치시")
    );

    private static final java.util.Map<String, String> CITY_ROOT_TO_PROVINCE = buildCityRootToProvince();

    private static final java.util.Map<String, String> DISTRICT_TO_PROVINCE = KoreaDistrictData.buildDistrictToProvince();

    private static List<String> buildLocalRegionTerms() {
        List<String> terms = new ArrayList<>(List.of(
            "서울특별시", "부산광역시", "대구광역시", "인천광역시", "광주광역시", "대전광역시", "울산광역시",
            "세종특별자치시", "제주특별자치도", "전북특별자치도", "강원특별자치도",
            "경기도", "강원도", "충청북도", "충청남도", "전라북도", "전라남도", "경상북도", "경상남도",
            "충북", "충남", "전북", "전남", "경북", "경남",
            "서울", "부산", "대구", "인천", "광주", "대전", "울산", "세종", "제주", "경기", "강원"
        ));
        for (List<String> cities : PROVINCE_CITY_ROOTS.values()) {
            for (String city : cities) {
                terms.add(city);
                terms.add(city + "시");
            }
        }
        for (List<String> districts : KoreaDistrictData.provinceDistricts().values()) {
            terms.addAll(districts);
            for (String district : districts) {
                String root = KoreaDistrictData.districtRoot(district);
                if (root != null && root.length() >= 2) {
                    terms.add(root);
                }
            }
        }
        return terms;
    }

    private static java.util.Map<String, String> buildCityRootToProvince() {
        java.util.Map<String, String> map = new java.util.HashMap<>();
        PROVINCE_CITY_ROOTS.forEach((province, cities) -> {
            for (String city : cities) {
                if (PROVINCE_CITY_ROOTS.containsKey(city)) {
                    map.put(city + "시", province);
                } else {
                    map.put(city, province);
                }
            }
        });
        return java.util.Collections.unmodifiableMap(map);
    }

    private static final Pattern METRO_WITH_DISTRICT = Pattern.compile(
            "(서울특별시|부산광역시|대구광역시|인천광역시|광주광역시|대전광역시|울산광역시|세종특별자치시|제주특별자치도|"
                    + "전북특별자치도|강원특별자치도|경기도|강원도|충청북도|충청남도|전라북도|전라남도|경상북도|경상남도)"
                    + "\\s*([가-힣]+(?:시|군|구))");

    private static final Pattern STANDALONE_CITY = Pattern.compile("([가-힣]{2,5}시)(?!청|민|장)");

    private static final Pattern PAREN_DISTRICT = Pattern.compile("\\(([가-힣]+구)\\)");

    private static final Pattern SHORT_METRO_WITH_DISTRICT = Pattern.compile(
            "(서울|부산|대구|인천|광주|대전|울산|세종)\\s+([가-힣]+(?:구|군))");

    /**
     * sync 시 policy.region 저장용 — 기관명 우선, 없으면 제목.
     */
    public String inferRegion(String title, String sprvsnInst, String operInst, String rgtrInst) {
        for (String source : List.of(rgtrInst, operInst, sprvsnInst, title)) {
            String parsed = parseRegionFromText(source);
            if (parsed != null && !parsed.isBlank()) {
                return trimRegion(parsed);
            }
        }
        return null;
    }

    /**
     * 기존 DB 정책 backfill (제목·요약·eligibility).
     */
    public String inferRegionFromPolicy(Policy policy) {
        if (policy == null) {
            return null;
        }
        for (String source : List.of(policy.getTitle(), policy.getSummary(), policy.getEligibility())) {
            String parsed = parseRegionFromText(source);
            if (parsed != null && !parsed.isBlank()) {
                return trimRegion(parsed);
            }
        }
        return null;
    }

    public boolean isNationwideRegion(String region) {
        if (region == null || region.isBlank()) {
            return false;
        }
        String r = region.trim();
        return "전국".equals(r) || "전체".equals(r);
    }

    public boolean isNationwideOnlyPolicy(Policy policy) {
        if (policy == null) {
            return false;
        }
        String combined = joinNonBlank(
                policy.getTitle(), policy.getSummary(), policy.getRegion(), policy.getEligibility());
        if (combined.isEmpty()) {
            return false;
        }
        boolean hasNationwide = containsAnyMarker(combined);
        if (!hasNationwide) {
            return false;
        }
        return findLocalRegionTerms(combined).isEmpty();
    }

    /**
     * eligibility에 사용자 지역명이 있을 때만 +10.
     */
    public double computeEligibilityRegionMatchBonus(String userRegion, Policy policy) {
        if (userRegion == null || userRegion.isBlank() || policy == null) {
            return 0.0;
        }
        String eligibility = policy.getEligibility();
        if (eligibility == null || eligibility.isBlank()) {
            return 0.0;
        }
        List<String> userKeywords = extractUserRegionKeywords(userRegion);
        if (userKeywords.isEmpty()) {
            return 0.0;
        }
        if (matchesUserRegion(userKeywords, eligibility) || containsAnyKeyword(eligibility, userKeywords)) {
            return ELIGIBILITY_REGION_MATCH_BONUS;
        }
        return 0.0;
    }

    /**
     * eligibility에 타 지역명이 있으면 -30.
     */
    public double computeEligibilityForeignPenalty(String userRegion, Policy policy) {
        if (userRegion == null || userRegion.isBlank() || policy == null) {
            return 0.0;
        }
        if (isNationwideOnlyPolicy(policy)) {
            return 0.0;
        }
        List<String> userKeywords = extractUserRegionKeywords(userRegion);
        if (userKeywords.isEmpty()) {
            return 0.0;
        }
        if (hasForeignLocalRegion(policy.getEligibility(), userKeywords)) {
            return ELIGIBILITY_FOREIGN_PENALTY;
        }
        return 0.0;
    }

    /**
     * title/summary/region/eligibility·title에 타 지역명 → 감점 (최대 90).
     */
    public double computeForeignRegionPenalty(String userRegion, Policy policy) {
        if (userRegion == null || userRegion.isBlank() || policy == null) {
            return 0.0;
        }
        if (isNationwideOnlyPolicy(policy)) {
            return 0.0;
        }

        List<String> userKeywords = extractUserRegionKeywords(userRegion);
        if (userKeywords.isEmpty()) {
            return 0.0;
        }

        double penalty = 0.0;

        if (hasForeignLocalRegion(policy.getTitle(), userKeywords)) {
            penalty += TITLE_FOREIGN_PENALTY;
        } else if (hasForeignLocalRegion(policy.getSummary(), userKeywords)) {
            penalty += SUMMARY_FOREIGN_PENALTY;
        }

        if (policy.getRegion() != null && !policy.getRegion().isBlank()
                && !isNationwideRegion(policy.getRegion())
                && !matchesUserRegion(userKeywords, policy.getRegion())) {
            penalty = Math.max(penalty, TITLE_FOREIGN_PENALTY);
        }

        return Math.min(penalty, MAX_FOREIGN_PENALTY);
    }

    public boolean matchesUserRegion(List<String> userKeywords, String policyRegionText) {
        if (policyRegionText == null || policyRegionText.isBlank() || userKeywords.isEmpty()) {
            return false;
        }
        if (isNationwideRegion(policyRegionText)) {
            return true;
        }
        List<String> policyTerms = findLocalRegionTerms(policyRegionText);
        if (policyTerms.isEmpty()) {
            return containsAnyKeyword(policyRegionText, userKeywords);
        }
        for (String term : policyTerms) {
            if (termMatchesUserKeywords(term, userKeywords)) {
                return true;
            }
        }
        return false;
    }

    public List<String> extractUserRegionKeywords(String userRegion) {
        if (userRegion == null || userRegion.isBlank()) {
            return List.of();
        }
        Set<String> keywords = new LinkedHashSet<>();
        String provinceKey = resolveProvinceKey(userRegion);

        if (provinceKey != null) {
            keywords.add(provinceKey);
            String display = PROVINCE_DISPLAY.get(provinceKey);
            if (display != null) {
                keywords.add(display);
                String normalizedDisplay = normalizeToken(display);
                if (normalizedDisplay != null) {
                    keywords.add(normalizedDisplay);
                }
            }
            List<String> cities = PROVINCE_CITY_ROOTS.get(provinceKey);
            if (cities != null) {
                for (String city : cities) {
                    addCityKeywords(keywords, city);
                }
            }
            addProvinceDistrictKeywords(keywords, provinceKey);
        }

        for (String part : userRegion.trim().split("\\s+")) {
            String normalized = normalizeToken(part);
            if (normalized != null && normalized.length() >= 2) {
                keywords.add(normalized);
            }
            if (part.length() >= 2) {
                keywords.add(part.trim());
            }
        }
        for (String term : findLocalRegionTerms(userRegion)) {
            String n = normalizeToken(term);
            if (n != null && n.length() >= 2) {
                keywords.add(n);
            }
        }
        return new ArrayList<>(keywords);
    }

    /**
     * 사용자 region 문자열에서 도(광역시) 키 추출. 예: "전남 전라남도" → "전남"
     */
    public String resolveProvinceKey(String userRegion) {
        if (userRegion == null || userRegion.isBlank()) {
            return null;
        }
        String trimmed = userRegion.trim();
        for (String key : PROVINCE_CITY_ROOTS.keySet()) {
            if (trimmed.equals(key) || trimmed.startsWith(key + " ")) {
                return key;
            }
        }
        for (var entry : PROVINCE_DISPLAY.entrySet()) {
            if (trimmed.equals(entry.getValue()) || trimmed.startsWith(entry.getValue())) {
                return entry.getKey();
            }
        }
        String first = trimmed.split("\\s+")[0];
        String normalized = normalizeToken(first);
        if (normalized != null) {
            for (var entry : PROVINCE_DISPLAY.entrySet()) {
                if (entry.getKey().equals(normalized) || entry.getValue().contains(normalized)) {
                    return entry.getKey();
                }
            }
        }
        return null;
    }

    private void addCityKeywords(Set<String> keywords, String cityRoot) {
        if (cityRoot == null || cityRoot.isBlank()) {
            return;
        }
        keywords.add(cityRoot);
        keywords.add(cityRoot + "시");
    }

    private void addProvinceDistrictKeywords(Set<String> keywords, String provinceKey) {
        String display = PROVINCE_DISPLAY.get(provinceKey);
        for (String district : KoreaDistrictData.districtsForProvince(provinceKey)) {
            String root = KoreaDistrictData.districtRoot(district);
            boolean ambiguous = root != null && KoreaDistrictData.AMBIGUOUS_DISTRICT_ROOTS.contains(root);
            if (!ambiguous) {
                keywords.add(district);
                if (root != null) {
                    keywords.add(root);
                }
            }
            if (display != null) {
                keywords.add(display + " " + district);
            }
            keywords.add(provinceKey + " " + district);
        }
    }

    public boolean regionsAlign(String userRegion, String policyRegion) {
        if (policyRegion == null || policyRegion.isBlank()) {
            return false;
        }
        if (isNationwideRegion(policyRegion)) {
            return true;
        }
        if (userRegion == null || userRegion.isBlank()) {
            return false;
        }
        return matchesUserRegion(extractUserRegionKeywords(userRegion), policyRegion)
                || userRegion.contains(policyRegion)
                || policyRegion.contains(userRegion);
    }

    private List<String> findLocalRegionTerms(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        Set<String> found = new LinkedHashSet<>();
        String normalized = text.replace(" ", "");

        for (String term : LOCAL_REGION_TERMS) {
            if (normalized.contains(term.replace(" ", ""))) {
                found.add(term);
            }
        }

        found.addAll(findCityRootsInText(normalized));

        found.addAll(findDistrictsInText(normalized));

        Matcher metro = METRO_WITH_DISTRICT.matcher(text);
        while (metro.find()) {
            found.add(trimRegion(metro.group(1) + " " + metro.group(2)));
        }

        Matcher city = STANDALONE_CITY.matcher(text);
        while (city.find()) {
            found.add(city.group(1));
        }

        Matcher paren = PAREN_DISTRICT.matcher(text);
        while (paren.find()) {
            String district = paren.group(1);
            String metroPrefix = findMetroPrefixInText(text);
            if (metroPrefix != null) {
                found.add(trimRegion(metroPrefix + " " + district));
            }
        }

        return found.stream()
                .sorted(Comparator.comparingInt(String::length).reversed())
                .toList();
    }

    /** 정책 텍스트에서 등록된 시·군 이름 탐지 (평택청년 → 평택) */
    private List<String> findCityRootsInText(String normalizedText) {
        List<String> found = new ArrayList<>();
        List<String> allRoots = new ArrayList<>();
        for (List<String> cities : PROVINCE_CITY_ROOTS.values()) {
            allRoots.addAll(cities);
        }
        allRoots.sort(Comparator.comparingInt(String::length).reversed());
        for (String cityRoot : allRoots) {
            if (containsCityRoot(normalizedText, cityRoot)) {
                found.add(cityRoot);
                found.add(cityRoot + "시");
            }
        }
        return found;
    }

    /** 정책 텍스트에서 등록된 구·군 이름 탐지 */
    private List<String> findDistrictsInText(String normalizedText) {
        List<String> found = new ArrayList<>();
        for (List<String> districts : KoreaDistrictData.provinceDistricts().values()) {
            for (String district : districts) {
                if (normalizedText.contains(district)) {
                    found.add(district);
                    String root = KoreaDistrictData.districtRoot(district);
                    if (root != null && root.length() >= 2) {
                        found.add(root);
                    }
                }
            }
        }
        return found;
    }

    private boolean containsDistrictInText(String normalizedText, String district) {
        return district != null && normalizedText.contains(district);
    }

    private boolean textMentionsProvince(String text, String provinceKey) {
        if (text == null || provinceKey == null) {
            return false;
        }
        String display = PROVINCE_DISPLAY.get(provinceKey);
        if (text.contains(provinceKey)) {
            return true;
        }
        return display != null && text.contains(display);
    }

    private boolean containsCityRoot(String normalizedText, String cityRoot) {
        if (normalizedText == null || cityRoot == null || cityRoot.isBlank()) {
            return false;
        }
        int idx = 0;
        while ((idx = normalizedText.indexOf(cityRoot, idx)) >= 0) {
            int end = idx + cityRoot.length();
            if (end >= normalizedText.length()) {
                return true;
            }
            char next = normalizedText.charAt(end);
            if (next == '시' || next == '군' || next == '구' || next == '청' || next == ' ') {
                return true;
            }
            idx = end;
        }
        return false;
    }

    private String findMetroPrefixInText(String text) {
        for (String metro : List.of("서울", "부산", "대구", "인천", "광주", "대전", "울산", "세종", "제주")) {
            if (text.contains(metro)) {
                return toMetroFullName(metro);
            }
        }
        return null;
    }

    private String parseRegionFromText(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }

        Matcher metro = METRO_WITH_DISTRICT.matcher(text);
        if (metro.find()) {
            return trimRegion(metro.group(1) + " " + metro.group(2));
        }

        Matcher shortMetro = SHORT_METRO_WITH_DISTRICT.matcher(text);
        if (shortMetro.find()) {
            return trimRegion(toMetroFullName(shortMetro.group(1)) + " " + shortMetro.group(2));
        }

        for (String term : LOCAL_REGION_TERMS) {
            if (!text.contains(term)) {
                continue;
            }
            if (term.endsWith("시") && !term.contains("광역") && !term.contains("특별")) {
                return term;
            }
            if (term.contains("특별") || term.contains("광역") || term.endsWith("도")) {
                Matcher m = METRO_WITH_DISTRICT.matcher(text);
                if (m.find()) {
                    return trimRegion(m.group(1) + " " + m.group(2));
                }
                return term;
            }
            return canonicalRegionLabel(term);
        }

        Matcher city = STANDALONE_CITY.matcher(text);
        if (city.find()) {
            return city.group(1);
        }

        Matcher paren = PAREN_DISTRICT.matcher(text);
        if (paren.find()) {
            String district = paren.group(1);
            if (text.contains("인천")) {
                return "인천광역시 " + district;
            }
            if (text.contains("부산")) {
                return "부산광역시 " + district;
            }
            if (text.contains("대구")) {
                return "대구광역시 " + district;
            }
        }

        return null;
    }

    private boolean termMatchesUserKeywords(String term, List<String> userKeywords) {
        String normalizedTerm = normalizeToken(term);
        if (normalizedTerm == null) {
            return false;
        }
        for (String uk : userKeywords) {
            if (normalizedTerm.contains(uk) || uk.contains(normalizedTerm)) {
                return true;
            }
            if (sameRegionGroup(normalizedTerm, uk)) {
                return true;
            }
        }
        String termProvince = CITY_ROOT_TO_PROVINCE.get(normalizedTerm);
        if (termProvince != null) {
            for (String uk : userKeywords) {
                if (termProvince.equals(uk) || termProvince.equals(resolveProvinceKey(uk))) {
                    return true;
                }
            }
        }
        String districtProvince = DISTRICT_TO_PROVINCE.get(normalizedTerm);
        if (districtProvince == null && term != null) {
            districtProvince = DISTRICT_TO_PROVINCE.get(term.trim());
        }
        if (districtProvince != null) {
            for (String uk : userKeywords) {
                if (districtProvince.equals(uk) || districtProvince.equals(resolveProvinceKey(uk))) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean hasForeignLocalRegion(String text, List<String> userKeywords) {
        if (text == null || text.isBlank()) {
            return false;
        }
        String userProvince = userKeywords.stream()
                .map(this::resolveProvinceKey)
                .filter(k -> k != null)
                .findFirst()
                .orElse(null);
        if (userProvince == null && !userKeywords.isEmpty()) {
            userProvince = userKeywords.get(0);
        }

        String normalized = text.replace(" ", "");
        for (List<String> cities : PROVINCE_CITY_ROOTS.values()) {
            for (String cityRoot : cities) {
                if (!containsCityRoot(normalized, cityRoot)) {
                    continue;
                }
                String cityProvince = CITY_ROOT_TO_PROVINCE.get(cityRoot);
                if (userProvince != null && userProvince.equals(cityProvince)) {
                    continue;
                }
                if (userProvince != null && cityProvince != null && !userProvince.equals(cityProvince)) {
                    return true;
                }
            }
        }

        for (var entry : KoreaDistrictData.provinceDistricts().entrySet()) {
            String districtProvince = entry.getKey();
            for (String district : entry.getValue()) {
                if (!containsDistrictInText(normalized, district)) {
                    continue;
                }
                String root = KoreaDistrictData.districtRoot(district);
                if (userProvince != null && userProvince.equals(districtProvince)) {
                    continue;
                }
                if (root != null && KoreaDistrictData.AMBIGUOUS_DISTRICT_ROOTS.contains(root)
                        && !textMentionsProvince(text, districtProvince)) {
                    continue;
                }
                if (userProvince != null && !userProvince.equals(districtProvince)) {
                    return true;
                }
            }
        }

        for (String term : findLocalRegionTerms(text)) {
            if (!termMatchesUserKeywords(term, userKeywords)) {
                String root = normalizeToken(term);
                if (root != null && root.length() >= 2) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean sameRegionGroup(String a, String b) {
        return rootOf(a).equals(rootOf(b));
    }

    private String rootOf(String token) {
        if (token == null) {
            return "";
        }
        String t = token.toLowerCase(Locale.ROOT);
        if (t.startsWith("서울")) return "서울";
        if (t.startsWith("부산")) return "부산";
        if (t.startsWith("대구")) return "대구";
        if (t.startsWith("인천")) return "인천";
        if (t.startsWith("광주")) return "광주";
        if (t.startsWith("대전")) return "대전";
        if (t.startsWith("울산")) return "울산";
        if (t.startsWith("세종")) return "세종";
        if (t.startsWith("제주")) return "제주";
        if (t.startsWith("경기")) return "경기";
        if (t.startsWith("강원")) return "강원";
        if (t.startsWith("충북") || t.contains("충청북")) return "충북";
        if (t.startsWith("충남") || t.contains("충청남")) return "충남";
        if (t.startsWith("전북") || t.contains("전라북") || t.startsWith("익산") || t.startsWith("전주") || t.startsWith("군산")) return "전북";
        if (t.startsWith("전남") || t.contains("전라남")) return "전남";
        if (t.startsWith("경북") || t.contains("경상북")) return "경북";
        if (t.startsWith("경남") || t.contains("경상남")) return "경남";
        return normalizeToken(token);
    }

    private String canonicalRegionLabel(String term) {
        if (term == null || term.isBlank()) {
            return null;
        }
        return switch (term) {
            case "서울" -> "서울특별시";
            case "부산" -> "부산광역시";
            case "대구" -> "대구광역시";
            case "인천" -> "인천광역시";
            case "광주" -> "광주광역시";
            case "대전" -> "대전광역시";
            case "울산" -> "울산광역시";
            case "세종" -> "세종특별자치시";
            case "제주" -> "제주특별자치도";
            case "경기" -> "경기도";
            case "강원" -> "강원특별자치도";
            case "충북" -> "충청북도";
            case "충남" -> "충청남도";
            case "전북" -> "전북특별자치도";
            case "전남" -> "전라남도";
            case "경북" -> "경상북도";
            case "경남" -> "경상남도";
            case "익산", "전주", "군산", "김제", "남원", "정읍" -> term + "시";
            case "수원", "성남", "고양", "용인", "부천", "안산", "안양", "남양주",
                 "청주", "충주", "천안", "창원", "김해", "포항", "서귀포" -> term + "시";
            default -> term.endsWith("시") || term.endsWith("도") ? term : term;
        };
    }

    private String toMetroFullName(String shortName) {
        return canonicalRegionLabel(shortName);
    }

    private String normalizeToken(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        String t = token.trim();
        String[] suffixes = {"특별자치시", "특별자치도", "광역시", "특별시", "도", "시", "군", "구"};
        for (String suffix : suffixes) {
            if (t.endsWith(suffix) && t.length() > suffix.length()) {
                t = t.substring(0, t.length() - suffix.length());
                break;
            }
        }
        return t.isEmpty() ? null : t;
    }

    private boolean containsAnyMarker(String text) {
        for (String marker : NATIONWIDE_MARKERS) {
            if (text.contains(marker)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsAnyKeyword(String text, List<String> keywords) {
        for (String kw : keywords) {
            if (text.contains(kw)) {
                return true;
            }
        }
        return false;
    }

    private String joinNonBlank(String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p != null && !p.isBlank()) {
                sb.append(p).append(' ');
            }
        }
        return sb.toString().trim();
    }

    private String trimRegion(String region) {
        if (region == null) {
            return null;
        }
        String r = region.trim().replaceAll("\\s+", " ");
        return r.length() > 100 ? r.substring(0, 100) : r;
    }
}
