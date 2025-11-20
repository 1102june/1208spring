package com.example.youth.dto.publicdata;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class LHRentalNoticeResponse {
    // 실제 API 응답은 배열로 시작: [{"dsSch":[...]}, {"dsList":[...]}]
    // 첫 번째 요소는 검색 조건, 두 번째 요소는 데이터 리스트
    @JsonProperty("dsList")
    private List<Item> dsList;

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
        // 공고문 기본 정보 (실제 API 응답 필드명)
        @JsonProperty("PAN_ID")
        private String panId; // 공고ID

        @JsonProperty("PAN_NM")
        private String panNm; // 공고명

        @JsonProperty("DTL_URL")
        private String dtlUrl; // 공고 상세 URL

        @JsonProperty("PAN_NT_ST_DT")
        private String panNtStDt; // 공고게시일 (YYYY.MM.DD)

        @JsonProperty("CLSG_DT")
        private String clsgDt; // 공고마감일 (YYYY.MM.DD)

        @JsonProperty("PAN_DT")
        private String panDt; // 공고일 (YYYYMMDD)

        // 단지 정보
        @JsonProperty("hsmpSn")
        private String hsmpSn; // 단지 식별자

        @JsonProperty("hsmpNm")
        private String hsmpNm; // 단지명

        @JsonProperty("CNP_CD")
        private String cnpCd; // 지역코드

        @JsonProperty("CNP_CD_NM")
        private String cnpCdNm; // 지역명

        // 공고 유형 정보
        @JsonProperty("UPP_AIS_TP_CD")
        private String uppAisTpCd; // 상위 공고유형코드

        @JsonProperty("UPP_AIS_TP_NM")
        private String uppAisTpNm; // 상위 공고유형명

        @JsonProperty("AIS_TP_CD")
        private String aisTpCd; // 공고유형코드

        @JsonProperty("AIS_TP_CD_NM")
        private String aisTpCdNm; // 공고유형명

        @JsonProperty("PAN_SS")
        private String panSs; // 공고상태

        // 기타 정보
        @JsonProperty("ALL_CNT")
        private String allCnt; // 전체조회건수

        // 이전 필드명과의 호환성을 위한 getter 메서드
        public String getPblancNo() {
            return panId;
        }

        public String getPblancNm() {
            return panNm;
        }

        public String getPblancUrl() {
            return dtlUrl;
        }

        public String getRceptBgnde() {
            // PAN_NT_ST_DT를 YYYYMMDD 형식으로 변환
            if (panNtStDt != null && panNtStDt.length() >= 10) {
                return panNtStDt.replace(".", "").substring(0, 8);
            }
            return null;
        }

        public String getRceptEndde() {
            // CLSG_DT를 YYYYMMDD 형식으로 변환
            if (clsgDt != null && clsgDt.length() >= 10) {
                return clsgDt.replace(".", "").substring(0, 8);
            }
            return null;
        }

        public String getPblancDe() {
            return panDt;
        }

        public String getBrtcNm() {
            return cnpCdNm;
        }

        public String getSuplyTyNm() {
            return aisTpCdNm;
        }

        // HousingSyncService에서 사용하는 추가 getter 메서드
        public String getBrtcCode() {
            return cnpCd;
        }

        public String getSignguCode() {
            // API 응답에 시군구 코드가 없을 수 있음
            return null;
        }

        public String getSignguNm() {
            // API 응답에 시군구명이 없을 수 있음
            return null;
        }

        public String getInsttNm() {
            // API 응답에 기관명이 없을 수 있음
            return null;
        }
    }
}

