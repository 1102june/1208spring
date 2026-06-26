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
    private static final double MAX_FOREIGN_PENALTY = 60.0;

    private static final List<String> NATIONWIDE_MARKERS = List.of(
            "전국", "전체", "국가", "중앙부처", "중앙정부", "고용노동부", "보건복지부",
            "기획재정부", "과학기술정보통신부", "여성가족부", "중소벤처기업부", "국토교통부",
            "문화체육관광부", "농림축산식품부", "교육부", "행정안전부", "국무조정실"
    );

    /** 긴 패턴 우선 (contains 검색) */
    private static final List<String> LOCAL_REGION_TERMS = List.of(
            "서울특별시", "부산광역시", "대구광역시", "인천광역시", "광주광역시", "대전광역시", "울산광역시",
            "세종특별자치시", "제주특별자치도", "전북특별자치도", "강원특별자치도",
            "경기도", "강원도", "충청북도", "충청남도", "전라북도", "전라남도", "경상북도", "경상남도",
            "충북", "충남", "전북", "전남", "경북", "경남",
            "익산시", "전주시", "군산시", "김제시", "남원시", "정읍시",
            "수원시", "성남시", "고양시", "용인시", "부천시", "안산시", "안양시", "남양주시",
            "청주시", "충주시", "천안시", "전주", "익산", "군산",
            "창원시", "김해시", "포항시", "제주시", "서귀포시",
            "서울", "부산", "대구", "인천", "광주", "대전", "울산", "세종", "제주", "경기", "강원"
    );

    private static final Pattern METRO_WITH_DISTRICT = Pattern.compile(
            "(서울특별시|부산광역시|대구광역시|인천광역시|광주광역시|대전광역시|울산광역시|세종특별자치시|제주특별자치도|"
                    + "전북특별자치도|강원특별자치도|경기도|강원도|충청북도|충청남도|전라북도|전라남도|경상북도|경상남도)"
                    + "\\s*([가-힣]+(?:시|군|구))");

    private static final Pattern STANDALONE_CITY = Pattern.compile("([가-힣]{2,5}시)(?!청|민|장)");

    private static final Pattern PAREN_DISTRICT = Pattern.compile("\\(([가-힣]+구)\\)");

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
     * 기존 DB 정책 backfill (제목·요약만 사용).
     */
    public String inferRegionFromPolicy(Policy policy) {
        if (policy == null) {
            return null;
        }
        String fromTitle = inferRegion(policy.getTitle(), null, null, null);
        if (fromTitle != null) {
            return fromTitle;
        }
        return parseRegionFromText(policy.getSummary());
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
        String combined = joinNonBlank(policy.getTitle(), policy.getSummary(), policy.getRegion());
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
     * 사용자 지역과 다른 지역명이 title/summary/region에 있으면 감점 (최대 60).
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
        for (String part : userRegion.trim().split("\\s+")) {
            String normalized = normalizeToken(part);
            if (normalized != null && normalized.length() >= 2) {
                keywords.add(normalized);
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

    private boolean hasForeignLocalRegion(String text, List<String> userKeywords) {
        if (text == null || text.isBlank()) {
            return false;
        }
        for (String term : findLocalRegionTerms(text)) {
            if (!termMatchesUserKeywords(term, userKeywords)) {
                return true;
            }
        }
        return false;
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
            found.add(paren.group(1));
        }

        return found.stream()
                .sorted(Comparator.comparingInt(String::length).reversed())
                .toList();
    }

    private String parseRegionFromText(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }

        Matcher metro = METRO_WITH_DISTRICT.matcher(text);
        if (metro.find()) {
            return trimRegion(metro.group(1) + " " + metro.group(2));
        }

        for (String term : LOCAL_REGION_TERMS) {
            if (text.contains(term)) {
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
            }
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
