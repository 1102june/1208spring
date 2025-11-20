package com.example.youth.dto.kakao;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class KakaoGeocodingResponse {
    @JsonProperty("meta")
    private Meta meta;

    @JsonProperty("documents")
    private List<Document> documents;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Meta {
        @JsonProperty("total_count")
        private Integer totalCount;

        @JsonProperty("pageable_count")
        private Integer pageableCount;

        @JsonProperty("is_end")
        private Boolean isEnd;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Document {
        @JsonProperty("address")
        private Address address;

        @JsonProperty("road_address")
        private RoadAddress roadAddress;

        @JsonProperty("x")  // 경도 (longitude)
        private String longitude;

        @JsonProperty("y")  // 위도 (latitude)
        private String latitude;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Address {
        @JsonProperty("address_name")
        private String addressName;

        @JsonProperty("y")
        private String latitude;

        @JsonProperty("x")
        private String longitude;

        @JsonProperty("address_type")
        private String addressType;

        @JsonProperty("region_1depth_name")
        private String region1depthName;

        @JsonProperty("region_2depth_name")
        private String region2depthName;

        @JsonProperty("region_3depth_name")
        private String region3depthName;

        @JsonProperty("region_3depth_h_name")
        private String region3depthHName;

        @JsonProperty("h_code")
        private String hCode;

        @JsonProperty("b_code")
        private String bCode;

        @JsonProperty("mountain_yn")
        private String mountainYn;

        @JsonProperty("main_address_no")
        private String mainAddressNo;

        @JsonProperty("sub_address_no")
        private String subAddressNo;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RoadAddress {
        @JsonProperty("address_name")
        private String addressName;

        @JsonProperty("y")
        private String latitude;

        @JsonProperty("x")
        private String longitude;

        @JsonProperty("address_type")
        private String addressType;

        @JsonProperty("road_name")
        private String roadName;

        @JsonProperty("building_name")
        private String buildingName;

        @JsonProperty("main_building_no")
        private String mainBuildingNo;

        @JsonProperty("sub_building_no")
        private String subBuildingNo;

        @JsonProperty("zone_no")
        private String zoneNo;
    }
}

