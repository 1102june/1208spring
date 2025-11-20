package com.example.youth.dto.publicdata;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class YouthPolicyResponse {
    @JsonProperty("resultCode")
    private Integer resultCode;

    @JsonProperty("resultMessage")
    private String resultMessage;

    @JsonProperty("result")
    private Result result;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Result {
        @JsonProperty("pagging")
        private Pagging pagging;

        @JsonProperty("youthPolicyList")
        private List<Item> youthPolicyList;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Pagging {
        @JsonProperty("totCount")
        private Integer totCount;

        @JsonProperty("pageNum")
        private Integer pageNum;

        @JsonProperty("pageSize")
        private Integer pageSize;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Item {
        // CSV 파일의 실제 필드명에 맞춰서 작성
        @JsonProperty("plcyNo")
        private String plcyNo; // 정책번호

        @JsonProperty("plcyNm")
        private String plcyNm; // 정책명

        @JsonProperty("plcyExplnCn")
        private String plcyExplnCn; // 정책설명내용

        @JsonProperty("lclsfNm")
        private String lclsfNm; // 대분류명

        @JsonProperty("mclsfNm")
        private String mclsfNm; // 중분류명

        @JsonProperty("plcySprtCn")
        private String plcySprtCn; // 정책지원내용

        @JsonProperty("sprtTrgtMinAge")
        private String sprtTrgtMinAge; // 지원대상최소연령

        @JsonProperty("sprtTrgtMaxAge")
        private String sprtTrgtMaxAge; // 지원대상최대연령

        @JsonProperty("aplyUrlAddr")
        private String aplyUrlAddr; // 신청URL주소

        @JsonProperty("refUrlAddr1")
        private String refUrlAddr1; // 참고URL주소1

        @JsonProperty("refUrlAddr2")
        private String refUrlAddr2; // 참고URL주소2

        @JsonProperty("bizPrdBgngYmd")
        private String bizPrdBgngYmd; // 사업기간시작일자

        @JsonProperty("bizPrdEndYmd")
        private String bizPrdEndYmd; // 사업기간종료일자

        @JsonProperty("aplyYmd")
        private String aplyYmd; // 신청일자

        @JsonProperty("plcyAplyMthdCn")
        private String plcyAplyMthdCn; // 정책신청방법내용

        @JsonProperty("sbmsnDcmntCn")
        private String sbmsnDcmntCn; // 제출서류내용

        @JsonProperty("srngMthdCn")
        private String srngMthdCn; // 선정방법내용

        @JsonProperty("sprvsnInstCdNm")
        private String sprvsnInstCdNm; // 주관기관코드명

        @JsonProperty("operInstCdNm")
        private String operInstCdNm; // 운영기관코드명

        @JsonProperty("rgtrInstCdNm")
        private String rgtrInstCdNm; // 등록기관코드명

        @JsonProperty("addAplyQlfcCndCn")
        private String addAplyQlfcCndCn; // 추가신청자격조건내용

        @JsonProperty("etcMttrCn")
        private String etcMttrCn; // 기타사항내용

        // 추가 필드들 (필요시 사용)
        @JsonProperty("plcyKywdNm")
        private String plcyKywdNm; // 정책키워드명

        @JsonProperty("bizPrdEtcCn")
        private String bizPrdEtcCn; // 사업기간기타내용

        @JsonProperty("ptcpPrpTrgtCn")
        private String ptcpPrpTrgtCn; // 참여예정대상내용

        @JsonProperty("frstRegDt")
        private String frstRegDt; // 최초등록일시

        @JsonProperty("lastMdfcnDt")
        private String lastMdfcnDt; // 최종수정일시
    }
}
