package com.example.youth.dto.publicdata;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class LHRentalHouseListResponse {
    @JsonProperty("response")
    private Response response;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Response {
        @JsonProperty("header")
        private Header header;

        @JsonProperty("body")
        private Body body;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Header {
        @JsonProperty("resultCode")
        private String resultCode;

        @JsonProperty("resultMsg")
        private String resultMsg;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Body {
        @JsonProperty("items")
        private Items items;

        @JsonProperty("numOfRows")
        private Integer numOfRows;

        @JsonProperty("pageNo")
        private Integer pageNo;

        @JsonProperty("totalCount")
        private Integer totalCount;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Items {
        @JsonProperty("item")
        private List<Item> item;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Item {
        @JsonProperty("hsmpSn")
        private String hsmpSn; // 단지 식별자

        @JsonProperty("insttNm")
        private String insttNm; // 기관 명

        @JsonProperty("brtcCode")
        private String brtcCode; // 광역시도 코드

        @JsonProperty("brtcNm")
        private String brtcNm; // 광역시도 명

        @JsonProperty("signguCode")
        private String signguCode; // 시군구 코드

        @JsonProperty("signguNm")
        private String signguNm; // 시군구 명

        @JsonProperty("hsmpNm")
        private String hsmpNm; // 단지 명

        @JsonProperty("rnAdres")
        private String rnAdres; // 도로명 주소

        @JsonProperty("pnu")
        private String pnu;

        @JsonProperty("competDe")
        private String competDe; // 준공 일자 (YYYYMMDD)

        @JsonProperty("hshldCo")
        private String hshldCo; // 세대 수

        @JsonProperty("suplyTyNm")
        private String suplyTyNm; // 공급 유형 명 (예: 50년임대)

        @JsonProperty("styleNm")
        private String styleNm; // 형 명

        @JsonProperty("suplyPrvuseAr")
        private String suplyPrvuseAr; // 공급 전용 면적 (㎡)

        @JsonProperty("suplyCmnuseAr")
        private String suplyCmnuseAr; // 공급 공용 면적 (㎡)

        @JsonProperty("houseTyNm")
        private String houseTyNm; // 주택 유형 명 (예: 아파트)

        @JsonProperty("heatMthdDetailNm")
        private String heatMthdDetailNm; // 난방 방식

        @JsonProperty("buldStleNm")
        private String buldStleNm; // 건물 형태

        @JsonProperty("elvtrInstlAtNm")
        private String elvtrInstlAtNm; // 승강기 설치여부

        @JsonProperty("parkngCo")
        private String parkngCo; // 주차수

        @JsonProperty("bassRentGtn")
        private String bassRentGtn; // 기본 임대보증금

        @JsonProperty("bassMtRntchrg")
        private String bassMtRntchrg; // 기본 월임대료

        @JsonProperty("bassCnvrsGtnLmt")
        private String bassCnvrsGtnLmt; // 기본 전환보증금
    }
}

