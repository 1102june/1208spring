package com.example.youth.util;

import com.example.youth.DB.HousingComplex;
import com.example.youth.DB.HousingNotice;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 임대주택 API 응답용 표시 문자열 정제.
 */
public final class HousingFormatUtils {

    private static final DateTimeFormatter DATE_DOT = DateTimeFormatter.ofPattern("yyyy.MM.dd");

    private HousingFormatUtils() {
    }

    /** LH API 금액(원) → 만원/억 단위 표시 */
    public static String formatMoneyWon(Integer amountWon) {
        if (amountWon == null || amountWon <= 0) {
            return "미정";
        }
        long manWon = amountWon / 10_000L;
        if (manWon >= 10_000L) {
            long eok = manWon / 10_000L;
            long restMan = manWon % 10_000L;
            if (restMan == 0) {
                return String.format("%,d억원", eok);
            }
            return String.format("%,d억 %,d만원", eok, restMan);
        }
        return String.format("%,d만원", manWon);
    }

    public static String formatDepositMonthly(Integer depositWon, Integer monthlyRentWon) {
        return "보증금 " + formatMoneyWon(depositWon) + " · 월세 " + formatMoneyWon(monthlyRentWon);
    }

    public static String formatSupplyArea(Double supplyAreaSqm) {
        if (supplyAreaSqm == null || supplyAreaSqm <= 0) {
            return "면적 미정";
        }
        int sqm = (int) Math.round(supplyAreaSqm);
        double pyeong = supplyAreaSqm / 3.3058;
        return String.format("%d㎡ (%.1f평)", sqm, pyeong);
    }

    public static String formatDistanceMeters(Double distanceMeters) {
        if (distanceMeters == null || distanceMeters < 0) {
            return null;
        }
        if (distanceMeters < 1_000) {
            return String.format("%.0fm", distanceMeters);
        }
        return String.format("%.1fkm", distanceMeters / 1_000.0);
    }

    public static String formatRegionLabel(HousingComplex complex) {
        if (complex == null) {
            return "";
        }
        if (complex.getSignguNm() != null && !complex.getSignguNm().isBlank()) {
            if (complex.getBrtcNm() != null && !complex.getBrtcNm().isBlank()) {
                return complex.getBrtcNm() + " " + complex.getSignguNm();
            }
            return complex.getSignguNm();
        }
        return complex.getBrtcNm() != null ? complex.getBrtcNm() : "";
    }

    public static String normalizeNoticeRegion(String cnpCdNm) {
        if (cnpCdNm == null || cnpCdNm.isBlank()) {
            return "";
        }
        String trimmed = cnpCdNm.trim();
        if (trimmed.endsWith(" 외")) {
            return trimmed.substring(0, trimmed.length() - 2).trim();
        }
        return trimmed;
    }

    public static String resolveNoticeStatus(java.sql.Date applicationEnd) {
        if (applicationEnd == null) {
            return "상시";
        }
        LocalDate end = applicationEnd.toLocalDate();
        LocalDate today = LocalDate.now();
        if (end.isBefore(today)) {
            return "마감";
        }
        return "접수중";
    }

    public static String formatApplicationPeriod(java.sql.Date start, java.sql.Date end) {
        if (start == null && end == null) {
            return "상시 모집";
        }
        String startStr = start != null ? start.toLocalDate().format(DATE_DOT) : "";
        String endStr = end != null ? end.toLocalDate().format(DATE_DOT) : "";
        if (!startStr.isEmpty() && !endStr.isEmpty()) {
            return startStr + " ~ " + endStr;
        }
        if (!endStr.isEmpty()) {
            return "~ " + endStr;
        }
        return startStr;
    }

    public static String buildComplexSummary(HousingComplex complex) {
        if (complex == null) {
            return "";
        }
        String type = firstNonBlank(complex.getHouseTyNm(), complex.getSuplyTyNm(), "임대주택");
        String units = complex.getTotalUnits() != null && complex.getTotalUnits() > 0
                ? complex.getTotalUnits() + "세대"
                : null;
        String area = complex.getSupplyArea() != null && complex.getSupplyArea() > 0
                ? formatSupplyArea(complex.getSupplyArea())
                : null;
        StringBuilder sb = new StringBuilder(type);
        if (units != null) {
            sb.append(" · ").append(units);
        }
        if (area != null) {
            sb.append(" · ").append(area);
        }
        return sb.toString();
    }

    public static String buildNoticeTitle(HousingNotice notice) {
        if (notice == null) {
            return "";
        }
        if (notice.getPanNm() != null && !notice.getPanNm().isBlank()) {
            return notice.getPanNm().trim();
        }
        if (notice.getHsmpNm() != null && !notice.getHsmpNm().isBlank()) {
            return notice.getHsmpNm().trim() + " 입주자 모집";
        }
        String region = normalizeNoticeRegion(notice.getCnpCdNm());
        return region.isEmpty() ? "임대주택 모집 공고" : region + " 임대주택 모집";
    }

    public static String resolveGeocodeAddress(HousingComplex complex) {
        if (complex == null) {
            return null;
        }
        if (complex.getRnAdres() != null && !complex.getRnAdres().isBlank()) {
            return complex.getRnAdres().trim();
        }
        StringBuilder sb = new StringBuilder();
        if (complex.getBrtcNm() != null && !complex.getBrtcNm().isBlank()) {
            sb.append(complex.getBrtcNm());
        }
        if (complex.getSignguNm() != null && !complex.getSignguNm().isBlank()) {
            if (sb.length() > 0) {
                sb.append(" ");
            }
            sb.append(complex.getSignguNm());
        }
        if (complex.getHsmpNm() != null && !complex.getHsmpNm().isBlank()) {
            if (sb.length() > 0) {
                sb.append(" ");
            }
            sb.append(complex.getHsmpNm());
        }
        return sb.length() > 0 ? sb.toString() : null;
    }

    public static boolean matchesProfileRegion(String profileRegion, HousingComplex complex) {
        if (profileRegion == null || profileRegion.isBlank() || complex == null) {
            return true;
        }
        String brtc = complex.getBrtcNm() != null ? complex.getBrtcNm() : "";
        String signgu = complex.getSignguNm() != null ? complex.getSignguNm() : "";
        String combined = brtc + " " + signgu;
        for (String part : profileRegion.trim().split("\\s+")) {
            if (part.length() < 2) {
                continue;
            }
            String token = part
                    .replace("특별자치시", "")
                    .replace("특별자치도", "")
                    .replace("특별시", "")
                    .replace("광역시", "")
                    .replace("도", "");
            if (combined.contains(part) || brtc.contains(token) || signgu.contains(token)) {
                return true;
            }
        }
        return false;
    }

    public static boolean matchesProfileRegion(String profileRegion, HousingNotice notice) {
        if (profileRegion == null || profileRegion.isBlank() || notice == null) {
            return true;
        }
        String region = normalizeNoticeRegion(notice.getCnpCdNm());
        if (region.isBlank()) {
            return true;
        }
        for (String part : profileRegion.trim().split("\\s+")) {
            if (part.length() < 2) {
                continue;
            }
            String token = part
                    .replace("특별자치시", "")
                    .replace("특별자치도", "")
                    .replace("특별시", "")
                    .replace("광역시", "")
                    .replace("도", "");
            if (region.contains(part) || region.contains(token)) {
                return true;
            }
        }
        return false;
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }
}
